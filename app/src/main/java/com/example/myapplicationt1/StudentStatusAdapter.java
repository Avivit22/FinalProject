package com.example.myapplicationt1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView עבור הצגת מצב נוכחות של חניכים ברשימת Adapter.
 * מאפשר עדכון סטטוס, תורן, הערות, ותצוגת פרטי חניך.
 */
public class StudentStatusAdapter extends RecyclerView.Adapter<StudentStatusAdapter.StatusViewHolder> {

    private final List<StudentStatus> studentStatuses; // רשימת המצב של כל החניכים
    private final Context context;

    public StudentStatusAdapter(Context context, List<StudentStatus> studentStatuses) {
        this.context = context;
        this.studentStatuses = studentStatuses;
    }


    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת ה-View עבור כל חניך מתוך קובץ ה-layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_status, parent, false);
        return new StatusViewHolder(view, parent.getContext());
    }


    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        // קשירת הנתונים של חניך ספציפי לתוך ה-ViewHolder
        StudentStatus status = studentStatuses.get(position);
        holder.bind(status);
    }

    @Override
    public int getItemCount() {
        return studentStatuses.size();
    }  // מחזיר כמה חניכים יש לנו ברשימה

    /**
     * פונקציה לאיסוף הסטטוסים המעודכנים מהטופס.
     */
    public List<StudentStatus> collectStatuses() {
        return studentStatuses;
    }

    /**
     * פנימי עבור הצגת חניך אחד ברשימה ViewHolder  .
     */
    public class StatusViewHolder extends RecyclerView.ViewHolder {
        private final Context context;

        // אלמנטים מה-layout
        TextView tvStudentName;
        TextView tvSpecialLabel;
        RadioGroup rgAttendanceStatus;
        RadioButton rbPresent, rbAbsent, rbReplacement;
        CheckBox cbToran;
        EditText etNotes;
        Button btnDetails;
        public StatusViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;

            // אתחול Views
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvSpecialLabel = itemView.findViewById(R.id.tvSpecialLabel); // חדש
            rgAttendanceStatus = itemView.findViewById(R.id.rgAttendanceStatus);
            rbPresent = itemView.findViewById(R.id.rbPresent);
            rbAbsent = itemView.findViewById(R.id.rbAbsent);
            rbReplacement = itemView.findViewById(R.id.rbReplacement);
            cbToran = itemView.findViewById(R.id.cbToran);
            etNotes = itemView.findViewById(R.id.etNotes);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }

        /**
         * פונקציה לקשירת נתוני הסטטוס של חניך אחד ל-View.
         */
        public void bind(StudentStatus status) {
            tvStudentName.setText(status.getStudentName());

            //  אם notes כולל "השלמה" או "שיעור נוסף" – נציג תווית אדומה
            if (status.getNotes() != null) {
                if (status.getNotes().contains("שיעור השלמה")) {
                    tvSpecialLabel.setText("(שיעור השלמה)");
                    tvSpecialLabel.setTextColor(Color.RED);
                    tvSpecialLabel.setVisibility(View.VISIBLE);
                } else if (status.getNotes().contains("שיעור נוסף")) {
                    tvSpecialLabel.setText("(שיעור נוסף)");
                    tvSpecialLabel.setTextColor(Color.parseColor("#FF5722")); // כתום
                    tvSpecialLabel.setVisibility(View.VISIBLE);
                } else {
                    tvSpecialLabel.setVisibility(View.GONE);
                }
            } else {
                tvSpecialLabel.setVisibility(View.GONE);
            }


            // שחזור סטטוס נבחר
            switch (status.getStatus()) {
                case "נוכח": rbPresent.setChecked(true); break;
                case "חיסור": rbAbsent.setChecked(true); break;
                case "השלמה": rbReplacement.setChecked(true); break;
            }

            cbToran.setChecked(status.isToran());
            etNotes.setText(status.getNotes());

            // מאזינים לעדכונים
            rgAttendanceStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == rbPresent.getId()) status.setStatus("נוכח");
                else if (checkedId == rbAbsent.getId()) status.setStatus("חיסור");
                else if (checkedId == rbReplacement.getId()) status.setStatus("השלמה");
            });

            cbToran.setOnCheckedChangeListener((v, isChecked) -> status.setToran(isChecked));

            etNotes.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    status.setNotes(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // כפתור להצגת פרטי החניך
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ShowUpdateStudentActivity.class);
                intent.putExtra("STUDENT_NAME", status.getStudentName());
                itemView.getContext().startActivity(intent);
            });


        }
    }

}
