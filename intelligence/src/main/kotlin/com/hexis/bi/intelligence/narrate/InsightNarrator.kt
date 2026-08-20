package com.hexis.bi.intelligence.narrate

import com.hexis.bi.intelligence.config.CopyConfig
import com.hexis.bi.intelligence.model.Finding

data class NarratedInsight(
    val heading: String?,
    val explanation: String,
)

private const val CORROBORATED_BY = "corroborated_by_"
private const val DRIVEN_BY = "driven_by_"
private const val TEMPLATE_CORROBORATED = "corroborated_by"
private const val TEMPLATE_DRIVEN = "driven_by"
private const val LABEL_PLACEHOLDER = "{label}"
private const val PERIOD_PLACEHOLDER = "{period}"
private const val FACTS_PLACEHOLDER = "{facts}"
private const val JOIN_SAME_VERB = "same_verb"
private const val JOIN_BETWEEN_GROUPS = "between_groups"
private const val JOIN_CORROBORATION = "corroboration"
private const val JOIN_LIST = "list"
private const val DEFAULT_LIST = ", "
private const val DEFAULT_SAME_VERB = " and "
private const val DEFAULT_BETWEEN_GROUPS = " while "
private const val DEFAULT_CORROBORATION = ", and "

private data class Composed(val suffix: String, val label: String)

object InsightNarrator {

    fun standalone(finding: Finding, copy: CopyConfig, confident: Boolean): String? {
        copy.heading(finding.interpretation, confident)?.let { return it }
        val fact = finding.facts.firstOrNull() ?: return null
        return copy.narrative(fact, confident)
    }

    fun narrate(finding: Finding, copy: CopyConfig): NarratedInsight? {
        val heading = copy.headings[finding.interpretation]
        val explanation = if (finding.interpretation in copy.headingOnly) {
            ""
        } else {
            sentence(finding.facts, finding.period, copy, bare = heading == null)
        }
        if (heading == null && explanation.isBlank()) return null
        return NarratedInsight(heading, explanation)
    }

    fun sentence(facts: List<String>, period: String, copy: CopyConfig, bare: Boolean): String {

        val composed = mutableListOf<Composed>()
        val standalone = mutableListOf<String>()
        val corroboration = mutableListOf<String>()
        val trailing = mutableListOf<String>()

        for (fact in facts) {
            when (val rendered = render(fact, copy)) {
                is Rendered.Grouped -> composed += rendered.value
                is Rendered.Standalone -> standalone += rendered.value
                is Rendered.Corroboration -> corroboration += rendered.value
                is Rendered.Trailing -> trailing += rendered.value
                Rendered.Unknown -> Unit
            }
        }

        val groups = composed.groupBy { it.suffix }.map { (suffix, items) ->
            val labels = items.map { it.label }.distinct()
            val verb = if (labels.size > 1) {
                copy.verbsPlural[suffix] ?: copy.verbs.getValue(suffix)
            } else {
                copy.verbs.getValue(suffix)
            }
            nouns(labels, copy) + " " + verb
        }
        var body = clauses(groups + standalone, copy)
        if (trailing.isNotEmpty()) {
            body += copy.joiner(JOIN_LIST, DEFAULT_LIST) +
                trailing.joinToString(copy.joiner(JOIN_LIST, DEFAULT_LIST))
        }
        if (corroboration.isNotEmpty()) {
            body += copy.joiner(JOIN_CORROBORATION, DEFAULT_CORROBORATION) +
                corroboration.joinToString(copy.joiner(JOIN_SAME_VERB, DEFAULT_SAME_VERB))
        }
        if (body.isBlank()) return ""
        val template = if (bare && copy.explanationBare.isNotBlank()) {
            copy.explanationBare
        } else {
            copy.explanation
        }
        if (template.isBlank()) return ""
        val label = copy.periods[period] ?: period
        return template
            .replace(PERIOD_PLACEHOLDER, label)
            .replace(FACTS_PLACEHOLDER, if (bare) body.replaceFirstChar { it.uppercase() } else body)
    }

    private fun nouns(parts: List<String>, copy: CopyConfig): String = when (parts.size) {
        0, 1 -> parts.firstOrNull().orEmpty()
        2 -> parts.joinToString(copy.joiner(JOIN_SAME_VERB, DEFAULT_SAME_VERB))
        else -> parts.dropLast(1).joinToString(copy.joiner(JOIN_LIST, DEFAULT_LIST)) +
            copy.joiner(JOIN_LIST, DEFAULT_LIST).trimEnd() +
            copy.joiner(JOIN_SAME_VERB, DEFAULT_SAME_VERB) + parts.last()
    }

    private fun clauses(parts: List<String>, copy: CopyConfig): String = when (parts.size) {
        0 -> ""
        1 -> parts.first()
        2 -> parts.joinToString(copy.joiner(JOIN_BETWEEN_GROUPS, DEFAULT_BETWEEN_GROUPS))
        else -> parts.dropLast(1).joinToString(copy.joiner(JOIN_LIST, DEFAULT_LIST)) +
            copy.joiner(JOIN_CORROBORATION, DEFAULT_CORROBORATION) + parts.last()
    }

    private sealed interface Rendered {
        data class Grouped(val value: Composed) : Rendered
        data class Standalone(val value: String) : Rendered
        data class Corroboration(val value: String) : Rendered
        data class Trailing(val value: String) : Rendered
        data object Unknown : Rendered
    }

    private fun render(fact: String, copy: CopyConfig): Rendered {
        copy.phrases[fact]?.let { return Rendered.Standalone(it) }

        if (fact.startsWith(CORROBORATED_BY)) {
            val subject = fact.removePrefix(CORROBORATED_BY)
            val template = copy.templates[TEMPLATE_CORROBORATED] ?: return Rendered.Unknown
            return Rendered.Corroboration(template.replace(LABEL_PLACEHOLDER, label(subject, copy)))
        }
        if (fact.startsWith(DRIVEN_BY)) {
            val subject = fact.removePrefix(DRIVEN_BY)
            val template = copy.templates[TEMPLATE_DRIVEN] ?: return Rendered.Unknown
            return Rendered.Trailing(template.replace(LABEL_PLACEHOLDER, label(subject, copy)))
        }

        val suffix = copy.verbs.keys
            .filter { fact.length > it.length + 1 && fact.endsWith("_$it") }
            .maxByOrNull { it.length }
            ?: return Rendered.Unknown
        val subject = fact.dropLast(suffix.length + 1)
        val noun = copy.labels[subject] ?: return Rendered.Unknown
        return Rendered.Grouped(Composed(suffix = suffix, label = noun))
    }

    private fun label(subject: String, copy: CopyConfig): String = copy.labels[subject] ?: subject
}
