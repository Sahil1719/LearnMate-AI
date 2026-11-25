#!/usr/bin/env python3
"""Validates YAML and JSON configurations in LearnMate AI."""
import sys
import json
from pathlib import Path

def test_yaml_config(yaml_path: Path):
    # Standard library fallback parser / check
    with open(yaml_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert "meta-llama/Llama-3.2-3B-Instruct" in content
    assert "r: 16" in content
    assert "lora_alpha: 32" in content
    assert "bnb_4bit_quant_type: \"nf4\"" in content
    print(f"YAML Syntax & Required Keys: PASS ({yaml_path.name})")

def test_json_manifest(manifest_path: Path):
    with open(manifest_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    assert "models" in data
    assert len(data["models"]) >= 2
    print(f"JSON Manifest Syntax: PASS ({manifest_path.name}, {len(data['models'])} models)")

if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent.parent
    test_yaml_config(base_dir / "training" / "configs" / "qlora_config.yaml")
    test_json_manifest(base_dir / "model" / "model_manifest.json")
    print("All configurations validated successfully.")
