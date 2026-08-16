@echo off
title Brad's Playground Launcher
cd /d "%~dp0"
setlocal EnableDelayedExpansion

REM ============================================================
REM  Brad's Playground - Client Launcher
REM
REM   - Idle blackscreen   : forces pure software rendering (no
REM                          DirectDraw, no D3D, no OpenGL).
REM   - Server-crash freeze: short network read timeout (20s)
REM                          surfaces dead sockets fast.
REM   - Clean exit         : if the user closes the client, the
REM                          launcher exits. No prompt.
REM   - Crash recovery     : ONLY auto-restarts on non-zero exit
REM                          (actual JVM crash), after 7s cooldown.
REM ============================================================

:loop
title Brad's Playground (running)
echo.
echo [%TIME%] Starting client...
echo.

jre\bin\java.exe ^
  -Xms256m -Xmx2048m -Xss2m ^
  -XX:+UseG1GC ^
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

REM Clean exit (user closed the client) -> quit the launcher.
if "%EXITCODE%"=="0" goto end

REM Non-zero exit = crash. Auto-restart after cooldown.
echo Crash detected. Auto-restarting in 7 seconds...
echo Press Ctrl+C now to abort.
timeout /t 7 /nobreak > nul 2>&1
echo.
goto loop

:end
echo.
echo Client closed. Launcher exiting.
timeout /t 2 > nul 2>&1
exit /b 0
