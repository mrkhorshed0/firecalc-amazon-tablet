#!/usr/bin/env bash
set -e

echo "===================================================="
echo "  Building Signed Android App Bundle (.aab)         "
echo "  Target: Amazon Appstore & Amazon Fire Tablets     "
echo "===================================================="

if [ ! -f "app/firecalc-release.jks" ]; then
    echo "Generating release keystore..."
    keytool -genkeypair -v \
        -keystore app/firecalc-release.jks \
        -alias firecalc_key \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "FireCalc2026!" -keypass "FireCalc2026!" \
        -dname "CN=FireCalc Developer, OU=Mobile, O=FireCalc, L=City, ST=State, C=US"
fi

if command -v ./gradlew &> /dev/null && [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    ./gradlew bundleRelease assembleRelease
elif command -v gradle &> /dev/null; then
    gradle bundleRelease assembleRelease
else
    echo "Error: Gradle is not installed on PATH. Open this folder in Android Studio and select:"
    echo "Build -> Build Bundle(s) / APK(s) -> Build Bundle(s)"
    exit 1
fi

echo ""
echo "SUCCESS! Signed Android App Bundle (.aab) generated at:"
echo "  -> app/build/outputs/bundle/release/app-release.aab"
echo "Signed Universal APK generated at:"
echo "  -> app/build/outputs/apk/release/app-release.apk"
