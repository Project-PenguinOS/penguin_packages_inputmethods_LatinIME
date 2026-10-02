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
- Third-party dictionaries must be listed below with their upstream source
  and license.

## Third-party dictionaries (Indian languages)

Prebuilt `.dict` files (format v202) downloaded unmodified from the HeliBoard
dictionary repository, https://codeberg.org/Helium314/aosp-dictionaries
(`dictionaries/main_<lang>.dict`, retrieved 2026-10-03). The word lists they
were built from are in that repository's `wordlists/` directory.

| Resource | Language | Upstream source | License |
|---|---|---|---|
| main_bn | Bangla | `wordlists/main_bn.source`: Leipzig Corpora Collection word lists, proofread (https://wortschatz.uni-leipzig.de/en/download) | CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/) |
| main_hi | Hindi | https://gitlab.com/indicproject/dictionaries (`hi_wordlist.combined`) | GPLv2 |
| main_ta | Tamil | indicproject dictionaries (`wordlists/main_ta.source`) | GPLv2 |
| main_te | Telugu | indicproject dictionaries (`wordlists/main_te.source`) | GPLv2 |
| main_mr | Marathi | indicproject dictionaries (`wordlists/main_mr.source`) | GPLv2 |
| main_kn | Kannada | indicproject dictionaries (`wordlists/main_kn.source`) | GPLv2 |
| main_ml | Malayalam | indicproject dictionaries (`wordlists/main_ml.source`) | GPLv2 |
| main_gu | Gujarati | indicproject dictionaries (`wordlists/main_gu.source`) | GPLv2 |
| main_pa | Punjabi | indicproject dictionaries (`wordlists/main_pa.source`) | GPLv2 |
| main_ur | Urdu | indicproject dictionaries (`wordlists/main_ur.source`) | GPLv2 |

The GPLv2 dictionaries are separate data files read by the keyboard at run
time; their license text is in `dictionaries/GPL-2.0.txt` and the
corresponding source (the word lists) is available at the URLs above.
Bangla attribution: word lists © Leipzig Corpora Collection, CC BY 4.0;
proofread and compiled by the HeliBoard dictionary contributors.

Notes:
- Gujarati, Punjabi and Urdu have no keyboard layout/subtype yet, so their
  dictionaries are only used once such a subtype exists.
- Hinglish (`hi_ZZ`) is not bundled: the only available word list has no
  stated license.
