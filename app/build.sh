#!/bin/sh
set -eu
cd "$(dirname "$0")"
command -v java >/dev/null 2>&1 || { echo 'Cần JDK 17 trở lên.' >&2; exit 1; }
mkdir -p target/classes
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -source 17 -target 17 -d target/classes src/main/java/spacare/web/*.java
else
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -d target/classes src/main/java/spacare/web/*.java
fi
cp -R src/main/resources/. target/classes/
