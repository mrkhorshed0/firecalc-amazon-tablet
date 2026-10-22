# FireCalc — Amazon Fire Tablet Calculator App (AAB & APK Ready)

A modern, responsive, zero-dependency scientific and standard calculator designed specifically for **Amazon Fire Tablets** (Fire 7, Fire HD 8, Fire HD 10, and Fire Max 11) and pre-configured to build a signed **Android App Bundle (`.aab`)** for the **Amazon Appstore**.

---

## 1. Pre-Configured Android App Bundle (`.aab`) & Release Signing

This project is pre-configured with:
- **Compliant Package ID (`com.firecalc.tablet`)**: Amazon Appstore strictly prohibits the word `amazon` in third-party package names. Using `com.firecalc.tablet` passes Amazon Developer Console ingestion checks cleanly.
- **Pre-Generated Production Keystore (`app/firecalc-release.jks`)**: A 2048-bit RSA (`SHA256withRSA`) key pair valid for 10,000 days (until March 2054), automatically wired into `app/build.gradle` via `keystore.properties`:
  - **Keystore File**: `app/firecalc-release.jks`
  - **Key Alias**: `firecalc_key`
  - **Store / Key Password**: `FireCalc2026!`
- **Amazon `bundletool` Split Configuration**: Enables density, ABI, and language splits in `app/build.gradle` (`bundle {}` block) and uses zero non-install-time asset packs, ensuring 100% compatibility with Amazon Appstore's `bundletool` (v1.11+) processor.

---

## 2. Three Ways to Build the Signed `.aab` File

Because compiling an `.aab` requires the Android SDK (`aapt2`, `android.jar`) and Maven dependencies from Google's repository, choose any of the three methods below:

### Option A: Zero-Install Cloud Build via GitHub Actions (Fastest — No Local Setup Needed)
1. Create a new repository on GitHub and upload/push this `FireCalc` project folder.
2. Go to the **Actions** tab in your GitHub repository.
3. The included workflow (`.github/workflows/build-aab.yml`) will automatically run on GitHub's cloud servers, install JDK 17 + Gradle 8.2 + Android SDK, and compile the signed `.aab` and `.apk`.
4. Download **`FireCalc-Release-AAB`** (`app-release.aab`) directly from the workflow summary page.

### Option B: Build in Android Studio (GUI)
1. Open Android Studio and select **Open** -> choose the `FireCalc` folder.
2. Wait for Gradle sync to complete.
3. In the top menu, select **Build > Build Bundle(s) / APK(s) > Build Bundle(s)** (or run the `bundleRelease` Gradle task).
4. Grab your signed `.aab` at:
   `app/build/outputs/bundle/release/app-release.aab`

### Option C: One-Click Command Line Script
- **macOS / Linux**:
  ```bash
  ./build_aab.sh
  ```
- **Windows**:
  ```cmd
  build_aab.bat
  ```
Both scripts invoke `gradle bundleRelease assembleRelease` using the pre-configured `firecalc-release.jks` keystore and output:
- Signed App Bundle: `app/build/outputs/bundle/release/app-release.aab`
- Signed Universal APK: `app/build/outputs/apk/release/app-release.apk`

---

## 3. Validating & Testing Your `.aab` for Amazon Appstore

Before uploading to the Amazon Developer Console, you can verify your `.aab` locally with Google/Amazon `bundletool`:

1. **Validate the Bundle Structure**:
   ```bash
   bundletool validate --bundle=app/build/outputs/bundle/release/app-release.aab
   ```
2. **Generate Optimized Fire Tablet APKs Locally**:
   ```bash
   bundletool build-apks \
     --bundle=app/build/outputs/bundle/release/app-release.aab \
     --output=firecalc.apks \
     --mode=persistent \
     --ks=app/firecalc-release.jks \
     --ks-pass=pass:FireCalc2026! \
     --ks-key-alias=firecalc_key \
     --key-pass=pass:FireCalc2026!
   ```
3. **Install onto a Connected Fire Tablet via ADB**:
   ```bash
   bundletool install-apks --apks=firecalc.apks
   ```

---

## 4. Why Fire OS Requires Special Attention (No Google Play Services)

Amazon Fire tablets run **Fire OS** (Fire OS 7 = Android 9 / API 28; Fire OS 8 = Android 11 / API 30) and **do not include Google Play Services (GMS)**:
1. **Zero GMS Dependencies**: Contains zero references to `com.google.android.gms:play-services-*` or GMS-dependent Firebase SDKs, preventing `ClassNotFoundException` and `GooglePlayServicesNotAvailableException` crashes.
2. **Non-Telephony Manifest**: Declares `android.hardware.telephony` and `android.hardware.touchscreen` as `required="false"` so Amazon Appstore device filtering never blocks Wi-Fi-only Fire Tablets.
3. **Safe Hardware Haptics**: Checks `vibrator != null && vibrator.hasVibrator()` before triggering haptic feedback on keypad presses.
