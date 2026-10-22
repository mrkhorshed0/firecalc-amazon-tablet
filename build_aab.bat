@echo off
echo ====================================================
echo   Building Signed Android App Bundle (.aab)
echo   Target: Amazon Appstore ^& Amazon Fire Tablets
echo ====================================================

if not exist "app\firecalc-release.jks" (
    echo Generating release keystore...
    keytool -genkeypair -v -keystore app\firecalc-release.jks -alias firecalc_key -keyalg RSA -keysize 2048 -validity 10000 -storepass "FireCalc2026!" -keypass "FireCalc2026!" -dname "CN=FireCalc Developer, OU=Mobile, O=FireCalc, L=City, ST=State, C=US"
)

if exist "gradle\wrapper\gradle-wrapper.jar" (
    call gradlew.bat bundleRelease assembleRelease
) else (
    call gradle bundleRelease assembleRelease
)

echo.
echo SUCCESS! Signed Android App Bundle (.aab) generated at:
echo   -^> app\build\outputs\bundle\release\app-release.aab
pause
