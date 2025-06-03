package com.ncusoft.myapplication7;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

public class LineChartView extends View {
    private List<Float> incomeData;
    private List<Float> expenseData;
    private List<String> xLabels;
    private Paint incomePaint, expensePaint, axisPaint, textPaint, zeroLinePaint;
    private Float touchX = null;
    private int highlightIndex = -1;
    private Paint dashLinePaint, highlightTextPaint;

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        incomePaint = new Paint();
        incomePaint.setColor(Color.parseColor("#388E3C"));
        incomePaint.setStrokeWidth(6f);
        incomePaint.setStyle(Paint.Style.STROKE);

        expensePaint = new Paint();
        expensePaint.setColor(Color.parseColor("#D32F2F"));
        expensePaint.setStrokeWidth(6f);
        expensePaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint();
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(2f);

        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);

        zeroLinePaint = new Paint();
        zeroLinePaint.setColor(Color.RED);
        zeroLinePaint.setStrokeWidth(3f);
        zeroLinePaint.setStyle(Paint.Style.STROKE);
        zeroLinePaint.setAntiAlias(true);

        dashLinePaint = new Paint();
        dashLinePaint.setColor(Color.GRAY);
        dashLinePaint.setStrokeWidth(2f);
        dashLinePaint.setStyle(Paint.Style.STROKE);
        dashLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        highlightTextPaint = new Paint();
        highlightTextPaint.setColor(Color.BLACK);
        highlightTextPaint.setTextSize(36f);
        highlightTextPaint.setAntiAlias(true);
    }

    public void setData(List<Float> income, List<Float> expense) {
        this.incomeData = income;
        this.expenseData = expense;
        invalidate();
    }

    public void setDataWithLabels(List<Float> income, List<String> xLabels) {
        this.incomeData = (income != null) ? income : new java.util.ArrayList<>();
        this.xLabels = (xLabels != null) ? xLabels : new java.util.ArrayList<>();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (incomeData == null || xLabels == null || incomeData.size() < 2) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchX = event.getX();
                // 计算最近的点
                int w = getWidth(), h = getHeight();
                int paddingBottom = 50;
                int paddingTop = 30;
                int paddingRight = 30;
                int ySteps = 6;
                float max = incomeData.get(0), min = incomeData.get(0);
                for (float v : incomeData) {
                    if (v > max) max = v;
                    if (v < min) min = v;
                }
                if (max == min) {
                    max = min + 1;
                }
                float valueRange = Math.max(Math.abs(max), Math.abs(min));
                float displayMin = -valueRange, displayMax = valueRange;
                float maxLabelWidth = 0f;
                for (int i = 0; i <= ySteps; i++) {
                    float value = displayMax - (displayMax - displayMin) * i / ySteps;
                    String valueStr;
                    if (Math.abs(value) >= 1000000 || Math.abs(value) < 0.01) {
                        valueStr = String.format("%.2e", value);
                    } else if (Math.abs(value) >= 10000) {
                        valueStr = String.format("%.0f", value);
                    } else if (Math.abs(value) >= 1) {
                        valueStr = String.format("%.2f", value);
                    } else {
                        valueStr = String.format("%.4f", value);
                    }
                    float labelWidth = textPaint.measureText(valueStr);
                    if (labelWidth > maxLabelWidth) maxLabelWidth = labelWidth;
                }
                int paddingLeft = (int) (maxLabelWidth + 24);
                int count = incomeData.size();
                float minDist = Float.MAX_VALUE;
                int nearest = -1;
                for (int i = 0; i < count; i++) {
                    float x = paddingLeft + (w - paddingLeft - paddingRight) * i / (count - 1);
                    float dist = Math.abs(x - touchX);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = i;
                    }
                }
                highlightIndex = nearest;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchX = null;
                highlightIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (incomeData == null || xLabels == null || incomeData.size() < 2 || xLabels.size() < 2 || incomeData.size() != xLabels.size()) {
            return;
        }
        int w = getWidth(), h = getHeight();
        int paddingBottom = 50;
        int paddingTop = 30;
        int paddingRight = 30;
        int count = incomeData.size();

        // 计算最大最小值，支持负数
        float max = incomeData.get(0), min = incomeData.get(0);
        for (float v : incomeData) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        if (max == min) {
            max = min + 1;
        }

        // 画y轴刻度和数值前，动态计算最大y轴标签宽度
        int ySteps = 6;
        float valueRange = Math.max(Math.abs(max), Math.abs(min));
        float displayMin = -valueRange, displayMax = valueRange;
        float maxLabelWidth = 0f;
        for (int i = 0; i <= ySteps; i++) {
            float value = displayMax - (displayMax - displayMin) * i / ySteps;
            String valueStr;
            if (Math.abs(value) >= 1000000 || Math.abs(value) < 0.01) {
                valueStr = String.format("%.2e", value);
            } else if (Math.abs(value) >= 10000) {
                valueStr = String.format("%.0f", value);
            } else if (Math.abs(value) >= 1) {
                valueStr = String.format("%.2f", value);
            } else {
                valueStr = String.format("%.4f", value);
            }
            float labelWidth = textPaint.measureText(valueStr);
            if (labelWidth > maxLabelWidth) maxLabelWidth = labelWidth;
        }
        int paddingLeft = (int) (maxLabelWidth + 24); // 10px间距+8px刻度+6px冗余

        // 画坐标轴
        canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, axisPaint);
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, h - paddingBottom, axisPaint);

        // 计算0值在原始坐标中的y
        float zeroY = (h - paddingBottom + paddingTop) / 2f;

        // 画y=0红色参考线在中心
        canvas.drawLine(paddingLeft, zeroY, w - paddingRight, zeroY, zeroLinePaint);

        // 画折线（根据正负切换颜色）
        float lastX = 0, lastY = 0; // 修正：声明lastX/lastY
        for (int i = 0; i < incomeData.size(); i++) {
            float x = paddingLeft + (w - paddingLeft - paddingRight) * i / (count - 1);
            float y = zeroY - (h - paddingTop - paddingBottom) / 2 * (incomeData.get(i) / valueRange);
            if (i > 0) {
                // 判断两点的符号，分段变色
                float v1 = incomeData.get(i - 1);
                float v2 = incomeData.get(i);
                Paint paint = (v1 >= 0 && v2 >= 0) ? incomePaint : (v1 < 0 && v2 < 0) ? expensePaint : null;
                if (paint != null) {
                    canvas.drawLine(lastX, lastY, x, y, paint);
                } else {
                    // 跨越0，分两段画
                    float zeroX1 = lastX + (x - lastX) * (v1 / (v1 - v2));
                    float zeroY1 = zeroY;
                    // 先画v1到0
                    canvas.drawLine(lastX, lastY, zeroX1, zeroY1, v1 >= 0 ? incomePaint : expensePaint);
                    // 再画0到v2
                    canvas.drawLine(zeroX1, zeroY1, x, y, v2 >= 0 ? incomePaint : expensePaint);
                }
            }
            lastX = x; lastY = y;
        }

        // 画y轴刻度和数值（0在中心，正负对称）
        for (int i = 0; i <= ySteps; i++) {
            float value = displayMax - (displayMax - displayMin) * i / ySteps;
            float y = zeroY - (h - paddingTop - paddingBottom) / 2 * (value / valueRange);
            canvas.drawLine(paddingLeft - 8, y, paddingLeft, y, axisPaint);
            String valueStr;
            if (Math.abs(value) >= 1000000 || Math.abs(value) < 0.01) {
                valueStr = String.format("%.2e", value);
            } else if (Math.abs(value) >= 10000) {
                valueStr = String.format("%.0f", value);
            } else if (Math.abs(value) >= 1) {
                valueStr = String.format("%.2f", value);
            } else {
                valueStr = String.format("%.4f", value);
            }
            canvas.drawText(valueStr, paddingLeft - textPaint.measureText(valueStr) - 10, y + 10, textPaint);
        }

        // 画x轴刻度和标签
        int labelStep = Math.max(1, count / 10);
        for (int i = 0; i < count; i += labelStep) {
            float x = paddingLeft + (w - paddingLeft - paddingRight) * i / (count - 1);
            canvas.drawLine(x, h - paddingBottom, x, h - paddingBottom + 8, axisPaint);
            String label = xLabels.get(i);
            canvas.drawText(label, x - textPaint.measureText(label) / 2, h - paddingBottom + 30, textPaint);
        }

        // 新增：画虚线和数值提示
        if (touchX != null && highlightIndex >= 0 && highlightIndex < count) {
            float x = paddingLeft + (w - paddingLeft - paddingRight) * highlightIndex / (count - 1);
            // 画虚线
            canvas.drawLine(x, paddingTop, x, h - paddingBottom, dashLinePaint);
            // 画数值气泡
            float value = incomeData.get(highlightIndex);
            String valueStr;
            if (Math.abs(value) >= 1000000 || Math.abs(value) < 0.01) {
                valueStr = String.format("%.2e", value);
            } else if (Math.abs(value) >= 10000) {
                valueStr = String.format("%.0f", value);
            } else if (Math.abs(value) >= 1) {
                valueStr = String.format("%.2f", value);
            } else {
                valueStr = String.format("%.4f", value);
            }
            // 使用已定义的zeroY
            float y = zeroY - (h - paddingTop - paddingBottom) / 2 * (value / valueRange);

            // 气泡背景
            float textWidth = highlightTextPaint.measureText(valueStr);
            float textHeight = highlightTextPaint.getTextSize();
            float rectLeft = x - textWidth / 2 - 16;
            float rectTop = y - textHeight - 24;
            float rectRight = x + textWidth / 2 + 16;
            float rectBottom = y - 8;
            Paint bubblePaint = new Paint();
            bubblePaint.setColor(Color.WHITE);
            bubblePaint.setStyle(Paint.Style.FILL);
            bubblePaint.setShadowLayer(8, 0, 0, Color.LTGRAY);
            setLayerType(LAYER_TYPE_SOFTWARE, bubblePaint);
            canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 16, 16, bubblePaint);
            // 气泡边框
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.GRAY);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f);
            canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 16, 16, borderPaint);

            // 画数值
            canvas.drawText(valueStr, x - textWidth / 2, rectBottom - 12, highlightTextPaint);

            // 画x轴标签
            String label = xLabels.get(highlightIndex);
            float labelWidth = highlightTextPaint.measureText(label);
            canvas.drawText(label, x - labelWidth / 2, h - paddingBottom + 60, highlightTextPaint);
        }
    }
}
