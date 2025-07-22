package com.example.myapplicationt1;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  (RecyclerView) מותאם אישית להצגת בקשות שיעור השלמה ברשימה Adapter
 * אחראי להציג מידע על כל בקשה, ולאפשר אישור או דחייה שלה מצד המנהל.
 * תומך גם במצב היסטוריה (isHistoryMode) שבו אפשר לשנות סטטוס בקשות שכבר אושרו/נדחו.
 */
public class CompletionRequestAdapter extends RecyclerView.Adapter<CompletionRequestAdapter.RequestViewHolder> {

    private final List<CompletionRequest> requests;
    private final Context context;
    private final FirebaseFirestore db;
    private final boolean isHistoryMode;

    /**
     * בנאי.
     */
    public CompletionRequestAdapter(Context context, List<CompletionRequest> requests, boolean isHistoryMode) {
        this.context = context;
        this.requests = requests;
        this.db = FirebaseFirestore.getInstance();
        this.isHistoryMode = isHistoryMode;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת ViewHolder חדש עם פריסת item_completion_request
        View view = LayoutInflater.from(context).inflate(R.layout.item_completion_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        CompletionRequest request = requests.get(position);

        // הצגת שם החניך
        holder.tvStudentName.setText("שם החניך: " + request.getStudentName());

        // עיבוד תאריכים
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvCompletionDate.setText("תאריך השלמה: " + format.format(request.getCompletionDate()));
        holder.tvMissingDate.setText(request.getMissingDate() != null ? "חיסור: " + format.format(request.getMissingDate()) : "אין תאריך חיסור");

        // הצגת שם המדריך
        db.collection("users").document(request.getSubmittedBy()).get()
                .addOnSuccessListener(snapshot -> {
                    String guideName = snapshot.getString("fullName");
                    holder.tvGuideName.setText("מדריך: " + (guideName != null ? guideName : "לא ידוע"));
                });

        // לחיצה על אישור בקשה
        holder.btnApprove.setOnClickListener(v -> {
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


        // לחיצה על דחייה
        holder.btnReject.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String managerId = currentUser != null ? currentUser.getUid() : "unknown";

            db.collection("completions").document(request.getId())
                    .update(
                            "status", "rejected",
                            "type", "שיעור נוסף",
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

        // מצב היסטוריה — הסתרת כפתורי אישור/דחייה, הצגת סטטוס וכפתור עדכון
        if (isHistoryMode) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

            // הצג סטטוס
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("סטטוס: " + (request.getStatus().equals("approved") ? "אושר" : "נדחה"));

            // כפתור עדכון
            holder.btnUpdate.setVisibility(View.VISIBLE);
            holder.btnUpdate.setOnClickListener(v -> showUpdateDialog(request, position));
        } else {
            holder.tvStatus.setVisibility(View.GONE);
            holder.btnUpdate.setVisibility(View.GONE);
        }

        // צבע סטטוס
        holder.tvStatus.setTextColor(
                request.getStatus().equals("approved") ? Color.GREEN : Color.RED
        );



    }

    /**
     *   פונקציה למשיכת היום בשבוע לפי התאריך
     */
    private String getDayOfWeek(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("he", "IL"));
        return sdf.format(date); // לדוגמה: "יום שלישי"
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }


    /**
     * ViewHolder עבור פריטי הבקשות.
     */
    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvGuideName, tvCompletionDate, tvMissingDate, tvStatus;
        Button btnApprove, btnReject, btnUpdate;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvGuideName = itemView.findViewById(R.id.tvGuideName);
            tvCompletionDate = itemView.findViewById(R.id.tvCompletionDate);
            tvMissingDate = itemView.findViewById(R.id.tvMissingDate);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
        }
    }

    /**
     * הצגת דיאלוג עדכון סטטוס בקשה בהיסטוריה.
     */
    private void showUpdateDialog(CompletionRequest request, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_update_request, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView dialogTitle = view.findViewById(R.id.dialogTitle);
        TextView dialogMessage = view.findViewById(R.id.dialogMessage);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // עדכון הטקסט לפי סטטוס
        if (request.getStatus().equals("rejected")) {
            btnConfirm.setText("אשר");
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_green_light));
        } else if (request.getStatus().equals("approved")) {
            btnConfirm.setText("דחה");
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_light));
        }

        // פעולה בעת לחיצה על כפתור אישור
        btnConfirm.setOnClickListener(v -> {
            if (request.getStatus().equals("rejected")) {
                approveRequest(request, position);
            } else {
                rejectRequest(request, position);
            }
            dialog.dismiss();  // סגור את הדיאלוג לאחר הפעולה
        });

        // פעולה בעת לחיצה על ביטול
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * פונקציה לעדכון בקשה לאישור במצב היסטוריה.
     */
    private void approveRequest(CompletionRequest request, int position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String managerId = currentUser != null ? currentUser.getUid() : "unknown";

        db.collection("completions").document(request.getId())
                .update(
                        "status", "approved",
                        "approvedBy", managerId,
                        "decisionAt", FieldValue.serverTimestamp(),
                        "seenByGuide", false
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "הבקשה אושרה", Toast.LENGTH_SHORT).show();
                    request.setStatus("approved");
                    notifyItemChanged(position);
                });
    }

    /**
     * פונקציה לעדכון בקשה לדחייה במצב היסטוריה.
     */
    private void rejectRequest(CompletionRequest request, int position) {
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
                    request.setStatus("rejected");
                    notifyItemChanged(position);
                });
    }





}
