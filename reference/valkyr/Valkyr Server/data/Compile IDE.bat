@echo off
echo Compiling IDE
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/io/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/loaders/images/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/loaders/items/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/store/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/tools/itemsDefsEditor/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/util/bzip2/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/util/crc32/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/util/gzip/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/util/whirlpool/*.java
"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/com/alex/utils/*.java

"C:\Program Files (x86)\Java\jdk1.7.0_21/bin/javac.exe" -d bin -sourcepath src src/org/apache/tools/bzip2/*.java
@echo Finished.
pause