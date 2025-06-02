package com.example.myapplicationt1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class CompletionRequestAdapter extends RecyclerView.Adapter<CompletionRequestAdapter.RequestViewHolder> {

    private final List<CompletionRequest> requests;
    private final Context context;
    private final FirebaseFirestore db;

    public CompletionRequestAdapter(Context context, List<CompletionRequest> requests) {
        this.context = context;
        this.requests = requests;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_completion_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        CompletionRequest request = requests.get(position);
        holder.tvStudentName.setText("שם החניך: " + request.getStudentName());

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvCompletionDate.setText("תאריך השלמה: " + format.format(request.getCompletionDate()));
        holder.tvMissingDate.setText(request.getMissingDate() != null ? "חיסור: " + format.format(request.getMissingDate()) : "אין תאריך חיסור");

        db.collection("users").document(request.getSubmittedBy()).get()
                .addOnSuccessListener(snapshot -> {
                    String guideName = snapshot.getString("fullName");
                    holder.tvGuideName.setText("מדריך: " + (guideName != null ? guideName : "לא ידוע"));
                });

        holder.btnApprove.setOnClickListener(v -> {
            db.collection("completions").document(request.getStudentName() + "_" + request.getCompletionDate().getTime())
                    .update("approved", true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "אושר", Toast.LENGTH_SHORT).show();
                        requests.remove(position);
                        notifyItemRemoved(position);
                    });
        });

        holder.btnReject.setOnClickListener(v -> {
            db.collection("completions").document(request.getStudentName() + "_" + request.getCompletionDate().getTime())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "הבקשה נמחקה", Toast.LENGTH_SHORT).show();
                        requests.remove(position);
                        notifyItemRemoved(position);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvGuideName, tvCompletionDate, tvMissingDate;
        Button btnApprove, btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvGuideName = itemView.findViewById(R.id.tvGuideName);
            tvCompletionDate = itemView.findViewById(R.id.tvCompletionDate);
            tvMissingDate = itemView.findViewById(R.id.tvMissingDate);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
