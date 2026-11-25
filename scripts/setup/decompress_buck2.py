import zstd
import os
import stat

cache_dir = "$HOME/.buck2/cache"
bin_dir = "$HOME/.local/bin"
os.makedirs(cache_dir, exist_ok=True)
os.makedirs(bin_dir, exist_ok=True)

with open("/tmp/buck2.zst", "rb") as f:
    data = f.read()

decompressed = zstd.decompress(data)

p1 = os.path.join(cache_dir, "buck2-3bbde7daa94987db468d021ad625bc93dc62ba7fcb16945cb09b64aab077f284")
p2 = os.path.join(bin_dir, "buck2")

with open(p1, "wb") as f:
    f.write(decompressed)
os.chmod(p1, 0o755)

with open(p2, "wb") as f:
    f.write(decompressed)
os.chmod(p2, 0o755)

print("BUCK2_INSTALL_SUCCESSFUL")
