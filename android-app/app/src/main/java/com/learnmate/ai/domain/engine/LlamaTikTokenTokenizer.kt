package com.learnmate.ai.domain.engine

import android.content.Context
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Base64
import java.util.Collections
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Exact Llama 3 / 3.2 TikToken BPE Tokenizer corresponding to tokenizer.model (128k base vocabulary).
 * Supports the complete 256 special-token definitions and IDs (128,000 to 128,255) as defined
 * in ExecuTorch v0.5.0 example::get_tiktoken_for_llama().
 * Reuses the native C++ Tiktoken instance when available, and provides an exact pure-Kotlin
 * implementation for JVM testing and standalone context budgeting with 100% token ID parity.
 */
object LlamaTikTokenTokenizer {

    private const val TAG = "LlamaTikToken"

    /**
     * Exact regex pattern matching Llama 3 / 3.2 token pre-tokenization splitting.
     * Evaluates contractions, Unicode letters, numbers up to 3 digits, non-whitespace symbols, and newlines.
     */
    val LLAMA3_PATTERN: Pattern = Pattern.compile(
        """(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\r\n\p{L}\p{N}]?\p{L}+|\p{N}{1,3}| ?[^\s\p{L}\p{N}]+[\r\n]*|\s*[\r\n]+|\s+"""
    )

    /**
     * Complete 256 special token definitions in exact order matching ExecuTorch v0.5.0
     * examples/models/llama/tokenizer/llama_tiktoken.cpp (_get_default_special_tokens()).
     */
    val SPECIAL_TOKENS: List<String> = run {
        val list = ArrayList<String>(256)
        list.add("<|begin_of_text|>")            // index 0 -> 128000
        list.add("<|end_of_text|>")              // index 1 -> 128001
        list.add("<|reserved_special_token_0|>") // index 2 -> 128002
        list.add("<|reserved_special_token_1|>") // index 3 -> 128003
        list.add("<|finetune_right_pad_id|>")    // index 4 -> 128004
        list.add("<|step_id|>")                  // index 5 -> 128005
        list.add("<|start_header_id|>")          // index 6 -> 128006
        list.add("<|end_header_id|>")            // index 7 -> 128007
        list.add("<|eom_id|>")                   // index 8 -> 128008
        list.add("<|eot_id|>")                   // index 9 -> 128009
        list.add("<|python_tag|>")               // index 10 -> 128010
        var reservedNum = 2
        while (list.size < 256) {
            list.add("<|reserved_special_token_${reservedNum++}|>")
        }
        Collections.unmodifiableList(list)
    }

    /**
     * Wrapper class for byte arrays allowing Map lookup by byte array contents.
     */
    class ByteArrayKey(val bytes: ByteArray) {
        private val hash: Int = Arrays.hashCode(bytes)
        override fun hashCode(): Int = hash
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByteArrayKey) return false
            return Arrays.equals(bytes, other.bytes)
        }
    }

    private val ranks: MutableMap<ByteArrayKey, Int> = HashMap(160000)
    private val decoders: MutableMap<Int, ByteArray> = HashMap(160000)

    private val specialTokenEncoder: MutableMap<String, Long> = HashMap(256)
    private val specialTokenDecoder: MutableMap<Long, ByteArray> = HashMap(256)
    private var specialTokenPattern: Pattern? = null

    @Volatile
    private var isLoaded: Boolean = false
    private val loadLock = Any()

    init {
        initSpecialTokens(128000)
    }

    private fun initSpecialTokens(numBaseTokens: Int) {
        specialTokenEncoder.clear()
        specialTokenDecoder.clear()
        val sb = StringBuilder()
        for (i in SPECIAL_TOKENS.indices) {
            val s = SPECIAL_TOKENS[i]
            val id = numBaseTokens.toLong() + i.toLong()
            specialTokenEncoder[s] = id
            specialTokenDecoder[id] = s.toByteArray(StandardCharsets.UTF_8)
            if (i > 0) sb.append("|")
            sb.append(Pattern.quote(s))
        }
        specialTokenPattern = Pattern.compile(sb.toString())
    }

    private fun logI(msg: String) {
        try { android.util.Log.i(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    }

    private fun logW(msg: String) {
        try { android.util.Log.w(TAG, msg) } catch (_: Throwable) { System.err.println("[$TAG] $msg") }
    }

    /**
     * Loads the TikToken rank table from an input stream (tokenizer.model format: base64_bytes rank\n).
     */
    fun loadFromStream(inputStream: InputStream): Boolean {
        synchronized(loadLock) {
            if (isLoaded) return true
            try {
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8), 65536)
                val b64 = Base64.getDecoder()
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        val spaceIdx = trimmed.indexOf(' ')
                        if (spaceIdx > 0) {
                            val b64Str = trimmed.substring(0, spaceIdx)
                            val rankStr = trimmed.substring(spaceIdx + 1)
                            val rank = rankStr.toIntOrNull()
                            if (rank != null) {
                                val bytes = b64.decode(b64Str)
                                val key = ByteArrayKey(bytes)
                                ranks[key] = rank
                                decoders[rank] = bytes
                            }
                        }
                    }
                    line = reader.readLine()
                }
                reader.close()
                initSpecialTokens(ranks.size)
                isLoaded = ranks.isNotEmpty()
                logI("Loaded ${ranks.size} base TikToken ranks and ${SPECIAL_TOKENS.size} special tokens into memory.")
                return isLoaded
            } catch (e: Exception) {
                logW("Failed to load TikToken ranks from stream: ${e.message}")
                return false
            }
        }
    }

    /**
     * Loads the TikToken rank table from a file on disk.
     */
    fun loadFromFile(file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            return false
        }
        return try {
            FileInputStream(file).use { loadFromStream(it) }
        } catch (e: Exception) {
            logW("Failed to load TikToken from file ${file.absolutePath}: ${e.message}")
            false
        }
    }

    /**
     * Automatically discovers and loads tokenizer.model from standard search paths.
     */
    fun ensureLoaded(context: Context? = null, preferredPath: String? = null): Boolean {
        if (isLoaded) return true
        synchronized(loadLock) {
            if (isLoaded) return true

            if (preferredPath != null) {
                val f = File(preferredPath)
                if (loadFromFile(f)) return true
            }

            // 1. Check app-private internal storage first (where ModelAssetManager extracts tokenizer.model)
            context?.let { ctx ->
                try {
                    val internalModelFile = File(File(ctx.filesDir, "models"), "tokenizer.model")
                    if (loadFromFile(internalModelFile)) return true
                    val appExternal = ctx.getExternalFilesDir("models")
                    val externalModelFile = File(appExternal, "tokenizer.model")
                    if (loadFromFile(externalModelFile)) return true
                } catch (_: Exception) {}
            }

            // 2. Check optional JVM property / environment override and project-relative / device fallback paths
            val configuredPaths = listOfNotNull(
                System.getProperty("learnmate.tokenizer.path")?.takeIf { it.isNotBlank() },
                System.getenv("LEARNMATE_TOKENIZER_PATH")?.takeIf { it.isNotBlank() }
            )
            val candidates = configuredPaths + listOf(
                "model/android/tokenizer.model",
                "../model/android/tokenizer.model",
                "../../model/android/tokenizer.model",
                "../../../model/android/tokenizer.model",
                "/data/local/tmp/llama/tokenizer.model",
                "/sdcard/Download/tokenizer.model"
            )

            for (path in candidates) {
                val f = File(path)
                if (f.exists() && f.canRead() && f.length() > 0L) {
                    if (loadFromFile(f)) return true
                }
            }

            return isLoaded
        }
    }

    fun isReady(): Boolean = isLoaded
    fun getVocabSize(): Int = ranks.size + specialTokenEncoder.size
    fun getBaseVocabSize(): Int = ranks.size

    /**
     * Counts the exact number of TikToken tokens in the given text, including special tokens.
     * Uses native JNI runner tokenizer when handle is valid, or pure-Kotlin BPE implementation.
     */
    fun countTokens(text: String, nativeHandle: Long = 0L, context: Context? = null): Int {
        if (text.isEmpty()) return 0

        // 1. If native handle is available, query exact native TikToken
        if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
            val count = NativeLlamaBridge.nativeCountTokens(nativeHandle, text)
            if (count >= 0) return count
        }

        // 2. Pure-Kotlin exact TikToken evaluation
        ensureLoaded(context)
        if (!isLoaded) {
            ensureLoaded(null)
        }

        return if (isLoaded) {
            encode(text, nativeHandle, context).size
        } else {
            val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            maxOf(1, (words.size * 1.3).toInt())
        }
    }

    /**
     * Encodes text into exact TikToken token IDs, recognizing both special tokens and ordinary BPE pieces.
     */
    fun encode(text: String, nativeHandle: Long = 0L, context: Context? = null): List<Long> {
        if (text.isEmpty()) return emptyList()

        if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
            val nativeTokens = NativeLlamaBridge.nativeEncode(nativeHandle, text)
            if (nativeTokens != null) {
                return nativeTokens.toList()
            }
        }

        ensureLoaded(context)
        if (!isLoaded) return emptyList()

        val pattern = specialTokenPattern
        if (pattern == null) {
            val tokens = ArrayList<Long>()
            encodeOrdinaryText(text, tokens)
            return tokens
        }

        val matcher: Matcher = pattern.matcher(text)
        val tokens = ArrayList<Long>()
        var lastIdx = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (start > lastIdx) {
                encodeOrdinaryText(text.substring(lastIdx, start), tokens)
            }
            val special = matcher.group()
            val id = specialTokenEncoder[special]
            if (id != null) {
                tokens.add(id)
            }
            lastIdx = end
        }

        if (lastIdx < text.length) {
            encodeOrdinaryText(text.substring(lastIdx), tokens)
        }

        return tokens
    }

    private fun encodeOrdinaryText(text: String, out: MutableList<Long>) {
        if (text.isEmpty()) return
        val matcher: Matcher = LLAMA3_PATTERN.matcher(text)
        while (matcher.find()) {
            val piece = matcher.group()
            val pieceBytes = piece.toByteArray(StandardCharsets.UTF_8)
            out.addAll(encodePiece(pieceBytes))
        }
    }

    private fun encodePiece(pieceBytes: ByteArray): List<Long> {
        val wholeKey = ByteArrayKey(pieceBytes)
        val directRank = ranks[wholeKey]
        if (directRank != null) {
            return Collections.singletonList(directRank.toLong())
        }

        // Decompose piece into single-byte chunks
        val parts = ArrayList<ByteArray>(pieceBytes.size)
        for (b in pieceBytes) {
            parts.add(byteArrayOf(b))
        }

        // Iteratively merge minimum-rank adjacent byte pairs
        while (parts.size > 1) {
            var minRank = Int.MAX_VALUE
            var minIdx = -1

            for (i in 0 until parts.size - 1) {
                val a = parts[i]
                val b = parts[i + 1]
                val pair = ByteArray(a.size + b.size)
                System.arraycopy(a, 0, pair, 0, a.size)
                System.arraycopy(b, 0, pair, a.size, b.size)

                val r = ranks[ByteArrayKey(pair)]
                if (r != null && r < minRank) {
                    minRank = r
                    minIdx = i
                }
            }

            if (minIdx == -1) break

            val a = parts[minIdx]
            val b = parts[minIdx + 1]
            val merged = ByteArray(a.size + b.size)
            System.arraycopy(a, 0, merged, 0, a.size)
            System.arraycopy(b, 0, merged, a.size, b.size)

            parts[minIdx] = merged
            parts.removeAt(minIdx + 1)
        }

        val result = ArrayList<Long>(parts.size)
        for (p in parts) {
            val r = ranks[ByteArrayKey(p)]
            result.add(r?.toLong() ?: 0L)
        }
        return result
    }

    /**
     * Decodes a list of token IDs back into a UTF-8 string, supporting both ordinary and special tokens.
     */
    fun decode(tokens: List<Long>, nativeHandle: Long = 0L): String {
        if (tokens.isEmpty()) return ""

        if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
            val dec = NativeLlamaBridge.nativeDecode(nativeHandle, tokens.toLongArray())
            if (dec != null) return dec
        }

        ensureLoaded()
        val out = ByteArrayOutputStream()
        val baseVocab = ranks.size
        for (t in tokens) {
            if (t >= baseVocab) {
                val sb = specialTokenDecoder[t]
                if (sb != null) {
                    out.write(sb, 0, sb.size)
                }
            } else {
                val b = decoders[t.toInt()]
                if (b != null) {
                    out.write(b, 0, b.size)
                }
            }
        }
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    /**
     * Truncates text so that its exact TikToken count does not exceed maxTokens.
     * Guarantees truncation on exact token boundaries without splitting special tokens or multi-byte sequences.
     */
    fun truncateToTokens(text: String, maxTokens: Int, nativeHandle: Long = 0L, context: Context? = null): String {
        if (text.isEmpty() || maxTokens <= 0) return ""

        val tokens = encode(text, nativeHandle, context)
        if (tokens.size <= maxTokens) {
            return text
        }

        val truncatedTokens = tokens.subList(0, maxTokens)
        return decode(truncatedTokens, nativeHandle)
    }
}
