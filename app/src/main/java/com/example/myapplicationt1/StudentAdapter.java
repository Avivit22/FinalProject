package com.example.myapplicationt1;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;


public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final List<String> studentNames;

    private final Map<String, String> completionTypes;



    public StudentAdapter(List<String> studentNames, Map<String, String> completionTypes) {
        this.studentNames = studentNames;
        this.completionTypes = completionTypes;
    }


    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_for_show_schedule, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        String name = studentNames.get(position);
        holder.tvName.setText(name);

        String type = completionTypes.get(name);
        if ("שיעור השלמה".equals(type)) {
            holder.tvCompletionLabel.setText("(שיעור השלמה)");
            holder.tvCompletionLabel.setTextColor(Color.RED);
            holder.tvCompletionLabel.setVisibility(View.VISIBLE);
        } else if ("שיעור נוסף".equals(type)) {
            holder.tvCompletionLabel.setText("(שיעור נוסף)");
            holder.tvCompletionLabel.setTextColor(Color.parseColor("#FF9800")); // כתום
            holder.tvCompletionLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvCompletionLabel.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return studentNames.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCompletionLabel;

        public StudentViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvCompletionLabel = itemView.findViewById(R.id.tvCompletionLabel);
        }
    }
}

