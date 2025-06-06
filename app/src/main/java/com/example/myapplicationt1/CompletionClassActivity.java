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
    private boolean isManager = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completion_class);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            if ("manager".equalsIgnoreCase(userType)) {
                                isManager = true;
                                Button noButton = findViewById(R.id.no_button);
                                noButton.setText("לא");
                                requiresManagerApproval = false; // אוטומטי אצל מנהל
                            }
                        }
                    });
        }


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
            if (!isManager) {
                requiresManagerApproval = true;
                findViewById(R.id.missing_date_label).setVisibility(View.GONE);
                findViewById(R.id.missing_date_button).setVisibility(View.GONE);
                missingDate = "";
                Toast.makeText(this, "הבקשה תישלח לאישור מנהל", Toast.LENGTH_SHORT).show();
            } else {
                // אם המנהל לוחץ – לא משנים את requiresManagerApproval
                Toast.makeText(this, "שיעור נרשם כמנהל - אושר אוטומטית", Toast.LENGTH_SHORT).show();
            }
        });


        completionDateButton.setOnClickListener(v -> {
            showDatePickerDialog((date) -> {
                completionDate = date;
                completionDateSelected.setText("התאריך שנבחר: " + date);
                completionDateSelected.setVisibility(View.VISIBLE);
                checkIfFormIsValid();
            });
        });

        missingDateButton.setOnClickListener(v -> {
            showDatePickerDialog((date) -> {
                missingDate = date;
                missingDateSelected.setText("התאריך שנבחר: " + date);
                missingDateSelected.setVisibility(View.VISIBLE);
            });
        });

        // Load student names for AutoComplete
        loadStudentNames();

        // שמירה
        saveButton.setOnClickListener(v -> saveCompletion());

        // ניווט בלחיצה על לוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());

        saveButton.setEnabled(false);
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
                        checkIfFormIsValid();
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
        Date parsedCompletionDate;
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
        data.put("completionDate", parsedCompletionDate);
        data.put("missingDate", parsedMissingDate);  // זה יכול להיות null
        data.put("status", (isManager || !requiresManagerApproval) ? "approved" : "pending");
        data.put("requiresManagerApproval", requiresManagerApproval);
        data.put("submittedAt", FieldValue.serverTimestamp());
        data.put("isCompletion", true);

        if (!requiresManagerApproval || isManager) {
            data.put("type", "שיעור השלמה"); //  נרשם כשיעור השלמה אוטומטית
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            data.put("submittedBy", user.getUid());
        } else {
            data.put("submittedBy", "unknown");
        }

        String docId = selectedStudentName + "_" + parsedCompletionDate.getTime();
        db.collection("completions")
                .document(docId)
                .set(data)
                .addOnSuccessListener(docRef -> {
                    if (isManager) {
                        Toast.makeText(this, "השיעור נשמר", Toast.LENGTH_LONG).show();
                    } else if (requiresManagerApproval) {
                        Toast.makeText(this, "הבקשה נשלחה לאישור מנהל", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "שיעור השלמה נרשם בהצלחה", Toast.LENGTH_LONG).show();
                    }

                    resetForm(); // איפוס הטופס לאחר שמירה
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשליחה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    // פונקציה לאיפוס הטופס לאחר שמירה ל-FireStore
    private void resetForm() {
        selectedStudentName = "";
        regularDay = "";
        completionDate = "";
        missingDate = "";
        requiresManagerApproval = false;

        // איפוס UI
        searchStudentView.setText("");
        regularDayText.setText("");
        completionDateSelected.setText("");
        completionDateSelected.setVisibility(View.GONE);
        missingDateSelected.setText("");
        missingDateSelected.setVisibility(View.GONE);

        findViewById(R.id.missing_date_selected).setVisibility(View.GONE);
        findViewById(R.id.missing_date_button).setVisibility(View.GONE);
        findViewById(R.id.missing_date_label).setVisibility(View.GONE);
    }

    //הפעלת הכפתור שמור רק כשיש גם תלמיד וגם תאריך
    private void checkIfFormIsValid() {
        boolean valid = !selectedStudentName.isEmpty() && !completionDate.isEmpty();
        saveButton.setEnabled(valid);
    }


}
