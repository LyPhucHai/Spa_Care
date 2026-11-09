   @echo off
   chcp 65001 >nul
   call mvn -q compile exec:java -Ddemo.main=spacare.BookingMenu