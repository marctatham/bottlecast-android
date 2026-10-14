# Third-party assets

Fonts and icons bundled in the app, where each came from and the licence it's under. Downloads are pinned to a commit, so the source can be checked byte for byte.

| Asset | Repo path | Source (pinned commit) | SHA-256 | Licence |
|---|---|---|---|---|
| Roboto Flex 3.200 (variable) | `app/src/main/res/font/roboto_flex.ttf` | [google/fonts `ofl/robotoflex`](https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/ofl/robotoflex) | `9b523f7d82593df0107173849ebb8c817471a1df4b4fb2c3cbf40cfd810c8281` | SIL OFL 1.1, [`RobotoFlex-OFL.txt`](RobotoFlex-OFL.txt) |
| Roboto Mono 3.001 Regular | `app/src/main/res/font/roboto_mono_regular.ttf` | [googlefonts/RobotoMono `fonts/ttf`](https://github.com/googlefonts/RobotoMono/tree/111eb14e367888c9374da4da0b018e72cf8ac46d/fonts/ttf) | `af0bff7599c3df3831755c16e39b3c496df74b8c8d8a1161b14dc8461be17cb4` | SIL OFL 1.1, [`RobotoMono-OFL.txt`](RobotoMono-OFL.txt) |
| Roboto Slab 2.001 (Regular, Medium, SemiBold, Bold, Black) | `app/src/main/res/font/robotoslab_*.ttf` | Bundled before the redesign; the family is [google/fonts `apache/robotoslab`](https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/apache/robotoslab) | — | Apache 2.0, [`RobotoSlab-LICENSE.txt`](RobotoSlab-LICENSE.txt) |
| Material Symbols Rounded (wght 400, fill 0, 24px): account_circle, arrow_back, check, chevron_right, close, flag, flag (fill 1), help, info, inventory_2, language, lock, more_vert, notifications, person, refresh, share, shield, waves, wifi_off | `app/src/main/res/drawable/ic_symbol_*.xml` | [google/material-design-icons `symbols/android`](https://github.com/google/material-design-icons/tree/49d4db35df873165d6bd6ba09b063c7dafbac2f4/symbols/android) | — | Apache 2.0, [`MaterialSymbols-LICENSE.txt`](MaterialSymbols-LICENSE.txt) |

The Symbols are copied as published, with one change: the AppCompat attribute `android:tint="?attr/colorControlNormal"` is removed, because Compose tints icons itself. `ic_symbol_flag_filled.xml` is upstream's `flag_fill1_24px.xml`.

The app's own glyphs (`ic_glyph_shore.xml`, `ic_glyph_bottle.xml`, `ic_glyph_seal.xml`, `ic_glyph_wax_seal.xml`) are drawn from `docs/design/boards/TEMPLATE.md` and aren't third-party.

`ic_glyph_google.xml` is Google's "G" mark, also taken from `TEMPLATE.md`. It's Google's trademark, not ours, and appears only inside "Continue with Google", in its own colours, as Google's sign-in branding guidelines ask.
