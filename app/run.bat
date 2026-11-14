@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
if not exist target\classes mkdir target\classes
javac -encoding UTF-8 -source 17 -target 17 -d target\classes src\main\java\spacare\web\*.java
if errorlevel 1 exit /b 1
xcopy /E /I /Y src\main\resources target\classes >nul
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh -cp "target\classes;lib\h2-2.2.224.jar" spacare.web.SpaCareServer
