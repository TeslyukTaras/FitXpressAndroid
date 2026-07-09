package com.hexis.bi.ui.avatar

/**
 * GLSL shader sources for [MetricAvatarRenderer]: the body mesh (vertex-colour wireframe + skin
 * masking), the radial preview gradient, measurement leader lines, and body rings.
 */

/** Radial gradient matching [@color/metric_avatar_preview_gradient_inner] / _outer (Compose + GL). */
internal const val GRADIENT_VERTEX_SHADER = """
    attribute vec2 aClip;
    void main() {
        gl_Position = vec4(aClip, 0.0, 1.0);
    }
"""

internal const val GRADIENT_FRAGMENT_SHADER = """
    precision mediump float;
    uniform vec2 uResolution;
    uniform vec3 uInner;
    uniform vec3 uOuter;
    void main() {
        vec2 uv = vec2(
            gl_FragCoord.x / uResolution.x,
            1.0 - gl_FragCoord.y / uResolution.y
        );
        vec2 c = vec2(0.5, 0.5);
        float d = distance(uv, c) * 1.41421356;
        float t = clamp(d, 0.0, 1.0);
        vec3 col = mix(uInner, uOuter, t);
        gl_FragColor = vec4(col, 1.0);
    }
"""

internal const val LEADER_VERTEX_SHADER = """
    uniform mat4 uMvp;
    attribute vec3 aPos;
    void main() {
        gl_Position = uMvp * vec4(aPos, 1.0);
    }
"""

internal const val LEADER_FRAGMENT_SHADER = """
    precision mediump float;
    uniform vec4 uColor;
    void main() {
        gl_FragColor = uColor;
    }
"""

internal const val RING_VERTEX_SHADER = """
    uniform mat4 uMvp;
    attribute vec3 aPos;
    attribute float aAlpha;
    varying float vAlpha;
    void main() {
        vAlpha = aAlpha;
        gl_Position = uMvp * vec4(aPos, 1.0);
    }
"""

internal const val RING_FRAGMENT_SHADER = """
    precision mediump float;
    uniform vec4 uColor;
    varying float vAlpha;
    void main() {
        gl_FragColor = vec4(uColor.rgb, uColor.a * vAlpha);
    }
"""

internal const val VERTEX_SHADER = """
    attribute vec3 aPosition; attribute vec3 aBary; attribute vec3 aEdgeMask;
    attribute vec3 aColor;
    uniform mat4 uMvp; varying float vModelY; varying vec3 vModelPos;
    varying vec3 vBary; varying vec3 vEdgeMask; varying vec3 vColor;
    void main() {
        vModelY = aPosition.y; vModelPos = aPosition; vBary = aBary; vEdgeMask = aEdgeMask;
        vColor = aColor;
        gl_Position = uMvp * vec4(aPosition, 1.0);
    }
"""

internal val FRAGMENT_SHADER = """
    #extension GL_OES_standard_derivatives : enable
    #ifdef GL_FRAGMENT_PRECISION_HIGH
    precision highp float;
    #else
    precision mediump float;
    #endif
    varying float vModelY; varying vec3 vModelPos; varying vec3 vBary; varying vec3 vEdgeMask;
    varying vec3 vColor;
    uniform vec4 uSkinColor; uniform vec4 uSuitColor; uniform vec4 uMeshColor;
    uniform float uShowSkin; uniform mat4 uModelView; uniform float uUseVertexColor;
    uniform float uMeshGlow;

    const float WIRE_WIDTH = ${WIRE_WIDTH_VIEW_PX * RENDER_SUPERSAMPLE};
    const float WIRE_GLOW_RIM_POWER = 3.5;
    const float WIRE_INTENSITY = 0.46;
    // Wire takes the same key light as the fill; the ambient floor keeps back-facing contours visible.
    const float WIRE_AMBIENT = 0.52;
    const float WIRE_DIFFUSE = 0.66;
    const float WIRE_SPECULAR = 1.20;
    const float WIRE_SPECULAR_POWER = 64.0;
    const float ANALYSIS_WIRE_INTENSITY_BOOST = 0.08;
    const float STITCH_START = 0.91;
    const float STITCH_END = 0.985;
    const float STITCH_INTENSITY = 0.28;
    const float ANALYSIS_ACCENT_STRENGTH = 0.92;
    const float ANALYSIS_LUMA_RECOVERY = 0.65;
    const float ANALYSIS_TEAL_HIGHLIGHT = 0.06;
    const float ANALYSIS_COLOR_CONTRAST = 1.16;

    float skinMaskForModelPosition(vec3 modelPos) {
        float hands = step(0.36, abs(modelPos.x)) * step(-1.05, modelPos.y) * step(modelPos.y, 0.05);
        float feet = step(modelPos.y, -1.32);
        return min(1.0, hands + feet) * uShowSkin;
    }

    vec3 viewNormalFromModelDerivatives(vec3 modelPos) {
        vec3 fdx = dFdx(modelPos);
        vec3 fdy = dFdy(modelPos);
        return normalize((uModelView * vec4(normalize(cross(fdx, fdy)), 0.0)).xyz);
    }

    float lightingFalloff(vec3 viewNormal, vec3 lightDir) {
        float brightness = smoothstep(-0.15, 0.55, dot(viewNormal, lightDir));
        return pow(brightness, 1.8);
    }

    vec3 bodyBaseColor(float falloff) {
        return mix(vec3(0.0, 0.004, 0.008), uSuitColor.rgb + uMeshColor.rgb * 0.10, falloff);
    }

    float meshEdgeCoverage(vec3 bary, vec3 edgeMask) {
        vec3 baryWidth = fwidth(bary);
        vec3 baryAA = smoothstep(vec3(0.0), baryWidth * WIRE_WIDTH, bary);
        vec3 edgeVec = mix(vec3(1.0), baryAA, edgeMask);
        return 1.0 - min(min(edgeVec.x, edgeVec.y), edgeVec.z);
    }

    vec3 shadedWireColor(vec3 viewNormal, vec3 lightDir, float falloff) {
        vec3 halfDir = normalize(lightDir + vec3(0.0, 0.0, 1.0));
        float spec = pow(max(dot(viewNormal, halfDir), 0.0), WIRE_SPECULAR_POWER);
        float level = WIRE_AMBIENT + WIRE_DIFFUSE * falloff + spec * WIRE_SPECULAR;
        return min(vec3(1.0), uMeshColor.rgb * level);
    }

    vec3 colorAnalysisWire(vec3 tealWire, vec3 analysisColor) {
        vec3 contrastedColor = clamp(
            (analysisColor - vec3(0.5)) * ANALYSIS_COLOR_CONTRAST + vec3(0.5),
            vec3(0.0),
            vec3(1.0)
        );
        vec3 accentedWire = mix(tealWire, contrastedColor, ANALYSIS_ACCENT_STRENGTH);
        float tealLuma = dot(tealWire, vec3(0.2126, 0.7152, 0.0722));
        float accentLuma = dot(accentedWire, vec3(0.2126, 0.7152, 0.0722));
        float lift = max(tealLuma - accentLuma, 0.0) * ANALYSIS_LUMA_RECOVERY;
        vec3 brightenedAccent = min(
            vec3(1.0),
            accentedWire + vec3(lift) + tealWire * ANALYSIS_TEAL_HIGHLIGHT
        );
        return mix(tealWire, brightenedAccent, uUseVertexColor);
    }

    vec3 applyMeshWire(vec3 baseColor, vec3 wireColor, float edgeCoverage, float analysisEnabled) {
        float intensity = WIRE_INTENSITY + ANALYSIS_WIRE_INTENSITY_BOOST * analysisEnabled;
        return mix(baseColor, wireColor, edgeCoverage * intensity);
    }

    vec3 applyJunctionStitch(vec3 baseColor, vec3 wireColor, vec3 bary) {
        // Junction stitch: not a highlight/glow. It uses the same shaded wire color to
        // cover tiny barycentric gaps where mesh edges meet at triangle vertices.
        float maxBaryForStitch = max(max(bary.x, bary.y), bary.z);
        float vertexStitch = smoothstep(STITCH_START, STITCH_END, maxBaryForStitch);
        return mix(baseColor, wireColor, vertexStitch * STITCH_INTENSITY);
    }

    vec3 applyPointHighlightNotUsedForNow(vec3 baseColor, vec3 wireColor, vec3 bary) {
        // Not used for now: design uses a flat, dim wireframe without cyan point glow.
        float maxBary = max(max(bary.x, bary.y), bary.z);
        float pointMask = smoothstep(0.88, 0.95, maxBary);
        vec3 pointColor = min(vec3(1.0), wireColor * 1.18);
        return mix(baseColor, pointColor, pointMask * 0.02);
    }

    vec3 applyRimGlowNotUsedForNow(vec3 baseColor, vec3 viewNormal) {
        // Not used for now: keep this function so we can restore/tune rim if design changes.
        float rim = pow(1.0 - max(viewNormal.z, 0.0), 8.0);
        return baseColor + uMeshColor.rgb * rim * 0.12;
    }

    void main() {
        float skinMask = skinMaskForModelPosition(vModelPos);
        vec3 nView = viewNormalFromModelDerivatives(vModelPos);
        vec3 lightDir = normalize(vec3(0.0, 0.6, 0.8));
        float falloff = lightingFalloff(nView, lightDir);

        vec3 body = bodyBaseColor(falloff);
        float edge = meshEdgeCoverage(vBary, vEdgeMask);
        vec3 wireCol = shadedWireColor(nView, lightDir, falloff);
        wireCol = colorAnalysisWire(wireCol, vColor);
        vec3 finalBodyCol = applyMeshWire(body, wireCol, edge, uUseVertexColor);
        finalBodyCol = applyJunctionStitch(finalBodyCol, wireCol, vBary);

        float rim = pow(1.0 - max(nView.z, 0.0), WIRE_GLOW_RIM_POWER);
        finalBodyCol += uMeshColor.rgb * rim * clamp(uMeshGlow, 0.0, 1.0);

        // Not used for now:
        // finalBodyCol = applyPointHighlightNotUsedForNow(finalBodyCol, wireCol, vBary);
        // finalBodyCol = applyRimGlowNotUsedForNow(finalBodyCol, nView);

        gl_FragColor = vec4(mix(finalBodyCol, uSkinColor.rgb, skinMask), 1.0);
    }
"""
