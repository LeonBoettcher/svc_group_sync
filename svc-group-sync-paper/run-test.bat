@echo off
setlocal

echo ===== Wechsle ins Projektverzeichnis =====
cd /d C:\Users\leonm\IdeaProjects\SimpleVoicechatServerwideGroups\svc-group-sync-paper

echo ===== Starte Gradle Build =====
call gradlew.bat clean
call gradlew.bat jar

if %errorlevel% neq 0 (
    echo ❌ Gradle Build fehlgeschlagen!
    pause
    exit /b %errorlevel%
)

echo ===== Build erfolgreich =====

:: ============================
:: JAR-Dateiname + Source
:: ============================

set JAR_NAME=svc-group-sync-paper-1.0.0.jar
set SOURCE_PATH=%CD%\build\libs\%JAR_NAME%

if not exist "%SOURCE_PATH%" (
    echo ❌ JAR Datei nicht gefunden: %SOURCE_PATH%
    pause
    exit /b 1
)

:: ============================
:: SERVER 1
:: ============================

echo.
echo ===== Deploy Server 1 =====

set DEST_DIR=C:\Users\leonm\Downloads\testerver\server1\plugins
set DEST_PATH=%DEST_DIR%\%JAR_NAME%

if exist "%DEST_PATH%" (
    echo Entferne alte Datei...
    del "%DEST_PATH%"
)

echo Kopiere neue Datei...
copy "%SOURCE_PATH%" "%DEST_DIR%"

if %errorlevel% neq 0 (
    echo ❌ Fehler beim Kopieren nach Server 1!
    pause
    exit /b %errorlevel%
)

echo ✔ Server 1 fertig


:: ============================
:: SERVER 2
:: ============================

echo.
echo ===== Deploy Server 2 =====

set DEST_DIR=C:\Users\leonm\Downloads\testerver\server2\plugins
set DEST_PATH=%DEST_DIR%\%JAR_NAME%

if exist "%DEST_PATH%" (
    echo Entferne alte Datei...
    del "%DEST_PATH%"
)

echo Kopiere neue Datei...
copy "%SOURCE_PATH%" "%DEST_DIR%"

if %errorlevel% neq 0 (
    echo ❌ Fehler beim Kopieren nach Server 2!
    pause
    exit /b %errorlevel%
)

echo ✔ Server 2 fertig


echo.
echo ===== ✅ Deployment abgeschlossen! =====
echo ===== Starte Server =====
cd /d C:\Users\leonm\Downloads\testerver
start "" start.bat
pause
