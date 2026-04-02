#!/usr/bin/env python3
"""Scan lsd.jar for Sport HMI–related UTF-8 strings in .class files (obfuscation-aware)."""
import sys
import zipfile

NEEDLES = [
    b"CAR_FUNCTION_SPORT_HMI",
    b"SPORT_HMI",
    b"CAR_FUNC_SPORT",
    b"SportComponent",
    b"600289",
    b"602113",
    b"602114",
    b"LD_CAR_SEL_SPORT",
]


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: discover_sport_hmi.py /path/to/lsd.jar", file=sys.stderr)
        return 2
    path = sys.argv[1]
    with zipfile.ZipFile(path, "r") as z:
        for name in z.namelist():
            if not name.endswith(".class"):
                continue
            data = z.read(name)
            hit = [n.decode("ascii", "replace") for n in NEEDLES if n in data]
            if hit:
                print(name, "->", ", ".join(hit))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
