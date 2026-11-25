import json
import os
import sys

sys.path.insert(0, "$HOME/executorch")
from executorch.examples.models.llama.tokenizer.tiktoken import Tokenizer

TOK_PATH = "./model/android/tokenizer.model"
OUT_JSON = "./scripts/phase_3_5_prompts.json"

tok = Tokenizer(TOK_PATH)

QUESTIONS = [
    ("Q1", "C Programming", "What is a pointer in C?", [
        "A pointer stores a memory address.",
        "It can refer to another object's memory location.",
        "Avoid circular or incorrect definitions."
    ]),
    ("Q2", "Computer Architecture", "What is the difference between RAM and ROM?", [
        "RAM is generally volatile.",
        "ROM is non-volatile.",
        "Explain their different purposes."
    ]),
    ("Q3", "Embedded Systems", "What is an interrupt in embedded systems?", [
        "An event or signal requiring processor attention.",
        "Temporarily interrupts normal execution.",
        "Interrupt service routine (ISR)."
    ]),
    ("Q4", "Microcontroller Architecture", "What is the difference between a microcontroller and a microprocessor?", [
        "CPU and peripheral integration.",
        "Memory and peripheral architecture.",
        "Typical application differences."
    ]),
    ("Q5", "Signal Conversion", "Explain the working of an ADC in an embedded system.", [
        "Sampling.",
        "Quantization.",
        "Encoding.",
        "Digital representation of an analog input."
    ]),
    ("Q6", "Memory Addressing", "What happens when a null pointer is dereferenced in C?", [
        "Undefined behaviour in C.",
        "Possible crash or segmentation fault.",
        "No guaranteed valid result."
    ]),
    ("Q7", "Digital Electronics", "What is the difference between combinational and sequential logic?", [
        "Combinational output depends on current inputs.",
        "Sequential output depends on current inputs and stored state.",
        "Sequential logic uses memory elements such as flip-flops."
    ]),
    ("Q8", "Communication Systems", "What is the purpose of modulation in communication systems?", [
        "Information signal is translated onto a carrier.",
        "Facilitates transmission.",
        "Enables practical frequency allocation and communication."
    ]),
]

records = []
for qid, domain, q_text, expected in QUESTIONS:
    prompt = (
        "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
        "You are a helpful, respectful and honest assistant.<|eot_id|>"
        "<|start_header_id|>user<|end_header_id|>\n\n"
        f"{q_text}<|eot_id|>"
        "<|start_header_id|>assistant<|end_header_id|>\n\n"
    )
    ids = tok.model.encode(prompt, allowed_special="all")
    gen_budget = 128 - len(ids)
    records.append({
        "question_id": qid,
        "domain": domain,
        "question_text": q_text,
        "expected_concepts": expected,
        "prompt": prompt,
        "token_ids": ids,
        "prompt_tokens": len(ids),
        "max_gen_budget": gen_budget,
    })
    print(f"{qid} ({domain:28s}): prompt_tokens={len(ids)}, gen_budget={gen_budget} (P+G<=128)")

with open(OUT_JSON, "w", encoding="utf-8") as f:
    json.dump(records, f, indent=2)
print(f"Saved exact prompts and token IDs to {OUT_JSON}")
