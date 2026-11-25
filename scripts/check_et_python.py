import sys
print("Python executable:", sys.executable)
print("Python version:", sys.version)

try:
    import torch
    print("PyTorch version:", torch.__version__)
except ImportError as e:
    print("PyTorch not installed:", e)

try:
    import executorch
    print("ExecuTorch package found at:", executorch.__file__)
except ImportError as e:
    print("ExecuTorch package not installed in site-packages:", e)

try:
    import torchao
    print("TorchAO version:", torchao.__version__)
except ImportError as e:
    print("TorchAO not installed:", e)
