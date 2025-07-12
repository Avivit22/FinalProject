package com.example.myapplicationt1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

/**
 *   RecyclerViewמותאם אישית להצגת רשימת מדריכים בAdapter .
 * כל פריט מציג שם של מדריך (מחרוזת בלבד).
 */
public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {

    private List<String> guides;

    /**
     * בנאי
     */
    public GuideAdapter(List<String> guides) {
        this.guides = guides;
    }

    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת תצוגה (View) חדשה עבור פריט בודד מהרשימה (Item)
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_for_show_schedule, parent, false);
        return new GuideViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
        // מקבל את שם המדריך לפי מיקום ומציב בתיבת הטקסט
        String guideName = guides.get(position);
        holder.tvInstructorName.setText(guideName);
    }

    @Override
    public int getItemCount() {
        // מחזיר את כמות הפריטים ברשימה
        return guides.size();
    }

    /**
     * ViewHolder מותאם לפריט ברשימת המדריכים
     * מחזיק הפניה לרכיב TextView שמציג את שם המדריך.
     */
    static class GuideViewHolder extends RecyclerView.ViewHolder {
        TextView tvInstructorName;

        GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור לתיבת הטקסט מתוך ה-XML
            tvInstructorName = itemView.findViewById(R.id.tvInstructorName);
        }
    }
}
