# Macra

Macra is a nutrition tracker for Android. Log food, water, and weight, and compare each day with your calorie and macro targets.

## Features

- Diary with a week strip, four meals, water, and a nutrient summary.
- Food search in a built-in list and in Open Food Facts.
- Barcode scan with the camera.
- Weight log with a 7-day average, and charts for energy, protein, and macros.
- Reminders for meals, water, and weigh-ins.
- Weight sync with Health Connect.
- CSV export of food, water, and weight.
- Dark and light themes, with a choice of accent colors.

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

## Data

- The app stores all of your data on the phone, in one file: `macra.json`.
- The app has no server and no account.
- The built-in food list has 21 generic foods. The values are per 100 g.
- Barcode scans and search use [Open Food Facts](https://world.openfoodfacts.org). The request sends only the barcode or the search text.
- Open Food Facts data is available under the [Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).
