@echo off
chcp 65001 >nul
rem Menu tuong tac (can Maven). Kich ban demo: run_demo.bat. Luu ra file: run_saved.bat
call mvn -q compile exec:java
