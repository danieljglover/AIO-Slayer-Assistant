@echo off
setlocal EnableExtensions DisableDelayedExpansion
title AIO Slayer - Clan Preview
pushd "%~dp0" || goto :folder_error

if not exist "aio-slayer-preview.jar" goto :missing_files
if not exist "lib\" goto :missing_files

set "AIO_JAVA="
if exist "%LOCALAPPDATA%\RuneLite\jre\bin\java.exe" set "AIO_JAVA=%LOCALAPPDATA%\RuneLite\jre\bin\java.exe"
if not defined AIO_JAVA if exist "%ProgramFiles%\RuneLite\jre\bin\java.exe" set "AIO_JAVA=%ProgramFiles%\RuneLite\jre\bin\java.exe"
if not defined AIO_JAVA if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "AIO_JAVA=%JAVA_HOME%\bin\java.exe"
if not defined AIO_JAVA for %%J in (java.exe) do set "AIO_JAVA=%%~$PATH:J"
if not defined AIO_JAVA goto :missing_java

echo Starting AIO Slayer with your own RuneLite settings and Plugin Hub plugins.
echo Keep this console open until you close the preview client.
echo For Jagex Accounts, follow READ-ME-FIRST.txt before logging in.
echo.
"%AIO_JAVA%" -ea -jar "aio-slayer-preview.jar" %*
set "AIO_RESULT=%ERRORLEVEL%"
if "%AIO_RESULT%"=="0" goto :finished
echo.
echo The preview exited with an error. Check the message above.
echo This build needs 64-bit Java 11 or newer. See READ-ME-FIRST.txt.
pause
:finished
popd
exit /b %AIO_RESULT%

:missing_java
echo Java was not found. Install normal 64-bit RuneLite, or install Java 11+.
echo For a standalone Java installer, see:
echo https://adoptium.net/temurin/releases/?version=11
echo Enable the installer's JAVA_HOME or PATH option, then retry.
echo Full instructions are in READ-ME-FIRST.txt.
pause
popd
exit /b 1

:missing_files
echo Extract the entire ZIP first. Keep this script, the JAR and lib together.
echo Do not run the script inside Windows' compressed-folder preview.
pause
popd
exit /b 1

:folder_error
echo Could not open the preview folder. Extract it to a local folder and retry.
pause
exit /b 1
