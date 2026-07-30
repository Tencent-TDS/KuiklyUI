@echo off
REM Copy Kotlin/JS compiled artifacts
REM Usage: copy-kotlin-libs.bat [dev]
REM   dev - Copy from development build directory
REM
REM Note: h5 module includes base module code, only need to copy h5 module

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set H5APP_JS_DIR=%SCRIPT_DIR%..
set KUIKLY_ROOT=%H5APP_JS_DIR%\..

echo Copying Kotlin/JS compiled artifacts...

REM 创建目标目录
if not exist "%H5APP_JS_DIR%\src\libs" mkdir "%H5APP_JS_DIR%\src\libs"

REM 检查是否使用开发模式
if "%1"=="dev" (
    set BUILD_DIR=developmentExecutable
    echo    [using DEVELOPMENT build]
) else (
    set BUILD_DIR=productionExecutable
    echo    [using PRODUCTION build]
)

REM 源文件路径 - 使用 webpack 打包版本（包含所有依赖）
set H5_MODULE=%KUIKLY_ROOT%\core-render-web\h5\build\kotlin-webpack\js\%BUILD_DIR%\KuiklyCore-render-web-h5.js
set H5_DTS=%KUIKLY_ROOT%\core-render-web\h5\build\compileSync\js\main\%BUILD_DIR%\kotlin\KuiklyCore-render-web-h5.d.ts

if not exist "%H5_MODULE%" (
    echo Error: KuiklyCore-render-web-h5.js not found!
    echo    Expected path: %H5_MODULE%
    if "%1"=="dev" (
        echo    Please run: gradlew :core-render-web:h5:jsBrowserDevelopmentWebpack
    ) else (
        echo    Please run: gradlew :core-render-web:h5:jsBrowserProductionWebpack
    )
    exit /b 1
)

REM 复制文件
echo Copying KuiklyCore-render-web-h5.js...
copy /Y "%H5_MODULE%" "%H5APP_JS_DIR%\src\libs\" >nul

REM 复制 TypeScript 声明文件（如果存在）
if exist "%H5_DTS%" (
    echo Copying KuiklyCore-render-web-h5.d.ts...
    copy /Y "%H5_DTS%" "%H5APP_JS_DIR%\src\libs\" >nul
)

echo Done! Kotlin/JS libraries copied to src/libs/
echo.
echo Files copied:
echo    - src/libs/KuiklyCore-render-web-h5.js
if exist "%H5_DTS%" (
    echo    - src/libs/KuiklyCore-render-web-h5.d.ts
)
