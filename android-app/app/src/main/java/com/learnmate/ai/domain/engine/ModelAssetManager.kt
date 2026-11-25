package com.learnmate.ai.domain.engine

import android.content.Context
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

sealed class ModelPreparationState {
    object Checking : ModelPreparationState()
    data class Extracting(
        val stage: String,
        val bytesCopied: Long,
        val totalBytes: Long,
        val progress: Float
    ) : ModelPreparationState()
    data class Verifying(val stage: String) : ModelPreparationState()
    data class InitializingEngine(val message: String) : ModelPreparationState()
    data class Ready(
        val modelPath: String,
        val tokenizerPath: String,
        val modelSha256: String,
        val tokenizerSha256: String,
        val extractedFreshly: Boolean
    ) : ModelPreparationState()
    data class Error(
        val title: String,
        val message: String,
        val isStorageError: Boolean = false
    ) : ModelPreparationState()
}

/**
 * Extracts and SHA-256 verifies the bundled Meta Llama 3.2 3B INT4 PTE and tokenizer
 * from APK assets into app-private internal storage (filesDir/models) once on first launch.
 */
class ModelAssetManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelAssetManager"
        const val ASSET_PTE_PATH = "models/learnmate_llama3_2_3b.pte"
        const val ASSET_TOKENIZER_PATH = "models/tokenizer.model"
        const val PTE_FILENAME = "learnmate_llama3_2_3b.pte"
        const val TOKENIZER_FILENAME = "tokenizer.model"
        const val MARKER_FILENAME = ".verified_manifest"

        const val PTE_EXPECTED_SIZE = 1929907360L
        const val PTE_EXPECTED_SHA256 = "8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91"

        const val TOKENIZER_EXPECTED_SIZE = 2183982L
        const val TOKENIZER_EXPECTED_SHA256 = "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55"

        private const val SAFETY_MARGIN_BYTES = 100L * 1024L * 1024L // 100 MB headroom
        private const val BUFFER_BYTES = 1024 * 1024 // 1 MB streaming buffer
        private const val PROGRESS_EMIT_STEP_BYTES = 16L * 1024L * 1024L // 16 MB intervals
    }

    private val _state = MutableStateFlow<ModelPreparationState>(ModelPreparationState.Checking)
    val state: StateFlow<ModelPreparationState> = _state.asStateFlow()

    private val prepMutex = Mutex()

    fun getModelsDir(): File = File(context.filesDir, "models")

    fun getExtractedModelFile(): File = File(getModelsDir(), PTE_FILENAME)

    fun getExtractedTokenizerFile(): File = File(getModelsDir(), TOKENIZER_FILENAME)

    private fun expectedMarkerContent(): String =
        "$PTE_EXPECTED_SHA256\n$TOKENIZER_EXPECTED_SHA256\n$PTE_EXPECTED_SIZE\n$TOKENIZER_EXPECTED_SIZE"

    private fun isAlreadyVerifiedOnDisk(): Boolean {
        val pteFile = getExtractedModelFile()
        val tokFile = getExtractedTokenizerFile()
        val marker = File(getModelsDir(), MARKER_FILENAME)
        if (!pteFile.exists() || !tokFile.exists() || !marker.exists()) return false
        if (pteFile.length() != PTE_EXPECTED_SIZE || tokFile.length() != TOKENIZER_EXPECTED_SIZE) return false
        return try {
            marker.readText().trim() == expectedMarkerContent()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun prepareAndInitialize(engine: ExecuTorchLlmEngine): Result<ModelPreparationState.Ready> =
        withContext(Dispatchers.IO) {
            prepMutex.withLock {
                try {
                    val modelsDir = getModelsDir()
                    if (!modelsDir.exists()) {
                        modelsDir.mkdirs()
                    }
                    val pteFile = getExtractedModelFile()
                    val tokFile = getExtractedTokenizerFile()
                    val marker = File(modelsDir, MARKER_FILENAME)

                    var extractedFreshly = false

                    if (!isAlreadyVerifiedOnDisk()) {
                        _state.value = ModelPreparationState.Checking
                        // Clean up stale partial files first so free-space check is accurate
                        File(modelsDir, "$PTE_FILENAME.tmp").delete()
                        File(modelsDir, "$TOKENIZER_FILENAME.tmp").delete()
                        marker.delete()

                        val pteValid = pteFile.exists() && pteFile.length() == PTE_EXPECTED_SIZE &&
                            computeFileSha256(pteFile) == PTE_EXPECTED_SHA256
                        val tokValid = tokFile.exists() && tokFile.length() == TOKENIZER_EXPECTED_SIZE &&
                            computeFileSha256(tokFile) == TOKENIZER_EXPECTED_SHA256

                        val bytesNeeded = (if (pteValid) 0L else PTE_EXPECTED_SIZE) +
                            (if (tokValid) 0L else TOKENIZER_EXPECTED_SIZE) + SAFETY_MARGIN_BYTES

                        val availableBytes = StatFs(modelsDir.absolutePath).availableBytes
                        if (availableBytes < bytesNeeded) {
                            val availMb = availableBytes / (1024 * 1024)
                            val reqMb = bytesNeeded / (1024 * 1024)
                            val msg = "Insufficient internal storage to unpack offline AI model. " +
                                "Available: $availMb MB, Required: $reqMb MB (includes 100 MB safety margin)."
                            _state.value = ModelPreparationState.Error(
                                title = "Insufficient Storage Space",
                                message = msg,
                                isStorageError = true
                            )
                            return@withLock Result.failure(IllegalStateException(msg))
                        }

                        if (!tokValid) {
                            tokFile.delete()
                            val tokHash = extractAssetWithSha256(
                                assetPath = ASSET_TOKENIZER_PATH,
                                destFile = tokFile,
                                expectedSize = TOKENIZER_EXPECTED_SIZE,
                                stageLabel = "Extracting tokenizer (2.1 MB)...",
                                totalExpectedBytes = PTE_EXPECTED_SIZE + TOKENIZER_EXPECTED_SIZE,
                                baseCopiedBytes = 0L
                            )
                            if (tokHash != TOKENIZER_EXPECTED_SHA256) {
                                tokFile.delete()
                                val msg = "Tokenizer SHA-256 verification failed.\nExpected: $TOKENIZER_EXPECTED_SHA256\nActual: $tokHash"
                                _state.value = ModelPreparationState.Error("Checksum Verification Failed", msg)
                                return@withLock Result.failure(IllegalStateException(msg))
                            }
                            extractedFreshly = true
                        }

                        if (!pteValid) {
                            pteFile.delete()
                            val pteHash = extractAssetWithSha256(
                                assetPath = ASSET_PTE_PATH,
                                destFile = pteFile,
                                expectedSize = PTE_EXPECTED_SIZE,
                                stageLabel = "Extracting Llama 3.2 3B INT4 model (1.84 GiB)...",
                                totalExpectedBytes = PTE_EXPECTED_SIZE + TOKENIZER_EXPECTED_SIZE,
                                baseCopiedBytes = TOKENIZER_EXPECTED_SIZE
                            )
                            if (pteHash != PTE_EXPECTED_SHA256) {
                                pteFile.delete()
                                val msg = "Model SHA-256 verification failed.\nExpected: $PTE_EXPECTED_SHA256\nActual: $pteHash"
                                _state.value = ModelPreparationState.Error("Checksum Verification Failed", msg)
                                return@withLock Result.failure(IllegalStateException(msg))
                            }
                            extractedFreshly = true
                        }

                        _state.value = ModelPreparationState.Verifying("Recording SHA-256 verification manifest...")
                        marker.writeText(expectedMarkerContent())
                        Log.i(TAG, "Verified extracted model & tokenizer SHA-256 in ${modelsDir.absolutePath}")
                    } else {
                        Log.i(TAG, "Persisted model & tokenizer already verified on disk (${modelsDir.absolutePath})")
                    }

                    _state.value = ModelPreparationState.InitializingEngine(
                        "Loading ExecuTorch XNNPACK NEON runtime (~30s on 4 GB RAM)..."
                    )

                    val initResult = engine.initialize(
                        modelPath = pteFile.absolutePath,
                        tokenizerPath = tokFile.absolutePath
                    )
                    if (initResult.isFailure) {
                        val err = initResult.exceptionOrNull()?.message ?: "ExecuTorch JNI failed to load extracted model."
                        _state.value = ModelPreparationState.Error(
                            title = "Native Runtime Initialization Failed",
                            message = err,
                            isStorageError = false
                        )
                        return@withLock Result.failure(initResult.exceptionOrNull() ?: RuntimeException(err))
                    }

                    val ready = ModelPreparationState.Ready(
                        modelPath = pteFile.absolutePath,
                        tokenizerPath = tokFile.absolutePath,
                        modelSha256 = PTE_EXPECTED_SHA256,
                        tokenizerSha256 = TOKENIZER_EXPECTED_SHA256,
                        extractedFreshly = extractedFreshly
                    )
                    _state.value = ready
                    Result.success(ready)
                } catch (e: Exception) {
                    Log.e(TAG, "Model preparation failed: ${e.message}", e)
                    val msg = e.message ?: "Unexpected failure preparing local model."
                    _state.value = ModelPreparationState.Error(
                        title = "Model Preparation Error",
                        message = msg,
                        isStorageError = msg.contains("ENOSPC", ignoreCase = true) || msg.contains("space", ignoreCase = true)
                    )
                    Result.failure(e)
                }
            }
        }

    private fun extractAssetWithSha256(
        assetPath: String,
        destFile: File,
        expectedSize: Long,
        stageLabel: String,
        totalExpectedBytes: Long,
        baseCopiedBytes: Long
    ): String {
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
        if (tmpFile.exists()) tmpFile.delete()

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        var bytesWritten = 0L
        var nextEmitThreshold = 0L

        context.assets.open(assetPath).use { input ->
            FileOutputStream(tmpFile).use { output ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    bytesWritten += read
                    if (bytesWritten >= nextEmitThreshold || bytesWritten >= expectedSize) {
                        val overallCopied = baseCopiedBytes + bytesWritten
                        val pct = (overallCopied.toFloat() / totalExpectedBytes.toFloat()).coerceIn(0f, 1f)
                        _state.value = ModelPreparationState.Extracting(
                            stage = stageLabel,
                            bytesCopied = overallCopied,
                            totalBytes = totalExpectedBytes,
                            progress = pct
                        )
                        nextEmitThreshold = bytesWritten + PROGRESS_EMIT_STEP_BYTES
                    }
                }
                output.fd.sync()
            }
        }

        if (bytesWritten != expectedSize) {
            tmpFile.delete()
            throw IllegalStateException(
                "Extracted size mismatch for $assetPath: expected $expectedSize bytes, got $bytesWritten bytes"
            )
        }
        if (!tmpFile.renameTo(destFile)) {
            tmpFile.delete()
            throw IllegalStateException("Failed to finalize extracted file ${destFile.absolutePath}")
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
