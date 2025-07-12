package com.example.myapplicationt1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Pending Requests -להצגת רשימת בקשות ממתינות במסך ה Adapter.
 */
public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    // רשימת המסמכים המייצגת את הבקשות הממתינות
    private final List<DocumentSnapshot> pendingRequests;

    /**
     * בנאי שמקבל רשימת בקשות ממתינות
     * @param pendingRequests רשימת המסמכים מ-Firestore
     */
    public PendingRequestsAdapter(List<DocumentSnapshot> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    /**
     * יצירת ViewHolder חדש עבור כל פריט ברשימה
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    /**
     * קישור הנתונים מהבקשה הממתינה אל התצוגה של הפריט
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = pendingRequests.get(position);

        String student = doc.getString("studentName");  // שם החניך
        String regularDay = doc.getString("regularDay");  // היום הקבוע

        // תאריך שיעור ההשלמה
        Timestamp ts = doc.getTimestamp("completionDate");
        String date = ts != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate()) : "תאריך לא ידוע";

        // הצגת המידע בפריט
        holder.tvTitle.setText("בקשה עבור: " + student);
        holder.tvDate.setText("שיעור נוסף ליום " + regularDay + " בתאריך: " + date);
        holder.tvStatus.setText("ממתין לאישור");  // סטטוס קבוע לכל בקשות ממתינות
    }

    /**
     * מחזיר את מספר הפריטים ברשימה
     */
    @Override
    public int getItemCount() {
        return pendingRequests.size();
    }

    /**
     * מחלקת ViewHolder שמחזיקה את רכיבי ה-UI של פריט בודד
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPendingTitle);  // כותרת
            tvDate = itemView.findViewById(R.id.tvPendingDate);    // תאריך
            tvStatus = itemView.findViewById(R.id.tvPendingStatus);// סטטוס
        }
    }
}
