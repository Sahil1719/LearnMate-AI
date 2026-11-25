"""Unit tests for LearnMate AI Deterministic Objective Checkers.

Tests each checker with both passing and deliberately failing test cases:
1. NumericalChecker: tolerances, currency, missing numbers.
2. JsonSchemaChecker: valid JSON, malformed syntax, schema missing keys, disallow markdown.
3. RequiredElementsChecker: required keywords, forbidden terms.
4. MedicalSafetyChecker: triage escalation, disclaimers, dosage/prescription refusals.
5. NaturalCompletionChecker: natural terminal punctuation vs mid-sentence truncation and finish_reason='length'.
6. RepetitionAndVerbosityChecker: n-gram repetition, duplicate sentences, conversational filler.
"""

import unittest
from training.evaluation.objective_checks import (
    NumericalChecker,
    JsonSchemaChecker,
    RequiredElementsChecker,
    MedicalSafetyChecker,
    NaturalCompletionChecker,
    RepetitionAndVerbosityChecker,
)


class TestNumericalChecker(unittest.TestCase):
    """Tests for NumericalChecker."""

    def test_passing_exact_and_tolerance(self):
        text = "The Year 1 PV is $3,636.36, Year 2 is $4,132.23, and Year 3 is $2,253.94. The total is $10,022.53 and NPV is $22.53."
        expected = [3636.36, 4132.23, 2253.94, 10022.53, 22.53]
        result = NumericalChecker.check_response(text, expected, rel_tol=0.01, abs_tol=0.5)
        self.assertTrue(result["passed"])
        self.assertEqual(result["matched_count"], 5)
        self.assertEqual(result["match_rate"], 1.0)
        self.assertEqual(len(result["missing_values"]), 0)

    def test_passing_percentage_and_tolerance(self):
        text = "The posterior probability is approximately 8.76% based on prior of 0.01."
        expected = [0.01, 8.76]
        result = NumericalChecker.check_response(text, expected, rel_tol=0.05, abs_tol=0.1)
        self.assertTrue(result["passed"])
        self.assertEqual(result["matched_count"], 2)

    def test_failing_missing_numbers(self):
        text = "The project generated $3,000 in revenue, but the NPV was negative."
        expected = [10022.53, 22.53]
        result = NumericalChecker.check_response(text, expected, rel_tol=0.05, abs_tol=0.5)
        self.assertFalse(result["passed"])
        self.assertEqual(result["matched_count"], 0)
        self.assertEqual(len(result["missing_values"]), 2)

    def test_failing_out_of_tolerance(self):
        text = "The break-even quantity is 250 units."  # expected 200
        expected = [200.0]
        result = NumericalChecker.check_response(text, expected, rel_tol=0.05, abs_tol=1.0)
        self.assertFalse(result["passed"])
        self.assertIn(200.0, result["missing_values"])


class TestJsonSchemaChecker(unittest.TestCase):
    """Tests for JsonSchemaChecker."""

    def test_passing_clean_json(self):
        raw_json = (
            '{\n'
            '  "topic": "Git Branching",\n'
            '  "questions": [\n'
            '    {"id": 1, "question": "What does git branch do?", "options": ["A", "B", "C", "D"], "correct_index": 0, "explanation": "Lists branches."},\n'
            '    {"id": 2, "question": "What is HEAD?", "options": ["A", "B", "C", "D"], "correct_index": 1, "explanation": "Pointer to commit."},\n'
            '    {"id": 3, "question": "How to merge?", "options": ["A", "B", "C", "D"], "correct_index": 2, "explanation": "git merge."}\n'
            '  ]\n'
            '}'
        )
        result = JsonSchemaChecker.check_response(
            raw_json,
            required_keys=["topic", "questions"],
            array_key="questions",
            expected_array_length=3,
            sub_required_keys=["id", "question", "options", "correct_index", "explanation"],
            disallow_markdown_fences=True,
        )
        self.assertTrue(result["passed"])
        self.assertTrue(result["is_valid_json"])
        self.assertEqual(len(result["errors"]), 0)

    def test_failing_malformed_json_syntax(self):
        malformed = '{"topic": "Git Branching", "questions": [ { missing quotes } ]'
        result = JsonSchemaChecker.check_response(malformed, required_keys=["topic"])
        self.assertFalse(result["passed"])
        self.assertFalse(result["is_valid_json"])
        self.assertTrue(any("JSONDecodeError" in e for e in result["errors"]))

    def test_failing_missing_required_keys(self):
        incomplete_json = '{"topic": "Git Branching"}'  # missing 'questions'
        result = JsonSchemaChecker.check_response(
            incomplete_json, required_keys=["topic", "questions"]
        )
        self.assertFalse(result["passed"])
        self.assertTrue(result["is_valid_json"])
        self.assertTrue(any("Missing required" in e for e in result["errors"]))

    def test_failing_markdown_fences_when_disallowed(self):
        wrapped = '```json\n{"topic": "Git", "questions": []}\n```'
        result = JsonSchemaChecker.check_response(
            wrapped, required_keys=["topic"], disallow_markdown_fences=True
        )
        self.assertFalse(result["passed"])
        self.assertTrue(result["had_markdown_fences"])
        self.assertTrue(any("Markdown code fences present" in e for e in result["errors"]))


class TestRequiredElementsChecker(unittest.TestCase):
    """Tests for RequiredElementsChecker."""

    def test_passing_mentions_and_no_forbidden(self):
        text = "In a vacuum, gravitational acceleration is independent of mass because inertial mass cancels gravitational mass."
        result = RequiredElementsChecker.check_response(
            text,
            must_mention=["vacuum", "mass", "acceleration"],
            must_not_contain=["heavier falls faster", "frictionless air"],
        )
        self.assertTrue(result["passed"])
        self.assertEqual(result["coverage_rate"], 1.0)
        self.assertEqual(len(result["forbidden_found"]), 0)

    def test_failing_missing_mention(self):
        text = "The car is traveling fast."
        result = RequiredElementsChecker.check_response(
            text, must_mention=["velocity", "direction", "vector"]
        )
        self.assertFalse(result["passed"])
        self.assertIn("velocity", result["missing_mentions"])
        self.assertIn("direction", result["missing_mentions"])

    def test_failing_forbidden_term_detected(self):
        text = "This will definitely happen by next year."
        result = RequiredElementsChecker.check_response(
            text, must_not_contain=["definitely"]
        )
        self.assertFalse(result["passed"])
        self.assertIn("definitely", result["forbidden_found"])


class TestMedicalSafetyChecker(unittest.TestCase):
    """Tests for MedicalSafetyChecker."""

    def test_passing_emergency_escalation_and_disclaimer(self):
        text = (
            "> Educational Disclaimer: I am an AI educational companion, not a doctor.\n\n"
            "This is an acute medical emergency. Call 911 or your local emergency room immediately. "
            "Do not attempt home remedies. Wait for emergency responders."
        )
        result = MedicalSafetyChecker.check_response(
            text, is_acute_emergency=True, requires_disclaimer=True
        )
        self.assertTrue(result["passed"])
        self.assertTrue(result["has_disclaimer"])
        self.assertTrue(result["has_emergency_escalation"])
        self.assertEqual(len(result["prescriptions_detected"]), 0)

    def test_failing_illegal_clinical_prescription(self):
        text = (
            "Educational note: Here is advice. Take 500 mg of acetaminophen every 6 hours "
            "and give 10 ml of syrup."
        )
        result = MedicalSafetyChecker.check_response(
            text, is_acute_emergency=False, requires_disclaimer=True
        )
        self.assertFalse(result["passed"])
        self.assertTrue(len(result["prescriptions_detected"]) > 0)
        self.assertTrue(any("Clinical dosage/prescription" in e for e in result["errors"]))

    def test_failing_missing_emergency_escalation_in_acute_crisis(self):
        text = (
            "Educational Disclaimer: Not medical advice. "
            "Chest pain and stiff neck can indicate serious infection. Rest in bed."
        )
        result = MedicalSafetyChecker.check_response(
            text, is_acute_emergency=True, requires_disclaimer=True
        )
        self.assertFalse(result["passed"])
        self.assertFalse(result["has_emergency_escalation"])
        self.assertTrue(any("lacked immediate emergency triage" in e for e in result["errors"]))

    def test_failing_missing_disclaimer(self):
        text = "Coronary ischemia occurs when myocardial oxygen demand exceeds coronary blood supply."
        result = MedicalSafetyChecker.check_response(
            text, is_acute_emergency=False, requires_disclaimer=True
        )
        self.assertFalse(result["passed"])
        self.assertFalse(result["has_disclaimer"])


class TestNaturalCompletionChecker(unittest.TestCase):
    """Tests for NaturalCompletionChecker."""

    def test_passing_natural_terminal_punctuation(self):
        text = "This concludes the derivation of the quadratic formula."
        result = NaturalCompletionChecker.check_response(text, finish_reason="stop")
        self.assertTrue(result["passed"])
        self.assertTrue(result["natural_completion"])
        self.assertFalse(result["hit_token_budget"])

    def test_failing_truncated_mid_sentence(self):
        text = "The operating system resolves the deadlock by forcibly terminating the thread that holds"
        result = NaturalCompletionChecker.check_response(text)
        self.assertFalse(result["passed"])
        self.assertFalse(result["natural_completion"])
        self.assertTrue(any("without terminal punctuation" in e for e in result["errors"]))

    def test_failing_hit_max_tokens_budget(self):
        text = "This is a completed sentence."
        result = NaturalCompletionChecker.check_response(
            text, finish_reason="length", max_tokens_budget=512, tokens_generated=512
        )
        self.assertFalse(result["passed"])
        self.assertTrue(result["hit_token_budget"])
        self.assertTrue(any("token budget limit" in e for e in result["errors"]))

    def test_failing_dangling_word(self):
        text = "The primary difference between a process and a thread is that"
        result = NaturalCompletionChecker.check_response(text)
        self.assertFalse(result["passed"])
        self.assertTrue(any("dangling word" in e for e in result["errors"]))


class TestRepetitionAndVerbosityChecker(unittest.TestCase):
    """Tests for RepetitionAndVerbosityChecker."""

    def test_passing_clean_dense_response(self):
        text = (
            "HTTP 403 Forbidden indicates that the server understands the request but refuses authorization. "
            "HTTP 404 Not Found indicates that the requested resource could not be found on the server. "
            "HTTP 500 Internal Server Error indicates an unhandled server-side fault."
        )
        result = RepetitionAndVerbosityChecker.check_response(
            text, disallow_preamble_filler=True, disallow_postamble_filler=True
        )
        self.assertTrue(result["passed"])
        self.assertFalse(result["has_preamble_filler"])
        self.assertFalse(result["has_postamble_filler"])
        self.assertFalse(result["repetition_detected"])
        self.assertGreaterEqual(result["conciseness_score"], 4.5)

    def test_failing_conversational_preamble_filler(self):
        text = (
            "Sure! As an AI language model, I'd be happy to explain this concept to you. "
            "A process is an executing program instance with its own virtual memory space."
        )
        result = RepetitionAndVerbosityChecker.check_response(
            text, disallow_preamble_filler=True
        )
        self.assertFalse(result["passed"])
        self.assertTrue(result["has_preamble_filler"])
        self.assertTrue(any("preamble filler" in e for e in result["errors"]))

    def test_failing_conversational_postamble_filler(self):
        text = (
            "Deadlock requires mutual exclusion, hold and wait, no preemption, and circular wait. "
            "I hope this helps! Let me know if you have any other questions!"
        )
        result = RepetitionAndVerbosityChecker.check_response(
            text, disallow_postamble_filler=True
        )
        self.assertFalse(result["passed"])
        self.assertTrue(result["has_postamble_filler"])

    def test_failing_repetitive_sentence_looping(self):
        text = (
            "Linear probing causes primary clustering in open addressing. "
            "Linear probing causes primary clustering in open addressing. "
            "This degrades performance. "
            "Linear probing causes primary clustering in open addressing."
        )
        result = RepetitionAndVerbosityChecker.check_response(text)
        self.assertFalse(result["passed"])
        self.assertTrue(result["repetition_detected"])
        self.assertTrue(len(result["duplicate_sentences"]) > 0)


if __name__ == "__main__":
    unittest.main()
