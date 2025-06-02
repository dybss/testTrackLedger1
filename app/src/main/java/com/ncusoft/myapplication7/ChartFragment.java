package com.ncusoft.myapplication7;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import android.widget.Spinner;
import android.widget.AdapterView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.widget.ArrayAdapter;
import android.content.SharedPreferences;

public class ChartFragment extends Fragment {
    private Spinner spinnerYear, spinnerMonth;
    private LineChartView lineChart;
    private int userId = -1;
    private int selectedYear;
    private int selectedMonth; // 0表示全年

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);
        spinnerYear = view.findViewById(R.id.spinner_year);
        spinnerMonth = view.findViewById(R.id.spinner_month);
        lineChart = view.findViewById(R.id.line_chart);

        // 优先从SharedPreferences获取userId，保证每次都能拿到
        SharedPreferences sp = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        userId = sp.getInt("userId", -1);

        // 初始化年份和月份
        initYearMonthSpinners();

        return view;
    }

    private void initYearMonthSpinners() {
        // 年份范围：近10年
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String[] years = new String[10];
        for (int i = 0; i < 10; i++) {
            years[i] = String.valueOf(currentYear - 9 + i);
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(9); // 默认当前年

        // 月份
        String[] months = new String[13];
        months[0] = getString(R.string.whole_year);
        for (int i = 1; i <= 12; i++) {
            months[i] = i + getString(R.string.month_suffix);
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(0);

        // 监听
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = Integer.parseInt(years[position]);
                requestAndShowChart();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position; // 0=全年, 1~12=1~12月
                requestAndShowChart();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 默认加载
        selectedYear = Integer.parseInt(years[9]);
        selectedMonth = 0;
        requestAndShowChart();
    }

    private void requestAndShowChart() {
        if (userId == -1) return;
        final String url;
        if (selectedMonth > 0) {
            url = "/transactions/summary?userId=" + userId + "&year=" + selectedYear + "&month=" + selectedMonth;
        } else {
            url = "/transactions/summary?userId=" + userId + "&year=" + selectedYear;
        }
        new Thread(() -> {
            try {
                String response = HttpUtils.sendGetRequest(url);
                if (response != null) {
                    JSONArray arr = new JSONArray(response);
                    final List<Float> yList = new ArrayList<>();
                    final List<String> xLabels = new ArrayList<>();
                    int xCount;
                    if (selectedMonth > 0) {
                        // 按天
                        xCount = 31;
                        float[] dayAmount = new float[xCount];
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int day = obj.optInt("day", 0);
                            float amount = (float) obj.optDouble("totalAmount", 0);
                            if (day > 0 && day <= xCount) {
                                dayAmount[day - 1] = amount;
                            }
                        }
                        for (int i = 0; i < xCount; i++) {
                            yList.add(dayAmount[i]);
                            xLabels.add(String.valueOf(i + 1));
                        }
                    } else {
                        // 按月
                        xCount = 12;
                        float[] monthAmount = new float[xCount];
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int m = obj.optInt("month", 0);
                            float amount = (float) obj.optDouble("totalAmount", 0);
                            if (m > 0 && m <= xCount) {
                                monthAmount[m - 1] = amount;
                            }
                        }
                        for (int i = 0; i < xCount; i++) {
                            yList.add(monthAmount[i]);
                            xLabels.add((i + 1) + getString(R.string.month_suffix));
                        }
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            lineChart.setDataWithLabels(yList, xLabels);
                        });
                    }
                } else {
                    // 建议加提示
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            android.widget.Toast.makeText(getContext(), getString(R.string.no_data), android.widget.Toast.LENGTH_SHORT).show()
                        );
                        // 清空图表
                        lineChart.setDataWithLabels(new ArrayList<>(), new ArrayList<>());
                    }
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        android.widget.Toast.makeText(getContext(), getString(R.string.load_chart_failed), android.widget.Toast.LENGTH_SHORT).show()
                    );
                    // 清空图表
                    lineChart.setDataWithLabels(new ArrayList<>(), new ArrayList<>());
                }
            }
        }).start();
    }
}
