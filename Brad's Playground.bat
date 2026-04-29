@echo off
title Brad's Playground Launcher
cd /d "%~dp0"
setlocal EnableDelayedExpansion

REM ============================================================
REM  Brad's Playground - Client Launcher with restart watchdog
REM
REM  Fixes for:
REM   - Idle blackscreen   : forces pure software rendering (no
REM                          DirectDraw, no D3D, no OpenGL). Slower
REM                          but doesn't blackscreen when idle.
REM   - Server-crash freeze: short network read timeout (20s) so
REM                          dead sockets surface a disconnect
REM                          instead of leaving the client hung.
REM   - Crash recovery     : auto-restart loop. On clean exit,
REM                          prompts to restart (Y in 5s, N to quit).
REM                          On crash (non-zero exit), auto-restarts
REM                          after 7s cooldown.
REM ============================================================

:loop
title Brad's Playground (running)
echo.
echo [%TIME%] Starting client...
echo.

REM Resource tuning:
REM   -Xms512m         : larger initial heap = less GC churn during startup
REM   -Xss1m           : per-thread stack 1MB (was 2MB) - significant savings
REM                      since the client spawns dozens of threads
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
  -cp "client\bin;client\clientlibs.jar" ^
  game.RS3Applet

set EXITCODE=%errorlevel%
title Brad's Playground (exited - code %EXITCODE%)
echo.
echo [%TIME%] Client exited with code %EXITCODE%.

if "%EXITCODE%"=="0" (
    echo.
    choice /c yn /t 5 /d y /m "Restart in 5s (Y/N)"
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
