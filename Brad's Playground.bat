@echo off
title Brad's Playground
cd /d "%~dp0"
jre\bin\java.exe -Xms128m -Xmx800m -Xss2m -Dsun.java2d.noddraw=true -XX:CompileThreshold=10000 -cp "client\bin;client\clientlibs.jar" game.RS3Applet
pause
