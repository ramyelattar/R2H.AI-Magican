package com.r2h.magican.features.library.data

import com.r2h.magican.features.library.domain.LibraryDocument
import com.r2h.magican.features.library.domain.PdfMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the [LibraryRepository.search] snapshot behavior.
 *
 * Because [LibraryRepository]'s collaborators (EncryptedVault, LibraryIndexStore) are
 * concrete Android classes that cannot be subclassed in JVM tests without a framework like
 * Robolectric, these tests exercise the search logic through a minimal in-process harness:
 *
 * - [LibrarySearchIndex] is a pure-Kotlin class and is used directly.
 * - The document snapshot is managed via a [MutableStateFlow], mirroring what
 *   [LibraryRepository] does internally.
 * - The [search] helper below replicates the three-line search() body verbatim so
 *   every branch (blank query, non-matching query, matching query) is covered.
 *
 * Separately, [LibraryRepository.seedForTest] (internal, @VisibleForTesting) exists for
 * integration-style tests that do have an instrumented runtime available.
 */
class LibraryRepositorySearchTest {

    private lateinit var searchIndex: LibrarySearchIndex
    private lateinit var snapshot: MutableStateFlow<List<LibraryDocument>>

    // ---------------------------------------------------------------------------
    // Minimal reproduction of LibraryRepository.search() — see LibraryRepository.kt
    // ---------------------------------------------------------------------------

    /**
     * Replicates the body of [LibraryRepository.search] exactly so that the snapshot
     * and search-index interaction is tested without requiring the full repository graph.
     */
    private fun search(query: String): List<LibraryDocument> {
        val q = query.trim()
        val docs = snapshot.value
        if (q.isBlank()) return docs

        val matchingIds = searchIndex.search(q, maxResults = 100)
        val docMap = docs.associateBy { it.id }
        return matchingIds.mapNotNull { docMap[it] }
    }

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private fun makeDocument(
        id: String,
        displayName: String,
        extractedText: String = "",
        importedAt: Long = System.currentTimeMillis()
    ) = LibraryDocument(
        id = id,
        displayName = displayName,
        vaultFileName = "$id.enc",
        sha256 = id,
        metadata = PdfMetadata(
            title = null,
            author = null,
            subject = null,
            keywords = null,
            pageCount = 1,
            fileSizeBytes = 1024L,
            importedAtEpochMs = importedAt
        ),
        extractedText = extractedText,
        summary = null,
        bookmarks = emptyList()
    )

    private fun seed(docs: List<LibraryDocument>) {
        snapshot.value = docs
        searchIndex.rebuild(docs)
    }

    @Before
    fun setUp() {
        searchIndex = LibrarySearchIndex()
        snapshot = MutableStateFlow(emptyList())
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `search with empty string returns all documents`() {
        val docs = listOf(
            makeDocument("1", "Tarot Reading Guide"),
            makeDocument("2", "Astrology Basics"),
            makeDocument("3", "Numerology 101")
        )
        seed(docs)

        val results = search("")

        assertEquals(3, results.size)
    }

    @Test
    fun `search with blank string returns all documents`() {
        val docs = listOf(
            makeDocument("1", "Tarot Reading Guide"),
            makeDocument("2", "Astrology Basics")
        )
        seed(docs)

        val results = search("  ")

        assertEquals(2, results.size)
    }

    @Test
    fun `search with keyword delegates to searchIndex and maps by id`() {
        val tarotDoc = makeDocument(
            id = "1",
            displayName = "Tarot Reading Guide",
            extractedText = "tarot cards spread reading"
        )
        val unrelatedDoc = makeDocument(
            id = "2",
            displayName = "Astrology Basics",
            extractedText = "planets stars zodiac"
        )
        seed(listOf(tarotDoc, unrelatedDoc))

        val results = search("tarot")

        assertEquals(1, results.size)
        assertEquals("1", results[0].id)
        assertEquals("Tarot Reading Guide", results[0].displayName)
    }

    @Test
    fun `search returns empty list when no document matches searchIndex`() {
        seed(
            listOf(
                makeDocument("1", "Astrology Basics", extractedText = "planets stars zodiac"),
                makeDocument("2", "Numerology 101", extractedText = "numbers vibration frequency")
            )
        )

        val results = search("tarot")

        assertTrue(results.isEmpty())
    }
}
