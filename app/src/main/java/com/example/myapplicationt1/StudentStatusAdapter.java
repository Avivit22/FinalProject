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

public class StudentStatusAdapter extends RecyclerView.Adapter<StudentStatusAdapter.StatusViewHolder> {

    private final List<StudentStatus> studentStatuses;
    private final Context context;

    public StudentStatusAdapter(Context context, List<StudentStatus> studentStatuses) {
        this.context = context;
        this.studentStatuses = studentStatuses;
    }


    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_status, parent, false);
        return new StatusViewHolder(view, parent.getContext());
    }


    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        StudentStatus status = studentStatuses.get(position);
        holder.bind(status);
    }

    @Override
    public int getItemCount() {
        return studentStatuses.size();
    }

    public List<StudentStatus> collectStatuses() {
        return studentStatuses;
    }

    public class StatusViewHolder extends RecyclerView.ViewHolder {
        private final Context context;

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

        public void bind(StudentStatus status) {
            tvStudentName.setText(status.getStudentName());

            //  אם notes כולל "השלמה" – נציג תווית אדומה
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


            // Restore selection
            switch (status.getStatus()) {
                case "נוכח": rbPresent.setChecked(true); break;
                case "חיסור": rbAbsent.setChecked(true); break;
                case "השלמה": rbReplacement.setChecked(true); break;
            }

            cbToran.setChecked(status.isToran());
            etNotes.setText(status.getNotes());

            // Listeners
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
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ShowUpdateStudentActivity.class);
                intent.putExtra("STUDENT_NAME", status.getStudentName());
                itemView.getContext().startActivity(intent);
            });


        }
    }

}
