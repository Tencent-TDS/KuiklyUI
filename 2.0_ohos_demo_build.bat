@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Kuikly Ohos Demo Build Script (Windows)
echo ========================================

:: 0. Check and set HarmonyOS SDK environment variables
:: First, try to resolve DEVECO_SDK_HOME if it contains variable references
if defined DEVECO_SDK_HOME (
    :: Expand any nested variables in DEVECO_SDK_HOME
    for /f "delims=" %%A in ('echo %DEVECO_SDK_HOME%') do set "RESOLVED_SDK_HOME=%%A"
    echo [Step 0] Using DEVECO_SDK_HOME: !RESOLVED_SDK_HOME!
) else if defined TOOL_HOME (
    :: Fallback: use TOOL_HOME directly
    set "RESOLVED_SDK_HOME=%TOOL_HOME%\sdk"
    echo [Step 0] DEVECO_SDK_HOME not set, using TOOL_HOME: !RESOLVED_SDK_HOME!
) else (
    echo [Error] Neither DEVECO_SDK_HOME nor TOOL_HOME environment variable is set!
    echo Please set DEVECO_SDK_HOME or TOOL_HOME in your system environment variables
    pause
    exit /b 1
)

:: Set OHOS_SDK_HOME for Kotlin Native
:: Try to find OpenHarmony SDK location
if exist "!RESOLVED_SDK_HOME!\default\openharmony" (
    set "OHOS_SDK_HOME=!RESOLVED_SDK_HOME!\default\openharmony"
) else if exist "!RESOLVED_SDK_HOME!\openharmony" (
    set "OHOS_SDK_HOME=!RESOLVED_SDK_HOME!\openharmony"
) else (
    echo [Error] OpenHarmony SDK not found!
    echo Expected location: !RESOLVED_SDK_HOME!\default\openharmony
    echo Actual resolved path: !RESOLVED_SDK_HOME!
    pause
    exit /b 1
)
echo [Step 0] Using OHOS_SDK_HOME: !OHOS_SDK_HOME!

:: 1. Record original gradle url
echo [Step 1] Backup original gradle version...
for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" gradle\wrapper\gradle-wrapper.properties') do (
    set "ORIGIN_DISTRIBUTION_URL=%%a"
)
echo Origin gradle url: %ORIGIN_DISTRIBUTION_URL%

:: 2. Backup original file
copy gradle\wrapper\gradle-wrapper.properties gradle\wrapper\gradle-wrapper.properties.bak >nul

:: 3. Switch gradle version
echo [Step 2] Switch gradle version to 8.0...
powershell -Command "(Get-Content gradle\wrapper\gradle-wrapper.properties) -replace 'distributionUrl=.*', 'distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip' | Set-Content gradle\wrapper\gradle-wrapper.properties"

:: 4. Start build
echo [Step 3] Building ohos artifact...
set KUIKLY_AGP_VERSION=7.4.2
set KUIKLY_KOTLIN_VERSION=2.0.21-KBA-010
:: Note: Do NOT set KONAN_DATA_DIR here, let Gradle auto-download Kotlin Native toolchain to default location (~/.konan)
:: If you want to use a custom location, ensure the toolchain is already installed there
call gradlew.bat -c settings.2.0.ohos.gradle.kts :demo:linkSharedDebugSharedOhosArm64 --stacktrace

if %ERRORLEVEL% neq 0 (
    echo [Error] Build failed!
    goto :restore
)

echo [Step 4] Build succeeded!

:: 5. Restore gradle config
:restore
echo [Step 5] Restore gradle version...
if exist "gradle\wrapper\gradle-wrapper.properties.bak" (
    move /y "gradle\wrapper\gradle-wrapper.properties.bak" "gradle\wrapper\gradle-wrapper.properties" >nul
    if %ERRORLEVEL% neq 0 (
        echo [Warning] Failed to restore gradle properties, trying alternative method...
        copy /y "gradle\wrapper\gradle-wrapper.properties.bak" "gradle\wrapper\gradle-wrapper.properties" >nul
        del "gradle\wrapper\gradle-wrapper.properties.bak" >nul
    )
) else (
    echo [Warning] Backup file not found at gradle\wrapper\gradle-wrapper.properties.bak
)

:: 6. Copy so file
echo [Step 6] Copying artifact files...

set OHOS_RENDER_PROJECT_DIR=ohosApp
set TARGET_SO_PATH=demo\build\bin\ohosArm64\sharedDebugShared\libshared.so
set OHO_SO_PROJECT_PATH=%OHOS_RENDER_PROJECT_DIR%\entry\libs\arm64-v8a

if not exist "%OHO_SO_PROJECT_PATH%" (
    mkdir "%OHO_SO_PROJECT_PATH%"
)

if exist "%TARGET_SO_PATH%" (
    copy /y "%TARGET_SO_PATH%" "%OHO_SO_PROJECT_PATH%\" >nul
    echo libshared.so: copied to %OHO_SO_PROJECT_PATH%
) else (
    echo [Warning] libshared.so not found at %TARGET_SO_PATH%
)

:: 7. Copy header file
set TARGET_SO_HEADER_PATH=demo\build\bin\ohosArm64\sharedDebugShared\libshared_api.h
set OHO_SO_HEADER_PATH=%OHOS_RENDER_PROJECT_DIR%\entry\src\main\cpp\thirdparty\biz_entry

if not exist "%OHO_SO_HEADER_PATH%" (
    mkdir "%OHO_SO_HEADER_PATH%"
)

if exist "%TARGET_SO_HEADER_PATH%" (
    copy /y "%TARGET_SO_HEADER_PATH%" "%OHO_SO_HEADER_PATH%\" >nul
    echo libshared_api.h: copied to %OHO_SO_HEADER_PATH%
) else (
    echo [Warning] libshared_api.h not found at %TARGET_SO_HEADER_PATH%
)

echo ========================================
echo Build completed!
echo Now open ohosApp in DevEco Studio to run
echo ========================================

endlocal
pause
