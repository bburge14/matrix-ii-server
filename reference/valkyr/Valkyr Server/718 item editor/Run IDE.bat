@echo off
title IDE
@echo Running Item Definitions Editor...
java -client -Xmx512m -cp bin; com.rs.tools.itemsDefsEditor.Application
pause