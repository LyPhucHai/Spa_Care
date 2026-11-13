#!/bin/sh
set -eu
cd "$(dirname "$0")"
mode=${1:-menu}
case "$mode" in menu|saved|demo|check|test) ;; *) echo 'Dùng: sh run.sh [menu|saved|demo|check|test]' >&2; exit 2;; esac
command -v java >/dev/null 2>&1 || { echo 'Cần JDK 17 trở lên.' >&2; exit 1; }
mkdir -p target/classes
# Dùng Maven nếu có; nếu không có, dùng JDK và H2 có sẵn trong repo.
if command -v mvn >/dev/null 2>&1; then
    mvn -q compile
    h2="${HOME}/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar"
else
    h2='../../02_Booking_Conflict/Java/h2-2.2.224.jar'
    [ -f "$h2" ] || { echo 'Thiếu H2; hãy cài Maven rồi chạy lại.' >&2; exit 1; }
    if command -v javac >/dev/null 2>&1; then
        javac -encoding UTF-8 -source 17 -target 17 -d target/classes src/main/java/spacare/*.java
    else
        java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -d target/classes src/main/java/spacare/*.java
    fi
    cp src/main/resources/*.sql target/classes/
fi
cp_path="target/classes:$h2"
case "$mode" in
    saved) exec java -Dfile.encoding=UTF-8 '-Ddb.url=jdbc:h2:file:./data/spacare' -cp "$cp_path" spacare.SpaCareConsole ;;
    demo) exec java -Dfile.encoding=UTF-8 -cp "$cp_path" spacare.PackageDemo ;;
    check) exec java -Dfile.encoding=UTF-8 -cp "$cp_path" spacare.PricingCheck ;;
    test)
        java -Dfile.encoding=UTF-8 -cp "$cp_path" spacare.PricingCheck
        mkdir -p target/test-classes
        if command -v javac >/dev/null 2>&1; then
            javac -encoding UTF-8 -source 17 -target 17 -cp "$cp_path" -d target/test-classes src/test/java/spacare/*.java
        else
            java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$cp_path" -d target/test-classes src/test/java/spacare/*.java
        fi
        exec java -Dfile.encoding=UTF-8 -cp "target/test-classes:$cp_path" spacare.WorkflowCheck ;;
    menu) exec java -Dfile.encoding=UTF-8 -cp "$cp_path" spacare.SpaCareConsole ;;
esac
