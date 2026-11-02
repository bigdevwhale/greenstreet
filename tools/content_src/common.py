"""Shared helpers for content modules: every module builds rows and calls write()."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTENT = ROOT / "app/src/main/assets/content"


def clean(v):
    s = "" if v is None else str(v)
    if "\t" in s or "\n" in s or "\r" in s:
        raise ValueError(f"tab/newline in cell: {s[:60]!r}")
    return s.strip()


def write(rel, cols, rows):
    """rows: lists/tuples in column order, or dicts keyed by column name."""
    out = CONTENT / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = ["\t".join(cols)]
    for r in rows:
        if isinstance(r, dict):
            extra = set(r) - set(cols)
            if extra:
                raise ValueError(f"{rel}: unknown columns {extra}")
            r = [r.get(c, "") for c in cols]
        if len(r) != len(cols):
            raise ValueError(f"{rel}: row has {len(r)} cells, want {len(cols)}: {r[:3]}")
        lines.append("\t".join(clean(v) for v in r))
    out.write_bytes(("\n".join(lines) + "\n").encode("utf-8"))  # LF on every OS
    return len(lines) - 1
