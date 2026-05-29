@echo off
REM Rebuild and copy Kotlin/JS artifacts
REM Usage: rebuild-kotlin-and-copy.bat [dev]
REM   dev - Use development build (jsBrowserDevelopmentWebpack)
REM
REM Note: h5 module includes base module code, only need to compile h5 module

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set H5APP_JS_DIR=%SCRIPT_DIR%..
set KUIKLY_ROOT=%H5APP_JS_DIR%\..

REM 检查是否使用开发模式
set BUILD_MODE=production
set WEBPACK_TASK=jsBrowserProductionWebpack

if "%1"=="dev" (
    set BUILD_MODE=development
    set WEBPACK_TASK=jsBrowserDevelopmentWebpack
    echo Rebuilding Kotlin/JS modules [DEVELOPMENT mode]...
) else (
    echo Rebuilding Kotlin/JS modules [PRODUCTION mode]...
)
echo.

REM 进入项目根目录
cd /d "%KUIKLY_ROOT%"

echo Step 1/2: Compiling core-render-web:h5 [includes base module]...
call gradlew.bat :core-render-web:h5:clean :core-render-web:h5:%WEBPACK_TASK%

if %errorlevel% neq 0 (
    echo Failed to compile h5 module
    exit /b 1
)

echo.
echo Step 2/2: Copying compiled artifacts...
cd /d "%H5APP_JS_DIR%"
call "%SCRIPT_DIR%copy-kotlin-libs.bat" %1

if %errorlevel% neq 0 (
    echo Failed to copy artifacts
    exit /b 1
)

echo.
echo All done! You can now run 'npm start' to start the dev server.
