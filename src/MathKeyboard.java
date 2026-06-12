import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
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
            JButton b = new JButton(sym);
            b.setFont(font);
            b.setFocusable(false);                 // 按鈕不可取得焦點
            b.setMargin(new Insets(6, 4, 6, 4));
            b.setToolTipText(desc.isEmpty() ? sym : sym + "　" + desc);  // 滑鼠移上顯示中文說明
            b.addActionListener(e -> insertSymbol(sym));
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

    /** 把符號送到目前焦點所在的輸入框 */
    private static void insertSymbol(String symbol) {
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

        // 放入要插入的符號
        clip.setContents(new StringSelection(symbol), null);

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

    /** 各分類的數學符號（內容對照 symbols.docx）：每筆為 {符號, 中文說明} */
    private static Map<String, String[][]> categories() {
        Map<String, String[][]> m = new LinkedHashMap<>();

        m.put("常用符號", new String[][]{
                {"±", "正負號"}, {"×", "乘號"}, {"÷", "除號"}, {"≠", "不等於"}, {"≒", "約等於"},
                {"√", "根號"}, {"π", "圓周率"}, {"°", "度"}, {"φ", "黃金比例"}
        });

        m.put("比較與邏輯", new String[][]{
                {"≤", "小於等於"}, {"≥", "大於等於"}, {"≡", "恆等於"}, {"∝", "正比於"}, {"∴", "所以"},
                {"∵", "因為"}, {"⇒", "蘊含"}, {"⇔", "若且唯若"}, {"∀", "對所有"}, {"∃", "存在"}
        });

        m.put("集合", new String[][]{
                {"∈", "屬於"}, {"∉", "不屬於"}, {"⊂", "真子集"}, {"⊆", "子集"}, {"∪", "聯集"},
                {"∩", "交集"}, {"∅", "空集合"}, {"ℝ", "實數集"}, {"ℤ", "整數集"}, {"ℕ", "自然數集"}
        });

        m.put("微積分", new String[][]{
                {"∫", "積分"}, {"∬", "二重積分"}, {"∭", "三重積分"}, {"∮", "曲線積分"}, {"∂", "偏微分"},
                {"∇", "梯度"}, {"∑", "求和"}, {"∏", "連乘"}, {"→", "趨近"}, {"∆", "差分"}
        });

        m.put("希臘字母", new String[][]{
                {"α", "alpha"}, {"β", "beta"}, {"γ", "gamma"}, {"δ", "delta"}, {"θ", "theta"},
                {"λ", "lambda"}, {"μ", "mu"}, {"σ", "sigma"}, {"φ", "phi"}, {"ω", "omega"},
                {"Δ", "Delta 大寫"}, {"Σ", "Sigma 大寫"}, {"Ω", "Omega 大寫"}, {"Π", "Pi 大寫"}
        });

        return m;
    }
}
