package com.example.myapplicationt1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {

    private List<String> guides;

    public GuideAdapter(List<String> guides) {
        this.guides = guides;
    }

    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_for_show_schedule, parent, false);
        return new GuideViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
        String guideName = guides.get(position);
        holder.tvInstructorName.setText(guideName);
    }

    @Override
    public int getItemCount() {
        return guides.size();
    }

    static class GuideViewHolder extends RecyclerView.ViewHolder {
        TextView tvInstructorName;

        GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInstructorName = itemView.findViewById(R.id.tvInstructorName);
        }
    }
}
