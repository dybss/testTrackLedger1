package com.ncusoft.myapplication7;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ncusoft.myapplication7.TransactionAdapter;
import com.ncusoft.myapplication7.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.widget.CalendarView;

public class CalendarFragment extends Fragment {
    private int userId = -1;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private CalendarView calendarView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        recyclerView = view.findViewById(R.id.daily_transactions_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext());
        recyclerView.setAdapter(adapter);

        calendarView = view.findViewById(R.id.calendarView);

        // 优先从SharedPreferences获取userId
        SharedPreferences sp = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        userId = sp.getInt("userId", -1);

        if (userId != -1) {
            long millis = calendarView.getDate();
            loadTransactionsByDate(millis);
        }

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            loadTransactionsByDate(year, month + 1, dayOfMonth);
        });

        return view;
    }

    private void loadTransactionsByDate(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("MM", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("dd", Locale.getDefault());
        int year = Integer.parseInt(sdfYear.format(date));
        int month = Integer.parseInt(sdfMonth.format(date));
        int day = Integer.parseInt(sdfDay.format(date));
        loadTransactionsByDate(year, month, day);
    }

    private void loadTransactionsByDate(int year, int month, int day) {
        if (userId == -1) return;
        String url = "/transactions/search?userId=" + userId +
                "&year=" + year + "&month=" + month + "&day=" + day;
        new Thread(() -> {
            try {
                String response = HttpUtils.sendGetRequest(url);
                if (response != null) {
                    JSONArray jsonArray = new JSONArray(response);
                    List<Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id");
                        int type = obj.getInt("type");
                        BigDecimal amount = new BigDecimal(obj.getString("amount"));
                        String note = obj.getString("note");
                        String timestampStr = obj.getString("timestamp");
                        long timestamp = parseTimestamp(timestampStr);
                        Transaction t = new Transaction(type, amount, note, timestamp);
                        t.setId(id);
                        transactions.add(t);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter.setTransactions(transactions);
                        });
                    }
                } else {
                    showToast("获取数据失败");
                }
            } catch (IOException | JSONException e) {
                showToast("网络或数据异常");
            }
        }).start();
    }

    private long parseTimestamp(String timestampStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(timestampStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show());
        }
    }
}
