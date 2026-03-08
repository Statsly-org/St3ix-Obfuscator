@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo  St3ix Obfuscator - Test Script
echo ============================================
echo.

echo [1/5] Building obfuscator...
call gradlew.bat dist
if errorlevel 1 goto :error
echo.

echo [2/5] Copying config.yml to build\dist\...
if exist config.yml (
    copy /Y config.yml build\dist\config.yml >nul
    echo   Config copied.
) else (
    echo   WARN: config.yml not found in project root.
    echo   Using default config or existing build\dist\config.yml
)
echo.

echo [3/5] Building example project...
cd "Example\Java Project"
call gradlew.bat build
if errorlevel 1 goto :error
echo.

echo [4/5] Obfuscating (output below)...
echo --------------------------------------------
call gradlew.bat obfuscate
if errorlevel 1 goto :error
echo --------------------------------------------
echo.

echo ============================================
echo  Done.
echo ============================================
goto :eof

:error
echo.
echo ERROR: A step failed.
exit /b 1
