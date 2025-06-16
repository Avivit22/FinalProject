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

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private final List<DocumentSnapshot> pendingRequests;

    public PendingRequestsAdapter(List<DocumentSnapshot> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = pendingRequests.get(position);

        String student = doc.getString("studentName");
        String regularDay = doc.getString("regularDay");

        Timestamp ts = doc.getTimestamp("completionDate");
        String date = ts != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate()) : "תאריך לא ידוע";

        holder.tvTitle.setText("בקשה עבור: " + student);
        holder.tvDate.setText("שיעור נוסף ליום " + regularDay + " בתאריך: " + date);
        holder.tvStatus.setText("ממתין לאישור");
    }

    @Override
    public int getItemCount() {
        return pendingRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPendingTitle);
            tvDate = itemView.findViewById(R.id.tvPendingDate);
            tvStatus = itemView.findViewById(R.id.tvPendingStatus);
        }
    }
}
