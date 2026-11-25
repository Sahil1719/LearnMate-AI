import os
import sys
import torch
from torch.export import export
from executorch.exir import to_edge

print("=== EXPORTING SYNTHETIC EXECUTORCH PTE ===")
print("Python:", sys.version)
print("PyTorch:", torch.__version__)

class SyntheticAddModule(torch.nn.Module):
    def __init__(self):
        super().__init__()

    def forward(self, a: torch.Tensor, b: torch.Tensor) -> torch.Tensor:
        return a + b

model = SyntheticAddModule().eval()

# Deterministic test inputs
a = torch.tensor([[1.0, 2.0, 3.0, 4.0]], dtype=torch.float32)
b = torch.tensor([[10.0, 20.0, 30.0, 40.0]], dtype=torch.float32)

print("\nInput A:", a.tolist())
print("Input B:", b.tolist())

# PyTorch Reference Execution
expected_output = model(a, b)
print("Expected Output (A + B):", expected_output.tolist())

# 1. Export with PyTorch 2.6.0 torch.export
print("\n[Step 1] Running torch.export.export...")
aten_ep = export(model, (a, b))
print("ExportedProgram successfully captured.")

# 2. Lower to ExecuTorch Edge dialect
print("[Step 2] Lowering to ExecuTorch Edge Dialect (to_edge)...")
edge_ep = to_edge(aten_ep)
print("EdgeProgram captured successfully.")

# 3. Lower to ExecuTorch program
print("[Step 3] Emitting ExecuTorch Program (to_executorch)...")
et_prog = edge_ep.to_executorch()
print("ExecuTorchProgram successfully generated.")

# 4. Save to PTE file
output_dir = "./model/synthetic"
os.makedirs(output_dir, exist_ok=True)
pte_path = os.path.join(output_dir, "synthetic_add.pte")

with open(pte_path, "wb") as f:
    f.write(et_prog.buffer)

file_size = os.path.getsize(pte_path)
print(f"\n[Step 4] Saved PTE artifact:")
print(f"  Path: {pte_path}")
print(f"  Size: {file_size:,} bytes ({file_size/1024:.2f} KB)")

# Save expected inputs and outputs as raw binary floats for easy C++ reading
a_raw_path = os.path.join(output_dir, "input_a.bin")
b_raw_path = os.path.join(output_dir, "input_b.bin")
expected_raw_path = os.path.join(output_dir, "expected_out.bin")

a.detach().numpy().tofile(a_raw_path)
b.detach().numpy().tofile(b_raw_path)
expected_output.detach().numpy().tofile(expected_raw_path)

print(f"Saved raw test tensors to {output_dir}")
print("=== PTE EXPORT VERIFICATION COMPLETE ===")
