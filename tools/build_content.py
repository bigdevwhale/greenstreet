"""Runs every module in tools/content_src (each has main()) to regenerate the content TSVs.

python tools/build_content.py            # all
python tools/build_content.py people_a   # only some modules
"""
import importlib
import sys
from pathlib import Path

SRC = Path(__file__).resolve().parent / "content_src"
sys.path.insert(0, str(SRC))

names = sys.argv[1:] or sorted(p.stem for p in SRC.glob("*.py") if p.stem != "common" and not p.stem.startswith("_"))
for n in names:
    m = importlib.import_module(n)
    if not hasattr(m, "main"):
        print(f"skip {n}: no main()")
        continue
    m.main()
    print(f"built {n}")
