package com.hexis.bi.ui.avatar

import android.util.LruCache
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

internal object ObjParser {
    private const val NORMALIZED_TARGET_SIZE = 3.18f
    private const val FLOATS_PER_VERTEX = 9
    private const val POSITION_FLOATS_PER_VERTEX = 3
    private const val BYTES_PER_FLOAT = 4
    private const val VERTEX_MERGE_RADIUS = 0.012f
    private const val TRIANGLE_KEY_BITS = 21
    private const val TRIANGLE_KEY_MASK = (1L shl TRIANGLE_KEY_BITS) - 1L
    private const val TRIANGLE_KEY_MAX_INDEX = (1 shl TRIANGLE_KEY_BITS) - 1
    private const val GRID_KEY_BITS = 21
    private const val GRID_KEY_MASK = (1L shl GRID_KEY_BITS) - 1L
    private const val LONG_HASH_SET_MAX_LOAD_PERCENT = 70

    /** Minimum whitespace-separated tokens for a valid `v x y z` vertex line. */
    private const val OBJ_VERTEX_LINE_MIN_TOKENS = 4

    private const val OBJ_VERTEX_COLOR_LINE_MIN_TOKENS = 7

    private const val DEFAULT_VERTEX_GREY = 0.4f

    private const val MESH_CACHE_MAX_ENTRIES = 6

    private val meshCache = LruCache<String, ObjMesh>(MESH_CACHE_MAX_ENTRIES)

    /** Hoisted so the pattern is compiled once, not per OBJ line (parse is called for thousands). */
    private val whitespaceRegex = Regex("\\s+")

    /**
     * Body regions where vertices are not merged aggressively.
     * Head uses the neck clip plane from [MetricAvatarMeasurementGuide.neckClipPlane].
     */
    private const val PROTECTED_REGION_HANDS_ABS_X = 0.36f
    private const val PROTECTED_REGION_HANDS_MIN_Y = -1.05f
    private const val PROTECTED_REGION_HANDS_MAX_Y = 0.05f
    private const val PROTECTED_REGION_FEET_MAX_Y = -1.32f

    private data class Cluster(
        var x: Float,
        var y: Float,
        var z: Float,
        var count: Int,
        var cx: Int,
        var cy: Int,
        var cz: Int
    )

    private data class ClusterResult(val vertices: List<FloatArray>, val remap: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ClusterResult
            return vertices == other.vertices && remap.contentEquals(other.remap)
        }

        override fun hashCode(): Int {
            var result = vertices.hashCode()
            result = 31 * result + remap.contentHashCode()
            return result
        }
    }

    private fun distSq(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return dx * dx + dy * dy + dz * dz
    }

    fun parse(url: String, cacheDir: File, includeVertexColors: Boolean = false): ObjMesh {
        val cacheKey = if (includeVertexColors) "$url#colors" else url
        meshCache.get(cacheKey)?.let { return it.withIndependentBuffers() }

        val vertices = ArrayList<FloatArray>()
        val colors = if (includeVertexColors) ArrayList<FloatArray>() else null
        var hasColors = false
        val faces = ArrayList<IntArray>()

        ObjDiskCache.objFileReader(url, cacheDir).use { reader ->
            reader.lineSequence().forEach { line ->
                when {
                    line.startsWith("v ") -> if (parseVertex(line, vertices, colors)) hasColors =
                        true

                    line.startsWith("f ") -> parseFaceIndices(line, vertices.size)?.let {
                        faces.add(
                            it
                        )
                    }
                }
            }
        }

        if (faces.isEmpty()) throw IllegalArgumentException("OBJ contains no supported faces")

        val usedVertices = BooleanArray(vertices.size)
        faces.forEach { face -> face.forEach { index -> usedVertices[index] = true } }
        normalizeVertices(vertices, usedVertices)
        val modelBounds = boundsOfUsedVertices(vertices, usedVertices)

        val measurementGuide = buildMeasurementGuide(vertices, usedVertices, faces)

        val clusterResult =
            clusterCloseVertices(vertices, usedVertices, measurementGuide.neckClipPlane)
        val clusterColors =
            colors?.takeIf { hasColors }?.let { buildClusterColors(it, clusterResult) }

        val triangleFloatEstimate = estimateTriangleFloatCount(faces)
        val wireFloatEstimate = estimateWireFloatCount(faces)
        val triangles = FloatAccumulator(triangleFloatEstimate)
        val wireLines = FloatAccumulator(wireFloatEstimate)
        val triangleColors = if (clusterColors != null) {
            FloatAccumulator(triangleFloatEstimate / FLOATS_PER_VERTEX * POSITION_FLOATS_PER_VERTEX)
        } else {
            null
        }
        val emittedTriangles = LongHashSet(faces.size)
        val emittedWireEdges = LongHashSet(faces.size * 3)

        faces.forEach { face ->
            appendClusteredFace(
                face,
                clusterResult,
                emittedTriangles,
                triangles,
                emittedWireEdges,
                wireLines,
                clusterColors,
                triangleColors,
            )
        }
        if (triangles.isEmpty()) throw IllegalArgumentException("OBJ contains no supported triangles")

        val buffer = triangles.toDirectFloatBuffer()
        val wireBuffer = wireLines.toDirectFloatBuffer()

        val mesh = ObjMesh(
            vertexBuffer = buffer,
            vertexCount = triangles.size / FLOATS_PER_VERTEX,
            wireVertexBuffer = wireBuffer,
            wireVertexCount = wireLines.size / POSITION_FLOATS_PER_VERTEX,
            bounds = modelBounds,
            measurementGuide = measurementGuide,
            colorBuffer = triangleColors?.toDirectFloatBuffer(),
        )
        meshCache.put(cacheKey, mesh)
        return mesh.withIndependentBuffers()
    }

    // Two previews can render one cached mesh without sharing buffer positions.
    private fun ObjMesh.withIndependentBuffers(): ObjMesh = copy(
        vertexBuffer = vertexBuffer.duplicate(),
        wireVertexBuffer = wireVertexBuffer.duplicate(),
        colorBuffer = colorBuffer?.duplicate(),
    )

    private fun parseVertex(
        line: String,
        vertices: MutableList<FloatArray>,
        colors: MutableList<FloatArray>?,
    ): Boolean {
        val parts = line.trim().split(whitespaceRegex)
        if (parts.size < OBJ_VERTEX_LINE_MIN_TOKENS) return false
        vertices.add(
            floatArrayOf(
                parts[1].toFloatOrNull() ?: 0f,
                parts[2].toFloatOrNull() ?: 0f,
                parts[3].toFloatOrNull() ?: 0f,
            )
        )
        if (colors == null) return false
        return if (parts.size >= OBJ_VERTEX_COLOR_LINE_MIN_TOKENS) {
            colors.add(
                floatArrayOf(
                    parts[4].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                    parts[5].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                    parts[6].toFloatOrNull() ?: DEFAULT_VERTEX_GREY,
                )
            )
            true
        } else {
            colors.add(floatArrayOf(DEFAULT_VERTEX_GREY, DEFAULT_VERTEX_GREY, DEFAULT_VERTEX_GREY))
            false
        }
    }

    private fun buildClusterColors(
        colors: List<FloatArray>,
        clusterResult: ClusterResult,
    ): FloatArray {
        val out = FloatArray(clusterResult.vertices.size * POSITION_FLOATS_PER_VERTEX)
        val assigned = BooleanArray(clusterResult.vertices.size)
        for (originalIndex in colors.indices) {
            val clusterIndex = clusterResult.remap.getOrElse(originalIndex) { -1 }
            if (clusterIndex in clusterResult.vertices.indices && !assigned[clusterIndex]) {
                val color = colors[originalIndex]
                val base = clusterIndex * POSITION_FLOATS_PER_VERTEX
                out[base] = color[0]
                out[base + 1] = color[1]
                out[base + 2] = color[2]
                assigned[clusterIndex] = true
            }
        }
        return out
    }

    private fun parseFaceIndices(line: String, vertexCount: Int): IntArray? {
        val faceParts = line.trim().split(whitespaceRegex)
        val count = faceParts.size - 1 // first token is the "f" tag
        if (count < 3) return null

        val indices = IntArray(count)
        for (i in 0 until count) {
            val token = faceParts[i + 1]
            val rawIndex = token.substringBefore("/").toIntOrNull() ?: return null
            val index = if (rawIndex < 0) vertexCount + rawIndex else rawIndex - 1
            if (index !in 0 until vertexCount) return null
            indices[i] = index
        }

        return indices
    }

    private fun appendClusteredFace(
        face: IntArray,
        clusterResult: ClusterResult,
        emittedTriangles: LongHashSet,
        triangles: FloatAccumulator,
        emittedWireEdges: LongHashSet,
        wireLines: FloatAccumulator,
        clusterColors: FloatArray?,
        triangleColors: FloatAccumulator?,
    ) {
        val remap = clusterResult.remap
        val clusteredVertices = clusterResult.vertices

        for (i in 1 until face.size - 1) {
            val c0 = remap[face[0]]
            val c1 = remap[face[i]]
            val c2 = remap[face[i + 1]]

            if (c0 < 0 || c1 < 0 || c2 < 0) continue
            if (c0 == c1 || c1 == c2 || c2 == c0) continue

            val key = triangleKey(c0, c1, c2)
            if (!emittedTriangles.add(key)) continue

            val v0 = clusteredVertices[c0]
            val v1 = clusteredVertices[c1]
            val v2 = clusteredVertices[c2]

            var mx = 1f
            var my = 1f
            var mz = 1f
            val d01 = distSq(v0, v1)
            val d12 = distSq(v1, v2)
            val d20 = distSq(v2, v0)
            if (d12 >= d20 && d12 >= d01) mx = 0f else if (d20 >= d01) my = 0f else mz = 0f

            appendVertex(triangles, v0, 1f, 0f, 0f, mx, my, mz)
            appendVertex(triangles, v1, 0f, 1f, 0f, mx, my, mz)
            appendVertex(triangles, v2, 0f, 0f, 1f, mx, my, mz)

            if (triangleColors != null && clusterColors != null) {
                appendVertexColor(triangleColors, clusterColors, c0)
                appendVertexColor(triangleColors, clusterColors, c1)
                appendVertexColor(triangleColors, clusterColors, c2)
            }

            if (mz > 0f) appendWireEdge(wireLines, emittedWireEdges, c0, c1, v0, v1)
            if (mx > 0f) appendWireEdge(wireLines, emittedWireEdges, c1, c2, v1, v2)
            if (my > 0f) appendWireEdge(wireLines, emittedWireEdges, c2, c0, v2, v0)
        }
    }

    private fun appendWireEdge(
        into: FloatAccumulator,
        emittedEdges: LongHashSet,
        aIndex: Int,
        bIndex: Int,
        a: FloatArray,
        b: FloatArray,
    ) {
        if (!emittedEdges.add(edgeKey(aIndex, bIndex))) return
        appendPosition(into, a)
        appendPosition(into, b)
    }

    private fun clusterCloseVertices(
        vertices: List<FloatArray>,
        usedVertices: BooleanArray,
        neckClipPlane: FloatArray,
    ): ClusterResult {
        val clusters = ArrayList<Cluster>()
        val remap = IntArray(vertices.size) { -1 }
        val grid = HashMap<Long, MutableList<Int>>()
        val radiusSq = VERTEX_MERGE_RADIUS * VERTEX_MERGE_RADIUS

        vertices.forEachIndexed { vertexIndex, vertex ->
            if (!usedVertices[vertexIndex]) return@forEachIndexed

            if (isProtectedDetailVertex(vertex, neckClipPlane)) {
                val clusterIndex = clusters.size
                clusters.add(Cluster(vertex[0], vertex[1], vertex[2], 1, 0, 0, 0))
                remap[vertexIndex] = clusterIndex
                return@forEachIndexed
            }

            val cellX = cellOf(vertex[0])
            val cellY = cellOf(vertex[1])
            val cellZ = cellOf(vertex[2])
            var bestCluster = -1
            var bestDistSq = radiusSq

            for (x in cellX - 1..cellX + 1) {
                for (y in cellY - 1..cellY + 1) {
                    for (z in cellZ - 1..cellZ + 1) {
                        grid[gridKey(x, y, z)]?.forEach { clusterIndex ->
                            val cluster = clusters[clusterIndex]
                            val dx = vertex[0] - cluster.x
                            val dy = vertex[1] - cluster.y
                            val dz = vertex[2] - cluster.z
                            val d = dx * dx + dy * dy + dz * dz
                            if (d <= bestDistSq) {
                                bestDistSq = d
                                bestCluster = clusterIndex
                            }
                        }
                    }
                }
            }

            if (bestCluster >= 0) {
                addVertexToCluster(bestCluster, vertex, clusters, grid)
                remap[vertexIndex] = bestCluster
            } else {
                val clusterIndex = clusters.size
                clusters.add(Cluster(vertex[0], vertex[1], vertex[2], 1, cellX, cellY, cellZ))
                grid.getOrPut(gridKey(cellX, cellY, cellZ)) { ArrayList() }.add(clusterIndex)
                remap[vertexIndex] = clusterIndex
            }
        }

        val mergedVertices =
            clusters.map { cluster -> floatArrayOf(cluster.x, cluster.y, cluster.z) }
        return ClusterResult(vertices = mergedVertices, remap = remap)
    }

    private fun isProtectedDetailVertex(vertex: FloatArray, neckClipPlane: FloatArray): Boolean {
        val x = vertex[0]
        val y = vertex[1]
        val towardHead = vertex[0] * neckClipPlane[0] + vertex[1] * neckClipPlane[1] +
                vertex[2] * neckClipPlane[2] + neckClipPlane[3] > 1e-5f
        val hands = abs(x) >= PROTECTED_REGION_HANDS_ABS_X &&
                y >= PROTECTED_REGION_HANDS_MIN_Y && y <= PROTECTED_REGION_HANDS_MAX_Y
        val feet = y <= PROTECTED_REGION_FEET_MAX_Y
        return towardHead || hands || feet
    }

    private fun addVertexToCluster(
        clusterIndex: Int,
        vertex: FloatArray,
        clusters: MutableList<Cluster>,
        grid: MutableMap<Long, MutableList<Int>>,
    ) {
        val cluster = clusters[clusterIndex]
        val oldKey = gridKey(cluster.cx, cluster.cy, cluster.cz)
        val newCount = cluster.count + 1

        cluster.x = (cluster.x * cluster.count + vertex[0]) / newCount
        cluster.y = (cluster.y * cluster.count + vertex[1]) / newCount
        cluster.z = (cluster.z * cluster.count + vertex[2]) / newCount
        cluster.count = newCount

        val newCellX = cellOf(cluster.x)
        val newCellY = cellOf(cluster.y)
        val newCellZ = cellOf(cluster.z)
        val newKey = gridKey(newCellX, newCellY, newCellZ)

        if (newKey != oldKey) {
            grid[oldKey]?.remove(clusterIndex)
            cluster.cx = newCellX
            cluster.cy = newCellY
            cluster.cz = newCellZ
            grid.getOrPut(newKey) { ArrayList() }.add(clusterIndex)
        }
    }

    private fun appendVertex(
        into: FloatAccumulator,
        v: FloatArray,
        b1: Float,
        b2: Float,
        b3: Float,
        m1: Float,
        m2: Float,
        m3: Float,
    ) {
        into.add(v[0]); into.add(v[1]); into.add(v[2])
        into.add(b1); into.add(b2); into.add(b3)
        into.add(m1); into.add(m2); into.add(m3)
    }

    private fun appendPosition(into: FloatAccumulator, v: FloatArray) {
        into.add(v[0]); into.add(v[1]); into.add(v[2])
    }

    private fun appendVertexColor(
        into: FloatAccumulator,
        clusterColors: FloatArray,
        clusterIndex: Int
    ) {
        val base = clusterIndex * POSITION_FLOATS_PER_VERTEX
        into.add(clusterColors[base]); into.add(clusterColors[base + 1]); into.add(clusterColors[base + 2])
    }

    private fun estimateTriangleFloatCount(faces: List<IntArray>): Int {
        var count = 0
        faces.forEach { face ->
            count += (face.size - 2).coerceAtLeast(0) * FLOATS_PER_VERTEX * 3
        }
        return count
    }

    private fun estimateWireFloatCount(faces: List<IntArray>): Int {
        var count = 0
        faces.forEach { face ->
            count += (face.size - 2).coerceAtLeast(0) * POSITION_FLOATS_PER_VERTEX * 2 * 3
        }
        return count
    }

    private fun normalizeVertices(vertices: MutableList<FloatArray>, usedVertices: BooleanArray) {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        vertices.forEachIndexed { index, vertex ->
            if (!usedVertices[index]) return@forEachIndexed
            minX = minOf(minX, vertex[0]); maxX = maxOf(maxX, vertex[0])
            minY = minOf(minY, vertex[1]); maxY = maxOf(maxY, vertex[1])
            minZ = minOf(minZ, vertex[2]); maxZ = maxOf(maxZ, vertex[2])
        }

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val scale = NORMALIZED_TARGET_SIZE / maxOf(maxX - minX, maxY - minY, maxZ - minZ, 1e-6f)

        vertices.forEach { vertex ->
            vertex[0] = (vertex[0] - cx) * scale
            vertex[1] = (vertex[1] - cy) * scale
            vertex[2] = (vertex[2] - cz) * scale
        }
    }

    private fun boundsOfUsedVertices(
        vertices: List<FloatArray>,
        usedVertices: BooleanArray,
    ): ModelBounds {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        vertices.forEachIndexed { index, vertex ->
            if (!usedVertices[index]) return@forEachIndexed
            minX = minOf(minX, vertex[0]); maxX = maxOf(maxX, vertex[0])
            minY = minOf(minY, vertex[1]); maxY = maxOf(maxY, vertex[1])
            minZ = minOf(minZ, vertex[2]); maxZ = maxOf(maxZ, vertex[2])
        }

        return ModelBounds(minX, maxX, minY, maxY, minZ, maxZ)
    }

    private fun cellOf(value: Float): Int =
        kotlin.math.floor((value / VERTEX_MERGE_RADIUS).toDouble()).toInt()

    private fun gridKey(x: Int, y: Int, z: Int): Long {
        val packedX = packGridCell(x)
        val packedY = packGridCell(y)
        val packedZ = packGridCell(z)
        return (packedX shl (GRID_KEY_BITS * 2)) or
                (packedY shl GRID_KEY_BITS) or
                packedZ
    }

    private fun packGridCell(value: Int): Long {
        val longValue = value.toLong()
        val packed = (longValue shl 1) xor (longValue shr (Long.SIZE_BITS - 1))
        check(packed in 0..GRID_KEY_MASK) { "OBJ grid cell is outside packable range" }
        return packed
    }

    private fun triangleKey(a: Int, b: Int, c: Int): Long {
        check(a <= TRIANGLE_KEY_MAX_INDEX && b <= TRIANGLE_KEY_MAX_INDEX && c <= TRIANGLE_KEY_MAX_INDEX) {
            "OBJ has too many clustered vertices to pack triangle keys"
        }
        val x = minOf(a, b, c)
        val z = maxOf(a, b, c)
        val y = a + b + c - x - z
        return (x.toLong() shl (TRIANGLE_KEY_BITS * 2)) or
                (y.toLong() shl TRIANGLE_KEY_BITS) or
                (z.toLong() and TRIANGLE_KEY_MASK)
    }

    private fun edgeKey(a: Int, b: Int): Long {
        val x = minOf(a, b)
        val y = maxOf(a, b)
        return (x.toLong() shl Int.SIZE_BITS) or (y.toLong() and 0xFFFF_FFFFL)
    }

    private class FloatAccumulator(initialCapacity: Int = 0) {
        private var values = FloatArray(initialCapacity.coerceAtLeast(16))
        var size: Int = 0
            private set

        fun isEmpty(): Boolean = size == 0

        fun add(value: Float) {
            ensureCapacity(size + 1)
            values[size] = value
            size += 1
        }

        fun toDirectFloatBuffer(): FloatBuffer =
            ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values, 0, size)
                    position(0)
                }

        private fun ensureCapacity(required: Int) {
            if (required <= values.size) return
            var newCapacity = values.size
            while (newCapacity < required) {
                newCapacity *= 2
            }
            values = values.copyOf(newCapacity)
        }
    }

    private class LongHashSet(expectedSize: Int = 0) {
        private var keys = LongArray(capacityFor(expectedSize)) { EMPTY }
        private var resizeThreshold = thresholdFor(keys.size)
        private var size = 0

        fun add(key: Long): Boolean {
            check(key != EMPTY) { "LongHashSet key collides with empty sentinel" }
            if (size + 1 > resizeThreshold) resize()
            return insert(key)
        }

        private fun insert(key: Long): Boolean {
            var index = smear(key) and (keys.size - 1)
            while (true) {
                val current = keys[index]
                when (current) {
                    EMPTY -> {
                        keys[index] = key
                        size += 1
                        return true
                    }

                    key -> return false
                    else -> index = (index + 1) and (keys.size - 1)
                }
            }
        }

        private fun resize() {
            val oldKeys = keys
            keys = LongArray(oldKeys.size * 2) { EMPTY }
            resizeThreshold = thresholdFor(keys.size)
            size = 0
            oldKeys.forEach { key ->
                if (key != EMPTY) insert(key)
            }
        }

        private companion object {
            private const val EMPTY = Long.MIN_VALUE

            private fun capacityFor(expectedSize: Int): Int {
                val needed =
                    ((expectedSize.coerceAtLeast(1) * 100) / LONG_HASH_SET_MAX_LOAD_PERCENT) + 1
                var capacity = 16
                while (capacity < needed) {
                    capacity *= 2
                }
                return capacity
            }

            private fun thresholdFor(capacity: Int): Int =
                (capacity * LONG_HASH_SET_MAX_LOAD_PERCENT) / 100

            private fun smear(value: Long): Int {
                var x = value
                x = x xor (x ushr 33)
                x *= -49064778989728563L
                x = x xor (x ushr 33)
                x *= -4265267296055464877L
                x = x xor (x ushr 33)
                return x.toInt()
            }
        }
    }
}
