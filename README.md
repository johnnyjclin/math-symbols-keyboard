# 數學符號鍵盤 (Math Symbols Keyboard)

一個 Windows 桌面小工具：打開是一個浮動鍵盤，分類列出常用數學符號。
**點任一符號，就會把它插入到目前游標所在的輸入框**（任何其它程式：Word、瀏覽器、編輯器都可以）。

## 特色

- 視窗常駐最上層、**點按鈕時不會搶走目標程式的焦點**（游標留在原本的輸入框）。
- 分類：基本、希臘小寫/大寫、微積分、集合/邏輯、箭頭、關係/幾何、字母符號、上下標。
- 純 Java Swing，**零外部依賴**，只需要 JDK。
- 可打包成免安裝 `.exe`。

## 運作原理

插入方式是：把符號放進剪貼簿 → 模擬 `Ctrl+V` 貼到目標程式 → 還原原本剪貼簿內容。
因為視窗設成「不可取得鍵盤焦點」，所以你點符號時，焦點仍停在你原本打字的地方。

---

## 怎麼產生 exe（在 Windows 上做）

> 重要：產生 Windows 原生 `.exe` 必須在 **Windows** 上執行，
> macOS / Linux 無法直接編譯出 Windows exe。

### 1. 安裝 JDK 17 或以上

到 https://adoptium.net 下載 **Temurin JDK 17（或更新）** 並安裝。
安裝時勾選「Add to PATH / Set JAVA_HOME」。

驗證：開啟「命令提示字元 (cmd)」輸入
```
java -version
jpackage --version
```
兩個都有版本號就 OK（`jpackage` 是 JDK 14+ 內建的）。

### 2. 一鍵打包

雙擊 **`build.bat`**（或在 cmd 執行）。完成後產物：

- 免安裝版：`dist\app\MathKeyboard\MathKeyboard.exe` ← **雙擊就能跑**
  （整個 `MathKeyboard` 資料夾複製到任何 Windows 都能用，免裝 Java）
- 可執行 JAR：`dist\MathKeyboard.jar`

### 3.（可選）做成安裝程式 exe

若要一個會「安裝」的 `.exe`，需先裝 [WiX Toolset 3.x](https://wixtoolset.org/)，
然後把 `build.bat` 裡的 `--type app-image` 改成 `--type exe` 再跑一次。

---

## 開發 / 快速測試

雙擊 **`run.bat`** 可直接編譯並執行，方便調整符號或介面。

## 想加 / 改符號？

打開 `src\MathKeyboard.java`，找到 `categories()` 方法，
每一個分類就是一個 `m.put("分類名", new String[]{ ... })`，
把要的符號字串加進陣列即可（每列建議 10 個對齊好看）。

---

## 注意事項

- 因為是用「剪貼簿 + Ctrl+V」插入，極少數不支援標準貼上的程式可能無效。
- 若某些符號顯示成方框，是該電腦缺對應字型；裝 *Segoe UI Symbol*（Windows 內建）通常都正常。
- 防毒軟體偶爾會對自製 exe 警示，這是未簽章程式的常見現象，可自行加白名單。
