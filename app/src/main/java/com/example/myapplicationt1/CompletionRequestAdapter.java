package com.example.myapplicationt1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
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
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String managerId = currentUser != null ? currentUser.getUid() : "unknown";

            db.collection("completions").document(request.getId())
                    .update(
                            "status", "approved",
                            "type", "שיעור נוסף",
                            "approvedBy", managerId,
                            "decisionAt", FieldValue.serverTimestamp(),
                            "seenByGuide", false
                    )

                    .addOnSuccessListener(aVoid -> {

                        // בדיקה אם כבר קיים שיעור נוסף באותו תאריך לאותו חניך
                        db.collection("schedule")
                                .whereEqualTo("studentName", request.getStudentName())
                                .whereEqualTo("date", request.getCompletionDate())
                                .get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        Toast.makeText(context, "כבר קיים שיעור בלוח בתאריך הזה", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // יצירת שיעור נוסף בלוח
                                    Map<String, Object> scheduleData = new HashMap<>();
                                    scheduleData.put("studentName", request.getStudentName());
                                    scheduleData.put("guideId", request.getSubmittedBy());
                                    scheduleData.put("isCompletion", true);
                                    scheduleData.put("type", "שיעור נוסף");
                                    scheduleData.put("date", request.getCompletionDate());
                                    scheduleData.put("time", "17:00 - 19:00");
                                    scheduleData.put("day", getDayOfWeek(request.getCompletionDate()));

                                    db.collection("schedule")
                                            .add(scheduleData)
                                            .addOnSuccessListener(ref -> {
                                                Toast.makeText(context, "השיעור נוסף ללוח החוגים", Toast.LENGTH_SHORT).show();
                                                requests.remove(position);
                                                notifyItemRemoved(position);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "שגיאה בהוספה ללוח: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                });

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "שגיאה באישור הבקשה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });



        holder.btnReject.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String managerId = currentUser != null ? currentUser.getUid() : "unknown";

            db.collection("completions").document(request.getId())
                    .update(
                            "status", "rejected",
                            "rejectedBy", managerId,
                            "decisionAt", FieldValue.serverTimestamp(),
                            "seenByGuide", false
                    )

                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "הבקשה נדחתה", Toast.LENGTH_SHORT).show();
                        requests.remove(position);
                        notifyItemRemoved(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private String getDayOfWeek(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("he", "IL"));
        return sdf.format(date); // לדוגמה: "יום שלישי"
    }

}
