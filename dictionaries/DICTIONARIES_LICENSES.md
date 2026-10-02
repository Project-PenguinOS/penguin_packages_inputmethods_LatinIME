# Bundled main dictionaries: sources and licenses

All `java/res/raw/main_*.dict` files are binary dictionaries in the LatinIME
v2 format (magic `9BC13AFE`, format version 202), which the native decoder
memory-maps directly from the APK (`noCompress 'dict'` in build.gradle).

Generated files are built from the wordlists in this directory with
`tools/dicttool/build-dicts.sh` (see the header of that script). The
wordlists are part of the AOSP / LineageOS LatinIME source tree and are
distributed under the project's license, the Apache License 2.0 (see the
top-level `NOTICE` file).

| Resource | Source wordlist | Origin | License |
|---|---|---|---|
| main_de, main_en, main_es, main_fr, main_it, main_pt_br, main_ru | prebuilt, shipped before this change (rebuilding from `de/en/es/fr/it/pt_BR/ru_wordlist.combined.gz` gives the same trie; only the header attribute order differs) | AOSP | Apache-2.0 |
| main_cs, main_da, main_el, main_en_au, main_en_gb, main_en_us, main_fi, main_hr, main_iw, main_lt, main_lv, main_nb, main_nl, main_pl, main_pt_pt, main_ro, main_sl, main_sr, main_sv, main_tr | `<locale>_wordlist.combined.gz` | AOSP LatinIME `dictionaries/` | Apache-2.0 |
| main_bg, main_ka, main_uk | `bg/ka/uk_wordlist.combined.gz` | LineageOS (CyanogenMod vendor/cm dictionaries; the Bulgarian update comes from OpenBoard PR #108) | Apache-2.0 (as part of LineageOS LatinIME) |
| main_hu | `hu_wordlist.combined.gz` | LineageOS (contributed by dh-harald, 2017) | Apache-2.0 (as part of LineageOS LatinIME) |
| main_lb | `lb_wordlist.combined.gz` | LineageOS (contributed by Michel Weimerskirch, 2015) | Apache-2.0 (as part of LineageOS LatinIME) |

Notes:
- `main_sr` is Cyrillic. The `sr_ZZ` (Serbian Latin) subtype falls back to it
  because there is no `main_sr_zz`, so it gives no useful suggestions there.
- Dictionaries for Indian languages and Hinglish are not bundled yet.
  Third-party dictionaries added later must be listed here with their
  upstream source and license.
