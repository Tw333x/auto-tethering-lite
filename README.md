# Auto WiFi Tethering Lite [![Travis Build Status](https://travis-ci.org/danielmroczka/auto-tethering-lite.png?branch=master)](https://travis-ci.org/danielmroczka/auto-tethering-lite) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/40f928c9b3444e5c9b5035a3e8a6f56e)](https://www.codacy.com/app/daniel-mroczka/auto-tethering-lite?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=danielmroczka/auto-tethering-lite&amp;utm_campaign=Badge_Grade)
![App Logo](https://lh3.googleusercontent.com/GwGTGX5OuwMvOlg10Vemwk6x_Pd8EKsbpy-x9pV-t-EI29FCdFXzUH5PV64b2HRVtNCh=h80)

Android widget allows to turn on/off WiFi tethering. Moreover it allows to start WiFi tethering upon the system boot.
Lite version of https://github.com/danielmroczka/auto-tethering 

Min. required Android version: 2.3 (less features but more devices supported). Rooting phone is not required.

Some of implemented features:
- [x] starts immediately after operation system boot (you don't have to switch on manually tethering and internet connection on your mobile phone)
- [x] android widget 
- [x] restore wifi status when tethering has been turned off

## Usage
Signing apk needs to set credentials in local file gradle.properties (expected location in folder ~/.gradle) and add following settings:
```
RELEASE_STORE_FILE={path to your keystore}
RELEASE_STORE_PASSWORD=*****
RELEASE_KEY_ALIAS=*****
RELEASE_KEY_PASSWORD=*****
```
Build app: gradle build

Example of [gradle.properties](https://gist.github.com/danielmroczka/b93eb61e4583c21da2a3) and [local.properties](https://gist.github.com/danielmroczka/246afe588f1841f6ffef) templates.

More about signing apk files you may find [here](http://developer.android.com/tools/publishing/app-signing.html)

Application is using [OpenCellID API](http://opencellid.org/) and therefore it requires to create account and provide API_KEY.
Once you get a key you need create a file in root folder of project [api.key](https://gist.github.com/danielmroczka/e9eaf9baf821eb5ad180913485018c6d) and add line OPEN_CELL_ID_API_KEY=<your key>
Otherwise application cannot retrieve cellular towers location.

## Google Play app
Built application you may install here: [Auto WiFi Tethering](https://play.google.com/store/apps/details?id=com.labs.dm.auto_tethering_lite)
