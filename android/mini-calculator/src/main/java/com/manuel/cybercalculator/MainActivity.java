package com.manuel.cybercalculator;

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
        {"AC", "( )", "%",   "÷"},
        {"7",  "8",   "9",   "×"},
        {"4",  "5",   "6",   "−"},
        {"1",  "2",   "3",   "+"},
        {"0",  ".",   "DEL", "="}
    };

    private final StringBuilder expression = new StringBuilder(32);
    private TextView expressionView;
    private GlowValueView resultView;
    private String lastResult = "0";
    private boolean justEvaluated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad10 = CyberViews.dp(this, 10);
        int pad16 = CyberViews.dp(this, 16);

        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(CyberViews.BG_DEEP);
        outer.setPadding(pad10, pad10, pad10, pad10);

        CyberFrameLayout frame = new CyberFrameLayout(this);
        frame.setPadding(pad16, CyberViews.dp(this, 22), pad16, CyberViews.dp(this, 22));

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
        display.setPadding(CyberViews.dp(this, 8), CyberViews.dp(this, 14),
            CyberViews.dp(this, 10), CyberViews.dp(this, 14));

        expressionView = new TextView(this);
        expressionView.setText(" ");
        expressionView.setTextColor(CyberViews.CYAN_SOFT);
        expressionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        expressionView.setGravity(Gravity.END);
        expressionView.setSingleLine(true);
        expressionView.setEllipsize(TextUtils.TruncateAt.START);

        resultView = new GlowValueView(this);

        display.addView(expressionView);
        display.addView(resultView);

        frame.addView(display, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.15f));

        // 5x4 Keypad
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        int gap = CyberViews.dp(this, 3);

        for (int r = 0; r < KEY_ROWS.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 4; c++) {
                final String label = KEY_ROWS[r][c];
                KeyStyle style = "=".equals(label) ? KeyStyle.EQUALS
                    : (r == 0 || c == 3 || "DEL".equals(label)) ? KeyStyle.OPERATOR : KeyStyle.NUMBER;

                CyberButton btn = new CyberButton(this, label, style, false);
                btn.setOnClickListener(v -> handleKey(label));

                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                bp.setMargins(gap, gap, gap, gap);
                row.addView(btn, bp);
            }
            grid.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        frame.addView(grid, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.85f));

        outer.addView(frame, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(outer);
    }

    private void handleKey(String key) {
        switch (key) {
            case "AC": clear(); break;
            case "DEL": backspace(); break;
            case "( )": toggleParen(); break;
            case ".": appendDecimal(); break;
            case "=": evaluate(); break;
            default:
                char c = key.charAt(0);
                if (Character.isDigit(c)) appendDigit(c);
                else appendOperator(c);
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
        resultView.setDisplayText("0", 48f);
    }

    private void backspace() {
        if (justEvaluated) clear();
        else if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
            updateLiveDisplay();
        }
    }

    private void appendDigit(char d) {
        resetIfEvaluated();
        if (expression.length() > 0 && (last() == ')' || last() == '%')) expression.append('×');
        expression.append(d);
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
        if (len > 0 && (last() == ')' || last() == '%')) expression.append("×0.");
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
        if (op == '%') {
            if (Character.isDigit(last) || last == ')') { expression.append('%'); updateLiveDisplay(); }
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
        expression.append(open > 0 && (Character.isDigit(last) || last == ')' || last == '%') ? ')' : '(');
        updateLiveDisplay();
    }

    private void evaluate() {
        if (expression.length() == 0) return;
        String raw = expression.toString();
        expressionView.setText(CyberEvaluator.toDisplay(raw));
        try {
            lastResult = CyberEvaluator.format(CyberEvaluator.eval(raw, true));
        } catch (RuntimeException ex) {
            lastResult = "Error";
        }
        resultView.setDisplayText(lastResult, 48f);
        justEvaluated = true;
    }

    private void updateLiveDisplay() {
        if (expression.length() == 0) {
            expressionView.setText(" ");
            resultView.setDisplayText("0", 48f);
            return;
        }
        String raw = expression.toString();
        resultView.setDisplayText(CyberEvaluator.toDisplay(raw), 48f);

        boolean hasOp = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (CyberEvaluator.isBinaryOp(c) || c == '%') { hasOp = true; break; }
        }
        if (hasOp) {
            try {
                expressionView.setText("= " + CyberEvaluator.format(CyberEvaluator.eval(raw, true)));
                return;
            } catch (RuntimeException ignored) { }
        }
        expressionView.setText(" ");
    }

    private char last() { return expression.charAt(expression.length() - 1); }
}

