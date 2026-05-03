@echo off
title Brad's Playground Launcher
cd /d "%~dp0"
setlocal EnableDelayedExpansion

REM ============================================================
REM  Brad's Playground - multi-cache launcher
REM
REM  At startup, prompts for which cache/client to use. The
REM  client folder is selected by the prompt; the SERVER's
REM  Settings.CACHE_PATH_PRIMARY tells the server which game
REM  cache to load.
REM
REM  Folder layout this script expects:
REM
REM    <repo-root>\
REM       Brad's Playground.bat          (this file)
REM       jre\                            (Java runtime - shared)
REM       client_830\                     (client built against 830 cache)
REM           bin\
REM           clientlibs.jar
REM       client_876\                     (client built against 876 cache)
REM           bin\
REM           clientlibs.jar
REM
REM  If you only have one client right now, name its folder
REM  client_830 (default) and pick option 1 at the prompt.
REM
REM  The cache selected here is just bookkeeping for the
REM  client-side launcher - the SERVER decides what cache
REM  to actually load via Settings.CACHE_PATH_PRIMARY.
REM ============================================================

:cache_select
cls
echo.
echo  ===========================================
echo   Brad's Playground - Cache / Client Picker
echo  ===========================================
echo.
echo   1.  830 cache  (client_830)
echo   2.  876 cache  (client_876)
echo   3.  Custom client folder
echo   Q.  Quit
echo.
choice /c 123Q /n /m "Choose: "
set CHOICE=%errorlevel%

if %CHOICE%==1 set CLIENT_DIR=client_830
if %CHOICE%==2 set CLIENT_DIR=client_876
if %CHOICE%==3 goto custom
if %CHOICE%==4 goto end
goto have_client

:custom
echo.
set /p CLIENT_DIR=Enter client folder name (e.g. client_876):
if not exist "%CLIENT_DIR%\bin\" (
    echo.
    echo ERROR: '%CLIENT_DIR%\bin\' does not exist.
    echo.
    pause
    goto cache_select
)

:have_client
if not exist "%CLIENT_DIR%\bin\" (
    echo.
    echo ERROR: client folder '%CLIENT_DIR%\bin\' does not exist.
    echo Make sure you have the matching client built and named correctly.
    echo.
    pause
    goto cache_select
)
if not exist "%CLIENT_DIR%\clientlibs.jar" (
    echo.
    echo WARNING: '%CLIENT_DIR%\clientlibs.jar' not found.
    echo The client may fail to load native libraries.
    echo.
    timeout /t 3 > nul 2>&1
)

echo.
echo Selected client: %CLIENT_DIR%
echo.

:loop
title Brad's Playground (running - %CLIENT_DIR%)
echo.
echo [%TIME%] Starting client from %CLIENT_DIR%...
echo.

REM Resource tuning kept identical to original:
REM   -Xms512m         : larger initial heap = less GC churn during startup
REM   -Xss1m           : per-thread stack 1MB - dozens of threads adds up
REM   MaxGCPauseMillis : target 50ms collection so frames stay smooth
REM   StringDedup      : G1 deduplicates identical strings (cache stores
REM                      lots of duplicate item/object names)
jre\bin\java.exe ^
  -Xms512m -Xmx2048m -Xss1m ^
  -XX:+UseG1GC ^
  -XX:MaxGCPauseMillis=50 ^
  -XX:+UseStringDeduplication ^
  -XX:CompileThreshold=10000 ^
  -Dsun.java2d.noddraw=true ^
  -Dsun.java2d.d3d=false ^
  -Dsun.java2d.opengl=false ^
  -Dsun.net.client.defaultConnectTimeout=10000 ^
  -Dsun.net.client.defaultReadTimeout=20000 ^
  -Dnetworkaddress.cache.ttl=10 ^
  -Dnetworkaddress.cache.negative.ttl=0 ^
  -cp "%CLIENT_DIR%\bin;%CLIENT_DIR%\clientlibs.jar" ^
  game.RS3Applet

set EXITCODE=%errorlevel%
title Brad's Playground (exited - code %EXITCODE%)
echo.
echo [%TIME%] Client exited with code %EXITCODE%.

if "%EXITCODE%"=="0" (
    echo.
    choice /c yns /t 5 /d y /m "Restart same client (Y), pick a different one (S), or quit (N)"
    if errorlevel 3 goto cache_select
    if errorlevel 2 goto end
    echo Restarting...
) else (
    echo Crash detected. Auto-restarting in 7 seconds...
    echo Press Ctrl+C now to abort.
    timeout /t 7 /nobreak > nul 2>&1
)

echo.
goto loop

:end
echo.
echo Goodbye.
timeout /t 2 > nul 2>&1
exit /b 0
