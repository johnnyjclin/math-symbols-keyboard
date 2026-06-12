import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 數學符號鍵盤 (Math Symbols Keyboard)
 *
 * 一個常駐最上層、不搶焦點的浮動鍵盤。點擊任一符號，會把該符號送到
 * 目前焦點所在的輸入位置（任何其它應用程式的輸入框）。
 *
 * 原理：
 *   - 視窗設定為「不可取得鍵盤焦點」(setFocusableWindowState(false))，
 *     所以點按鈕時不會把焦點從目標程式搶走，目標輸入框仍保有游標。
 *   - 點擊時把符號放進剪貼簿，再用 Robot 模擬 Ctrl+V 貼到目標程式，
 *     貼上後再把原本的剪貼簿內容還原。
 */
public class MathKeyboard {

    private static Robot robot;

    public static void main(String[] args) {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null,
                    "無法初始化鍵盤模擬器 (Robot)：" + e.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(MathKeyboard::buildUI);
    }

    private static void buildUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("數學符號鍵盤");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);

        // 關鍵：視窗不取得鍵盤焦點，點按鈕時不會搶走目標程式的游標
        frame.setFocusableWindowState(false);
        frame.setFocusable(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFocusable(false);

        Font symbolFont = pickSymbolFont(20f);

        for (Map.Entry<String, String[][]> cat : categories().entrySet()) {
            tabs.addTab(cat.getKey(), buildPanel(cat.getValue(), symbolFont));
        }

        JLabel hint = new JLabel("點符號即可插入到目前游標位置。視窗會保持最上層、不搶焦點。");
        hint.setBorder(new EmptyBorder(6, 10, 6, 10));
        hint.setForeground(new Color(90, 90, 90));

        frame.setLayout(new BorderLayout());
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(hint, BorderLayout.SOUTH);

        frame.setSize(560, 360);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private static JPanel buildPanel(String[][] items, Font font) {
        JPanel grid = new JPanel(new GridLayout(0, 5, 6, 6));
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));
        grid.setFocusable(false);

        for (String[] it : items) {
            String sym = it[0];
            String desc = it.length > 1 ? it[1] : "";
            String fontFamily = it.length > 2 ? it[2] : "Cambria Math";
            int sizePt = it.length > 3 ? Integer.parseInt(it[3]) : 14;
            JButton b = new JButton(sym);
            b.setFont(font);
            b.setFocusable(false);                 // 按鈕不可取得焦點
            b.setMargin(new Insets(6, 4, 6, 4));
            // 滑鼠移上顯示中文說明 + 將貼上的字型/大小
            b.setToolTipText(sym + "　" + desc + "（" + fontFamily + " " + sizePt + "pt）");
            b.addActionListener(e -> insertSymbol(sym, fontFamily, sizePt));
            grid.add(b);
        }

        JScrollPane sp = new JScrollPane(grid,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * 把符號送到目前焦點所在的輸入框，並帶上指定字型/大小的格式。
     * 作法：把帶 CSS 字型樣式的 HTML 放進剪貼簿，Word / Google Docs 貼上時會套用該字型與大小。
     * 同時也提供純文字版本，貼到不支援格式的純文字欄位時仍能正常插入符號。
     */
    private static void insertSymbol(String symbol, String fontFamily, int sizePt) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

        // 先保存原本的剪貼簿內容（若為文字）
        String previous = null;
        try {
            Transferable t = clip.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                previous = (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception ignored) {
        }

        // 組出帶字型/大小的 HTML；中文字型同時給西文與中文名稱以利對應
        String cssFamily = fontFamily.equals("新細明體") ? "'PMingLiU','新細明體'" : "'" + fontFamily + "'";
        String html = "<html><body><!--StartFragment--><span style=\"font-family:" + cssFamily
                + "; font-size:" + sizePt + "pt;\">" + escapeHtml(symbol)
                + "</span><!--EndFragment--></body></html>";

        // 放入剪貼簿（HTML + 純文字兩種格式）
        clip.setContents(new HtmlAndText(html, symbol), null);

        // 模擬 Ctrl+V 貼到目標程式
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        // 稍後把原本的剪貼簿內容還原，避免覆蓋使用者剪貼簿
        if (previous != null) {
            final String restore = previous;
            Timer timer = new Timer(300, e -> {
                try {
                    clip.setContents(new StringSelection(restore), null);
                } catch (Exception ignored) {
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 同時提供 HTML（帶格式）與純文字兩種剪貼簿格式 */
    private static final class HtmlAndText implements Transferable {
        private static final DataFlavor[] FLAVORS = {
                DataFlavor.allHtmlFlavor,
                DataFlavor.fragmentHtmlFlavor,
                DataFlavor.selectionHtmlFlavor,
                DataFlavor.stringFlavor
        };
        private final String html;
        private final String plain;

        HtmlAndText(String html, String plain) {
            this.html = html;
            this.plain = plain;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (DataFlavor f : FLAVORS) {
                if (f.equals(flavor)) return true;
            }
            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.stringFlavor)) return plain;
            if ("text".equals(flavor.getPrimaryType()) && "html".equals(flavor.getSubType())) return html;
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /** 找一個能完整顯示數學符號的字型 */
    private static Font pickSymbolFont(float size) {
        String[] prefer = {"Segoe UI Symbol", "Segoe UI", "Cambria Math",
                "DejaVu Sans", "Arial Unicode MS", "SansSerif"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(available));
        for (String name : prefer) {
            if (set.contains(name)) {
                return new Font(name, Font.PLAIN, (int) size);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }

    /**
     * 各分類的數學符號（內容/字型/大小皆對照 symbols.docx）。
     * 每筆為 {符號, 中文說明, 字型, 大小pt}。貼上時會帶這個字型與大小。
     */
    private static Map<String, String[][]> categories() {
        Map<String, String[][]> m = new LinkedHashMap<>();

        m.put("常用符號", new String[][]{
                {"±", "正負號", "Times New Roman", "12"}, {"×", "乘號", "Times New Roman", "12"},
                {"÷", "除號", "新細明體", "12"}, {"≠", "不等於", "新細明體", "12"},
                {"≒", "約等於", "新細明體", "12"}, {"√", "根號", "Cambria Math", "14"},
                {"π", "圓周率", "Cambria Math", "14"}, {"°", "度", "Times New Roman", "12"},
                {"φ", "黃金比例", "Cambria Math", "14"}
        });

        m.put("比較與邏輯", new String[][]{
                {"≤", "小於等於", "Times New Roman", "12"}, {"≥", "大於等於", "Times New Roman", "12"},
                {"≡", "恆等於", "Times New Roman", "12"}, {"∝", "正比於", "Cambria Math", "14"},
                {"∴", "所以", "Times New Roman", "12"}, {"∵", "因為", "新細明體", "12"},
                {"⇒", "蘊含", "Cambria Math", "14"}, {"⇔", "若且唯若", "Cambria Math", "14"},
                {"∀", "對所有", "Cambria Math", "14"}, {"∃", "存在", "Cambria Math", "14"}
        });

        m.put("集合", new String[][]{
                {"∈", "屬於", "Cambria Math", "14"}, {"∉", "不屬於", "Cambria Math", "14"},
                {"⊂", "真子集", "Cambria Math", "14"}, {"⊆", "子集", "Cambria Math", "14"},
                {"∪", "聯集", "Cambria Math", "14"}, {"∩", "交集", "Cambria Math", "14"},
                {"∅", "空集合", "Cambria Math", "14"}, {"ℝ", "實數集", "Cambria Math", "14"},
                {"ℤ", "整數集", "Cambria Math", "14"}, {"ℕ", "自然數集", "Cambria Math", "14"}
        });

        m.put("微積分", new String[][]{
                {"∫", "積分", "Cambria Math", "16"}, {"∬", "二重積分", "Cambria Math", "16"},
                {"∭", "三重積分", "Cambria Math", "16"}, {"∮", "曲線積分", "Cambria Math", "16"},
                {"∂", "偏微分", "Cambria Math", "14"}, {"∇", "梯度", "Cambria Math", "14"},
                {"∑", "求和", "Cambria Math", "16"}, {"∏", "連乘", "Cambria Math", "16"},
                {"→", "趨近", "Cambria Math", "14"}, {"∆", "差分", "Cambria Math", "14"}
        });

        m.put("希臘字母", new String[][]{
                {"α", "alpha", "Cambria Math", "14"}, {"β", "beta", "Cambria Math", "14"},
                {"γ", "gamma", "Cambria Math", "14"}, {"δ", "delta", "Cambria Math", "14"},
                {"θ", "theta", "Cambria Math", "14"}, {"λ", "lambda", "Cambria Math", "14"},
                {"μ", "mu", "Cambria Math", "14"}, {"σ", "sigma", "Cambria Math", "14"},
                {"φ", "phi", "Cambria Math", "14"}, {"ω", "omega", "Cambria Math", "14"},
                {"Δ", "Delta 大寫", "Cambria Math", "14"}, {"Σ", "Sigma 大寫", "Cambria Math", "16"},
                {"Ω", "Omega 大寫", "Cambria Math", "14"}, {"Π", "Pi 大寫", "Cambria Math", "16"}
        });

        return m;
    }
}
