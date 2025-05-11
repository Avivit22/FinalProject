package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.CalendarView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShowScheduleActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_schedule);

        // Firebase Init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // לוגו ניווט
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());

        // הצגת שעות החוגים
        CalendarView calendarView = findViewById(R.id.calendarView);
        TextView tvSchedule = findViewById(R.id.tvSchedule); // הוספנו את ה-TextView לשעות

        // מיפוי ימים לשעות קבועות
        Map<String, String> scheduleMap = new HashMap<>();
        scheduleMap.put("SUNDAY", "19:00 - 17:00");
        scheduleMap.put("MONDAY", "19:00 - 17:00");
        scheduleMap.put("TUESDAY", "19:00 - 17:00");
        scheduleMap.put("WEDNESDAY", "19:00 - 17:00");
        scheduleMap.put("THURSDAY", "19:00 - 17:00");
        scheduleMap.put("FRIDAY", "16:30 - 14:30");
        scheduleMap.put("SATURDAY", "אין חוגים");


        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            String dayNameEnglish = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toUpperCase();
            String dayNameHebrew = convertDayToHebrew(dayNameEnglish); // נוסיף תרגום לעברית

            String schedule = scheduleMap.getOrDefault(dayNameEnglish, "אין חוגים ביום זה");
            tvSchedule.setText("שעות החוג: " + schedule);

            loadGuidesForDay(dayNameHebrew); // נשלח את השם בעברית לשאילתה!
            loadStudentsForDay(dayNameHebrew);

        });

        //  ה-RecyclerView של המדריכים:
        RecyclerView rvInstructors = findViewById(R.id.rvInstructors);
        List<String> guideNames = new ArrayList<>();
        GuideAdapter adapter = new GuideAdapter(guideNames);
        rvInstructors.setAdapter(adapter);

        //  ה-RecyclerView של החניכים:
        RecyclerView rvStudents = findViewById(R.id.rvStudents);
        List<String> studentNames = new ArrayList<>();
        StudentAdapter studentAdapter = new StudentAdapter(studentNames);
        rvStudents.setAdapter(studentAdapter);
    }

    // ניווט לפי סוג משתמש
    private void routeUserBasedOnType() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            Intent intent;
                            if (userType != null && userType.equalsIgnoreCase("manager")) {
                                intent = new Intent(ShowScheduleActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(ShowScheduleActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertDayToHebrew(String englishDay) {
        switch (englishDay) {
            case "SUNDAY": return "ראשון";
            case "MONDAY": return "שני";
            case "TUESDAY": return "שלישי";
            case "WEDNESDAY": return "רביעי";
            case "THURSDAY": return "חמישי";
            case "FRIDAY": return "שישי";
            case "SATURDAY": return "שבת";
            default: return "";
        }
    }

    private void loadGuidesForDay(String dayName) {
        RecyclerView rvInstructors = findViewById(R.id.rvInstructors);
        List<String> guideNames = new ArrayList<>();
        GuideAdapter adapter = new GuideAdapter(guideNames);
        rvInstructors.setAdapter(adapter);

        db.collection("users")
                .whereEqualTo("userType", "guide")
                .whereEqualTo("dayOfWeek", dayName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    guideNames.clear();
                    queryDocumentSnapshots.forEach(document -> {
                        String name = document.getString("fullName");
                        if (name != null) {
                            guideNames.add(name);
                        }
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בשליפת מדריכים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadStudentsForDay(String dayName) {
        RecyclerView rvStudents = findViewById(R.id.rvStudents);
        List<String> studentNames = new ArrayList<>();
        StudentAdapter adapter = new StudentAdapter(studentNames);
        rvStudents.setAdapter(adapter);

        db.collection("students")
                .whereEqualTo("dayOfWeek", dayName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentNames.clear();
                    queryDocumentSnapshots.forEach(document -> {
                        String name = document.getString("fullName");
                        if (name != null) {
                            studentNames.add(name);
                        }
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בשליפת חניכים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


}
