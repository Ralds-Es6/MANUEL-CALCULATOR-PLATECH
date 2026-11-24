import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Single-file Cyber / Developer Main Scientific Calculator (Java 17+).
 * Run directly without generating .class files:
 *     java MainCalculator.java
 */
public class MainCalculator extends JFrame {

    private static final int WIN_W = 468, WIN_H = 696;
    private static final Color CYAN_BRIGHT = new Color(36, 236, 255);
    private static final Color CYAN_SOFT = new Color(138, 226, 238);
    private static final Color BG_DEEP = new Color(5, 13, 20);

    private static final String[][] KEY_ROWS = {
        {"DEG", "sin", "cos", "tan", "DEL"},
        {"x²",  "xʸ",  "√",   "ln",  "log"},
        {"AC",  "( )", "%",   "π",   "÷"},
        {"7",   "8",   "9",   "e",   "×"},
        {"4",   "5",   "6",   "n!",  "−"},
        {"1",   "2",   "3",   "±",   "+"},
        {"0",   ".",   "ANS", "="}
    };

    private final StringBuilder expression = new StringBuilder(64);
    private final JLabel expressionLabel;
    private final GlowValueLabel resultLabel;
    private CyberButton angleModeButton;

    private String lastResult = "0";
    private double lastAnswer = 0.0;
    private boolean degreeMode = true;
    private boolean justEvaluated = false;
    private Point dragOffset;

    public MainCalculator() {
        super("Main Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);
        setSize(WIN_W, WIN_H);
        setLocationRelativeTo(null);

        setBackground(BG_DEEP);
        try {
            setShape(new RoundRectangle2D.Double(4, 4, WIN_W - 8, WIN_H - 8, 36, 36));
        } catch (UnsupportedOperationException ignored) { }

        expressionLabel = new JLabel(" ");
        expressionLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        expressionLabel.setForeground(CYAN_SOFT);
        expressionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        resultLabel = new GlowValueLabel("0");
        resultLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        resultLabel.setPreferredSize(new Dimension(WIN_W - 56, 64));
        resultLabel.setMaximumSize(new Dimension(WIN_W - 56, 64));

        CyberFramePanel root = new CyberFramePanel(WIN_W, WIN_H);
        root.setLayout(new BorderLayout(0, 8));
        root.setBorder(new EmptyBorder(18, 18, 22, 18));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        setContentPane(root);

        installKeyboard();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 8, 4, 8));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        MouseAdapter dragger = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point p = e.getLocationOnScreen();
                    setLocation(p.x - dragOffset.x, p.y - dragOffset.y);
                }
            }
        };
        header.addMouseListener(dragger);
        header.addMouseMotionListener(dragger);

        JLabel title = new JLabel("Calculator");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(CYAN_BRIGHT);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel buildCenter() {
        JPanel display = new JPanel();
        display.setOpaque(false);
        display.setLayout(new BoxLayout(display, BoxLayout.Y_AXIS));
        display.setBorder(new EmptyBorder(12, 10, 10, 12));
        display.add(expressionLabel);
        display.add(Box.createVerticalStrut(4));
        display.add(resultLabel);

        JPanel keypad = new JPanel(new GridBagLayout());
        keypad.setOpaque(false);
        keypad.setBorder(new EmptyBorder(4, 2, 2, 2));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(2, 2, 2, 2);

        for (int r = 0; r < KEY_ROWS.length; r++) {
            int col = 0;
            for (String label : KEY_ROWS[r]) {
                int span = "0".equals(label) ? 2 : 1;
                KeyStyle style;
                if ("=".equals(label)) {
                    style = KeyStyle.EQUALS;
                } else if (r <= 2 || col == 4 || "e".equals(label) || "n!".equals(label)
                        || "±".equals(label) || "ANS".equals(label)) {
                    style = KeyStyle.OPERATOR;
                } else {
                    style = KeyStyle.NUMBER;
                }

                CyberButton btn = new CyberButton(label, style);
                if ("DEG".equals(label)) {
                    angleModeButton = btn;
                }
                final String key = label;
                btn.addActionListener(e -> handleKey(key));

                gbc.gridx = col;
                gbc.gridy = r;
                gbc.gridwidth = span;
                keypad.add(btn, gbc);
                col += span;
            }
        }

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(display, BorderLayout.NORTH);
        center.add(keypad, BorderLayout.CENTER);
        return center;
    }

    // ==================== Calculator Logic ====================

    private void handleKey(String key) {
        if ("AC".equals(key)) {
            clear();
        } else if ("DEL".equals(key)) {
            backspace();
        } else if ("DEG".equals(key)) {
            degreeMode = !degreeMode;
            angleModeButton.setText(degreeMode ? "DEG" : "RAD");
            updateLiveDisplay();
        } else if ("( )".equals(key)) {
            toggleParen();
        } else if (".".equals(key)) {
            appendDecimal();
        } else if ("=".equals(key)) {
            evaluate();
        } else if ("sin".equals(key) || "cos".equals(key) || "tan".equals(key)
                || "ln".equals(key) || "log".equals(key)) {
            appendFunction(key + "(");
        } else if ("√".equals(key)) {
            appendFunction("√(");
        } else if ("x²".equals(key)) {
            appendSquare();
        } else if ("xʸ".equals(key)) {
            appendOperator('^');
        } else if ("n!".equals(key)) {
            appendOperator('!');
        } else if ("π".equals(key) || "e".equals(key)) {
            appendConstant(key);
        } else if ("ANS".equals(key)) {
            appendAnswer();
        } else if ("±".equals(key)) {
            toggleSign();
        } else if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
            appendDigit(key.charAt(0));
        } else if (key.length() == 1) {
            appendOperator(key.charAt(0));
        }
    }

    private void resetIfEvaluated() {
        if (justEvaluated) {
            expression.setLength(0);
            expressionLabel.setText(" ");
            justEvaluated = false;
        }
    }

    private void clear() {
        expression.setLength(0);
        lastResult = "0";
        justEvaluated = false;
        expressionLabel.setText(" ");
        resultLabel.setText("0");
    }

    private void backspace() {
        if (justEvaluated) {
            clear();
            return;
        }
        int len = expression.length();
        if (len == 0) return;

        // Delete whole function token like "sin(" or "log(" in one press
        String[] funcs = {"sin(", "cos(", "tan(", "log(", "ln(", "√("};
        String current = expression.toString();
        for (String fn : funcs) {
            if (current.endsWith(fn)) {
                expression.setLength(len - fn.length());
                updateLiveDisplay();
                return;
            }
        }
        expression.deleteCharAt(len - 1);
        updateLiveDisplay();
    }

    private void appendDigit(char d) {
        resetIfEvaluated();
        expression.append(d);
        updateLiveDisplay();
    }

    private void appendConstant(String constant) {
        resetIfEvaluated();
        expression.append(constant);
        updateLiveDisplay();
    }

    private void appendFunction(String fn) {
        resetIfEvaluated();
        expression.append(fn);
        updateLiveDisplay();
    }

    private void appendAnswer() {
        resetIfEvaluated();
        String ansStr = Evaluator.formatRaw(lastAnswer);
        expression.append(ansStr);
        updateLiveDisplay();
    }

    private void appendSquare() {
        if (justEvaluated) {
            expression.setLength(0);
            if (!"Error".equals(lastResult)) {
                expression.append(lastResult.replace(",", ""));
            }
            justEvaluated = false;
        }
        if (expression.length() == 0) return;
        char last = expression.charAt(expression.length() - 1);
        if (Character.isDigit(last) || last == ')' || last == 'π' || last == 'e') {
            expression.append("^2");
            updateLiveDisplay();
        }
    }

    private void toggleSign() {
        if (justEvaluated) {
            if (!"Error".equals(lastResult)) {
                String clean = lastResult.replace(",", "");
                expression.setLength(0);
                expression.append(clean.startsWith("-") ? clean.substring(1) : "−" + clean);
                justEvaluated = false;
                updateLiveDisplay();
            }
            return;
        }
        if (expression.length() == 0) {
            expression.append('−');
            updateLiveDisplay();
            return;
        }
        // Toggle sign on the trailing number
        int idx = expression.length() - 1;
        while (idx >= 0 && (Character.isDigit(expression.charAt(idx)) || expression.charAt(idx) == '.')) {
            idx--;
        }
        if (idx >= 0 && (expression.charAt(idx) == '−' || expression.charAt(idx) == '-')) {
            if (idx == 0 || isBinaryOp(expression.charAt(idx - 1)) || expression.charAt(idx - 1) == '(') {
                expression.deleteCharAt(idx);
            } else {
                expression.insert(idx + 1, '−');
            }
        } else {
            expression.insert(idx + 1, '−');
        }
        updateLiveDisplay();
    }

    private void appendDecimal() {
        resetIfEvaluated();
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == '.') return;
            if (!Character.isDigit(c)) break;
        }
        int len = expression.length();
        expression.append(len == 0 || !Character.isDigit(expression.charAt(len - 1)) ? "0." : ".");
        updateLiveDisplay();
    }

    private void appendOperator(char op) {
        if (justEvaluated) {
            expression.setLength(0);
            if (!"Error".equals(lastResult)) expression.append(lastResult.replace(",", ""));
            justEvaluated = false;
        }
        int len = expression.length();
        if (len == 0) {
            if (op == '−' || op == '-') {
                expression.append('−');
                updateLiveDisplay();
            }
            return;
        }
        char last = expression.charAt(len - 1);
        if (op == '%' || op == '!') {
            if (Character.isDigit(last) || last == ')' || last == 'π' || last == 'e') {
                expression.append(op);
                updateLiveDisplay();
            }
            return;
        }
        if (isBinaryOp(last)) {
            expression.setCharAt(len - 1, op);
        } else if (last != '(' || op == '−' || op == '-') {
            expression.append(op);
        }
        updateLiveDisplay();
    }

    private void toggleParen() {
        resetIfEvaluated();
        int open = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') open++;
            else if (c == ')') open--;
        }
        char last = expression.length() > 0 ? expression.charAt(expression.length() - 1) : 0;
        boolean canClose = open > 0 && (Character.isDigit(last) || last == ')' || last == '%'
                || last == '!' || last == 'π' || last == 'e');
        expression.append(canClose ? ')' : '(');
        updateLiveDisplay();
    }

    private void evaluate() {
        if (expression.length() == 0) return;
        String raw = expression.toString();
        expressionLabel.setText(Evaluator.toDisplay(raw));
        try {
            double val = Evaluator.eval(raw, degreeMode);
            lastAnswer = val;
            lastResult = Evaluator.format(val);
        } catch (RuntimeException ex) {
            lastResult = "Error";
        }
        resultLabel.setText(lastResult);
        justEvaluated = true;
    }

    private void updateLiveDisplay() {
        if (expression.length() == 0) {
            expressionLabel.setText(" ");
            resultLabel.setText("0");
            return;
        }
        String raw = expression.toString();
        resultLabel.setText(Evaluator.toDisplay(raw));

        boolean complex = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (isBinaryOp(c) || c == '%' || c == '!' || c == 'π' || c == 'e' || c == '√' || Character.isLetter(c)) {
                complex = true;
                break;
            }
        }
        if (complex) {
            try {
                expressionLabel.setText("= " + Evaluator.format(Evaluator.eval(raw, degreeMode)));
                return;
            } catch (RuntimeException ignored) { }
        }
        expressionLabel.setText(" ");
    }

    private static boolean isBinaryOp(char c) {
        return c == '+' || c == '-' || c == '−' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '^';
    }

    private void installKeyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (!isActive() || e.getID() != KeyEvent.KEY_PRESSED) return false;
            int code = e.getKeyCode();
            char ch = e.getKeyChar();

            if (code == KeyEvent.VK_Q && e.isControlDown()) { dispose(); return true; }
            if (code == KeyEvent.VK_ESCAPE) { clear(); return true; }
            if (code == KeyEvent.VK_BACK_SPACE) { backspace(); return true; }
            if (code == KeyEvent.VK_ENTER || ch == '=') { evaluate(); return true; }
            if (Character.isDigit(ch)) { appendDigit(ch); return true; }

            if (ch == '.') { appendDecimal(); return true; }
            if (ch == '+') { appendOperator('+'); return true; }
            if (ch == '-') { appendOperator('−'); return true; }
            if (ch == '*') { appendOperator('×'); return true; }
            if (ch == '/') { appendOperator('÷'); return true; }
            if (ch == '%') { appendOperator('%'); return true; }
            if (ch == '^') { appendOperator('^'); return true; }
            if (ch == '!') { appendOperator('!'); return true; }
            if (ch == '(' || ch == ')') {
                resetIfEvaluated();
                expression.append(ch);
                updateLiveDisplay();
                return true;
            }
            return false;
        });
    }

    // ==================== Optimized Cyber UI Components ====================

    private enum KeyStyle { NUMBER, OPERATOR, EQUALS }

    /** Pre-renders the entire static cyber frame once into a BufferedImage for O(1) repaints. */
    private static final class CyberFramePanel extends JPanel {
        private final BufferedImage bgCache;

        CyberFramePanel(int w, int h) {
            setOpaque(true);
            setDoubleBuffered(true);
            setBackground(BG_DEEP);
            bgCache = renderFrame(w, h);
        }

        private static BufferedImage renderFrame(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_DEEP);
            g2.fillRect(0, 0, w, h);

            RoundRectangle2D outer = new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 36, 36);
            g2.setPaint(new GradientPaint(0, 0, BG_DEEP, w, h, new Color(3, 10, 16)));
            g2.fill(outer);

            g2.setClip(outer);
            g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(w * 0.80f, h * 0.88f), w * 0.55f, new float[]{0f, 1f},
                    new Color[]{new Color(0, 225, 255, 32), new Color(0, 225, 255, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(w * 0.5f, h * 0.56f), w * 0.65f, new float[]{0f, 1f},
                    new Color[]{new Color(0, 170, 200, 20), new Color(0, 170, 200, 0)}));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(0, 225, 255, 70));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                int o = i * 11;
                g2.drawLine(36 + o, 5, 24 + o, 17);
                g2.drawLine(24 + o, h - 17, 36 + o, h - 5);
            }

            g2.setColor(new Color(0, 215, 240, 110));
            g2.setStroke(new BasicStroke(1.2f));
            Path2D topLine = new Path2D.Float();
            topLine.moveTo(14, 96); topLine.lineTo(14, 42); topLine.lineTo(42, 14);
            topLine.lineTo(118, 14); topLine.lineTo(130, 22); topLine.lineTo(w - 100, 22);
            g2.draw(topLine);

            Path2D botLine = new Path2D.Float();
            botLine.moveTo(14, h - 78); botLine.lineTo(14, h - 42); botLine.lineTo(42, h - 14);
            botLine.lineTo(112, h - 14); botLine.lineTo(126, h - 22); botLine.lineTo(w - 46, h - 22);
            botLine.lineTo(w - 14, h - 54);
            g2.draw(botLine);

            g2.setClip(null);
            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(0, 225, 255, Math.max(4, 14 / i)));
                g2.setStroke(new BasicStroke(1.2f + i * 1.3f));
                g2.draw(outer);
            }
            g2.setColor(new Color(20, 196, 218, 210));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(outer);
            g2.dispose();
            return img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(bgCache, 0, 0, null);
        }
    }

    /** Right-aligned neon value display with cached font selection and static glow color. */
    private static final class GlowValueLabel extends JComponent {
        private static final Color TEXT_GLOW = new Color(0, 232, 255, 35);
        private static final Font[] FONTS = new Font[14];
        static {
            for (int i = 0; i < FONTS.length; i++) FONTS[i] = new Font("SansSerif", Font.BOLD, 46 - i * 2);
        }

        private String text;
        private Font fittedFont = FONTS[0];
        private boolean layoutDirty = true;

        GlowValueLabel(String initial) {
            this.text = initial;
            setOpaque(false);
        }

        void setText(String t) {
            if (!t.equals(text)) {
                text = t;
                layoutDirty = true;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (layoutDirty) {
                int maxW = getWidth() - 6;
                fittedFont = FONTS[FONTS.length - 1];
                for (Font f : FONTS) {
                    if (g2.getFontMetrics(f).stringWidth(text) <= maxW) { fittedFont = f; break; }
                }
                layoutDirty = false;
            }

            g2.setFont(fittedFont);
            FontMetrics fm = g2.getFontMetrics();
            int x = getWidth() - fm.stringWidth(text) - 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(TEXT_GLOW);
            g2.drawString(text, x - 2, y);
            g2.drawString(text, x + 2, y);
            g2.drawString(text, x, y - 2);
            g2.drawString(text, x, y + 2);
            g2.setColor(CYAN_BRIGHT);
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    /** Zero-allocation neon button using pre-cached strokes, colors, and a reused shape. */
    private static final class CyberButton extends JButton {
        private static final Color NUM_BG = new Color(7, 19, 28, 225);
        private static final Color NUM_HOVER = new Color(12, 34, 48, 240);
        private static final Color NUM_PRESS = new Color(18, 52, 70, 250);
        private static final Color NUM_BORDER = new Color(26, 98, 118, 190);
        private static final Color NUM_BORDER_H = new Color(0, 224, 245, 220);
        private static final Color NUM_TEXT = new Color(232, 249, 252);

        private static final Color OP_BG = new Color(8, 28, 39, 230);
        private static final Color OP_HOVER = new Color(12, 46, 62, 245);
        private static final Color OP_PRESS = new Color(18, 68, 90, 255);
        private static final Color OP_BORDER = new Color(22, 188, 208, 220);
        private static final Color OP_BORDER_H = new Color(72, 245, 255, 255);
        private static final Color OP_TEXT = new Color(152, 244, 252);
        private static final Color OP_TEXT_H = new Color(210, 253, 255);
        private static final Color OP_TEXT_GLOW = new Color(0, 235, 255, 55);

        private static final Color EQ_TOP = new Color(24, 232, 255);
        private static final Color EQ_TOP_H = new Color(78, 243, 255);
        private static final Color EQ_TOP_P = new Color(0, 198, 224);
        private static final Color EQ_BOT = new Color(0, 206, 235);
        private static final Color EQ_BOT_P = new Color(0, 175, 204);
        private static final Color EQ_TEXT = new Color(5, 22, 34);

        private static final Color SHEEN_BASE = new Color(0, 235, 255, 14);
        private static final Color SHEEN_HOVER = new Color(0, 235, 255, 28);
        private static final Color SHEEN_CLEAR = new Color(0, 235, 255, 0);

        private static final BasicStroke BORDER_NORMAL = new BasicStroke(1.2f);
        private static final BasicStroke BORDER_HOVER = new BasicStroke(1.55f);
        private static final BasicStroke[] GLOW_STROKES = {
            new BasicStroke(2.1f), new BasicStroke(2.7f), new BasicStroke(3.3f), new BasicStroke(3.9f)
        };
        private static final Color[] CYAN_ALPHA = new Color[45];
        static {
            for (int a = 0; a < CYAN_ALPHA.length; a++) CYAN_ALPHA[a] = new Color(0, 232, 255, a);
        }

        private final KeyStyle style;
        private final RoundRectangle2D.Float rect = new RoundRectangle2D.Float();

        CyberButton(String label, KeyStyle style) {
            super(label);
            this.style = style;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            int fontSize;
            if (style == KeyStyle.EQUALS) {
                fontSize = 26;
            } else if (style == KeyStyle.OPERATOR) {
                fontSize = label.length() >= 3 ? 15 : (label.length() == 2 ? 17 : 22);
            } else {
                fontSize = 21;
            }
            setFont(new Font("SansSerif", style == KeyStyle.NUMBER ? Font.PLAIN : Font.BOLD, fontSize));
        }

        @Override
        protected void paintComponent(Graphics g) {
            ButtonModel m = getModel();
            boolean hovered = m.isRollover();
            boolean pressed = m.isArmed() && m.isPressed();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float pad = pressed ? 5.6f : 4.8f;
            float x = pad, y = pad, w = getWidth() - pad * 2f, h = getHeight() - pad * 2f, arc = 20f;

            int layers = style == KeyStyle.EQUALS ? 5 : (style == KeyStyle.OPERATOR ? 4 : 3);
            int baseAlpha;
            if (style == KeyStyle.EQUALS) {
                baseAlpha = hovered ? 42 : 30;
            } else if (style == KeyStyle.OPERATOR) {
                baseAlpha = hovered ? 26 : 14;
            } else {
                baseAlpha = hovered ? 18 : 6;
            }

            for (int i = layers; i >= 1; i--) {
                float sp = i * 1.3f;
                g2.setColor(CYAN_ALPHA[Math.max(2, baseAlpha / i)]);
                if (style == KeyStyle.EQUALS) {
                    rect.setRoundRect(x - sp, y - sp, w + sp * 2, h + sp * 2, arc + sp, arc + sp);
                    g2.fill(rect);
                } else {
                    g2.setStroke(GLOW_STROKES[i - 1]);
                    rect.setRoundRect(x - sp * 0.4f, y - sp * 0.4f, w + sp * 0.8f, h + sp * 0.8f,
                            arc + sp * 0.4f, arc + sp * 0.4f);
                    g2.draw(rect);
                }
            }

            rect.setRoundRect(x, y, w, h, arc, arc);
            if (style == KeyStyle.EQUALS) {
                g2.setPaint(new GradientPaint(x, y, pressed ? EQ_TOP_P : (hovered ? EQ_TOP_H : EQ_TOP),
                        x, y + h, pressed ? EQ_BOT_P : EQ_BOT));
                g2.fill(rect);
            } else {
                g2.setColor(style == KeyStyle.OPERATOR
                        ? (pressed ? OP_PRESS : (hovered ? OP_HOVER : OP_BG))
                        : (pressed ? NUM_PRESS : (hovered ? NUM_HOVER : NUM_BG)));
                g2.fill(rect);

                g2.setPaint(new GradientPaint(x, y, hovered ? SHEEN_HOVER : SHEEN_BASE, x, y + h * 0.55f, SHEEN_CLEAR));
                g2.fill(rect);

                g2.setColor(style == KeyStyle.OPERATOR
                        ? (hovered ? OP_BORDER_H : OP_BORDER)
                        : (hovered ? NUM_BORDER_H : NUM_BORDER));
                g2.setStroke(hovered ? BORDER_HOVER : BORDER_NORMAL);
                g2.draw(rect);
            }

            String text = getText();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = Math.round(x + (w - fm.stringWidth(text)) / 2f);
            int ty = Math.round(y + ((h - fm.getHeight()) / 2f) + fm.getAscent());

            if (style == KeyStyle.OPERATOR) {
                g2.setColor(OP_TEXT_GLOW);
                g2.drawString(text, tx, ty + 1);
            }
            g2.setColor(style == KeyStyle.EQUALS ? EQ_TEXT
                    : (style == KeyStyle.OPERATOR ? (hovered ? OP_TEXT_H : OP_TEXT) : NUM_TEXT));
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }

    // ==================== Scientific Math Expression Evaluator ====================

    private static final class Evaluator {
        private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);
        private static final DecimalFormat STD_FMT = new DecimalFormat("#,##0.##########", US);
        private static final DecimalFormat RAW_FMT = new DecimalFormat("0.##########", US);
        private static final DecimalFormat SCI_FMT = new DecimalFormat("0.######E0", US);
        private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

        private final String s;
        private final boolean deg;
        private int pos;

        private Evaluator(String s, boolean deg) {
            this.s = s;
            this.deg = deg;
        }

        static double eval(String raw, boolean degreeMode) {
            if (raw == null || raw.isBlank()) return 0.0;
            StringBuilder clean = new StringBuilder(raw.length());
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == ',' || Character.isWhitespace(c)) continue;
                if (c == '×') clean.append('*');
                else if (c == '÷') clean.append('/');
                else if (c == '−') clean.append('-');
                else clean.append(c);
            }
            Evaluator p = new Evaluator(clean.toString(), degreeMode);
            double v = p.expr();
            if (p.pos < p.s.length() || Double.isNaN(v) || Double.isInfinite(v)) {
                throw new ArithmeticException("Invalid expression");
            }
            return v;
        }

        private double expr() {
            double v = term();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') { pos++; v += term(); }
                else if (c == '-') { pos++; v -= term(); }
                else break;
            }
            return v;
        }

        private double term() {
            double v = power();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                    v *= power();
                } else if (c == '/') {
                    pos++;
                    double d = power();
                    if (d == 0.0) throw new ArithmeticException("Division by zero");
                    v /= d;
                } else if (c == '(' || c == 'π' || c == '√' || Character.isLetter(c)) {
                    // Implicit multiplication: 2π, 2(3+4), 3sin(30)
                    v *= power();
                } else {
                    break;
                }
            }
            return v;
        }

        private double power() {
            double base = unary();
            if (pos < s.length() && s.charAt(pos) == '^') {
                pos++;
                return Math.pow(base, power());
            }
            return base;
        }

        private double unary() {
            if (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') { pos++; return unary(); }
                if (c == '-') { pos++; return -unary(); }
            }
            double v = primary();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '%') {
                    pos++;
                    v /= 100.0;
                } else if (c == '!') {
                    pos++;
                    v = factorial(v);
                } else {
                    break;
                }
            }
            return v;
        }

        private double primary() {
            if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end");
            char c = s.charAt(pos);

            if (c == '(') {
                pos++;
                double v = expr();
                if (pos < s.length() && s.charAt(pos) == ')') pos++;
                return v;
            }
            if (c == 'π') { pos++; return Math.PI; }
            if (c == '√') {
                pos++;
                double arg = (pos < s.length() && s.charAt(pos) == '(') ? primary() : unary();
                if (arg < 0) throw new ArithmeticException("Negative sqrt");
                return Math.sqrt(arg);
            }
            if (Character.isLetter(c)) {
                int start = pos;
                while (pos < s.length() && Character.isLetter(s.charAt(pos))) pos++;
                String name = s.substring(start, pos);
                if ("e".equals(name)) return Math.E;

                double arg = primary();
                double rad = deg ? Math.toRadians(arg) : arg;
                if ("sin".equals(name)) return cleanTrig(Math.sin(rad));
                if ("cos".equals(name)) return cleanTrig(Math.cos(rad));
                if ("tan".equals(name)) {
                    if (Math.abs(Math.cos(rad)) < 1e-15) throw new ArithmeticException("Undefined tan");
                    return cleanTrig(Math.tan(rad));
                }
                if ("ln".equals(name)) {
                    if (arg <= 0) throw new ArithmeticException("Domain error");
                    return Math.log(arg);
                }
                if ("log".equals(name)) {
                    if (arg <= 0) throw new ArithmeticException("Domain error");
                    return Math.log10(arg);
                }
                throw new IllegalArgumentException("Unknown function: " + name);
            }

            int start = pos;
            boolean dot = false;
            while (pos < s.length()) {
                char d = s.charAt(pos);
                if (Character.isDigit(d)) pos++;
                else if (d == '.' && !dot) { dot = true; pos++; }
                else break;
            }
            if (start == pos || (pos - start == 1 && s.charAt(start) == '.')) {
                throw new IllegalArgumentException("Expected number");
            }
            return Double.parseDouble(s.substring(start, pos));
        }

        private static double cleanTrig(double v) {
            return Math.abs(v) < 1e-14 ? 0.0 : v;
        }

        private static double factorial(double n) {
            if (n < 0 || n != Math.floor(n) || n > 170) throw new ArithmeticException("Invalid factorial");
            double res = 1.0;
            for (int i = 2; i <= (int) n; i++) res *= i;
            return res;
        }

        static String format(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
            if (value == 0.0) return "0";
            double abs = Math.abs(value);
            if (abs >= 1e12 || abs < 1e-8) return SCI_FMT.format(value);
            return STD_FMT.format(BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
        }

        static String formatRaw(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value) || value == 0.0) return "0";
            return RAW_FMT.format(BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
        }

        static String toDisplay(String expr) {
            if (expr == null || expr.isEmpty()) return "";
            StringBuilder out = new StringBuilder(expr.length());
            for (int i = 0; i < expr.length(); ) {
                char c = expr.charAt(i);
                if (c == '^' && i + 1 < expr.length()) {
                    int j = i + 1;
                    StringBuilder sup = new StringBuilder();
                    if (expr.charAt(j) == '-') { sup.append('⁻'); j++; }
                    int digitsStart = j;
                    while (j < expr.length() && Character.isDigit(expr.charAt(j))) {
                        sup.append(SUPERSCRIPTS.charAt(expr.charAt(j) - '0'));
                        j++;
                    }
                    if (j > digitsStart) { out.append(sup); i = j; continue; }
                }
                out.append(c);
                i++;
            }
            return out.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainCalculator().setVisible(true));
    }
}

