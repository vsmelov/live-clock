package com.vsmelov.liveclock.domain

/**
 * Matching for the action search box.
 *
 * A pure function taking already-resolved text rather than reading resources
 * itself, so it stays testable on a bare JVM and so the caller decides which
 * locale the label came from.
 */
object ActionSearch {

    /**
     * Whether an action matches [query]. Case-insensitive.
     *
     * [keywords] carries both languages at once, which is why a Russian query
     * finds an action with an English interface and the other way round.
     */
    fun matches(query: String, id: String, label: String, keywords: String): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return true
        return id.lowercase().contains(needle) ||
            label.lowercase().contains(needle) ||
            keywords.lowercase().contains(needle)
    }
}
