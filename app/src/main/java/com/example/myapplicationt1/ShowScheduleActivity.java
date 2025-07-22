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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * פעילות להצגת לוח חוגים לפי יום נבחר, כולל מדריכים ותלמידים.
 */
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

        // לחיצה על לוגו — מעבר למסך הראשי לפי סוג המשתמש
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

        // מאזין לבחירת תאריך בלוח
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            // שם יום באנגלית
            String dayNameEnglish = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toUpperCase();

            // שם יום בעברית
            String dayNameHebrew = convertDayToHebrew(dayNameEnglish); // נוסיף תרגום לעברית

            // תאריך נבחר בפורמט dd/MM/yyyy
            String selectedDateFormatted = dayOfMonth + "/" + (month + 1) + "/" + year;

            // עדכון שעות החוג בתצוגה
            String schedule = scheduleMap.getOrDefault(dayNameEnglish, "אין חוגים ביום זה");
            tvSchedule.setText("שעות החוג: " + schedule);

            // טעינת מדריכים ותלמידים
            loadGuidesForDay(dayNameHebrew); // נשלח את השם בעברית לשאילתה!
            loadStudentsAndCompletionsForDay(dayNameHebrew, selectedDateFormatted);

        });

        //  ה-RecyclerView של המדריכים:
        RecyclerView rvInstructors = findViewById(R.id.rvInstructors);
        List<String> guideNames = new ArrayList<>();
        GuideAdapter adapter = new GuideAdapter(guideNames);
        rvInstructors.setAdapter(adapter);
    }

    /**
     * פונקציה לנווט חזרה לדף הראשי לפי סוג המשתמש.
     */
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

    /**
     * פונקציה להמרת שם יום באנגלית לעברית.
     */
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

    /**
     * טעינת רשימת מדריכים עבור יום ספציפי.
     */
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

    /**
     * טעינת תלמידים ושיעורי השלמה/חיסור ליום ותאריך ספציפי.
     */
    private void loadStudentsAndCompletionsForDay(String dayName, String selectedDateFormatted) {
        RecyclerView rvStudents = findViewById(R.id.rvStudents);
        List<String> studentNames = new ArrayList<>();
        Map<String, String> completionTypes = new HashMap<>();

        // המרה מ-String לתאריך
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date parsedDate;
        try {
            parsedDate = format.parse(selectedDateFormatted);
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בפענוח תאריך", Toast.LENGTH_SHORT).show();
            return;
        }

        Date finalSelectedDate = parsedDate; // חייב להיות final עבור ה-Lambda

        // שליפת תלמידים קבועים
        db.collection("students")
                .whereEqualTo("dayOfWeek", dayName)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String name = doc.getString("fullName");
                        if (name != null) {
                            studentNames.add(name);
                        }
                    }

                    // שליפת שיעורי השלמה לתאריך זה
                    db.collection("completions")
                            .whereEqualTo("completionDate", finalSelectedDate)
                            .get()
                            .addOnSuccessListener(completions -> {
                                for (DocumentSnapshot doc : completions) {
                                    String name = doc.getString("studentName");
                                    String type = doc.getString("type");
                                    Boolean requiresApproval = doc.getBoolean("requiresManagerApproval");
                                    String status = doc.getString("status");

                                    // תנאים להופעה בלוח החוגים:
                                    boolean shouldShow = false;
                                    if (requiresApproval != null && requiresApproval) {
                                        shouldShow = "approved".equals(status);
                                    } else {
                                        shouldShow = true; // לא נדרש אישור, תמיד נציג
                                    }

                                    if (shouldShow && name != null && !studentNames.contains(name)) {
                                        studentNames.add(name);
                                    }

                                    if (shouldShow && name != null && type != null) {
                                        completionTypes.put(name, type);
                                    }
                                }

                                // שליפת חיסורים שצריך להסיר מהתאריך הזה
                                db.collection("completions")
                                        .whereEqualTo("missingDate", finalSelectedDate)
                                        .get()
                                        .addOnSuccessListener(missing -> {
                                            for (DocumentSnapshot doc : missing) {
                                                String name = doc.getString("studentName");
                                                if (name != null) {
                                                    studentNames.remove(name);
                                                }
                                            }

                                            // סידור והתצוגה
                                            Collections.sort(studentNames);
                                            StudentAdapter adapter = new StudentAdapter(studentNames, completionTypes);
                                            rvStudents.setAdapter(adapter);
                                        });
                            });
                });
    }

}
