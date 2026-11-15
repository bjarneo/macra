---
name: Macra
description: A free, private calorie and macro tracker for Android. The data stays on the phone.
colors:
  ground: "#0C0C0D"
  surface: "#18181A"
  surface-2: "#242427"
  bar: "#3A3A3E"
  ink: "#F5F5F2"
  muted: "#9B9B97"
  line: "rgb(255 255 255 / 0.08)"
  action-red: "#FF2E4C"
  action-red-hover: "#FF4D66"
  on-action: "#0C0C0D"
  ring-orange: "#FF7A1A"
  protein-blue: "#88AFFF"
  carbs-green: "#6ED889"
  fat-amber: "#F3BD5C"
  water-cyan: "#59C8E8"
typography:
  display:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "17cqi"
    fontWeight: 800
    lineHeight: 0.9
    letterSpacing: "-0.04em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "clamp(2.9rem, 5.4vw, 5.6rem)"
    fontWeight: 800
    lineHeight: 0.95
    letterSpacing: "-0.04em"
  title:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "clamp(2rem, 3.3vw, 3.2rem)"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.035em"
  lede:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "clamp(1.2rem, 1.6vw, 1.45rem)"
    fontWeight: 400
    lineHeight: 1.45
  body:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 400
    lineHeight: 1.55
    fontFeature: "tnum"
  label:
    fontFamily: "Host Grotesk Variable, system-ui, sans-serif"
    fontSize: "0.95rem"
    fontWeight: 650
    lineHeight: 1.4
  mono:
    fontFamily: "ui-monospace, SF Mono, JetBrains Mono, Menlo, monospace"
    fontSize: "0.88em"
    fontWeight: 400
rounded:
  xs: "6px"
  md: "16px"
  lg: "18px"
  xl: "20px"
  card: "24px"
  frame: "36px"
  pill: "999px"
spacing:
  sm: "12px"
  md: "20px"
  lg: "40px"
  section: "clamp(5rem, 16vh, 9rem)"
components:
  button-primary:
    backgroundColor: "{colors.action-red}"
    textColor: "{colors.on-action}"
    typography: "{typography.label}"
    rounded: "{rounded.lg}"
    padding: "0 24px"
    height: "56px"
  button-primary-hover:
    backgroundColor: "{colors.action-red-hover}"
    textColor: "{colors.on-action}"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "0 24px"
    height: "56px"
  button-outline-hover:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
  nav-link:
    textColor: "{colors.muted}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "8px 14px"
  nav-download:
    backgroundColor: "{colors.action-red}"
    textColor: "{colors.on-action}"
    rounded: "{rounded.pill}"
    padding: "8px 14px"
  code-inline:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.mono}"
    rounded: "{rounded.xs}"
    padding: "0.1em 0.4em"
  phone-frame:
    backgroundColor: "#000000"
    rounded: "{rounded.frame}"
    padding: "7px"
    width: "clamp(240px, 25vw, 370px)"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "{spacing.md}"
  chip:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: "0 14px"
    height: "34px"
  chip-selected:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.ground}"
---

# Design System: Macra

## Overview

**Creative North Star: "The Open Ring"**

Macra is a near-black instrument with one orange ring. The ring is the app icon. It also shows how much of the day is left. Every surface starts from a dark, flat ground. Large, heavy numbers carry the information. Color appears only when it means something: red for the one action, orange for progress, and four macro hues for protein, carbs, fat, and water.

The Android app in `app/src/main/java/app/macra/ui/` is the source of this world. `Theme.kt` holds the tokens, and `Components.kt` holds the controls. The product page in `site/src/pages/index.astro` extends the same world to the web. On the page, the ring grows to poster size and closes as the visitor scrolls to the download.

Density is calm. Surfaces are flat, corners are soft, and type is tight and heavy. The app also defines a light theme in `Theme.kt`. The page uses the dark theme only.

**Key Characteristics:**
- A near-black ground with tonal surface steps and no decorative shadows.
- One orange ring as the signature, at icon size and at poster size.
- One red action per view, with near-black text on the red.
- Host Grotesk at weight 800 with negative tracking for headlines and numbers.
- Tabular numerals everywhere.
- Macro colors only where they mean a macro.

## Colors

The palette is a dark neutral ground with three jobs for color: act, progress, and macro meaning.

### Primary
- **Action Red** `action-red`: The fill of the main action. On the page, this is Download APK and the Download link in the top bar. In the app, this is the accent button, the primary sheet button, and the on state of a toggle. The hover state is `action-red-hover`.

### Secondary
- **Ring Orange** `ring-orange`: The color of the app icon ring. On the page, it draws the ring, the focus outline, the text selection, the link underline on hover, and the step numbers in the install list.

### Tertiary
- **Protein Blue** `protein-blue`: Protein values and the word "protein".
- **Carbs Green** `carbs-green`: Carbohydrate values and the word "carbs".
- **Fat Amber** `fat-amber`: Fat values and the word "fat".
- **Water Cyan** `water-cyan`: Water values in the app. The page does not use it.

### Neutral
- **Ground** `ground`: The page and screen background. It is also the text color on Action Red.
- **Surface** `surface`: Cards, inline code, and the outline button on hover.
- **Surface 2** `surface-2`: Tracks of meters, segmented controls, and step buttons in the app. The scrollbar thumb on the page.
- **Bar** `bar`: Chip borders, outline button borders, and the sheet handle in the app.
- **Ink** `ink`: Headlines, body text, and the first paragraph of each section.
- **Muted** `muted`: Units, metadata, supporting paragraphs, and inactive navigation links.
- **Line** `line`: One-pixel dividers between sections, list rows, and the top bar.

### Named Rules
**The One Red Action Rule.** Action Red fills the main action only. Each view has one red action. Text on Action Red is `on-action`, because `onColor` in `Theme.kt` picks near-black for contrast.

**The Macro Meaning Rule.** Use Protein Blue, Carbs Green, Fat Amber, and Water Cyan only where they mean protein, carbs, fat, and water. Do not use them as decoration.

**The Ring Orange Rule.** Ring Orange marks the ring, progress, and focus. It does not fill a button.

## Typography

**Display Font:** Host Grotesk, with `system-ui` and `sans-serif` as fallbacks.
**Body Font:** Host Grotesk.
**Label/Mono Font:** `ui-monospace` stack for file names and checksums.

**Character:** One variable grotesk does all the work. Heavy weights with tight tracking give the numbers and headlines a dense, solid shape. The body text stays at weight 400 with open line height.

The app and the page use the same weight steps on the variable axis: 400, 500, 600, 650, 700, 750, and 800.

### Hierarchy
- **Display:** Weight 800, `17cqi` of the ring width, line height 0.9, tracking -0.04em. Use it for the kcal number in the center of the ring. The unit below it is weight 600 at `4.4cqi` in Muted.
- **Headline:** Weight 800, `clamp(2.9rem, 5.4vw, 5.6rem)`, line height 0.95, tracking -0.04em. Use it for the one page headline.
- **Title:** Weight 800, `clamp(2rem, 3.3vw, 3.2rem)`, line height 1, tracking -0.035em. Use it for section headings. Headlines and titles use `text-wrap: balance`.
- **Lede:** Weight 400, `clamp(1.2rem, 1.6vw, 1.45rem)`, line height 1.45, maximum 30ch.
- **Body:** Weight 400, 1.125rem, line height 1.55. Text blocks stay at or below 34rem.
- **Label:** Weight 650, 0.95rem. Use it for navigation links and metadata. Buttons use weight 750 at 1.05rem. Fact terms use weight 750 at 1.15rem.

### Named Rules
**The Tabular Numbers Rule.** All numbers use tabular figures. The page sets `font-variant-numeric: tabular-nums` on the body. The app sets `tnum` on every number.

**The Heavy Number Rule.** A key number is weight 800 with negative tracking. Its unit is smaller, weight 600, and Muted. The app component `BigNum` and the ring count follow this rule.

## Layout

On desktop, the page is a two-column grid with equal columns. The left column holds the ring in a sticky stage that is one viewport high. The right column holds the content flow. The ring width is `min(70vh, 40vw)`.

Below 900px, the grid is one column. The ring sits above the headline at `min(76vw, 380px)`. A second ring at `min(62vw, 300px)` appears in the install section, and the top bar shows a 28px ring next to the name.

Sections have vertical padding of `section` and a one-pixel Line at the top. Inside a section, copy and screenshots have a 40px gap. Copy blocks have a 20px gap. Action rows have a 12px gap and wrap. App cards use 20px padding and a 16px gap between children.

## Elevation & Depth

The system is flat. Depth comes from tonal steps: Ground, then Surface, then Surface 2, then Bar. Dividers are one-pixel Line rules. The top bar is Ground at 92% opacity and gets a Line border after the hero leaves the view.

### Shadow Vocabulary
- **Device shadow** `0 0 0 1px rgb(255 255 255 / 0.09), 0 28px 56px -18px rgb(0 0 0 / 0.75)`: Use it only on the frame around an app screenshot.

### Named Rules
**The Flat Surface Rule.** Cards, buttons, and bars have no shadow. The only shadow in the system is under a phone frame that holds a real app screenshot.

## Shapes

Corners are soft and generous. App containers use 20px and 24px radii. Buttons use 16px to 18px, or a full pill. Chips, toggles, and navigation links are pills. Inline code and the focus outline use 6px. The phone frame uses 36px outside and 29px on the screenshot inside.

The ring is the signature form. It is an SVG circle with radius 150 in a 400 viewBox, a stroke of 69, and round caps. The open arc is 292.6 degrees, rotated -30 degrees, so the gap is at the top right. The arc grows to 333.6 degrees, where the round caps meet. A full circle then fades in to close the ring.

## Components

### Buttons
Buttons are tall, solid, and direct.
- **Shape:** Soft corners `rounded.lg`, 56px high on the page.
- **Primary:** Action Red fill, near-black text, weight 750. An optional download icon sits before the text.
- **Hover / Focus:** The fill changes to `action-red-hover` in 0.2s. A press moves the button down 1px. Focus shows a 2px Ring Orange outline with a 3px offset.
- **Outline:** Transparent fill, Ink text, and a 1px border of Ink at 18% opacity. On hover, the border goes to 45% and the fill to Surface.
- **App variants:** `AccentBtn` and `OutlineBtn` are pills 40px to 44px high. `SheetBtn` is 52px high with 16px corners.

### Chips
- **Style:** A pill 34px high with a 1px Bar border and Ink text at weight 650.
- **State:** A selected chip fills with Ink and uses Ground text. The app segmented control `Seg` uses the same inversion on a Surface 2 track.

### Cards / Containers
- **Corner Style:** 24px for `Card`. Grouped lists in the app use 20px.
- **Background:** Surface on Ground.
- **Shadow Strategy:** None. See Elevation & Depth.
- **Border:** None.
- **Internal Padding:** 20px.

### Navigation
The top bar is sticky and 64px high. It shows the name at weight 800. The Source and Download links stay hidden while the hero buttons are on screen. They fade in after the hero leaves. Links are Muted pills that turn Ink on hover. The Download link is an Action Red pill. It hides while the install section is on screen.

### Fact List
A definition list for plain facts. Each row has a one-pixel Line above it, 16px vertical padding, a term at weight 750, and a Muted description. The last row also has a Line below it.

### Install Steps
An ordered list with the step number in Ring Orange at weight 800 in a 2.25rem column. The text sits in the second column.

### Phone Frame
A black frame with 7px padding and 36px corners around a real app screenshot. Two frames side by side offset the second one down by 3.5rem. On small screens, a pair scrolls sideways with scroll snap.

### Closing Ring
The signature component. The ring draws in Ring Orange with the kcal left in its center. On the page, scroll progress drives the arc and the count. The count goes from the daily goal to the demo value, then to zero at the install section. Then the ring closes. The count hides while app screenshots show their own kcal values. With reduced motion, the ring is static and shows the full goal. Transitions use `cubic-bezier(0.16, 1, 0.3, 1)`.

## Do's and Don'ts

### Do:
- **Do** use Ground as the base of every view and step up to Surface and Surface 2 for containers.
- **Do** put one Action Red action in each view, with `on-action` text.
- **Do** set every number in tabular figures, and set key numbers at weight 800 with negative tracking.
- **Do** use Ring Orange for the ring, for progress, and for the focus outline.
- **Do** color a macro word or value only with its own macro color.
- **Do** show real app screenshots in the phone frame, and mark demo data as demo data.
- **Do** draw web icons as inline SVG strokes, 22px, stroke width 2.2, with round caps and round joins.
- **Do** give the ring a static state for reduced motion.

### Don't:
- **Don't** add shadows to cards, buttons, or bars. The device shadow is for phone frames only.
- **Don't** use a macro color as a general accent or for decoration.
- **Don't** fill a button with Ring Orange.
- **Don't** put Ink or white text on Action Red.
- **Don't** add a second accent hue outside the tokens above.
