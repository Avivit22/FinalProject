package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.util.Log;


public class InsertStatusActivity extends AppCompatActivity {

    // משתנים עבור Firebase Auth ו-Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private RecyclerView rvStudents;
    private StudentStatusAdapter adapter;
    private List<String> studentNames = new ArrayList<>();
    private String todayDate;
    private String todayHebrewDay;
    private List<StudentStatus> studentStatuses = new ArrayList<>();
    private Map<String, StudentStatus> existingAttendance = new HashMap<>();
    private List<StudentStatus> filteredStatuses = new ArrayList<>(); // תוצאה מסוננת


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_status);

        // אתחול Firebase Auth ו-Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // תאריך של היום (לשמירה)
        SimpleDateFormat stringFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        todayDate = stringFormat.format(new Date());

        SimpleDateFormat firebaseFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String todayDateFirestoreFormat = firebaseFormat.format(new Date());

        rvStudents = findViewById(R.id.rvStudentsStatus);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentStatusAdapter(this, filteredStatuses);
        rvStudents.setAdapter(adapter);

        // היום בשבוע בעברית (לשליפה)
        Calendar calendar = Calendar.getInstance();
        String dayEnglish = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toUpperCase();
        todayHebrewDay = convertDayToHebrew(dayEnglish);

        // טען את החניכים שרלוונטיים להיום
        loadStudentsForToday(todayHebrewDay);

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveStatuses());

        // מאזין ללחיצה על תמונת הלוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());

        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStudentsForToday(String todayHebrewDay) {
        loadExistingAttendance(() -> {
            studentStatuses.clear();
            filteredStatuses.clear();

            SimpleDateFormat firebaseFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date todayDateForQuery;
            try {
                todayDateForQuery = firebaseFormat.parse(
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())
                );
            } catch (Exception e) {
                Toast.makeText(this, "שגיאה בפורמט תאריך", Toast.LENGTH_SHORT).show();
                return;
            }

            //  חניכים קבועים
            db.collection("students")
                    .whereEqualTo("dayOfWeek", todayHebrewDay)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String name = doc.getString("fullName");
                            if (name == null) continue;

                            StudentStatus status = existingAttendance.containsKey(name)
                                    ? existingAttendance.get(name)
                                    : new StudentStatus(name, "", false, "", todayDate);

                            studentStatuses.add(status);
                        }

                        //  חניכים שמבצעים השלמה או שיעור נוסף
                        db.collection("completions")
                                .whereEqualTo("completionDate", todayDateForQuery)
                                .get()
                                .addOnSuccessListener(completions -> {
                                    for (DocumentSnapshot doc : completions.getDocuments()) {
                                        String name = doc.getString("studentName");
                                        String type = doc.getString("type"); // שיעור השלמה / שיעור נוסף

                                        if (name == null) continue;

                                        // סינון לפי הצורך באישור
                                        Boolean requiresApproval = doc.getBoolean("requiresManagerApproval");


                                        // אם צריך אישור – תוודא שאושרה
                                        if (requiresApproval != null && requiresApproval && !"approved".equals(doc.getString("status"))) {
                                            continue;
                                        }



                                        boolean alreadyExists = studentStatuses.stream()
                                                .anyMatch(s -> s.getStudentName().equals(name));

                                        if (!alreadyExists) {
                                            String note = "";
                                            if ("שיעור השלמה".equals(type)) {
                                                note = "(שיעור השלמה)";
                                            } else if ("שיעור נוסף".equals(type)) {
                                                note = "(שיעור נוסף)";
                                            }

                                            StudentStatus status = existingAttendance.containsKey(name)
                                                    ? existingAttendance.get(name)
                                                    : new StudentStatus(name, "", false, note, todayDate);

                                            studentStatuses.add(status);
                                        }
                                    }


                                    //  חניכים שמחסירים היום
                                    db.collection("completions")
                                            .whereEqualTo("missingDate", todayDateForQuery)
                                            .get()
                                            .addOnSuccessListener(missing -> {
                                                for (DocumentSnapshot doc : missing.getDocuments()) {
                                                    String name = doc.getString("studentName");
                                                    if (name != null) {
                                                        studentStatuses.removeIf(s -> s.getStudentName().equals(name));
                                                    }
                                                }

                                                Collections.sort(studentStatuses, Comparator.comparing(StudentStatus::getStudentName));
                                                filteredStatuses.addAll(studentStatuses);
                                                adapter.notifyDataSetChanged();
                                            });
                                });
                    });
        });
    }



    private void saveStatuses() {
        List<StudentStatus> statuses = adapter.collectStatuses();
        for (StudentStatus s : statuses) {
            String docId = s.getStudentName() + "_" + s.getDate();

            db.collection("attendance")
                    .document(docId)
                    .set(s.toMap(), SetOptions.merge()) //  לא מוחק שדות קיימים
                    .addOnSuccessListener(aVoid -> {
                        // הצלחה אחת לכל חניך
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        Toast.makeText(this, "הנוכחות עודכנה בהצלחה", Toast.LENGTH_SHORT).show();
    }

    //  פונקציה לקבלת סוג המשתמש והעברה למסך בית מנהל או מדריך בהתאם לסוג משתמש
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
                                intent = new Intent(InsertStatusActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(InsertStatusActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(InsertStatusActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(InsertStatusActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(InsertStatusActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
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

    private void loadExistingAttendance(Runnable onComplete) {
        db.collection("attendance")
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(query -> {
                    existingAttendance.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        StudentStatus status = new StudentStatus(
                                doc.getString("studentName"),
                                doc.getString("status"),
                                doc.getBoolean("toran") != null && doc.getBoolean("toran"),
                                doc.getString("notes") != null ? doc.getString("notes") : "",
                                doc.getString("date")
                        );
                        existingAttendance.put(status.getStudentName(), status);
                    }
                    onComplete.run(); //  אחרי שסיים לטעון – ממשיך
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת נוכחות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    onComplete.run(); // גם בשגיאה נמשיך
                });
    }

    private void filterList(String query) {
        filteredStatuses.clear();
        for (StudentStatus status : studentStatuses) {
            if (status.getStudentName().toLowerCase().contains(query.toLowerCase())) {
                filteredStatuses.add(status);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
