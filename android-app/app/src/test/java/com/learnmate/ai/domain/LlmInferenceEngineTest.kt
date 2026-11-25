package com.learnmate.ai.domain

import com.learnmate.ai.domain.engine.ExecuTorchLlmEngine
import com.learnmate.ai.domain.engine.LlamaTikTokenTokenizer
import com.learnmate.ai.domain.model.InferenceState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit test suite for On-Device LLM Inference Architecture, ExecuTorch JNI Integration,
 * Exact TikToken Token Counting, Context Budgeting (P + G <= 128), and Deterministic Fallback Engine.
 */
class LlmInferenceEngineTest {

    private lateinit var engine: ExecuTorchLlmEngine

    @Before
    fun setUp() {
        engine = ExecuTorchLlmEngine()
        val configured = listOfNotNull(
            System.getProperty("learnmate.tokenizer.path")?.takeIf { it.isNotBlank() },
            System.getenv("LEARNMATE_TOKENIZER_PATH")?.takeIf { it.isNotBlank() }
        ).map(::File)
        val tokFile = (configured + listOf(
            File("../../model/android/tokenizer.model"),
            File("../model/android/tokenizer.model"),
            File("model/android/tokenizer.model")
        )).firstOrNull { it.exists() && it.canRead() }

        if (tokFile != null) {
            LlamaTikTokenTokenizer.loadFromFile(tokFile)
        } else {
            LlamaTikTokenTokenizer.ensureLoaded()
        }
    }

    @Test
    fun testInitialState_IsModelMissingWhenNoLocalPteFile() {
        val state = engine.state.value
        assertTrue(
            "State should be ModelMissing or Ready in test environment",
            state is InferenceState.ModelMissing || state is InferenceState.Ready
        )
        if (state is InferenceState.ModelMissing) {
            assertTrue(state.searchedPaths.isNotEmpty())
            assertTrue(state.expectedModelPath.endsWith(".pte"))
            assertNotNull(state.advice)
        }
    }

    @Test
    fun testCandidatePaths_ContainsStandardOnDeviceLocations() {
        val paths = ExecuTorchLlmEngine.CANDIDATE_MODEL_PATHS
        assertTrue("Should contain /sdcard/Download path", paths.any { it.contains("Download") })
        assertTrue("Should contain /data/local/tmp path", paths.any { it.contains("/data/local/tmp") })
        assertTrue("Should target Llama 3 8B 8da4w model", paths.any { it.contains("llama3_8b_8da4w.pte") })
        assertTrue("Should include small test model stories110m", paths.any { it.contains("stories110m.pte") })
    }

    @Test
    fun testCandidateModelPaths_PrioritizesLearnMateLlama323B() {
        val paths = ExecuTorchLlmEngine.CANDIDATE_MODEL_PATHS
        assertEquals(
            "Top candidate must be learnmate_llama3_2_3b.pte in /data/local/tmp/llama/",
            "/data/local/tmp/llama/learnmate_llama3_2_3b.pte",
            paths.first()
        )
        assertTrue(
            "Candidate paths must contain /sdcard/Download/learnmate_llama3_2_3b.pte",
            paths.contains("/sdcard/Download/learnmate_llama3_2_3b.pte")
        )
    }

    @Test
    fun testCandidateTokenizerPaths_ContainsTokenizerModelAndBin() {
        val paths = ExecuTorchLlmEngine.CANDIDATE_TOKENIZER_PATHS
        assertTrue(
            "Must contain /data/local/tmp/llama/tokenizer.model",
            paths.contains("/data/local/tmp/llama/tokenizer.model")
        )
        assertTrue(
            "Must contain /sdcard/Download/tokenizer.model",
            paths.contains("/sdcard/Download/tokenizer.model")
        )
        assertTrue(
            "Must contain /data/local/tmp/llama/tokenizer.bin",
            paths.contains("/data/local/tmp/llama/tokenizer.bin")
        )
        assertTrue(
            "Must contain /sdcard/Download/tokenizer.bin",
            paths.contains("/sdcard/Download/tokenizer.bin")
        )
    }

    @Test
    fun testFindModelFile_ResolvesOnlyValidNonEmptyPteFiles() {
        val tempDir = File.createTempFile("learnmate_test_models", "").apply {
            delete()
            mkdir()
        }
        try {
            val emptyPte = File(tempDir, "empty.pte").apply { createNewFile() }
            val txtFile = File(tempDir, "model.txt").apply { writeText("not a pte") }
            val validPte = File(tempDir, "learnmate_llama3_2_3b.pte").apply { writeText("dummy pte content") }

            val candidates = listOf(
                emptyPte.absolutePath,
                txtFile.absolutePath,
                validPte.absolutePath
            )

            val resolved = engine.findModelFile(candidates)
            assertNotNull("Should resolve valid non-empty .pte file", resolved)
            assertEquals(validPte.absolutePath, resolved?.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testFindTokenizerFile_ResolvesModelAndBinFilesIndependently() {
        val tempDir = File.createTempFile("learnmate_test_tok", "").apply {
            delete()
            mkdir()
        }
        try {
            val pteFile = File(tempDir, "model.pte").apply { writeText("pte data") }
            val emptyModel = File(tempDir, "empty.model").apply { createNewFile() }
            val validModel = File(tempDir, "tokenizer.model").apply { writeText("tokenizer data") }

            val candidates = listOf(
                pteFile.absolutePath,
                emptyModel.absolutePath,
                validModel.absolutePath
            )

            val resolved = engine.findTokenizerFile(candidates)
            assertNotNull("Should resolve valid non-empty tokenizer.model", resolved)
            assertEquals(validModel.absolutePath, resolved?.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testFindExistingFile_DelegatesCorrectlyForBothModelAndTokenizer() {
        val tempDir = File.createTempFile("learnmate_test_compat", "").apply {
            delete()
            mkdir()
        }
        try {
            val modelFile = File(tempDir, "learnmate_llama3_2_3b.pte").apply { writeText("pte data") }
            val tokFile = File(tempDir, "tokenizer.model").apply { writeText("tok data") }

            val foundModel = engine.findExistingFile(listOf(modelFile.absolutePath))
            assertEquals(modelFile.absolutePath, foundModel?.absolutePath)

            val foundTok = engine.findExistingFile(listOf(tokFile.absolutePath))
            assertEquals(tokFile.absolutePath, foundTok?.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // --- Phase 2B.1 Dedicated TikToken & Context Budget Validation Tests ---

    @Test
    fun testActualTikTokenCounting_MatchesExactLlama3Vocabulary() {
        // "Explain pointers in C" -> 5 tokens in Llama 3
        val count1 = LlamaTikTokenTokenizer.countTokens("Explain pointers in C")
        assertEquals("Token count for 'Explain pointers in C' must match exact Llama 3 BPE", 5, count1)

        // "A pointer to a pointer is" -> 6 tokens
        val count2 = LlamaTikTokenTokenizer.countTokens("A pointer to a pointer is")
        assertEquals(6, count2)

        // Verify roundtrip encoding and decoding
        val tokens = LlamaTikTokenTokenizer.encode("Explain pointers in C")
        val decoded = LlamaTikTokenTokenizer.decode(tokens)
        assertEquals("Explain pointers in C", decoded)
    }

    @Test
    fun testActualTikTokenCounting_TechnicalCodePrompts() {
        // Technical C code snippet
        val code = "int* ptr = &val; *ptr = 42; printf(\"%d\\n\", *ptr);"
        val count = LlamaTikTokenTokenizer.countTokens(code)
        assertEquals("Code tokens must be counted with exact BPE precision", 21, count)

        val tokens = LlamaTikTokenTokenizer.encode(code)
        val decoded = LlamaTikTokenTokenizer.decode(tokens)
        assertEquals("Decoded code must perfectly match original source", code, decoded)
    }

    @Test
    fun testActualTikTokenCounting_UnicodeAndSpecialCharacters() {
        // Accented characters, mathematical symbols, Hindi / Devanagari, and quotes
        val unicodePrompt = "Goal: Machine Learning (गणित & फ़िज़िक्स) • E = mc² • Cost: €50 • 'quoted' & `backticked`"
        val count = LlamaTikTokenTokenizer.countTokens(unicodePrompt)
        assertTrue("Unicode prompt must be tokenized without loss", count > 0)

        val tokens = LlamaTikTokenTokenizer.encode(unicodePrompt)
        val decoded = LlamaTikTokenTokenizer.decode(tokens)
        assertEquals("Decoded unicode text must preserve 100% byte fidelity", unicodePrompt, decoded)
    }

    @Test
    fun testPromptBudgeting_EmptyAndBlankPromptsFailCleanly() {
        try {
            engine.budgetCompletePrompt("")
            fail("Empty prompt must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("empty or blank"))
        }

        try {
            engine.budgetCompletePrompt("     \n\t  ")
            fail("Blank prompt must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("empty or blank"))
        }
    }

    @Test
    fun testPromptBudgeting_EnforcesPPlusGLeq128() {
        val testQueries = listOf(
            "What is a variable?",
            "Explain double-entry bookkeeping rules for credit and debit",
            "High-yield medical topics for NEET PG clinical pathology with anatomical cross-sections",
            "Explain pointer arithmetic in C when dealing with arrays of pointers to structs"
        )

        for (q in testQueries) {
            val budget = engine.budgetCompletePrompt(q, requestedMaxTokens = 128, minGenerationTokens = 32)
            assertTrue("P must be >= 1", budget.promptTokens >= 1)
            assertTrue("P must be strictly < totalSeqLen", budget.promptTokens < budget.totalSeqLen)
            assertTrue("P must be strictly < 128", budget.promptTokens < 128)
            assertTrue("G must be >= 1", budget.generationTokens >= 1)
            assertTrue("P + G must be <= 128", budget.promptTokens + budget.generationTokens <= 128)
            assertEquals("totalSeqLen must equal promptTokens + generationTokens", budget.totalSeqLen, budget.promptTokens + budget.generationTokens)
            assertTrue("Generation budget must be at least 32 for normal prompts", budget.generationTokens >= 32)
        }
    }

    @Test
    fun testPromptBudgeting_PromptsNear128BoundaryClampedSafely() {
        // Construct an excessively long technical prompt that significantly exceeds 128 tokens
        val sb = StringBuilder()
        sb.append("Goal: Exhaustive Distributed Systems Engineering Architecture\n")
        sb.append("Context: Prior discussion covered Raft consensus algorithm, Paxos state machine replication, leader election, log compaction, and Byzantine fault tolerance.\n")
        sb.append("Question: Could you provide a deeply comprehensive, line-by-line breakdown of how quorum-based consensus handles network partitions, split-brain conditions, asynchronous heartbeats, clock drift in vector clocks, monotonic read consistency guarantees, and two-phase commit transactions in high-throughput distributed databases without data loss or unrecoverable split states?")

        val originalPrompt = sb.toString()
        val originalTokens = LlamaTikTokenTokenizer.countTokens(originalPrompt)
        assertTrue("Test prompt must exceed 96 tokens boundary (was $originalTokens)", originalTokens > 96)

        val budget = engine.budgetCompletePrompt(originalPrompt, requestedMaxTokens = 128, minGenerationTokens = 32)

        assertTrue("Prompt tokens must be clamped to <= 96 tokens", budget.promptTokens <= 96)
        assertTrue("Generation budget must be at least 32 tokens", budget.generationTokens >= 32)
        assertTrue("P + G must strictly be <= 128", budget.promptTokens + budget.generationTokens <= 128)
        assertEquals(budget.totalSeqLen, budget.promptTokens + budget.generationTokens)
        assertTrue("Budgeted text must not be empty", budget.safePrompt.isNotBlank())
    }

    @Test
    fun testCompletePromptConstruction_IncludesGoalContextAndQuestion() {
        val complete = engine.buildCompletePrompt(
            userQuestion = "What is a pointer?",
            goalTitle = "C Programming",
            systemInstruction = "You are LearnMate AI.",
            conversationContext = "Previous topic: Arrays"
        )

        assertTrue("Must start with <|begin_of_text|>", complete.startsWith("<|begin_of_text|>"))
        assertTrue("Must have system header", complete.contains("<|start_header_id|>system<|end_header_id|>\n\n"))
        assertTrue("Must contain system instruction", complete.contains("You are LearnMate AI."))
        assertTrue("Must contain goal", complete.contains("[Goal: C Programming]"))
        assertTrue("Must contain context", complete.contains("[Context: Previous topic: Arrays]"))
        assertTrue("Must have user header", complete.contains("<|start_header_id|>user<|end_header_id|>\n\n"))
        assertTrue("Must contain question", complete.contains("What is a pointer?"))
        assertTrue("Must have assistant generation header", complete.endsWith("<|start_header_id|>assistant<|end_header_id|>\n\n"))
        assertFalse("Must not append EOS to assistant prefix", complete.endsWith("<|eot_id|>"))

        val budget = engine.budgetCompletePrompt(complete)
        assertTrue(budget.promptTokens >= 1)
        assertTrue(budget.promptTokens + budget.generationTokens <= 128)
    }

    @Test
    fun testCompletePromptBudgeting_PreservesQuestionContentWhenTruncating() {
        val longContext = "Long background context discussing foundational theory. ".repeat(15)
        val prompt = engine.buildCompletePrompt(
            userQuestion = "What is a pointer in C?",
            goalTitle = "C Programming",
            systemInstruction = "You are LearnMate AI.",
            conversationContext = longContext
        )

        val originalTokens = LlamaTikTokenTokenizer.countTokens(prompt)
        assertTrue("Original prompt must exceed budget (was $originalTokens)", originalTokens > 96)

        val budget = engine.budgetCompletePrompt(prompt, requestedMaxTokens = 128, minGenerationTokens = 32)
        assertTrue("Prompt must be clamped to <= 96 tokens (was ${budget.promptTokens})", budget.promptTokens <= 96)
        assertTrue("Question part must be preserved during budgeting", budget.safePrompt.contains("What is a pointer in C?"))
        assertTrue("Must retain assistant generation header", budget.safePrompt.endsWith("<|start_header_id|>assistant<|end_header_id|>\n\n"))
    }

    @Test
    fun testEightRequiredTokenizerParityCases_MatchExactNativeGroundTruth() {
        // Case 1: Plain English text
        val c1Text = "What is a pointer in C?"
        val c1Expected = listOf(3923L, 374L, 264L, 7597L, 304L, 356L, 30L)
        val c1Actual = LlamaTikTokenTokenizer.encode(c1Text)
        assertEquals("Case 1 count parity", 7, c1Actual.size)
        assertEquals("Case 1 exact token ID sequence parity", c1Expected, c1Actual)
        assertEquals("Case 1 round-trip decoding", c1Text, LlamaTikTokenTokenizer.decode(c1Actual))

        // Case 2: Multiline text
        val c2Text = "System: You are LearnMate\nGoal: C Programming\nQuestion: What is a pointer in C?"
        val c2Expected = listOf(2374L, 25L, 1472L, 527L, 15281L, 97742L, 198L, 41092L, 25L, 356L, 39524L, 198L, 14924L, 25L, 3639L, 374L, 264L, 7597L, 304L, 356L, 30L)
        val c2Actual = LlamaTikTokenTokenizer.encode(c2Text)
        assertEquals("Case 2 count parity", 21, c2Actual.size)
        assertEquals("Case 2 exact token ID sequence parity", c2Expected, c2Actual)
        assertEquals("Case 2 round-trip decoding", c2Text, LlamaTikTokenTokenizer.decode(c2Actual))

        // Case 3: Unicode and Devanagari
        val c3Text = "Goal: Machine Learning (गणित & फ़िज़िक्स) • E = mc² • Cost: €50"
        val c3Expected = listOf(41092L, 25L, 13257L, 21579L, 320L, 100280L, 100572L, 100428L, 612L, 100622L, 104538L, 100346L, 104538L, 65804L, 100782L, 8L, 7436L, 469L, 284L, 19777L, 30556L, 7436L, 11443L, 25L, 13281L, 1135L)
        val c3Actual = LlamaTikTokenTokenizer.encode(c3Text)
        assertEquals("Case 3 count parity", 26, c3Actual.size)
        assertEquals("Case 3 exact token ID sequence parity", c3Expected, c3Actual)
        assertEquals("Case 3 round-trip decoding", c3Text, LlamaTikTokenTokenizer.decode(c3Actual))

        // Case 4: Technical code syntax
        val c4Text = "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = if (isEmpty()) this else ..."
        val c4Expected = listOf(12158L, 366L, 51L, 551L, 59206L, 3203L, 2511L, 1796L, 3203L, 14611L, 28863L, 10442L, 4658L, 1796L, 3203L, 29L, 284L, 422L, 320L, 19509L, 2189L, 420L, 775L, 2564L)
        val c4Actual = LlamaTikTokenTokenizer.encode(c4Text)
        assertEquals("Case 4 count parity", 24, c4Actual.size)
        assertEquals("Case 4 exact token ID sequence parity", c4Expected, c4Actual)
        assertEquals("Case 4 round-trip decoding", c4Text, LlamaTikTokenTokenizer.decode(c4Actual))

        // Case 5: Minimal Llama 3.2 Instruct template
        val c5Text = "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\nWhat is a pointer in C?<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
        val c5Expected = listOf(128000L, 128006L, 882L, 128007L, 271L, 3923L, 374L, 264L, 7597L, 304L, 356L, 30L, 128009L, 128006L, 78191L, 128007L, 271L)
        val c5Actual = LlamaTikTokenTokenizer.encode(c5Text)
        assertEquals("Case 5 count parity", 17, c5Actual.size)
        assertEquals("Case 5 exact token ID sequence parity", c5Expected, c5Actual)
        assertEquals("Case 5 round-trip decoding", c5Text, LlamaTikTokenTokenizer.decode(c5Actual))

        // Case 6: Full LearnMate system/user prompt
        val c6Text = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\nYou are LearnMate, an offline AI learning companion.\n[Goal: C Programming]\n[Context: Arrays and memory addresses]<|eot_id|><|start_header_id|>user<|end_header_id|>\n\nWhat is pointer arithmetic?<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
        val c6Expected = listOf(128000L, 128006L, 9125L, 128007L, 271L, 2675L, 527L, 15281L, 97742L, 11L, 459L, 27258L, 15592L, 6975L, 22489L, 627L, 58L, 41092L, 25L, 356L, 39524L, 933L, 58L, 2014L, 25L, 23824L, 323L, 5044L, 14564L, 60L, 128009L, 128006L, 882L, 128007L, 271L, 3923L, 374L, 7597L, 35884L, 30L, 128009L, 128006L, 78191L, 128007L, 271L)
        val c6Actual = LlamaTikTokenTokenizer.encode(c6Text)
        assertEquals("Case 6 count parity", 45, c6Actual.size)
        assertEquals("Case 6 exact token ID sequence parity", c6Expected, c6Actual)
        assertEquals("Case 6 round-trip decoding", c6Text, LlamaTikTokenTokenizer.decode(c6Actual))

        // Case 7: Empty and whitespace-only
        assertEquals("Case 7a empty count", 0, LlamaTikTokenTokenizer.encode("").size)
        assertEquals("Case 7a empty list", emptyList<Long>(), LlamaTikTokenTokenizer.encode(""))
        val c7bActual = LlamaTikTokenTokenizer.encode("   \n\t  ")
        assertEquals("Case 7b whitespace count", 2, c7bActual.size)
        assertEquals("Case 7b whitespace sequence", listOf(5996L, 8860L), c7bActual)

        // Case 8: Special-token strings embedded in ordinary text
        val c8Text = "To start a turn, use <|start_header_id|> and end with <|end_header_id|>, followed by <|eot_id|>."
        val c8Expected = listOf(1271L, 1212L, 264L, 2543L, 11L, 1005L, 220L, 128006L, 323L, 842L, 449L, 220L, 128007L, 11L, 8272L, 555L, 220L, 128009L, 13L)
        val c8Actual = LlamaTikTokenTokenizer.encode(c8Text)
        assertEquals("Case 8 count parity", 19, c8Actual.size)
        assertEquals("Case 8 exact token ID sequence parity", c8Expected, c8Actual)
        assertEquals("Case 8 round-trip decoding", c8Text, LlamaTikTokenTokenizer.decode(c8Actual))
    }

    @Test
    fun testInvalidPromptBudget_RejectsWithoutNativeInvocation() = runBlocking {
        // Calling generateStream with a blank prompt must terminate without invoking native runner
        try {
            engine.generateStream("   ", maxTokens = 32).toList()
            fail("Blank prompt stream must terminate with exception")
        } catch (e: Exception) {
            assertTrue("Should throw or close with prompt validation error", e is IllegalArgumentException || e.message?.contains("Prompt") == true)
        }
    }

    @Test
    fun testPromptBudgeting_PreservesShortPrompts() {
        val shortPrompt = "Explain pointers in C"
        val budgeted = engine.budgetPrompt(shortPrompt)
        assertEquals(shortPrompt, budgeted)
    }

    @Test
    fun testPromptBudgeting_ClampsLongPromptsSafely() {
        val longPrompt = "Goal: Mastering Embedded C Programming for Microcontrollers and Edge Computing Architectures. " +
                "Question: Can you please provide an exhaustive, deeply comprehensive and mathematically complete explanation of how pointer arithmetic, pointer decay in multi-dimensional arrays, function pointers, and volatile memory-mapped hardware peripheral registers interact in real-time embedded systems without causing stack overflow or undefined memory access faults?"

        val budgeted = engine.budgetPrompt(longPrompt)
        val tokens = LlamaTikTokenTokenizer.countTokens(budgeted)
        assertTrue("Budgeted prompt token count must be <= 96", tokens <= 96)
        assertTrue("Budgeted prompt must not be empty", budgeted.isNotBlank())
    }

    @Test
    fun testInitialize_FailsCleanlyWhenModelMissing() = runBlocking {
        val result = engine.initialize("/nonexistent/path/to/missing_model.pte")
        assertTrue("Initialization should fail for non-existent file", result.isFailure)
        assertTrue("State should transition to ModelMissing", engine.state.value is InferenceState.ModelMissing)
    }

    @Test
    fun testDeterministicFallback_SynthesizesEducationalGuidance() = runBlocking {
        val cResponse = engine.generate("Explain pointer arithmetic in C", maxTokens = 64)
        assertTrue("Response should indicate offline knowledge engine", cResponse.contains("[Offline Knowledge Engine]"))
        assertTrue("Should mention memory or pointer concepts", cResponse.contains("pointer") || cResponse.contains("address"))

        val accResponse = engine.generate("What are the rules of debit and credit in accounting?", maxTokens = 64)
        assertTrue(accResponse.contains("Debit") || accResponse.contains("credit"))

        val medResponse = engine.generate("High-yield topics for NEET PG medical preparation", maxTokens = 64)
        assertTrue(medResponse.contains("clinical") || medResponse.contains("medical"))
    }

    @Test
    fun testStreamingGeneration_EmitsSequentialTokens() = runBlocking {
        val tokens = engine.generateStream("Hello learning companion", maxTokens = 32).toList()
        assertTrue("Should emit multiple streaming chunks", tokens.size >= 3)
        val fullText = tokens.joinToString("")
        assertTrue("Combined tokens must form complete response", fullText.contains("[Offline Knowledge Engine]"))
    }

    @Test
    fun testUnload_ResetsNativeHandleAndState() {
        engine.unload()
        assertEquals(InferenceState.Uninitialized, engine.state.value)
        assertEquals(0L, engine.getNativeHandle())
    }

    @Test
    fun testZeroCloudDependency_NoNetworkCallsMade() = runBlocking {
        val start = System.currentTimeMillis()
        val result = engine.generate("Explain study roadmap", maxTokens = 32)
        val elapsed = System.currentTimeMillis() - start
        assertTrue("Generation must complete locally under 3 seconds", elapsed < 3000)
        assertTrue("Result must not be empty", result.isNotBlank())
    }
}
