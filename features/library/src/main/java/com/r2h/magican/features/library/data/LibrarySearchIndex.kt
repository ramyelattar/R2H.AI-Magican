package com.r2h.magican.features.library.data

import com.r2h.magican.features.library.domain.LibraryDocument
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full-text search index for library documents.
 * Maintains inverted index: term → set of document IDs containing that term.
 * 
 * Performance: O(terms in query) instead of O(documents × document text length)
 */
@Singleton
class LibrarySearchIndex @Inject constructor() {
    private val minTokenLength = 2

    private val stopWords = setOf(
        "the", "and", "for", "are", "but", "not", "you", "with", "was", "this", "that",
        "from", "they", "have", "been", "has", "had", "will", "would", "can", "could"
    )

    // Inverted index: term → Set<docId>
    @Volatile
    private var index: Map<String, Set<String>> = emptyMap()

    // Document metadata cache for ranking
    @Volatile
    private var docCache: Map<String, DocumentCacheEntry> = emptyMap()

    /**
     * Rebuilds the full-text index from document list.
     * Should be called after document imports/deletions.
     */
    fun rebuild(documents: List<LibraryDocument>) {
        val newIndex = mutableMapOf<String, MutableSet<String>>()
        val newCache = mutableMapOf<String, DocumentCacheEntry>()

        for (doc in documents) {
            val tokens = tokenizeDocument(doc)
            
            // Add to inverted index
            for (token in tokens) {
                newIndex.getOrPut(token) { mutableSetOf() }.add(doc.id)
            }

            // Cache metadata for ranking
            newCache[doc.id] = DocumentCacheEntry(
                displayName = doc.displayName,
                title = doc.metadata.title,
                importedAtEpochMs = doc.metadata.importedAtEpochMs,
                tokenCount = tokens.size
            )
        }

        index = newIndex.mapValues { (_, ids) -> ids.toSet() }
        docCache = newCache.toMap()
    }

    /**
     * Searches documents matching the query.
     * Returns document IDs ranked by relevance.
     */
    fun search(query: String, maxResults: Int = 100): List<String> {
        val queryTokens = tokenizeQuery(query)
        if (queryTokens.isEmpty()) return emptyList()

        // Find documents containing ALL query terms (AND semantics)
        val candidateSets = queryTokens.mapNotNull { token ->
            index[token]
        }

        if (candidateSets.isEmpty()) return emptyList()

        // Intersection: documents containing all terms
        val matchingDocs = candidateSets.reduce { acc, set -> acc.intersect(set) }

        // Rank by relevance
        return matchingDocs
            .map { docId -> docId to scoreDocument(docId, queryTokens) }
            .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first })
            .take(maxResults)
            .map { it.first }
    }

    /**
     * Tokenizes a document into searchable terms.
     */
    private fun tokenizeDocument(doc: LibraryDocument): Set<String> {
        val tokens = mutableSetOf<String>()

        // Display name (high weight, keep exact)
        doc.displayName.splitToTokens().forEach { tokens += it }

        // Metadata fields
        doc.metadata.title?.splitToTokens()?.forEach { tokens += it }
        doc.metadata.author?.splitToTokens()?.forEach { tokens += it }
        doc.metadata.subject?.splitToTokens()?.forEach { tokens += it }
        doc.metadata.keywords?.splitToTokens()?.forEach { tokens += it }

        // Summary (if exists)
        doc.summary?.splitToTokens()?.forEach { tokens += it }

        // Bookmarks
        doc.bookmarks.forEach { bookmark ->
            bookmark.label.splitToTokens().forEach { tokens += it }
        }

        // Extracted text (limited to avoid huge indexes)
        // Take first 5000 words to balance index size vs. coverage
        val textTokens = doc.extractedText
            .splitToTokens()
            .take(5000)
        tokens.addAll(textTokens)

        return tokens
    }

    /**
     * Tokenizes a search query.
     */
    private fun tokenizeQuery(query: String): List<String> {
        return query
            .splitToTokens()
            .distinct()
    }

    /**
     * Splits text into normalized tokens.
     */
    private fun String.splitToTokens(): List<String> {
        return this.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= minTokenLength }
            .filter { it !in stopWords }
    }

    /**
     * Scores a document's relevance to query tokens.
     * Higher score = more relevant.
     */
    private fun scoreDocument(docId: String, queryTokens: List<String>): Double {
        val cached = docCache[docId] ?: return 0.0

        var score = 0.0

        // Boost if query term appears in title or display name
        for (token in queryTokens) {
            if (cached.displayName.lowercase().contains(token)) {
                score += 10.0
            }
            if (cached.title?.lowercase()?.contains(token) == true) {
                score += 5.0
            }
        }

        // Boost recent documents slightly
        val ageInDays = (System.currentTimeMillis() - cached.importedAtEpochMs) / (1000 * 60 * 60 * 24)
        val recencyBoost = when {
            ageInDays < 7 -> 2.0
            ageInDays < 30 -> 1.0
            else -> 0.0
        }
        score += recencyBoost

        // Penalize very large documents slightly (they match more but less specifically)
        score -= (cached.tokenCount / 1000.0).coerceAtMost(2.0)

        return score
    }

    private data class DocumentCacheEntry(
        val displayName: String,
        val title: String?,
        val importedAtEpochMs: Long,
        val tokenCount: Int
    )
}
