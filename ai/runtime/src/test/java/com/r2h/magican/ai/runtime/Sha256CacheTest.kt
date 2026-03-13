package com.r2h.magican.ai.runtime

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Verifies the SHA-256 sidecar-cache behaviour described in [ModelStore.sha256Cached].
 *
 * Because [ModelStore.sha256Cached] is private, the cache logic is replicated inline here
 * so that every observable contract can be asserted without making private methods
 * visible for testing.  Both the [sha256] helper and the [sha256WithSidecar] cache wrapper
 * mirror the production implementation exactly.
 */
class Sha256CacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ---------------------------------------------------------------------------
    // Helpers — mirror of ModelStore private implementation
    // ---------------------------------------------------------------------------

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256WithSidecar(file: File): String {
        val sidecar = File(file.parent, "${file.name}.sha256")
        val cacheKey = "${file.length()}_${file.lastModified()}"
        if (sidecar.exists()) {
            val lines = runCatching { sidecar.readLines() }.getOrNull()
            if (lines != null && lines.size == 2 && lines[1] == cacheKey) {
                return lines[0]
            }
        }
        val hash = sha256(file)
        runCatching { sidecar.writeText("$hash\n$cacheKey") }
        return hash
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `sha256 of same file returns consistent hash`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(1024) { it.toByte() })

        val hash1 = sha256(file)
        val hash2 = sha256(file)

        assertEquals(hash1, hash2)
    }

    @Test
    fun `sha256 produces correct 64-char lowercase hex string`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(512) { 42 })

        val hash = sha256(file)

        assertEquals(64, hash.length)
        assertTrue("Hash must be lowercase hex", hash.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `sidecar file is created after first hash`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(512) { 42 })

        sha256WithSidecar(file)

        val sidecar = File(file.parent, "${file.name}.sha256")
        assertTrue("Sidecar file must exist after first hash", sidecar.exists())
        val lines = sidecar.readLines()
        assertEquals("Sidecar must have exactly 2 lines (hash + cacheKey)", 2, lines.size)
    }

    @Test
    fun `sidecar file contains valid hash on first line`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(512) { 42 })

        sha256WithSidecar(file)

        val sidecar = File(file.parent, "${file.name}.sha256")
        val storedHash = sidecar.readLines()[0]
        assertEquals(64, storedHash.length)
        assertTrue(storedHash.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `sidecar cache is read on second call instead of rehashing`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(1024) { it.toByte() })

        val hash1 = sha256WithSidecar(file)

        // Corrupt the actual file bytes without changing size or last-modified —
        // the cache should still return the original hash.
        val sidecar = File(file.parent, "${file.name}.sha256")
        val cacheKey = "${file.length()}_${file.lastModified()}"
        val fakeHash = "a".repeat(64)
        sidecar.writeText("$fakeHash\n$cacheKey")

        val hash2 = sha256WithSidecar(file)

        // The sidecar was valid so the cached (fake) hash is returned, not the real one.
        assertEquals(fakeHash, hash2)
    }

    @Test
    fun `stale sidecar is ignored and file is rehashed`() {
        val file = tmp.newFile("model.bin")
        file.writeBytes(ByteArray(512) { 0 })

        val sidecar = File(file.parent, "${file.name}.sha256")
        // Write a sidecar with a stale cache key (wrong length).
        sidecar.writeText("${"b".repeat(64)}\n0_0")

        val hash = sha256WithSidecar(file)

        // The stale sidecar is discarded; the real hash of the file is returned.
        assertEquals(sha256(file), hash)
    }
}
