# Board template — values the boards were drawn with

The boards in this folder were drawn on 2026-10-12 as `.dc.html` files for the claude.ai Design canvas. This file maps their literal values to tokens: the Night and Dawn colour tokens, the type scale, shapes and component sizes, the icon SVGs, the bottle rig and the wax seal, plus the sample data every board shares. The product decisions the boards reflect live in [`../decisions.md`](../decisions.md), and it and [`../spec.md`](../spec.md) win wherever a board disagrees with them, except for the board layouts that spec §6 accepts.

## 1. Sample data
- Sample data (use consistently): user "Marc T." handle "@marct", pen name "Driftwood", avatar initials "MT"; stats 12 bottled · 30 found · 9 kept;
  streak 6 days; 1 calm-sea shield; wallets "Rainbow 0x12…9aF" (Receiving) and "MetaMask 0x7c…41E"; Keepsake #142;
  contract `0xB0…77c1`; sealed 12 Oct 2026. Spanish sample message: "Hoy el mar me devolvió algo que creía perdido: la calma."
  → English: "Today the sea gave me back something I thought I'd lost: calm." Other messages: "Hey, that's pretty cool",
  "If you're reading this, drink some water and text someone you miss.", "Leave the porch light on for the people still
  finding their way home."

## 2. Colour tokens (literal hex — use these, invent no greys)
| Token | Night | Dawn |
|---|---|---|
| background | #0D1214 | #F6F4EE |
| dome | #20272A | #D5E4E3 |
| domeGlow | #2E383B | #EEF5F3 |
| surface | #161D20 | #FFFFFF |
| surfaceHigh | #222A2D | #EEF1EC |
| outline | #7E8B85 | #6F7C75 |
| outlineVariant | #343E41 | #CBD3CE |
| onSurface | #E8ECE6 | #1A2320 |
| onSurfaceVariant | #A9B4AE | #4F5B55 |
| primary | #C3CFC6 | #3F5A4C |
| onPrimary | #17201B | #FFFFFF |
| primaryContainer | #2E3A34 | #D6E4DA |
| onPrimaryContainer | #DCE8DF | #10261A |
| glass | #63D3FF | #006688 |
| glassContainer | #0E3A4A | #CBEFFF |
| onGlassContainer | #63D3FF | #006688 |
| moon | #F0E0C4 | #7A5A1E |
| onMoon | #2B241A | #FFFFFF |
| moonContainer | #3A3122 | #F6E8CC |
| onMoonContainer | #F0E0C4 | #4A3510 |
| parchment | #E9DFCB | #FFF9EC |
| ink | #2B241A | #2B241A |
| inkMuted | #645C4F | #6B6459 |
| inkLink | #006688 | #006688 |
| error | #FFB4A8 | #B3261E |
| errorContainer | #5C1A12 | #FBDAD5 |
| onErrorContainer | #FFDAD4 | #410E0B |
| scrim | rgba(0,0,0,0.6) | rgba(26,35,32,0.4) |
| brandSeed (glass fill only, never text) | #00C2FF | #00C2FF |
| bottle tints (illustration only) | seaGlass #00C2FF · amber #E8A93A · moonglass #E9E4D6 | same |
| Google button | fill #131314, 1px #8E918F, text #E3E3E3 | fill #FFFFFF, 1px #747775, text #1F1F1F |

Parchment card: radius 28px, padding 24px. Night: `box-shadow: 0 0 0 1px rgba(240,224,196,0.2), 0 0 24px rgba(99,211,255,0.10)`.
Dawn: `border: 1px solid #E6DCC6; box-shadow: 0 2px 6px rgba(43,36,26,0.12)`.

## 3. Type (Google Fonts link in Main.dc.html loads all three)
- Slab = `'Roboto Slab', Georgia, serif` (voice). Flex = `'Roboto Flex', system-ui, sans-serif` (reading). Mono = `'Roboto Mono', ui-monospace, monospace`.
- headlineMedium: Slab 600 28px/36px · headlineSmall: Slab 600 24px/32px · titleLarge (message ≤120): Slab 700 22px/30px ·
  titleLarge-long: Slab 500 20px/28px · titleMedium / button labels: Slab 600 16px/24px · wordmark: Slab 900 20px/28px ·
  statNumeral: Slab 900 28px/32px (Shore uses Slab 600 18px) · countdown: Slab 600 36px/44px tabular-nums ·
  bodyLarge: Flex 400 16px/24px · bodyMedium: Flex 400 14px/20px · bodySmall: Flex 400 12px/16px ·
  labelLarge: Flex 500 14px/20px · labelSmall overline: Flex 600 11px/16px uppercase letter-spacing 0.8px ·
  mono: Mono 400 13px/18px.

## 4. Shape & components (copy markup from Main.dc.html where it exists)
- Radii: pill 999px (all buttons, chips, streak pill); parchment 28; cards/list groups 24; fields/toasts/banners/sheet rows 16;
  sheet top corners & dialogs 32.
- Screen gutters 16px; content width 358px.
- **Primary pill** 56px tall, width 358, bg primary, text onPrimary, Slab 600 16. **Outlined pill** 1.5px primary border,
  text primary. **Tonal pill** bg primaryContainer, text onPrimaryContainer. **Text button** primary text, 48px tall.
- **Status chip** 32px tall, pill, padding 0 12px 0 8px, gap 6px, 18px icon + labelLarge text:
  Not kept · 5d left (1px outline border / onSurfaceVariant, waves) · Kept (primaryContainer/onPrimaryContainer, chest) ·
  Waiting · 5h (moonContainer/onMoonContainer, hourglass) · Sealing… (glassContainer/onGlassContainer) ·
  Sealed (moon/onMoon, seal) · Sealing failed (errorContainer/onErrorContainer) · Drifting (glassContainer/onGlassContainer,
  waves) · Found (1px primary border/primary) · Under review (errorContainer/onErrorContainer, flag) ·
  Test network (moonContainer/onMoonContainer) · Base (glassContainer/onGlassContainer, link).
  On parchment: chips use ink: `border:1px solid #645C4F; color:#2B241A`.
- **App bar** (immersive screens): 64px row at top 44: 48×48 back button (`aria-label="Back"`, back icon) left; centred
  headlineSmall title optional; optional trailing icon buttons.
- **Bottom sheet**: scrim over the screen; sheet bg surfaceHigh, radius 32px 32px 0 0, padding 24px, a 32×4 drag handle
  (outline colour) centred 12px from top. Sheets anchor to the bottom of the 844 frame.
- **Dialog**: scrim, bg surfaceHigh, radius 32, padding 24, width 312, centred; title headlineSmall; actions right-aligned
  text buttons.
- **Snackbar** (inverse colours): Night bg #E8ECE6, text #1A2320, action text #3F5A4C; Dawn bg #1A2320, text #E8ECE6,
  action #C3CFC6. 16px radius, 48px min height, placed 16px above the nav bar (or 24px above the bottom).
- **Nav bar** (Shore/Chest/Me roots only): copy from Main.dc.html, move the active pill to the right tab.
  Links: Shore → `Main.dc.html`, Chest → `Chest-Found.dc.html`, Me → `Profile.dc.html`.
- **Dome** (CustomBox circle, 844px diameter, `border-radius:50%`, bg dome, left −227px): edge position by `top`:
  full (edge y=506): top −338px · expanded (edge y=590): top −254px · compact (edge y≈296): top −548px ·
  compact-high (edge y≈236): top −608px. Add the moonpath: same circle 2px larger with
  `border:1px solid transparent; border-bottom-color: rgba(240,224,196,0.28)` (Night) / `rgba(0,102,136,0.3)` (Dawn).
- **Moon** (Night): 40px disc `#F0E0C4` opacity .85 with `box-shadow: 0 0 0 10px rgba(240,224,196,0.06), 0 0 48px 12px rgba(240,224,196,0.18)`.
  Dawn: a pale sun-moon `#F6E8CC` with `box-shadow: 0 0 40px 10px rgba(122,90,30,0.12)`.

## 5. SVG library (copy verbatim; all icons 24×24, `fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"`)
Wrap as: `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">PATHS</svg>`
- back: `<path d="M19 12H5"></path><path d="M12 19l-7-7 7-7"></path>`
- close: `<path d="M18 6L6 18"></path><path d="M6 6l12 12"></path>`
- flag: `<path d="M5 21V4"></path><path d="M5 4h11l-2 4 2 4H5"></path>`
- share: `<circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><path d="M8.6 13.5l6.8 4"></path><path d="M15.4 6.5l-6.8 4"></path>`
- translate: `<path d="M4 5h8"></path><path d="M8 3v2"></path><path d="M5.5 5c.8 3 3 5.5 6 6.5"></path><path d="M10.5 5c-.8 3-3 5.5-6 6.5"></path><path d="M13 21l4-9 4 9"></path><path d="M14.5 18h5"></path>`
- waves: `<path d="M2 9c2-2 4-2 6 0s4 2 6 0 4-2 6 0"></path><path d="M2 15c2-2 4-2 6 0s4 2 6 0 4-2 6 0"></path>`
- chest: `<path d="M4 10V8a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v2"></path><rect x="3" y="10" width="18" height="10" rx="2"></rect><path d="M3 13h18"></path><rect x="10" y="11.5" width="4" height="4" rx="1"></rect>`
- shore: `<path d="M4 11l8-7 8 7"></path><path d="M6 9.5V16"></path><path d="M18 9.5V16"></path><path d="M3 20c1.5-1.2 3-1.2 4.5 0s3 1.2 4.5 0 3-1.2 4.5 0 3 1.2 4.5 0"></path>`
- person: `<circle cx="12" cy="8" r="4"></circle><path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"></path>`
- settings (sliders): `<path d="M4 6h9"></path><path d="M17 6h3"></path><circle cx="15" cy="6" r="2"></circle><path d="M4 12h3"></path><path d="M11 12h9"></path><circle cx="9" cy="12" r="2"></circle><path d="M4 18h11"></path><path d="M19 18h1"></path><circle cx="17" cy="18" r="2"></circle>`
- wallet: `<path d="M19 7V5a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4"></path><path d="M3 6v12a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-3"></path><path d="M18 12h4v4h-4a2 2 0 0 1 0-4z"></path>`
- check: `<path d="M5 12l5 5 9-10"></path>`
- link: `<path d="M10 14a4 4 0 0 0 5.66 0l3-3a4 4 0 0 0-5.66-5.66l-1 1"></path><path d="M14 10a4 4 0 0 0-5.66 0l-3 3a4 4 0 0 0 5.66 5.66l1-1"></path>`
- bell: `<path d="M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z"></path><path d="M10 20a2 2 0 0 0 4 0"></path>`
- hourglass: `<path d="M7 3h10"></path><path d="M7 21h10"></path><path d="M8 3c0 5 8 6 8 9s-8 4-8 9"></path><path d="M16 3c0 5-8 6-8 9s8 4 8 9"></path>`
- copy: `<rect x="9" y="9" width="11" height="11" rx="2"></rect><path d="M5 15V5a2 2 0 0 1 2-2h8"></path>`
- external: `<path d="M14 4h6v6"></path><path d="M20 4l-9 9"></path><path d="M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"></path>`
- shield: `<path d="M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6z"></path>`
- sparkle (AI helper): `<path d="M12 3l1.8 4.6L18 9.5l-4.2 1.9L12 16l-1.8-4.6L6 9.5l4.2-1.9z"></path><path d="M19 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z"></path>`
- refresh: `<path d="M20 11a8 8 0 1 0-2.3 5.7"></path><path d="M20 4v7h-7"></path>`
- wifiOff: `<path d="M2 2l20 20"></path><path d="M8.5 16.5a5 5 0 0 1 7 0"></path><path d="M5 12.5a10 10 0 0 1 5-2.8"></path><path d="M19 12.5a10 10 0 0 0-2.2-1.6"></path><path d="M12 20h.01"></path>`
- bottle (small glyph): `<path d="M10 2h4"></path><path d="M10.5 2v4c0 1-3.5 2-3.5 5v9a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-9c0-3-3.5-4-3.5-5V2"></path><path d="M9.5 14h5"></path>`
- chevronRight: `<path d="M9 6l6 6-6 6"></path>`
- more: `<path d="M12 5h.01"></path><path d="M12 12h.01"></path><path d="M12 19h.01"></path>` (use stroke-width="3")
- moon: `<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"></path>`
- block: `<circle cx="12" cy="12" r="9"></circle><path d="M5.6 5.6l12.8 12.8"></path>`
- download: `<path d="M12 3v12"></path><path d="M7 10l5 5 5-5"></path><path d="M5 21h14"></path>`
- clock: `<circle cx="12" cy="12" r="9"></circle><path d="M12 7v5l3 2"></path>`
- qr: `<rect x="3" y="3" width="7" height="7" rx="1"></rect><rect x="14" y="3" width="7" height="7" rx="1"></rect><rect x="3" y="14" width="7" height="7" rx="1"></rect><path d="M14 14h3v3h-3z"></path><path d="M21 14v.01"></path><path d="M21 21h-4v-4"></path>`
- trophy: `<path d="M8 4h8v5a4 4 0 0 1-8 0z"></path><path d="M8 6H5a3 3 0 0 0 3 4"></path><path d="M16 6h3a3 3 0 0 1-3 4"></path><path d="M12 13v4"></path><path d="M8 21h8"></path><path d="M9 17h6v4H9z"></path>`
- trash: `<path d="M4 7h16"></path><path d="M10 11v6"></path><path d="M14 11v6"></path><path d="M6 7l1 13h10l1-13"></path><path d="M9 7V4h6v3"></path>`
- logout: `<path d="M15 4h4v16h-4"></path><path d="M10 17l5-5-5-5"></path><path d="M15 12H3"></path>`
- lock: `<rect x="5" y="11" width="14" height="10" rx="2"></rect><path d="M8 11V8a4 4 0 0 1 8 0v3"></path>`
- info: `<circle cx="12" cy="12" r="9"></circle><path d="M12 11v5"></path><path d="M12 8h.01"></path>`
- seal (small glyph for chips): `<circle cx="12" cy="12" r="8"></circle><path d="M8.5 12l2.5 2.5 4.5-5"></path>`
- shake: `<rect x="8" y="3" width="8" height="18" rx="2"></rect><path d="M4 8l-2 4 2 4"></path><path d="M20 8l2 4-2 4"></path>`
- globe: `<circle cx="12" cy="12" r="9"></circle><path d="M3 12h18"></path><path d="M12 3a14 14 0 0 1 0 18"></path><path d="M12 3a14 14 0 0 0 0 18"></path>`

### Bottle rig (illustration — one `<svg>` is fine). Glass body + scroll + cork. Tint via the body fill.
Classic (viewBox 0 0 140 220):
```
<svg width="120" height="188" viewBox="0 0 140 220" aria-hidden="true">
<rect x="52" y="104" width="36" height="84" rx="9" fill="#F0E0D0"></rect>
<rect x="52" y="104" width="36" height="12" rx="6" fill="#E0C0A0"></rect>
<rect x="52" y="176" width="36" height="12" rx="6" fill="#E0C0A0"></rect>
<rect x="52" y="140" width="36" height="6" fill="#E8A93A"></rect>
<path d="M60 34H80V64C80 76 112 82 112 106V190C112 204 102 212 88 212H52C38 212 28 204 28 190V106C28 82 60 76 60 64Z" fill="#00C2FF" fill-opacity="0.42" stroke="#63D3FF" stroke-width="2"></path>
<path d="M40 112C37 140 37 168 41 194" fill="none" stroke="#FFFFFF" stroke-opacity="0.75" stroke-width="5" stroke-linecap="round"></path>
<path d="M66 40V60" fill="none" stroke="#FFFFFF" stroke-opacity="0.6" stroke-width="3" stroke-linecap="round"></path>
<circle cx="96" cy="120" r="3" fill="#FFFFFF" fill-opacity="0.7"></circle>
<rect x="56" y="30" width="28" height="8" rx="4" fill="#63D3FF" fill-opacity="0.85"></rect>
<path d="M60 8H80L78 32H62Z" fill="#C9A27A"></path>
<path d="M63 15H77M63 22H77" stroke="#A9825A" stroke-width="2" stroke-linecap="round"></path>
</svg>
```
Flask body path (replace the body `<path d>`; move the highlight to x≈36): `M60 34H80V70H106C111 70 114 73 114 78V196C114 205 108 212 99 212H41C32 212 26 205 26 196V78C26 73 29 70 34 70H60Z`
Gourd body path: `M60 34H80V58C96 62 100 76 92 90C116 100 118 130 114 160C110 198 92 212 70 212C48 212 30 198 26 160C22 130 24 100 48 90C40 76 44 62 60 58Z`
Tints: amber body `fill="#E8A93A"` + stroke `#F0C987`; moonglass body `fill="#E9E4D6"` + stroke `#FFFFFF`.
Uncorked variant: omit the two cork elements. Scroll out: draw the scroll group outside the neck, rotated.
Water/ripples under a floating bottle:
```
<svg width="240" height="40" viewBox="0 0 240 40" aria-hidden="true"><ellipse cx="120" cy="20" rx="110" ry="12" fill="none" stroke="#63D3FF" stroke-opacity="0.25" stroke-width="2"></ellipse><ellipse cx="120" cy="20" rx="70" ry="7" fill="none" stroke="#63D3FF" stroke-opacity="0.45" stroke-width="2"></ellipse><ellipse cx="120" cy="20" rx="34" ry="4" fill="#63D3FF" fill-opacity="0.18"></ellipse></svg>
```

### Wax seal (the ONE visual that means on-chain). 64×64, moon fill, embossed bottle, check badge = verified writer.
```
<svg width="64" height="64" viewBox="0 0 64 64" role="img" aria-label="Wax seal: sealed on Base by a verified account">
<path d="M 32.00 4.00 A 6.73 6.73 0 0 1 44.15 6.77 A 6.73 6.73 0 0 1 53.89 14.54 A 6.73 6.73 0 0 1 59.30 25.77 A 6.73 6.73 0 0 1 59.30 38.23 A 6.73 6.73 0 0 1 53.89 49.46 A 6.73 6.73 0 0 1 44.15 57.23 A 6.73 6.73 0 0 1 32.00 60.00 A 6.73 6.73 0 0 1 19.85 57.23 A 6.73 6.73 0 0 1 10.11 49.46 A 6.73 6.73 0 0 1 4.70 38.23 A 6.73 6.73 0 0 1 4.70 25.77 A 6.73 6.73 0 0 1 10.11 14.54 A 6.73 6.73 0 0 1 19.85 6.77 A 6.73 6.73 0 0 1 32.00 4.00 Z" fill="#F0E0C4"></path>
<circle cx="32" cy="32" r="19" fill="none" stroke="#C9B48E" stroke-width="2"></circle>
<path d="M29.5 20h5M30 20v4c0 1-4 2-4 6v9a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-9c0-4-4-5-4-6v-4" fill="none" stroke="#8C7350" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
<circle cx="48" cy="48" r="9" fill="#2B241A"></circle>
<path d="M44 48l3 3 5-6" fill="none" stroke="#F0E0C4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
</svg>
```
Scale it with width/height (e.g. 96 or 120). Without the check badge = sealed but writer unverified (don't use).

### Google "G" (only inside the Continue with Google button, 20×20)
```
<svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path></svg>
```
