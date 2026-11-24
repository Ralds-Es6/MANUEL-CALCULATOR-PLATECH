package com.manuel.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CyberEvaluator {
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);
    private static final DecimalFormat STD_FMT = new DecimalFormat("#,##0.##########", US);
    private static final DecimalFormat RAW_FMT = new DecimalFormat("0.##########", US);
    private static final DecimalFormat SCI_FMT = new DecimalFormat("0.######E0", US);
    private static final MathContext PRECISION = new MathContext(12, RoundingMode.HALF_UP);
    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private final String s;
    private final boolean deg;
    private int pos;

    private CyberEvaluator(String s, boolean deg) {
        this.s = s;
        this.deg = deg;
    }

    public static double eval(String raw, boolean degreeMode) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        StringBuilder clean = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ',' || Character.isWhitespace(c)) continue;
            if (c == '×') clean.append('*');
            else if (c == '÷') clean.append('/');
            else if (c == '−') clean.append('-');
            else clean.append(c);
        }
        // Auto-close any open parentheses so live preview & evaluation work seamlessly
        int open = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '(') open++;
            else if (c == ')') open--;
        }
        while (open-- > 0) clean.append(')');

        CyberEvaluator p = new CyberEvaluator(clean.toString(), degreeMode);
        double v = p.expr();
        if (p.pos < p.s.length() || Double.isNaN(v) || Double.isInfinite(v)) {
            throw new ArithmeticException("Invalid expression");
        }
        return v;
    }

    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        double abs = Math.abs(value);
        if (abs < 1e-12) return "0";
        if (abs >= 1e12 || abs < 1e-8) return SCI_FMT.format(value);
        BigDecimal bd = BigDecimal.valueOf(value)
            .round(PRECISION)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros();
        return bd.compareTo(BigDecimal.ZERO) == 0 ? "0" : STD_FMT.format(bd);
    }

    public static String formatRaw(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) < 1e-12) return "0";
        BigDecimal bd = BigDecimal.valueOf(value)
            .round(PRECISION)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros();
        return bd.compareTo(BigDecimal.ZERO) == 0 ? "0" : RAW_FMT.format(bd);
    }

    public static String toDisplay(String expr) {
        if (expr == null || expr.isEmpty()) return "";
        StringBuilder out = new StringBuilder(expr.length());
        for (int i = 0; i < expr.length(); ) {
            char c = expr.charAt(i);
            if (c == '^' && i + 1 < expr.length()) {
                int j = i + 1;
                StringBuilder sup = new StringBuilder();
                if (expr.charAt(j) == '-' || expr.charAt(j) == '−') {
                    sup.append('⁻');
                    j++;
                }
                int digitsStart = j;
                while (j < expr.length() && Character.isDigit(expr.charAt(j))) {
                    sup.append(SUPERSCRIPTS.charAt(expr.charAt(j) - '0'));
                    j++;
                }
                if (j > digitsStart) {
                    out.append(sup);
                    i = j;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static boolean isBinaryOp(char c) {
        return c == '+' || c == '-' || c == '−' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '^';
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
                if (Math.abs(Math.cos(rad)) < 1e-12) throw new ArithmeticException("Undefined tan");
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
        // Support scientific notation (e.g., 1E12 or 1.5E-9) when chaining results
        if (pos < s.length() && s.charAt(pos) == 'E') {
            int expPos = pos + 1;
            if (expPos < s.length() && (s.charAt(expPos) == '+' || s.charAt(expPos) == '-')) expPos++;
            int expDigits = expPos;
            while (expPos < s.length() && Character.isDigit(s.charAt(expPos))) expPos++;
            if (expPos > expDigits) pos = expPos;
        }
        return Double.parseDouble(s.substring(start, pos));
    }

    private static double cleanTrig(double v) {
        return Math.abs(v) < 1e-12 ? 0.0 : v;
    }

    private static double factorial(double n) {
        if (n < 0 || n != Math.floor(n) || n > 170) throw new ArithmeticException("Invalid factorial");
        double res = 1.0;
        for (int i = 2; i <= (int) n; i++) res *= i;
        return res;
    }
}

