#!/usr/bin/env python3
import os
import re
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CLDR = os.path.join(ROOT, "tools", "emoji", "cldr")
CATEGORIES = os.path.join(ROOT, "java", "res", "values-v19", "emoji-categories.xml")
OUT = os.path.join(ROOT, "java", "assets", "emoji")

PALETTE_ARRAYS = [
    "emoji_eight_smiley_people",
    "emoji_eight_animals_nature",
    "emoji_eight_food_drink",
    "emoji_eight_travel_places",
    "emoji_eight_activity",
    "emoji_eight_objects",
    "emoji_eight_symbols",
    "emoji_eight_flags",
]

TONES = ["\U0001F3FB", "\U0001F3FC", "\U0001F3FD", "\U0001F3FE", "\U0001F3FF"]
VS16 = "️"

POPULAR = (
    "😂 ❤️ 🤣 👍 😭 🙏 😘 🥰 😍 😊 🎉 😁 💕 🥺 😅 🔥 ☺️ 🤦 ♥️ 🤷 🙄 😆 🤗 😉 🎂 🤔 👏 🙂 😳 🥳 "
    "😎 👌 💜 😔 💪 ✨ 💖 👀 😋 😏 😢 👉 💗 😩 💯 🌹 💞 🎈 💙 😃 😡 💐 😜 🙈 🤞 😄 🤤 🙌 🤪 "
    "❣️ 😀 💋 💀 👇 💔 😌 💓 🤩 🙃 😬 😱 😴 🤭 😐 🌞 😒 😇 🌸 😈 🎶 ✌️ 🎊 🥵 😞 💚 ☀️ 🖤 💰 "
    "😚 👑 🎁 💥 🙋 ☹️ 😑 🥴 👈 💩 ✅ 👋 🤢 🧡 🤍 💛 🍕 🍺 🎄 ⭐ 😮 😕 😠 👎 🤝 🙌 💤"
).split()

EXTRA_KEYWORDS = {
    "❤️": ["love", "heart"],
    "😍": ["love"],
    "😂": ["lol", "haha", "funny"],
    "🤣": ["lmao", "rofl"],
    "😭": ["sad"],
    "👍": ["ok", "yes", "good", "like"],
    "👎": ["no", "bad", "dislike"],
    "🙏": ["thanks", "thank you", "please"],
    "🎉": ["congrats", "congratulations", "yay"],
    "🎂": ["birthday"],
    "🔥": ["lit"],
    "😴": ["tired"],
    "😘": ["kiss"],
    "👋": ["hi", "hello", "bye"],
    "😊": ["happy"],
    "😡": ["angry"],
    "🤔": ["hmm"],
    "💯": ["perfect"],
}


def strip_vs(s):
    return s.replace(VS16, "")


def parse_palette():
    tree = ET.parse(CATEGORIES)
    arrays = {a.get("name"): a for a in tree.getroot().iter("array")}
    order = []
    for name in PALETTE_ARRAYS:
        for item in arrays[name].iter("item"):
            spec = (item.text or "").strip()
            parts = spec.split("|")
            if len(parts) > 2:
                continue
            codes = parts[1] if len(parts) > 1 else parts[0]
            order.append("".join(chr(int(c, 16)) for c in codes.split(",")))
    return order


def parse_annotations(path):
    keywords, names = {}, {}
    for a in ET.parse(path).getroot().iter("annotation"):
        cp = a.get("cp")
        text = (a.text or "").strip()
        if a.get("type") == "tts":
            names[cp] = text.lower()
        else:
            keywords[cp] = [k.strip().lower() for k in text.split("|") if k.strip()]
    return keywords, names


def main():
    palette = parse_palette()
    palette_set = {strip_vs(e): e for e in palette}

    keywords, names = parse_annotations(os.path.join(CLDR, "annotations_en.xml"))
    dk, dn = parse_annotations(os.path.join(CLDR, "annotationsDerived_en.xml"))
    for k, v in dk.items():
        keywords.setdefault(k, v)
    for k, v in dn.items():
        names.setdefault(k, v)
    by_norm_kw = {strip_vs(k): v for k, v in keywords.items()}
    by_norm_name = {strip_vs(k): v for k, v in names.items()}

    order = []
    seen = set()
    for e in [p for p in POPULAR] + palette:
        n = strip_vs(e)
        if n in seen or n not in palette_set:
            continue
        seen.add(n)
        order.append(palette_set[n])

    os.makedirs(OUT, exist_ok=True)
    count = 0
    with open(os.path.join(OUT, "en.tsv"), "w", encoding="utf-8") as f:
        for e in order:
            n = strip_vs(e)
            name = by_norm_name.get(n)
            kws = list(by_norm_kw.get(n, []))
            for extra_e, extra in EXTRA_KEYWORDS.items():
                if strip_vs(extra_e) == n:
                    kws += extra
            if name is None and not kws:
                continue
            terms = []
            for t in ([name] if name else []) + kws:
                t = re.sub(r"[\t|]", " ", t).strip()
                if t and t not in terms:
                    terms.append(t)
            f.write(e + "\t" + "|".join(terms) + "\n")
            count += 1

    toned = {}
    for cp in names:
        tones_used = {c for c in cp if c in TONES}
        if len(tones_used) != 1:
            continue
        tone = tones_used.pop()
        base = strip_vs("".join(c for c in cp if c not in TONES))
        if base not in palette_set:
            continue
        toned.setdefault(base, {})[tone] = cp
    tone_count = 0
    with open(os.path.join(OUT, "skin_tones.tsv"), "w", encoding="utf-8") as f:
        for e in palette:
            variants = toned.get(strip_vs(e))
            if not variants or len(variants) != len(TONES):
                continue
            f.write(e + "\t" + "\t".join(variants[t] for t in TONES) + "\n")
            tone_count += 1

    with open(os.path.join(CLDR, "LICENSE"), encoding="utf-8") as src:
        text = src.read()
    marker = "All other content in this repo is released under UNICODE LICENSE V3"
    v3 = text[text.index(marker) + len(marker):].strip() if marker in text else text
    with open(os.path.join(OUT, "LICENSE"), "w", encoding="utf-8") as f:
        f.write("The emoji keyword and skin tone data in this folder is derived from the Unicode\n"
                "Common Locale Data Repository (CLDR), common/annotations and\n"
                "common/annotationsDerived, https://github.com/unicode-org/cldr\n\n"
                "UNICODE LICENSE V3\n\nCOPYRIGHT AND PERMISSION NOTICE\n\n"
                "Copyright © 1991-2026 Unicode, Inc.\n\n" + v3 + "\n")
    print("wrote %d emoji, %d with skin tones" % (count, tone_count))


if __name__ == "__main__":
    main()
