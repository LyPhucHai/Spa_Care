@echo off
chcp 65001 >nul
if not exist data mkdir data
call mvn -q compile exec:java -Ddb.url=jdbc:h2:file:./data/spacare
