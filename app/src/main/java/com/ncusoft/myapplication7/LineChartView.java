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
        int paddingLeft = 80; // 缩小左侧padding
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

        // 画y轴刻度和数值（间隔缩小，最多显示6位数，科学计数法显示极大极小数）
        int ySteps = 6;
        for (int i = 0; i <= ySteps; i++) {
            float y = h - paddingBottom - (h - paddingTop - paddingBottom) * i / ySteps;
            float value = min + (max - min) * i / ySteps;
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

        // 画y=0红色参考线在图表中心
        float centerY = (h - paddingBottom + paddingTop) / 2f;
        canvas.drawLine(paddingLeft, centerY, w - paddingRight, centerY, zeroLinePaint);

        // 画折线（支持负数）
        float lastX = 0, lastY = 0;
        for (int i = 0; i < incomeData.size(); i++) {
            float x = paddingLeft + (w - paddingLeft - paddingRight) * i / (count - 1);
            float y = h - paddingBottom - (h - paddingTop - paddingBottom) * (incomeData.get(i) - min) / (max - min);
            if (i > 0) canvas.drawLine(lastX, lastY, x, y, incomePaint);
            lastX = x; lastY = y;
        }
    }
}
