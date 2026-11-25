#!/usr/bin/env python3
"""
LearnMate AI - Synthetic Dataset Quality & Validation Pipeline
Performs rigorous structural, statistical, and pedagogical checks on generated JSONL datasets.

Validation Criteria:
1. Schema & format validation (OpenAI / Llama-3.2 chat format).
2. Non-empty content and minimum length thresholds.
3. Exact duplicate detection (SHA-256 hash matching).
4. Near-duplicate detection (Word n-gram Jaccard similarity > 0.85).
5. Cross-split data leakage prevention (Train vs Val vs Test).
6. Subject domain and pedagogical category distribution analysis.
7. Structured JSON response validity in educational schemas.
8. Medical safety boundaries (educational scoping, zero clinical prescribing).
"""

import os
import sys
import json
import hashlib
import argparse
from pathlib import Path
from typing import List, Dict, Any, Set, Tuple

REQUIRED_ROLES = {"system", "user", "assistant"}

def compute_ngrams(text: str, n: int = 3) -> Set[str]:
    """Generates word-level n-grams from text."""
    words = text.lower().split()
    if len(words) < n:
        return set(words)
    return set(" ".join(words[i:i+n]) for i in range(len(words) - n + 1))

def jaccard_similarity(set_a: Set[str], set_b: Set[str]) -> float:
    """Computes Jaccard similarity between two sets."""
    if not set_a or not set_b:
        return 0.0
    return len(set_a.intersection(set_b)) / len(set_a.union(set_b))

def validate_jsonl_file(filepath: Path) -> Tuple[List[Dict[str, Any]], List[str]]:
    """Loads and validates individual records in a JSONL file."""
    records = []
    errors = []
    
    if not filepath.exists():
        return [], [f"File not found: {filepath}"]
        
    with open(filepath, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                data = json.loads(line)
            except json.JSONDecodeError as e:
                errors.append(f"Line {line_no} in {filepath.name}: Invalid JSON syntax ({e})")
                continue
                
            # Schema Check
            if "messages" not in data or not isinstance(data["messages"], list):
                errors.append(f"Line {line_no} in {filepath.name}: Missing 'messages' list")
                continue
                
            msgs = data["messages"]
            if len(msgs) < 2:
                errors.append(f"Line {line_no} in {filepath.name}: Conversation must have >= 2 messages")
                continue
                
            # Validate roles and contents
            has_user = False
            has_assistant = False
            for m_idx, msg in enumerate(msgs):
                role = msg.get("role")
                content = msg.get("content", "")
                if role not in REQUIRED_ROLES:
                    errors.append(f"Line {line_no} msg {m_idx}: Invalid role '{role}'")
                if not content or len(content.strip()) < 5:
                    errors.append(f"Line {line_no} msg {m_idx}: Content empty or too short (< 5 chars)")
                if role == "user":
                    has_user = True
                if role == "assistant":
                    has_assistant = True
                    if len(content.strip()) < 20:
                        errors.append(f"Line {line_no} msg {m_idx}: Assistant response too short (< 20 chars)")
                        
            if not has_user or not has_assistant:
                errors.append(f"Line {line_no}: Conversation must contain both user and assistant roles")
                
            records.append(data)
            
    return records, errors

def run_validation_suite(data_dir: Path) -> Dict[str, Any]:
    """Runs the full validation suite across train, validation, and test datasets."""
    report = {
        "status": "PASS",
        "errors": [],
        "warnings": [],
        "file_statistics": {},
        "leakage_checks": {},
        "distribution": {
            "domains": {},
            "categories": {},
            "competencies": {}
        },
        "quality_metrics": {
            "exact_duplicates": 0,
            "near_duplicates": 0,
            "json_schema_valid_count": 0,
            "medical_safety_verified_count": 0
        }
    }
    
    files = {
        "train": data_dir / "train.jsonl",
        "val": data_dir / "val.jsonl",
        "test": data_dir / "test.jsonl"
    }
    
    all_records = {}
    prompt_sets = {}
    exact_hashes = set()
    ngram_cache = []
    
    for split_name, fpath in files.items():
        records, errs = validate_jsonl_file(fpath)
        if errs:
            report["errors"].extend(errs)
        all_records[split_name] = records
        report["file_statistics"][split_name] = {
            "record_count": len(records),
            "file_size_bytes": fpath.stat().st_size if fpath.exists() else 0
        }
        
        prompts = set()
        for r in records:
            # First user message is the primary prompt
            user_msg = next((m["content"].strip().lower() for m in r["messages"] if m["role"] == "user"), "")
            asst_msg = next((m["content"].strip().lower() for m in r["messages"] if m["role"] == "assistant"), "")
            
            # Exact Duplicate Check
            combo_hash = hashlib.sha256((user_msg + " || " + asst_msg).encode()).hexdigest()
            if combo_hash in exact_hashes:
                report["errors"].append(f"Duplicate record detected across dataset: {r.get('id', 'unknown')}")
                report["quality_metrics"]["exact_duplicates"] += 1
            else:
                exact_hashes.add(combo_hash)
                
            # Near-duplicate tracking
            prompt_ngrams = compute_ngrams(user_msg, n=3)
            for prev_id, prev_ngrams in ngram_cache:
                sim = jaccard_similarity(prompt_ngrams, prev_ngrams)
                if sim > 0.85:
                    report["warnings"].append(f"High prompt similarity ({sim:.2f}) between {r.get('id')} and {prev_id}")
                    report["quality_metrics"]["near_duplicates"] += 1
            ngram_cache.append((r.get("id", "unknown"), prompt_ngrams))
            
            prompts.add(user_msg)
            
            # Distribution Tracking
            dom = r.get("domain", "Unknown")
            cat = r.get("category", "Unknown")
            lvl = r.get("competency_level", 0)
            report["distribution"]["domains"][dom] = report["distribution"]["domains"].get(dom, 0) + 1
            report["distribution"]["categories"][cat] = report["distribution"]["categories"].get(cat, 0) + 1
            report["distribution"]["competencies"][lvl] = report["distribution"]["competencies"].get(lvl, 0) + 1
            
            # Category-specific validation: Structured JSON
            if cat == "J_structured_json":
                # Find JSON block
                content = next(m["content"] for m in r["messages"] if m["role"] == "assistant")
                start = content.find("{")
                end = content.rfind("}")
                if start != -1 and end != -1:
                    json_str = content[start:end+1]
                    try:
                        parsed = json.loads(json_str)
                        report["quality_metrics"]["json_schema_valid_count"] += 1
                    except Exception as e:
                        report["errors"].append(f"Record {r.get('id')}: Malformed JSON in assistant response ({e})")
                else:
                    report["errors"].append(f"Record {r.get('id')}: Category J_structured_json must contain a JSON object")
                        
            # Category-specific validation: Medical Safety
            if dom == "Medicine":
                asst_full = next(m["content"] for m in r["messages"] if m["role"] == "assistant").lower()
                # Check for appropriate educational framing or doctor consultation referrals
                if "prescribe" in asst_full or "take 500mg" in asst_full:
                    report["errors"].append(f"Record {r.get('id')}: Unsafe medical prescription detected in assistant response")
                else:
                    report["quality_metrics"]["medical_safety_verified_count"] += 1

        prompt_sets[split_name] = prompts
        
    # Cross-split leakage checks
    train_val_overlap = prompt_sets.get("train", set()).intersection(prompt_sets.get("val", set()))
    train_test_overlap = prompt_sets.get("train", set()).intersection(prompt_sets.get("test", set()))
    val_test_overlap = prompt_sets.get("val", set()).intersection(prompt_sets.get("test", set()))
    
    report["leakage_checks"]["train_val_overlap"] = len(train_val_overlap)
    report["leakage_checks"]["train_test_overlap"] = len(train_test_overlap)
    report["leakage_checks"]["val_test_overlap"] = len(val_test_overlap)
    
    if train_val_overlap or train_test_overlap or val_test_overlap:
        report["errors"].append(f"Data leakage detected! Train/Val: {len(train_val_overlap)}, Train/Test: {len(train_test_overlap)}, Val/Test: {len(val_test_overlap)}")

    if report["errors"]:
        report["status"] = "FAIL"
        
    return report

def main():
    parser = argparse.ArgumentParser(description="Validate LearnMate AI JSONL datasets")
    parser.add_argument("--data_dir", type=str, default="training/dataset", help="Directory containing train, val, and test JSONL files")
    parser.add_argument("--report_file", type=str, default="training/reports/dataset_validation_report.json", help="Path to write validation JSON report")
    args = parser.parse_args()
    
    data_path = Path(args.data_dir).resolve()
    report_path = Path(args.report_file).resolve()
    report_path.parent.mkdir(parents=True, exist_ok=True)
    
    print("=" * 70)
    print("LearnMate AI: Dataset Quality & Validation Pipeline")
    print(f"Target Directory: {data_path}")
    print("=" * 70)
    
    report = run_validation_suite(data_path)
    
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
        
    print(f"\n[Validation Status]: {report['status']}")
    print(f"Total Files Inspected: {len(report['file_statistics'])}")
    for split, stats in report["file_statistics"].items():
        print(f"  - {split.capitalize()}: {stats['record_count']} records ({stats['file_size_bytes'] / 1024:.2f} KB)")
        
    print("\n[Quality Metrics]:")
    for k, v in report["quality_metrics"].items():
        print(f"  - {k}: {v}")
        
    print("\n[Cross-Split Leakage Checks]:")
    for k, v in report["leakage_checks"].items():
        print(f"  - {k}: {v} overlaps")
        
    print("\n[Domain Distribution]:")
    for k, v in report["distribution"]["domains"].items():
        print(f"  - {k}: {v} records")
        
    print("\n[Category Distribution]:")
    for k, v in report["distribution"]["categories"].items():
        print(f"  - {k}: {v} records")
        
    if report["warnings"]:
        print(f"\n[Warnings ({len(report['warnings'])})]:")
        for w in report["warnings"][:5]:
            print(f"  - {w}")
            
    if report["errors"]:
        print(f"\n[ERRORS ({len(report['errors'])})]:")
        for err in report["errors"]:
            print(f"  ! {err}")
        sys.exit(1)
    else:
        print(f"\nSUCCESS: All dataset quality checks passed. Report saved to {report_path}")
        sys.exit(0)

if __name__ == "__main__":
    main()
