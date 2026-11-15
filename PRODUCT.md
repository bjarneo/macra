# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

- App: Kotlin and Jetpack Compose, one Android app in `app/`.
- Product page: Astro static site in `site/`. GitHub Actions builds it and deploys it to GitHub Pages. The user chose a static site generator. Astro was the named option.

## Users

People who track calories and macros every day and want a private, free app. They log food at each meal on the phone and check the remaining targets during the day.

## Product Purpose

Macra logs food, water, and weight, and compares each day with calorie and macro targets. The app is free. Success means a user logs a full day fast and trusts the numbers.

## Positioning

Macra has no account, no server, and no ads. All data stays on the phone in one file. Food lookup uses the open Open Food Facts database. The app is free, and the code is under the MIT license.

## Operating Context

- Logging at a meal: text search, barcode scan, or a recent food.
- A daily check of the diary summary: Ledger, Rings, or Blocks.
- A morning weigh-in, with a 7-day average.
- Reminders for meals, water, and weigh-ins.
- Distribution: an APK on GitHub Releases. Macra is not in Google Play.

## Capabilities and Constraints

- Diary with four meals: Breakfast, Lunch, Dinner, and Snacks.
- 21 built-in generic foods, with values per 100 g. The values come from the design prototype and are not checked against a reference database.
- Open Food Facts barcode lookup and text search.
- Camera barcode scan for EAN-13, EAN-8, UPC-A, and UPC-E.
- Health Connect sync for weight only, read and write.
- CSV export of food, water, and weight.
- Android 9 or newer.
- Not yet tested on a real device: the barcode scan and the Health Connect flow.

## Brand Commitments

- Name: Macra.
- App icon: an open ring.
- Voice: ASD-STE100 Simplified Technical English. Short sentences, active voice, no hype.

## Evidence on Hand

- The app itself. Screenshots come from the emulator with synthetic demo data.
- No user counts, reviews, ratings, download numbers, or press exist. Do not invent them.

## Product Principles

1. Data stays on the phone.
2. Logging a meal takes seconds.
3. Each number shows where it comes from.
4. The app is free and needs no account.
