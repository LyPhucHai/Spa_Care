@echo off
rem Chay khong can Maven. Cach dung:
rem   run_nomaven.bat          menu tuong tac (du lieu trong RAM, thoat la mat)
rem   run_nomaven.bat saved    menu tuong tac, luu ra file data\spacare (chay lai van con)
rem   run_nomaven.bat demo     kich ban demo tu dong
rem   run_nomaven.bat check    kiem tra luat tinh gia (khong can CSDL)
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "JDK=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
if exist "%JDK%\bin\javac.exe" set "PATH=%JDK%\bin;%PATH%"
if not exist lib mkdir lib
if not exist "lib\h2-2.2.224.jar" powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar' -OutFile 'lib\h2-2.2.224.jar'"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -cp "lib\h2-2.2.224.jar" -d out src\main\java\spacare\*.java
if errorlevel 1 exit /b 1
copy /y src\main\resources\*.sql out >nul
set "CP=out;lib\h2-2.2.224.jar"
if /i "%~1"=="demo" goto demo
if /i "%~1"=="check" goto check
if /i "%~1"=="saved" goto saved
java -Dfile.encoding=UTF-8 -cp "%CP%" spacare.SpaCareConsole
goto end
:demo
java -Dfile.encoding=UTF-8 -cp "%CP%" spacare.PackageDemo
goto end
:check
java -Dfile.encoding=UTF-8 -cp "%CP%" spacare.PricingCheck
goto end
:saved
if not exist data mkdir data
java -Dfile.encoding=UTF-8 "-Ddb.url=jdbc:h2:file:./data/spacare" -cp "%CP%" spacare.SpaCareConsole
:end
endlocal
