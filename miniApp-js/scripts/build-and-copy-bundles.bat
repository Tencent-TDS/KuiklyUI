@echo off
REM ###############################################################################
REM Kuikly H5 Bundle Builder and Copier
REM 
REM 功能：编译指定的 Kuikly 页面并将 bundle 文件复制到 h5App-js 项目
REM
REM 用法：
REM   build-and-copy-bundles.bat [dev] [pageNameList]
REM
REM 参数：
REM   dev          - 可选：传入 dev 则走开发环境；不传默认走生产环境
REM                  - dev:  编译 :demo:packEntryJSBundleDebug，复制 developmentExecutable 产物
REM                  - 默认: 编译 :demo:packEntryJSBundleRelease，复制 productionExecutable 产物
REM   pageNameList - 可选，逗号分隔的页面名称列表（如：router,home,detail）
REM                  如果不指定，则编译所有页面
REM
REM 示例：
REM   build-and-copy-bundles.bat dev HelloWorldPage,000
REM   build-and-copy-bundles.bat HelloWorldPage,000
REM   build-and-copy-bundles.bat
REM
REM ###############################################################################



setlocal enabledelayedexpansion

REM 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set H5APP_JS_DIR=%SCRIPT_DIR%..
set KUIKLY_ROOT=%H5APP_JS_DIR%\..

REM 参数解析：
REM - 允许多次出现 dev（任何位置都视为 mode=dev），避免被误当成页面名
REM - 允许页面名以空格分隔（HelloWorldPage 000）或逗号分隔（HelloWorldPage,000）
set "MODE=prod"
set "PAGE_NAME_LIST="

:parse_args
if "%~1"=="" goto args_parsed

if /I "%~1"=="dev" (
    set "MODE=dev"
    shift
    goto parse_args
)

if defined PAGE_NAME_LIST (
    set "PAGE_NAME_LIST=%PAGE_NAME_LIST%,%~1"
) else (
    set "PAGE_NAME_LIST=%~1"
)
shift
goto parse_args

:args_parsed


echo ========================================
echo   Kuikly Page Bundle Builder
echo ========================================
echo.

REM 显示配置信息
echo Project root: %KUIKLY_ROOT%
echo H5 app dir: %H5APP_JS_DIR%

echo Mode: %MODE%
if "%PAGE_NAME_LIST%"=="" (
    echo Page list: All pages [all]
) else (
    echo Page list: %PAGE_NAME_LIST%
)

echo.

REM 检查项目根目录
if not exist "%KUIKLY_ROOT%\gradlew.bat" (
    echo Error: gradlew.bat not found
    echo Please make sure you are running this script from the correct directory
    exit /b 1
)

REM 步骤 1: 清理构建产物
echo [1/5] Cleaning old build artifacts...
cd /d "%KUIKLY_ROOT%"
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo Clean failed
    exit /b 1
)
echo       Done
echo.

REM 步骤 2: 编译页面 bundle
echo [2/5] Compiling Kuikly page bundles...
cd /d "%KUIKLY_ROOT%"

set "GRADLE_TASK=:demo:packEntryJSBundleRelease"
if /I "%MODE%"=="dev" set "GRADLE_TASK=:demo:packEntryJSBundleDebug"


if "%PAGE_NAME_LIST%"=="" (
    REM 编译所有页面
    echo       Compiling all pages...
    call gradlew.bat %GRADLE_TASK% -PpageNameList=all
) else (
    REM 编译指定页面
    echo       Compiling pages: %PAGE_NAME_LIST%
    call gradlew.bat %GRADLE_TASK% "-PpageNameList=%PAGE_NAME_LIST%"
)


if %errorlevel% neq 0 (
    echo Compilation failed
    exit /b 1
)

echo       Done
echo.

REM 步骤 3: 检查产物
echo [3/5] Checking build artifacts...

set BUNDLE_SOURCE_DIR=%KUIKLY_ROOT%\demo\build\dist\js\productionExecutable
if /I "%MODE%"=="dev" set BUNDLE_SOURCE_DIR=%KUIKLY_ROOT%\demo\build\dist\js\developmentExecutable



if not exist "%BUNDLE_SOURCE_DIR%" (
    echo Error: Build output directory not found
    echo Expected: %BUNDLE_SOURCE_DIR%
    exit /b 1
)

REM 统计 bundle 文件数量
set BUNDLE_COUNT=0
for %%f in ("%BUNDLE_SOURCE_DIR%\*.bundle.js") do set /a BUNDLE_COUNT+=1

if %BUNDLE_COUNT%==0 (
    echo Error: No .bundle.js files found
    exit /b 1
)

echo       Found %BUNDLE_COUNT% bundle file(s)
echo.

REM 步骤 4: 清理目标目录并复制
echo [4/5] Cleaning target directory and copying bundle files...

set TARGET_BUNDLES_DIR=%H5APP_JS_DIR%\src\bundles

REM 创建目标目录
if not exist "%TARGET_BUNDLES_DIR%" mkdir "%TARGET_BUNDLES_DIR%"

REM 清理旧的 bundle 文件
echo       Cleaning old bundle files...
del /q "%TARGET_BUNDLES_DIR%\*.bundle.js" 2>nul
if exist "%TARGET_BUNDLES_DIR%\composeResources" rmdir /s /q "%TARGET_BUNDLES_DIR%\composeResources"
echo       Done

REM 复制 bundle 文件
echo       Copying bundle files...
set COPIED_COUNT=0
for %%f in ("%BUNDLE_SOURCE_DIR%\*.bundle.js") do (
    copy /y "%%f" "%TARGET_BUNDLES_DIR%\" >nul
    echo       Copied: %%~nxf
    set /a COPIED_COUNT+=1
)

REM 复制资源文件（如果存在）
set RESOURCES_SOURCE_DIR=%BUNDLE_SOURCE_DIR%\composeResources
if exist "%RESOURCES_SOURCE_DIR%" (
    set TARGET_RESOURCES_DIR=%TARGET_BUNDLES_DIR%\composeResources
    if not exist "!TARGET_RESOURCES_DIR!" mkdir "!TARGET_RESOURCES_DIR!"
    xcopy /s /y /q "%RESOURCES_SOURCE_DIR%\*" "!TARGET_RESOURCES_DIR!\" >nul 2>nul
    echo       Copied resource files to composeResources/
)

echo.

REM 步骤 5: 生成 manifest.json
echo [5/5] Generating bundle manifest file...

set MANIFEST_GENERATOR=%H5APP_JS_DIR%\scripts\generate-manifest.js

if not exist "%MANIFEST_GENERATOR%" (
    echo Error: Manifest generator script not found
    echo Expected: %MANIFEST_GENERATOR%
    exit /b 1
)

REM 运行 manifest 生成脚本
cd /d "%H5APP_JS_DIR%"
node scripts\generate-manifest.js
if %errorlevel% neq 0 (
    echo Failed to generate manifest.json
    exit /b 1
)
echo       manifest.json generated successfully

echo.
echo ========================================
echo   Done!
echo ========================================
echo.
echo Copied %COPIED_COUNT% bundle file(s) to:
echo    %TARGET_BUNDLES_DIR%
echo Generated manifest.json (contains bundle file list)
echo.
echo Tips:
echo    1. Bundle files are copied to src/bundles/ directory
echo    2. manifest.json is auto-generated for dynamic bundle loading
echo    3. Run 'npm run dev' to start the dev server
echo.
