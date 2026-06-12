@echo off
REM ============================================================
REM  在 Windows 上一鍵打包成 exe
REM  需求：安裝 JDK 17 或以上（含 jpackage），並把 bin 加入 PATH
REM  驗證：開 cmd 輸入  java -version  與  jpackage --version
REM ============================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo [1/4] 清理舊檔...
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist

echo [2/4] 編譯 Java 原始碼...
javac -encoding UTF-8 -d out src\MathKeyboard.java
if errorlevel 1 (
    echo 編譯失敗，請確認已安裝 JDK 並設定好 PATH。
    pause
    exit /b 1
)

echo [3/4] 打包成可執行 JAR...
jar --create --file dist\MathKeyboard.jar --main-class MathKeyboard -C out .
if errorlevel 1 (
    echo 打包 JAR 失敗。
    pause
    exit /b 1
)

echo [4/4] 用 jpackage 產生 exe（app-image，免安裝、解壓即用）...
jpackage ^
  --type app-image ^
  --name MathKeyboard ^
  --input dist ^
  --main-jar MathKeyboard.jar ^
  --main-class MathKeyboard ^
  --dest dist\app ^
  --java-options "-Dfile.encoding=UTF-8"
if errorlevel 1 (
    echo jpackage 失敗，請確認使用的是 JDK 17+（jpackage 內建）。
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  完成！
echo  - 免安裝版 exe：dist\app\MathKeyboard\MathKeyboard.exe
echo  - 可執行 JAR  ：dist\MathKeyboard.jar
echo ============================================================
echo.
echo （可選）若要做成「安裝程式 .exe」，需另外安裝 WiX Toolset，
echo  然後把上面的 --type app-image 改成 --type exe 重跑即可。
echo.
pause
endlocal
