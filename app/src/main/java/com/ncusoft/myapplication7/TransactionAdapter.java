package com.ncusoft.myapplication7;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ncusoft.myapplication7.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Transaction> transactions = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_EMPTY = 1;

    public TransactionAdapter(Context context) {
        this.context = context;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_empty_transaction, parent, false);
            return new EmptyViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_EMPTY) {
            // 无需绑定数据
            return;
        }
        Transaction transaction = transactions.get(position);

        TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
        transactionHolder.noteTextView.setText(transaction.getNote());
        transactionHolder.dateTextView.setText(dateFormat.format(new Date(transaction.getTimestamp())));

        if (transaction.isIncome()) {
            transactionHolder.amountTextView.setTextColor(context.getResources().getColor(R.color.income));
            transactionHolder.amountTextView.setText(String.format("+¥%.2f", transaction.getAmount()));
        } else {
            transactionHolder.amountTextView.setTextColor(context.getResources().getColor(R.color.expense));
            transactionHolder.amountTextView.setText(String.format("-¥%.2f", transaction.getAmount()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (transactions == null || transactions.isEmpty()) {
            return TYPE_EMPTY;
        }
        return TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        if (transactions == null || transactions.isEmpty()) {
            return 1; // 只显示一个空布局
        }
        return transactions.size();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView noteTextView, amountTextView, dateTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTextView = itemView.findViewById(R.id.note_text_view);
            amountTextView = itemView.findViewById(R.id.amount_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view);
        }
    }

    // 新增空布局ViewHolder
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }
}