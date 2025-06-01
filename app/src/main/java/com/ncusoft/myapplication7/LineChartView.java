package com.ncusoft.myapplication7;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

public class LineChartView extends View {
    private List<Float> incomeData;
    private List<Float> expenseData;
    private List<String> xLabels;
    private Paint incomePaint, expensePaint, axisPaint, textPaint, zeroLinePaint;

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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (incomeData == null || xLabels == null || incomeData.size() < 2 || xLabels.size() < 2 || incomeData.size() != xLabels.size()) {
            return;
        }
        int w = getWidth(), h = getHeight();
        int paddingLeft = 80;
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

        // 画坐标轴
        canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, axisPaint);
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, h - paddingBottom, axisPaint);

        // 计算0值在原始坐标中的y
        float zeroY = (h - paddingBottom + paddingTop) / 2f;

        // 画y=0红色参考线在中心
        canvas.drawLine(paddingLeft, zeroY, w - paddingRight, zeroY, zeroLinePaint);

        // 画折线（根据正负切换颜色）
        float valueRange = Math.max(Math.abs(max), Math.abs(min));
        float displayMin = -valueRange, displayMax = valueRange;
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
        int ySteps = 6;
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
    }
}
