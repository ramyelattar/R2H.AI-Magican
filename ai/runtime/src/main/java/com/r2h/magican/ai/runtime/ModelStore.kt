package com.r2h.magican.ai.runtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ModelStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun resolveVerifiedModel(spec: QuantizedModelSpec): ResolvedModel = withContext(Dispatchers.IO) {
        val target = spec.absolutePath?.let { File(it) } ?: run {
            val asset = requireNotNull(spec.assetPath)
            val root = File(context.filesDir, "llm_models").apply { mkdirs() }
            File(root, spec.copiedFileName).also { file ->
                if (!file.exists() || file.length() == 0L) {
                    context.assets.open(asset).use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                }
            }
        }

        require(target.exists() && target.isFile) { "Model file not found: ${target.absolutePath}" }
        val sizeBytes = target.length()
        if (spec.expectedSizeBytes != null) {
            require(spec.expectedSizeBytes == sizeBytes) {
                "Model size mismatch. expected=${spec.expectedSizeBytes} actual=$sizeBytes"
            }
        }

        val actualSha256 = sha256Cached(target)
        val expectedSha256 = spec.expectedSha256?.trim()?.lowercase().orEmpty()
        if (expectedSha256.isNotBlank()) {
            require(actualSha256 == expectedSha256) {
                "Model SHA-256 mismatch. expected=$expectedSha256 actual=$actualSha256"
            }
        }

        ResolvedModel(
            absolutePath = target.absolutePath,
            version = spec.version,
            sha256 = actualSha256,
            sizeBytes = sizeBytes
        )
    }

    /**
     * SHA-256 with a sidecar file cache to avoid rehashing large GGUF models on every load.
     * Cache key: file size + last-modified. Invalidated automatically when file changes.
     */
    private fun sha256Cached(file: File): String {
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
}
