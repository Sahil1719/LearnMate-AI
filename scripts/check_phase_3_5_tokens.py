import os
import sys

sys.path.insert(0, "$HOME/executorch")
from executorch.examples.models.llama.tokenizer.tiktoken import Tokenizer

TOK_PATH = "./model/android/tokenizer.model"
tok = Tokenizer(TOK_PATH)

QUESTIONS = [
    ("Q1", "What is a pointer in C?"),
    ("Q2", "What is the difference between RAM and ROM?"),
    ("Q3", "What is an interrupt in embedded systems?"),
    ("Q4", "What is the difference between a microcontroller and a microprocessor?"),
    ("Q5", "Explain the working of an ADC in an embedded system."),
    ("Q6", "What happens when a null pointer is dereferenced in C?"),
    ("Q7", "What is the difference between combinational and sequential logic?"),
    ("Q8", "What is the purpose of modulation in communication systems?"),
]

def build_prompt(q: str) -> str:
    return (
        "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
        "You are a helpful, respectful and honest assistant.<|eot_id|>"
        "<|start_header_id|>user<|end_header_id|>\n\n"
        f"{q}<|eot_id|>"
        "<|start_header_id|>assistant<|end_header_id|>\n\n"
    )

print(f"BOS ID: {tok.bos_id} | EOS ID: {tok.eos_id} | Stop tokens: {tok.stop_tokens}")
for qid, q in QUESTIONS:
    p = build_prompt(q)
    # runner.cpp calls tokenizer->encode(prompt, bos=1, eos=0) or bos=0? Let's check both!
    ids_nobos = tok.encode(p, bos=False, eos=False)
    ids_bos = tok.encode(p, bos=True, eos=False)
    print(f"{qid}: len(bos=False)={len(ids_nobos)}, len(bos=True)={len(ids_bos)}, last_4={ids_nobos[-4:]}")
