package com.firecalc.tablet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    private final List<HistoryItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public HistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<HistoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.tvExpression.setText(item.getExpression());
        holder.tvResult.setText("= " + item.getResult());
        holder.tvTime.setText(item.getFormattedTime());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvExpression;
        final TextView tvResult;
        final TextView tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpression = itemView.findViewById(R.id.tvHistoryExpression);
            tvResult = itemView.findViewById(R.id.tvHistoryResult);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
        }
    }
}
