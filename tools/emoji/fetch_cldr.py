#!/usr/bin/env python3
import os
import sys
import urllib.request

BASE = "https://raw.githubusercontent.com/unicode-org/cldr/{tag}/"
FILES = {
    "annotations_en.xml": "common/annotations/en.xml",
    "annotationsDerived_en.xml": "common/annotationsDerived/en.xml",
    "LICENSE": "LICENSE",
}


def main():
    tag = sys.argv[1] if len(sys.argv) > 1 else "main"
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cldr")
    os.makedirs(out, exist_ok=True)
    for name, path in FILES.items():
        url = BASE.format(tag=tag) + path
        print("fetching", url)
        with urllib.request.urlopen(url) as r, open(os.path.join(out, name), "wb") as f:
            f.write(r.read())


if __name__ == "__main__":
    main()
