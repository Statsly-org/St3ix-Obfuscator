

@echo off
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\javaw.exe"
    if not exist "%JAVA_EXE%" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    if exist "%JAVA_EXE%" goto run
)
set "JAVA_EXE=javaw.exe"
"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
    set "JAVA_EXE=java.exe"
    "%JAVA_EXE%" -version >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Java not found. Set JAVA_HOME or add Java 17+ to PATH.
        pause
        exit /b 1
    )
)

:run
set "JAR_PATH=%~dp0st3ix-obfuscator.jar"
if not exist "%JAR_PATH%" set "JAR_PATH=%~dp0..\build\dist\st3ix-obfuscator.jar"
if not exist "%JAR_PATH%" (
    echo ERROR: JAR not found.
    echo Run "gradlew dist" from project root first.
    pause
    exit /b 1
)
"%JAVA_EXE%" -jar "%JAR_PATH%" %*
