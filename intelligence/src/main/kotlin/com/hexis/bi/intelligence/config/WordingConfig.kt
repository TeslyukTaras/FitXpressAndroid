package com.hexis.bi.intelligence.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val SUPPORTED_WORDING_SCHEMA_VERSION = 1

@Serializable
data class WordingDocument(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("wording_version") val wordingVersion: String,
    val copy: CopyConfig = CopyConfig(),
)

@Serializable
data class CopyConfig(
    val headings: Map<String, String> = emptyMap(),
    @SerialName("headings_hedged") val headingsHedged: Map<String, String> = emptyMap(),
    val narratives: Map<String, String> = emptyMap(),
    @SerialName("narratives_hedged") val narrativesHedged: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap(),
    val verbs: Map<String, String> = emptyMap(),
    @SerialName("verbs_plural") val verbsPlural: Map<String, String> = emptyMap(),
    val phrases: Map<String, String> = emptyMap(),
    val templates: Map<String, String> = emptyMap(),
    val explanation: String = "",
    @SerialName("explanation_bare") val explanationBare: String = "",
    val periods: Map<String, String> = emptyMap(),
    @SerialName("heading_only") val headingOnly: List<String> = emptyList(),
    val join: Map<String, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = headings.isEmpty()

    fun joiner(key: String, fallback: String): String = join[key] ?: fallback

    fun heading(interpretation: String, confident: Boolean): String? =
        if (confident) headings[interpretation] else headingsHedged[interpretation] ?: headings[interpretation]

    fun narrative(fact: String, confident: Boolean): String? =
        if (confident) narratives[fact] else narrativesHedged[fact] ?: narratives[fact]
}

object WordingConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        allowSpecialFloatingPointValues = false
    }

    fun parse(text: String): Result<WordingDocument> =
        runCatching { json.decodeFromString<WordingDocument>(text) }
            .mapCatching { document ->
                val problems = validate(document)
                if (problems.isEmpty()) document else error(problems.joinToString("; "))
            }

    private fun validate(document: WordingDocument): List<String> = buildList {
        if (document.schemaVersion != SUPPORTED_WORDING_SCHEMA_VERSION) {
            add("schema_version ${document.schemaVersion} is not supported")
        }
        if (document.wordingVersion.isBlank()) add("wording_version is blank")
        val copy = document.copy
        if (copy.headings.isEmpty()) add("copy.headings is empty")
        if (copy.verbs.isEmpty()) add("copy.verbs is empty")
        if (copy.labels.isEmpty()) add("copy.labels is empty")
        if (copy.explanation.isBlank()) add("copy.explanation is blank")
        copy.headings.filterValues { it.isBlank() }.keys
            .forEach { add("copy.headings.$it is blank") }
        copy.verbs.filterValues { it.isBlank() }.keys
            .forEach { add("copy.verbs.$it is blank") }
        copy.verbsPlural.keys.filterNot { it in copy.verbs }
            .forEach { add("copy.verbs_plural.$it has no singular form") }
    }
}
