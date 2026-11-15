# Macra

Macra is a free calorie and macro tracker for Android. It has no account, no server, and no ads.

Product page: https://bjarneo.github.io/macra/

## Features

- A diary with the kcal and macros left for the day, four meals, water, and other nutrients.
- Food search in 21 built-in foods and in Open Food Facts.
- Barcode scan with the camera.
- A weight log with a 7-day average, and the kcal and macro averages for the last week.
- Reminders for meals, water, and weigh-ins.
- Weight sync with Health Connect.
- CSV export of food, water, and weight.
- Dark and light themes.

## Install

1. Download the APK from the [latest release](https://github.com/bjarneo/macra/releases/latest).
2. Open the file on your phone.
3. Allow the install when Android asks.

The app needs Android 9 or newer.

## Build

The build needs JDK 17 or newer and the Android SDK with platform 36.

```
./gradlew :app:assembleRelease
```

The APK is in `app/build/outputs/apk/release/`.

To sign the release build with your own key, create `keystore.properties` in the project root:

```
storeFile=/path/to/release.jks
storePassword=PASSWORD
keyAlias=ALIAS
keyPassword=PASSWORD
```

Without this file, the release build uses the debug key.

The product page is an Astro site in `site/`. GitHub Actions builds it and deploys it to GitHub Pages. To run it locally:

```
cd site
npm install
npm run dev
```

## Data

- The app stores all of your data on the phone, in one file: `macra.json`.
- The app has no server and no account.
- The built-in food list has 21 generic foods. The values are per 100 g.
- Barcode scans and search use [Open Food Facts](https://world.openfoodfacts.org). The request sends only the barcode or the search text.
- Open Food Facts data is available under the [Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).

## License

Macra is under the [MIT license](LICENSE).

The app includes these fonts:

- Host Grotesk, under the SIL Open Font License 1.1. See `licenses/OFL-HostGrotesk.txt`.
- Material Symbols, under the Apache License 2.0. See `licenses/Apache-2.0-MaterialSymbols.txt`.
