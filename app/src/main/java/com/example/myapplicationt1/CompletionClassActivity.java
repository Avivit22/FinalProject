package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;



public class CompletionClassActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView searchStudentView;
    private TextView regularDayText;
    private TextView completionDateSelected, missingDateSelected;
    private Button saveButton;
    private String selectedStudentName = "";
    private String regularDay = "";
    private String completionDate = "";
    private String missingDate = "";
    private boolean requiresManagerApproval = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completion_class);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI
        searchStudentView = findViewById(R.id.search_student);
        regularDayText = findViewById(R.id.regular_day);
        completionDateSelected = findViewById(R.id.completion_date_selected);
        missingDateSelected = findViewById(R.id.missing_date_selected);
        saveButton = findViewById(R.id.save_button);

        Button yesButton = findViewById(R.id.yes_button);
        Button noButton = findViewById(R.id.no_button);
        Button completionDateButton = findViewById(R.id.completion_date_button);
        Button missingDateButton = findViewById(R.id.missing_date_button);
        TextView missingDateLabel = findViewById(R.id.missing_date_label);

        missingDateLabel.setVisibility(View.GONE);
        missingDateButton.setVisibility(View.GONE);

        yesButton.setOnClickListener(v -> {
            requiresManagerApproval = false;
            missingDateLabel.setVisibility(View.VISIBLE);
            missingDateButton.setVisibility(View.VISIBLE);
        });

        noButton.setOnClickListener(v -> {
            requiresManagerApproval = true;
            missingDateLabel.setVisibility(View.GONE);
            missingDateButton.setVisibility(View.GONE);
            missingDate = "";
            Toast.makeText(this, "הבקשה תישלח לאישור מנהל", Toast.LENGTH_SHORT).show();
        });

        completionDateButton.setOnClickListener(v -> {
            showDatePickerDialog((date) -> {
                completionDate = date;
                completionDateSelected.setText("התאריך שנבחר: " + date);
                completionDateSelected.setVisibility(View.VISIBLE);
            });
        });

        missingDateButton.setOnClickListener(v -> {
            showDatePickerDialog((date) -> {
                missingDate = date;
                missingDateSelected.setText("התאריך שנבחר: " + date);
                missingDateSelected.setVisibility(View.VISIBLE);
            });
        });

        // 🔎 Load student names for AutoComplete
        loadStudentNames();

        // שמירה
        saveButton.setOnClickListener(v -> saveCompletion());

        // ניווט בלחיצה על לוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());
    }

    private void loadStudentNames() {
        db.collection("students")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("fullName");
                        if (name != null) names.add(name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                    searchStudentView.setAdapter(adapter);

                    searchStudentView.setOnItemClickListener((parent, view, position, id) -> {
                        selectedStudentName = parent.getItemAtPosition(position).toString();
                        fetchRegularDayForStudent(selectedStudentName);
                    });
                });
    }

    private void fetchRegularDayForStudent(String studentName) {
        db.collection("students")
                .whereEqualTo("fullName", studentName)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String day = query.getDocuments().get(0).getString("dayOfWeek");
                        regularDay = day != null ? day : "";
                        regularDayText.setText(regularDay);
                    }
                });
    }

    private void saveCompletion() {
        if (selectedStudentName.isEmpty() || completionDate.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date parsedCompletionDate = null;
        Date parsedMissingDate = null;

        try {
            parsedCompletionDate = format.parse(completionDate);
            if (!missingDate.isEmpty()) {
                parsedMissingDate = format.parse(missingDate);
            }
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בפענוח התאריכים", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studentName", selectedStudentName);
        data.put("regularDay", regularDay);
        data.put("completionDate", parsedCompletionDate); // Timestamp בפיירסטור
        data.put("missingDate", parsedMissingDate);       // Timestamp בפיירסטור
        data.put("requiresManagerApproval", requiresManagerApproval);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("completions")
                .add(data)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this, "השלמה נרשמה בהצלחה", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void showDatePickerDialog(OnDateSelected listener) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    listener.onDateSelected(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    interface OnDateSelected {
        void onDateSelected(String date);
    }

    // פונקציה לשאיבת סוג המשתמש מ-Firestore והעברה בהתאם לעמוד הבית של מנהל או מדריך
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
                                intent = new Intent(CompletionClassActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(CompletionClassActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(CompletionClassActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(CompletionClassActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(CompletionClassActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }
}
