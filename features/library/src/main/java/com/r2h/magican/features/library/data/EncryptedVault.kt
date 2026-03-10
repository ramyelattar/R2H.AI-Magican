package com.r2h.magican.features.library.data

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VaultImportReceipt(
    val bytesCopied: Long,
    val sha256: String,
    val vaultFileName: String
)

@Singleton
class EncryptedVault @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootDir = File(context.filesDir, "library_vault").apply { mkdirs() }
    private val masterKey: MasterKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    suspend fun importPdf(documentId: String, sourceUri: Uri): VaultImportReceipt = withContext(Dispatchers.IO) {
        val fileName = "$documentId.pdf.enc"
        val target = File(rootDir, fileName)
        if (target.exists()) {
            target.delete()
        }
        val digest = MessageDigest.getInstance("SHA-256")
        var bytesCopied = 0L

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            openEncryptedOutput(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    bytesCopied += read
                }
                output.flush()
            }
        } ?: error("Unable to open source URI: $sourceUri")

        VaultImportReceipt(
            bytesCopied = bytesCopied,
            sha256 = digest.digest().toHex(),
            vaultFileName = fileName
        )
    }

    suspend fun delete(vaultFileName: String) = withContext(Dispatchers.IO) {
        File(rootDir, vaultFileName).delete()
    }

    suspend fun readEncryptedIndex(indexFileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(rootDir, indexFileName)
        if (!file.exists()) return@withContext null
        openEncryptedInput(file).use { it.readBytes() }
    }

    suspend fun writeEncryptedIndex(indexFileName: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val file = File(rootDir, indexFileName)
        val tempFile = File(rootDir, "$indexFileName.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        openEncryptedOutput(tempFile).use {
            it.write(bytes)
            it.flush()
        }
        if (file.exists() && !file.delete()) {
            tempFile.delete()
            error("Failed to replace encrypted index: $indexFileName")
        }
        if (!tempFile.renameTo(file)) {
            tempFile.delete()
            error("Failed to move encrypted index into place: $indexFileName")
        }
    }

    private fun openEncryptedInput(file: File) = EncryptedFile.Builder(
        context,
        file,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build().openFileInput()

    private fun openEncryptedOutput(file: File) = EncryptedFile.Builder(
        context,
        file,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build().openFileOutput()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
