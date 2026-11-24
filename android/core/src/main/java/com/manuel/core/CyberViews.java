package com.manuel.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class CyberViews {
    public static final int BG_DEEP = Color.rgb(5, 13, 20);
    public static final int CYAN_BRIGHT = Color.rgb(36, 236, 255);
    public static final int CYAN_SOFT = Color.rgb(138, 226, 238);

    public enum KeyStyle { NUMBER, OPERATOR, EQUALS }

    public static int dp(Context ctx, float value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, ctx.getResources().getDisplayMetrics()
        ));
    }

    public static float dpf(Context ctx, float value) {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, ctx.getResources().getDisplayMetrics()
        );
    }

    // Cyber frame with radial cyan glows, diagonal corner hatches, circuit traces, and neon cyan border
    public static class CyberFrameLayout extends LinearLayout {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF outerRect = new RectF();
        private final Path clipPath = new Path();
        private final Path tracePath = new Path();

        public CyberFrameLayout(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setWillNotDraw(false);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
            borderPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            Context ctx = getContext();
            float inset = dpf(ctx, 4);
            float radius = dpf(ctx, 28);
            outerRect.set(inset, inset, w - inset, h - inset);

            // Base deep cyber background
            fillPaint.setShader(new LinearGradient(
                0, 0, w, h, BG_DEEP, Color.rgb(3, 10, 16), Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(outerRect, radius, radius, fillPaint);

            int save = canvas.save();
            clipPath.reset();
            clipPath.addRoundRect(outerRect, radius, radius, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // Subtle cyan radial glows
            glowPaint.setShader(new RadialGradient(
                w * 0.78f, h * 0.86f, w * 0.55f,
                Color.argb(34, 0, 225, 255), Color.argb(0, 0, 225, 255),
                Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0, 0, w, h, glowPaint);

            glowPaint.setShader(new RadialGradient(
                w * 0.50f, h * 0.56f, w * 0.65f,
                Color.argb(20, 0, 170, 200), Color.argb(0, 0, 170, 200),
                Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0, 0, w, h, glowPaint);

            // 4 diagonal cyan hash marks at top-left and bottom-left
            linePaint.setColor(Color.argb(72, 0, 225, 255));
            linePaint.setStrokeWidth(dpf(ctx, 2.0f));
            for (int i = 0; i < 4; i++) {
                float o = dpf(ctx, i * 10);
                canvas.drawLine(dpf(ctx, 34) + o, dpf(ctx, 5), dpf(ctx, 22) + o, dpf(ctx, 17), linePaint);
                canvas.drawLine(dpf(ctx, 22) + o, h - dpf(ctx, 17), dpf(ctx, 34) + o, h - dpf(ctx, 5), linePaint);
            }

            // Top and bottom cyber circuit trace lines
            linePaint.setColor(Color.argb(110, 0, 215, 240));
            linePaint.setStrokeWidth(dpf(ctx, 1.2f));

            tracePath.reset();
            tracePath.moveTo(dpf(ctx, 14), dpf(ctx, 90));
            tracePath.lineTo(dpf(ctx, 14), dpf(ctx, 40));
            tracePath.lineTo(dpf(ctx, 40), dpf(ctx, 14));
            tracePath.lineTo(dpf(ctx, 114), dpf(ctx, 14));
            tracePath.lineTo(dpf(ctx, 126), dpf(ctx, 22));
            tracePath.lineTo(w - dpf(ctx, 88), dpf(ctx, 22));
            canvas.drawPath(tracePath, linePaint);

            tracePath.reset();
            tracePath.moveTo(dpf(ctx, 14), h - dpf(ctx, 74));
            tracePath.lineTo(dpf(ctx, 14), h - dpf(ctx, 40));
            tracePath.lineTo(dpf(ctx, 40), h - dpf(ctx, 14));
            tracePath.lineTo(dpf(ctx, 108), h - dpf(ctx, 14));
            tracePath.lineTo(dpf(ctx, 122), h - dpf(ctx, 22));
            tracePath.lineTo(w - dpf(ctx, 44), h - dpf(ctx, 22));
            tracePath.lineTo(w - dpf(ctx, 14), h - dpf(ctx, 52));
            canvas.drawPath(tracePath, linePaint);

            canvas.restoreToCount(save);

            // Multi-layer glowing cyan outer border
            for (int i = 3; i >= 1; i--) {
                borderPaint.setColor(Color.argb(Math.max(5, 18 / i), 0, 225, 255));
                borderPaint.setStrokeWidth(dpf(ctx, 1.2f + i * 1.4f));
                canvas.drawRoundRect(outerRect, radius, radius, borderPaint);
            }
            borderPaint.setColor(Color.argb(215, 20, 196, 218));
            borderPaint.setStrokeWidth(dpf(ctx, 1.5f));
            canvas.drawRoundRect(outerRect, radius, radius, borderPaint);

            super.onDraw(canvas);
        }
    }

    // Glowing cyan display value view
    public static class GlowValueView extends TextView {
        public GlowValueView(Context context) {
            super(context);
            setText("0");
            setTextColor(CYAN_BRIGHT);
            setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            setSingleLine(true);
            setEllipsize(TextUtils.TruncateAt.START);
            setShadowLayer(dpf(context, 8), 0, 0, Color.argb(110, 0, 232, 255));
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f);
        }

        public void setDisplayText(String text, float maxSp) {
            setText(text);
            int len = text.length();
            float sp;
            if (len <= 9) sp = maxSp;
            else if (len <= 13) sp = maxSp * 0.78f;
            else if (len <= 18) sp = maxSp * 0.62f;
            else if (len <= 24) sp = maxSp * 0.48f;
            else sp = maxSp * 0.40f;
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
    }

    // Cyber neon button with cyan border glow, top glass sheen, and bright cyan equals button
    public static class CyberButton extends TextView {
        private static final int NUM_BG = Color.argb(228, 7, 19, 28);
        private static final int NUM_PRESS = Color.argb(250, 18, 52, 70);
        private static final int NUM_BORDER = Color.argb(195, 26, 98, 118);
        private static final int NUM_TEXT = Color.rgb(232, 249, 252);

        private static final int OP_BG = Color.argb(232, 8, 28, 39);
        private static final int OP_PRESS = Color.argb(255, 18, 68, 90);
        private static final int OP_BORDER = Color.argb(225, 22, 188, 208);
        private static final int OP_TEXT = Color.rgb(152, 244, 252);

        private static final int EQ_TOP = Color.rgb(24, 232, 255);
        private static final int EQ_TOP_P = Color.rgb(0, 198, 224);
        private static final int EQ_BOT = Color.rgb(0, 206, 235);
        private static final int EQ_BOT_P = Color.rgb(0, 175, 204);
        private static final int EQ_TEXT = Color.rgb(5, 22, 34);

        private final KeyStyle style;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean isDown = false;

        public CyberButton(Context context, String label, KeyStyle style, boolean compact) {
            super(context);
            this.style = style;

            setText(label);
            setGravity(Gravity.CENTER);
            setClickable(true);
            setFocusable(true);
            setTypeface(Typeface.SANS_SERIF, style == KeyStyle.NUMBER ? Typeface.NORMAL : Typeface.BOLD);

            float sp;
            if (style == KeyStyle.EQUALS) {
                sp = compact ? 25f : 28f;
                setTextColor(EQ_TEXT);
            } else if (style == KeyStyle.OPERATOR) {
                sp = label.length() >= 3 ? (compact ? 14f : 16f) : (label.length() == 2 ? 17f : (compact ? 21f : 24f));
                setTextColor(OP_TEXT);
                setShadowLayer(dpf(context, 4), 0, 0, Color.argb(70, 0, 235, 255));
            } else {
                sp = compact ? 20f : 23f;
                setTextColor(NUM_TEXT);
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);

            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dpf(context, 1.25f));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDown = true;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDown = false;
                    invalidate();
                    break;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            Context ctx = getContext();
            float pad = dpf(ctx, style == KeyStyle.EQUALS ? 3.5f : 2.2f);
            float radius = dpf(ctx, 18f);

            if (style == KeyStyle.EQUALS) {
                // Soft cyan outer glow halo
                glowPaint.setStyle(Paint.Style.FILL);
                for (int i = 3; i >= 1; i--) {
                    float sp = dpf(ctx, i * 1.1f);
                    glowPaint.setColor(Color.argb(isDown ? 28 / i : 36 / i, 0, 232, 255));
                    rect.set(pad - sp, pad - sp, w - pad + sp, h - pad + sp);
                    canvas.drawRoundRect(rect, radius + sp, radius + sp, glowPaint);
                }
                rect.set(pad, pad, w - pad, h - pad);
                fillPaint.setShader(new LinearGradient(
                    0, pad, 0, h - pad,
                    isDown ? EQ_TOP_P : EQ_TOP, isDown ? EQ_BOT_P : EQ_BOT,
                    Shader.TileMode.CLAMP
                ));
                canvas.drawRoundRect(rect, radius, radius, fillPaint);
            } else {
                rect.set(pad, pad, w - pad, h - pad);
                fillPaint.setShader(null);
                fillPaint.setColor(style == KeyStyle.OPERATOR
                    ? (isDown ? OP_PRESS : OP_BG)
                    : (isDown ? NUM_PRESS : NUM_BG));
                canvas.drawRoundRect(rect, radius, radius, fillPaint);

                // Subtle top cyan glass sheen
                sheenPaint.setShader(new LinearGradient(
                    0, pad, 0, pad + (h - pad * 2) * 0.55f,
                    Color.argb(isDown ? 32 : 16, 0, 235, 255), Color.argb(0, 0, 235, 255),
                    Shader.TileMode.CLAMP
                ));
                canvas.drawRoundRect(rect, radius, radius, sheenPaint);

                borderPaint.setColor(isDown
                    ? CYAN_BRIGHT
                    : (style == KeyStyle.OPERATOR ? OP_BORDER : NUM_BORDER));
                canvas.drawRoundRect(rect, radius, radius, borderPaint);
            }

            super.onDraw(canvas);
        }
    }
}

