package com.ncusoft.myapplication7;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    // 声明控件
    private EditText amountEditText, noteEditText, searchEditText;
    private TextView incomeTextView, expenseTextView, balanceTextView;
    private RecyclerView transactionsRecyclerView;
    private Button incomeButton, expenseButton;
    private ImageButton searchButton;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> originalTransactions = new ArrayList<>();
    private int userId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);

        // 优先从SharedPreferences获取userId
        SharedPreferences sp = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        userId = sp.getInt("userId", -1);

        if (userId != -1) {
            loadTransactions();
        }

        return view;
    }

    private void initViews(View view) {
        incomeTextView = view.findViewById(R.id.income_text_view);
        expenseTextView = view.findViewById(R.id.expense_text_view);
        balanceTextView = view.findViewById(R.id.balance_text_view);

        amountEditText = view.findViewById(R.id.amount_edit_text);
        noteEditText = view.findViewById(R.id.note_edit_text);
        searchEditText = view.findViewById(R.id.search_edit_text);

        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(getContext());
        transactionsRecyclerView.setAdapter(transactionAdapter);

        incomeButton = view.findViewById(R.id.income_button);
        expenseButton = view.findViewById(R.id.expense_button);
        searchButton = view.findViewById(R.id.search_button);

        incomeButton.setOnClickListener(v -> addTransaction(Transaction.TYPE_INCOME));
        expenseButton.setOnClickListener(v -> addTransaction(Transaction.TYPE_EXPENSE));
        searchButton.setOnClickListener(v -> searchTransactions());

        // 新增：监听搜索栏内容变化，清空时自动显示全部内容
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    // 自动恢复全部内容
                    transactionAdapter.setTransactions(new ArrayList<>(originalTransactions));
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadTransactions() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = HttpUtils.sendGetRequest("/transactions/" + userId);
                    if (response != null) {
                        JSONArray jsonArray = new JSONArray(response);
                        List<Transaction> transactions = new ArrayList<>();
                        final BigDecimal[] income = {BigDecimal.ZERO};
                        final BigDecimal[] expense = {BigDecimal.ZERO};
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("id");
                            int type = jsonObject.getInt("type");
                            BigDecimal amount = new BigDecimal(jsonObject.getString("amount"));
                            String note = jsonObject.getString("note");
                            String timestampStr = jsonObject.getString("timestamp");
                            long timestamp = parseTimestamp(timestampStr);

                            Transaction transaction = new Transaction(type, amount, note, timestamp);
                            transaction.setId(id);
                            transactions.add(transaction);

                            if (type == Transaction.TYPE_INCOME) {
                                income[0] = income[0].add(amount);
                            } else {
                                expense[0] = expense[0].add(amount);
                            }
                        }
                        originalTransactions = new ArrayList<>(transactions);
                        final BigDecimal balance = income[0].subtract(expense[0]);

                        // 更新UI
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                transactionAdapter.setTransactions(transactions);
                                DecimalFormat df = new DecimalFormat("+¥#.00;-¥#.00");
                                incomeTextView.setText(df.format(income[0]));
                                expenseTextView.setText(df.format(expense[0].negate())); // 支出显示为负数
                                balanceTextView.setText(df.format(balance));
                            });
                        }
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private long parseTimestamp(String timestampStr) {
        try {
            // 兼容"2024-01-01 12:00:00"格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(timestampStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    private void addTransaction(int type) {
        String amountStr = amountEditText.getText().toString().trim();
        String note = noteEditText.getText().toString().trim();
        if (amountStr.isEmpty()) {
            showToast("请输入金额");
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            showToast("金额格式错误");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showToast("金额必须大于0");
            return;
        }
        // 构造请求体
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userId);
            jsonBody.put("type", type);
            jsonBody.put("amount", amount);
            jsonBody.put("note", note);
            // 新增：传递当前时间字符串
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            jsonBody.put("timestamp", now);
        } catch (JSONException e) {
            showToast("数据异常");
            return;
        }

        new Thread(() -> {
            try {
                String response = HttpUtils.sendPostRequest("/transactions", jsonBody.toString());
                if (response != null) {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.optBoolean("success", false);
                    String message = jsonResponse.optString("message", "未知错误");
                    if (success) {
                        // 添加成功，刷新数据
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                amountEditText.setText("");
                                noteEditText.setText("");
                                showToast("添加成功");
                                loadTransactions();
                            });
                        }
                    } else {
                        showToast(message);
                    }
                } else {
                    showToast("网络异常，添加失败");
                }
            } catch (Exception e) {
                showToast("添加失败");
            }
        }).start();
    }

    private void searchTransactions() {
        String keyword = searchEditText.getText().toString().trim();
        if (keyword.isEmpty()) {
            // 恢复原始数据
            transactionAdapter.setTransactions(new ArrayList<>(originalTransactions));
            return;
        }

        StringBuilder urlBuilder = new StringBuilder("/transactions/search?userId=" + userId);

        boolean matched = false;
        // 年份 yyyy
        if (keyword.matches("^\\d{4}$")) {
            urlBuilder.append("&year=").append(keyword);
            matched = true;
        }
        // 年月 yyyy-MM
        else if (keyword.matches("^\\d{4}-\\d{1,2}$")) {
            String[] parts = keyword.split("-");
            urlBuilder.append("&year=").append(parts[0]);
            urlBuilder.append("&month=").append(Integer.parseInt(parts[1]));
            matched = true;
        }
        // 年月日 yyyy-MM-dd
        else if (keyword.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            String[] parts = keyword.split("-");
            urlBuilder.append("&year=").append(parts[0]);
            urlBuilder.append("&month=").append(Integer.parseInt(parts[1]));
            urlBuilder.append("&day=").append(Integer.parseInt(parts[2]));
            matched = true;
        }
        // 金额（整数或小数）
        else if (keyword.matches("^\\d+(\\.\\d+)?$")) {
            urlBuilder.append("&amount=").append(keyword);
            matched = true;
        }
        // 备注模糊搜索
        if (!matched) {
            urlBuilder.append("&note=").append(keyword);
        }

        new Thread(() -> {
            try {
                String response = HttpUtils.sendGetRequest(urlBuilder.toString());
                if (response != null) {
                    JSONArray jsonArray = new JSONArray(response);
                    List<Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        int id = jsonObject.getInt("id");
                        int type = jsonObject.getInt("type");
                        BigDecimal amount = new BigDecimal(jsonObject.getString("amount"));
                        String note = jsonObject.getString("note");
                        String timestampStr = jsonObject.getString("timestamp");
                        long timestamp = parseTimestamp(timestampStr);

                        Transaction transaction = new Transaction(type, amount, note, timestamp);
                        transaction.setId(id);
                        transactions.add(transaction);
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> transactionAdapter.setTransactions(transactions));
                    }
                } else {
                    showToast("搜索失败");
                }
            } catch (Exception e) {
                showToast("搜索异常");
            }
        }).start();
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> android.widget.Toast.makeText(getActivity(), msg, android.widget.Toast.LENGTH_SHORT).show());
        }
    }
}