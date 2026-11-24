package com.manuel.maincalculator;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.manuel.core.CyberEvaluator;
import com.manuel.core.CyberViews;
import com.manuel.core.CyberViews.CyberButton;
import com.manuel.core.CyberViews.CyberFrameLayout;
import com.manuel.core.CyberViews.GlowValueView;
import com.manuel.core.CyberViews.KeyStyle;

public class MainActivity extends Activity {
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
    private TextView expressionView;
    private GlowValueView resultView;
    private CyberButton angleModeBtn;

    private String lastResult = "0";
    private double lastAnswer = 0.0;
    private boolean degreeMode = true;
    private boolean justEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad10 = CyberViews.dp(this, 10);
        int pad14 = CyberViews.dp(this, 14);

        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(CyberViews.BG_DEEP);
        outer.setPadding(pad10, pad10, pad10, pad10);

        CyberFrameLayout frame = new CyberFrameLayout(this);
        frame.setPadding(pad14, CyberViews.dp(this, 18), pad14, CyberViews.dp(this, 18));

        // Header
        TextView title = new TextView(this);
        title.setText("Calculator");
        title.setTextColor(CyberViews.CYAN_BRIGHT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        title.setPadding(CyberViews.dp(this, 8), CyberViews.dp(this, 4), 0, CyberViews.dp(this, 4));
        frame.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Display
        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.BOTTOM | Gravity.END);
        display.setPadding(CyberViews.dp(this, 8), CyberViews.dp(this, 10),
            CyberViews.dp(this, 10), CyberViews.dp(this, 10));

        expressionView = new TextView(this);
        expressionView.setText(" ");
        expressionView.setTextColor(CyberViews.CYAN_SOFT);
        expressionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        expressionView.setGravity(Gravity.END);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);

        resultView = new GlowValueView(this);

        display.addView(expressionView);
        display.addView(resultView);

        frame.addView(display, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 7x5 Scientific Keypad with wide '0'
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        int gap = CyberViews.dp(this, 2.5f);

        for (int r = 0; r < KEY_ROWS.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
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

                final CyberButton btn = new CyberButton(this, label, style, true);
                if ("DEG".equals(label)) angleModeBtn = btn;
                final String key = label;
                btn.setOnClickListener(v -> handleKey(key));

                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, (float) span);
                bp.setMargins(gap, gap, gap, gap);
                row.addView(btn, bp);
                col += span;
            }
            grid.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        frame.addView(grid, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 3.3f));

        outer.addView(frame, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(outer);
    }

    private void handleKey(String key) {
        switch (key) {
            case "AC": clear(); break;
            case "DEL": backspace(); break;
            case "DEG":
                degreeMode = !degreeMode;
                angleModeBtn.setText(degreeMode ? "DEG" : "RAD");
                updateLiveDisplay();
                break;
            case "( )": toggleParen(); break;
            case ".": appendDecimal(); break;
            case "=": evaluate(); break;
            case "sin": case "cos": case "tan": case "ln": case "log":
                appendFunction(key + "(");
                break;
            case "√": appendFunction("√("); break;
            case "x²": appendSquare(); break;
            case "xʸ": appendOperator('^'); break;
            case "n!": appendOperator('!'); break;
            case "π": case "e": appendConstant(key); break;
            case "ANS": appendAnswer(); break;
            case "±": toggleSign(); break;
            default:
                if (key.length() == 1 && Character.isDigit(key.charAt(0))) appendDigit(key.charAt(0));
                else if (key.length() == 1) appendOperator(key.charAt(0));
                break;
        }
    }

    private void resetIfEvaluated() {
        if (justEvaluated) {
            expression.setLength(0);
            expressionView.setText(" ");
            justEvaluated = false;
        }
    }

    private void clear() {
        expression.setLength(0);
        lastResult = "0";
        justEvaluated = false;
        expressionView.setText(" ");
        resultView.setDisplayText("0", 44f);
    }

    private void backspace() {
        if (justEvaluated) { clear(); return; }
        int len = expression.length();
        if (len == 0) return;

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
        if (expression.length() > 0 && needsMultiplyBeforeDigit(last())) expression.append('×');
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
        if (expression.length() > 0 && needsMultiplyBeforeDigit(last())) expression.append('×');
        expression.append(CyberEvaluator.formatRaw(lastAnswer));
        updateLiveDisplay();
    }

    private void appendSquare() {
        if (justEvaluated) {
            expression.setLength(0);
            if (!"Error".equals(lastResult)) expression.append(lastResult.replace(",", ""));
            justEvaluated = false;
        }
        if (expression.length() == 0) return;
        char last = last();
        if (Character.isDigit(last) || last == ')' || last == 'π' || last == 'e') {
            expression.append("^2");
            updateLiveDisplay();
        }
    }

    private void toggleSign() {
        if (justEvaluated) {
            if (!"Error".equals(lastResult)) {
                String clean = lastResult.replace(",", "");
                if ("0".equals(clean)) return;
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
        int idx = expression.length() - 1;
        while (idx >= 0 && (Character.isDigit(expression.charAt(idx)) || expression.charAt(idx) == '.')) idx--;
        if (idx >= 0 && (expression.charAt(idx) == '−' || expression.charAt(idx) == '-')) {
            if (idx == 0 || CyberEvaluator.isBinaryOp(expression.charAt(idx - 1)) || expression.charAt(idx - 1) == '(') {
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
        if (len > 0 && needsMultiplyBeforeDigit(last())) expression.append("×0.");
        else expression.append(len == 0 || !Character.isDigit(last()) ? "0." : ".");
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
            if (op == '−' || op == '-') { expression.append('−'); updateLiveDisplay(); }
            return;
        }
        char last = last();
        if (op == '%' || op == '!') {
            if (Character.isDigit(last) || last == ')' || last == 'π' || last == 'e') {
                expression.append(op);
                updateLiveDisplay();
            }
            return;
        }
        if (CyberEvaluator.isBinaryOp(last)) expression.setCharAt(len - 1, op);
        else if (last != '(' || op == '−' || op == '-') expression.append(op);
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
        char last = expression.length() > 0 ? last() : 0;
        boolean canClose = open > 0 && (Character.isDigit(last) || last == ')' || last == '%'
            || last == '!' || last == 'π' || last == 'e');
        expression.append(canClose ? ')' : '(');
        updateLiveDisplay();
    }

    private void evaluate() {
        if (expression.length() == 0) return;
        String raw = expression.toString();
        expressionView.setText(CyberEvaluator.toDisplay(raw));
        try {
            double val = CyberEvaluator.eval(raw, degreeMode);
            lastAnswer = val;
            lastResult = CyberEvaluator.format(val);
        } catch (RuntimeException ex) {
            lastResult = "Error";
        }
        resultView.setDisplayText(lastResult, 44f);
        justEvaluated = true;
    }

    private void updateLiveDisplay() {
        if (expression.length() == 0) {
            expressionView.setText(" ");
            resultView.setDisplayText("0", 44f);
            return;
        }
        String raw = expression.toString();
        resultView.setDisplayText(CyberEvaluator.toDisplay(raw), 44f);

        boolean complex = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (CyberEvaluator.isBinaryOp(c) || c == '%' || c == '!' || c == 'π' || c == 'e' || c == '√' || Character.isLetter(c)) {
                complex = true;
                break;
            }
        }
        if (complex) {
            try {
                expressionView.setText("= " + CyberEvaluator.format(CyberEvaluator.eval(raw, degreeMode)));
                return;
            } catch (RuntimeException ignored) { }
        }
        expressionView.setText(" ");
    }

    private char last() { return expression.charAt(expression.length() - 1); }

    private static boolean needsMultiplyBeforeDigit(char c) {
        return c == ')' || c == 'π' || c == 'e' || c == '%' || c == '!';
    }
}

