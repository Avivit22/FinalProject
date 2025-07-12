package com.example.myapplicationt1;

import android.graphics.Color;
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
 * אדאפטר (Adapter) עבור RecyclerView להצגת התראות בממשק המשתמש.
 * כל התראה מציגה מידע על בקשת שיעור נוסף (מאושר/נדחה), תאריך, סטטוס וכו'.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<DocumentSnapshot> notifications;

    /**
     * בנאי שמקבל את רשימת ההתראות להצגה.
     * @param notifications רשימת התראות
     */
    public NotificationAdapter(List<DocumentSnapshot> notifications) {
        this.notifications = notifications;
    }

    /**
     * יצירת ViewHolder חדש (קריאה ראשונית כאשר אין פריטים ממוחזרים).
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // תצוגת פריט בודד מתוך ה-XML
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    /**
     * קשירת נתונים לכל פריט ברשימה (לפי מיקום).
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = notifications.get(position);

        String student = doc.getString("studentName");
        String status = doc.getString("status");
        String regularDay = doc.getString("regularDay");

        // תאריך השיעור
        Timestamp ts = doc.getTimestamp("completionDate");
        String date = ts != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate()) : "תאריך לא ידוע";

        // כותרת ההתראה
        holder.tvTitle.setText("בקשה עבור: " + student);
        // תיאור התאריך והיום הקבוע
        holder.tvDate.setText("שיעור נוסף ליום " + regularDay + " בתאריך: " + date);

        // סטטוס הבקשה
        if ("approved".equals(status)) {
            holder.tvStatus.setText("✅ אושר ע\"י מנהל");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvStatus.setText("❌ נדחה ע\"י מנהל");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        }

        // בדיקה אם ההתראה נקראה
        Boolean seen = doc.getBoolean("seenByGuide");
        if (seen == null || !seen) {
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.unseen_notification_bg); // מודגש
        } else {
            holder.unreadDot.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(R.drawable.rounded_bg_lightgray);
        }
    }


    /**
     * כמות הפריטים ברשימה.
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * מחלקת ViewHolder פנימית המחזיקה הפניות ל-Views שבתוך פריט ההתראה.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus; // טקסטים עבור כותרת, תאריך, סטטוס
        View unreadDot;                     // נקודה קטנה המופיעה אם לא נקרא

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvDate = itemView.findViewById(R.id.tvNotificationDate);
            tvStatus = itemView.findViewById(R.id.tvNotificationStatus);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }

}
