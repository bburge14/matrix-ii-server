#!/bin/bash
cd "$(dirname "$0")"
JAVA=/home/brad/Downloads/jdk8u482-b08/bin/java
CP="bin:data/libs/netty-3.7.0.Final.jar:data/libs/FileStore.jar:data/libs/minifs_v1.jar:data/libs/mysql-connector-java-5.0.8-bin.jar"
exec $JAVA -XX:-OmitStackTraceInFastThrow -server -cp "$CP" com.rs.LoginLauncher true true
