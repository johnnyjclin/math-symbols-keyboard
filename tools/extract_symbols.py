#!/usr/bin/env python3
"""
從 symbols.docx 抽出每個符號的「分頁 / 符號 / 說明 / 字型 / 大小」。

用法：
    python3 tools/extract_symbols.py ~/Downloads/symbols.docx

會先把 docx 解壓到暫存目錄，再解析 word/document.xml，
依【分頁標題】分組印出每個符號，方便對照後填進
src/MathKeyboard.java 的 categories()。

w:sz 是半點，已自動除以 2 換成 pt；缺值預設 12pt。
"""
import sys
import os
import re
import zipfile
import tempfile


def main():
    if len(sys.argv) < 2:
        print("用法: python3 tools/extract_symbols.py <symbols.docx>")
        sys.exit(1)

    docx = os.path.expanduser(sys.argv[1])
    if not os.path.isfile(docx):
        print(f"找不到檔案: {docx}")
        sys.exit(1)

    with tempfile.TemporaryDirectory() as tmp:
        with zipfile.ZipFile(docx) as z:
            z.extractall(tmp)
        xml = open(os.path.join(tmp, "word", "document.xml"), encoding="utf-8").read()

    current_tab = "(未分頁)"
    # 以段落為單位掃描，遇到【...】當作分頁標題，遇到符號 run 就輸出
    for para in re.split(r"</w:p>", xml):
        # 段落純文字（判斷是否為分頁標題）
        texts = re.findall(r"<w:t[^>]*>(.*?)</w:t>", para, re.S)
        joined = "".join(texts).strip()
        m_tab = re.match(r"^【(.+?)】$", joined)
        if m_tab:
            current_tab = m_tab.group(1)
            print(f"\n# 【{current_tab}】")
            continue

        # 段落內的每個 run：抓符號 + 字型 + 大小
        for r in re.findall(r"<w:r\b.*?</w:r>", para, re.S):
            t = "".join(re.findall(r"<w:t[^>]*>(.*?)</w:t>", r, re.S))
            # 只取單一字元的符號；表頭與中文說明（多字）自動略過
            if len(t) != 1 or t in ("符", "號", "說", "明"):
                continue
            # 字型只從 rFonts 取（避免誤抓 w:lang 的 zh-TW）
            rpr = re.search(r"<w:rFonts\b[^/>]*/?>", r)
            f = "Cambria Math"
            if rpr:
                font = re.search(r'w:ascii="([^"]+)"', rpr.group(0))
                ea = re.search(r'w:eastAsia="([^"]+)"', rpr.group(0))
                f = font.group(1) if font else (ea.group(1) if ea else "Cambria Math")
            sz = re.search(r'<w:sz w:val="(\d+)"', r)
            s = int(sz.group(1)) // 2 if sz else 12
            print(f'    {{"{t}", "說明", "{f}", "{s}"}},')


if __name__ == "__main__":
    main()
