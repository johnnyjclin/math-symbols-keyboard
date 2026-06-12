@echo off
REM 快速測試：直接編譯並執行（不打包），方便開發時試用
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\MathKeyboard.java || (pause & exit /b 1)
java -Dfile.encoding=UTF-8 -cp out MathKeyboard
endlocal
