package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Calendar;
import android.app.DatePickerDialog;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CompletionClassActivity extends AppCompatActivity {

    // הגדרת משתנים ברמת המחלקה
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completion_class); // טוען את קובץ ה-XML

        // אתחול כפתורים וטקסטים
        Button yesButton = findViewById(R.id.yes_button);
        TextView missingDateLabel = findViewById(R.id.missing_date_label);
        Button missingDateButton = findViewById(R.id.missing_date_button);
        Button completionDateButton = findViewById(R.id.completion_date_button);

        Button noButton = findViewById(R.id.no_button);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CompletionClassActivity.this, "הבקשה הועברה לאישור המנהל", Toast.LENGTH_SHORT).show();
            }
        });

        TextView completionDateSelected = findViewById(R.id.completion_date_selected);
        TextView missingDateSelected = findViewById(R.id.missing_date_selected);

        missingDateLabel.setVisibility(View.GONE);
        missingDateButton.setVisibility(View.GONE);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                missingDateLabel.setVisibility(View.VISIBLE);
                missingDateButton.setVisibility(View.VISIBLE);
            }
        });

        completionDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog("מועד שיעור השלמה", completionDateSelected);
            }
        });

        missingDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog("תאריך חיסור", missingDateSelected);
            }
        });

        // אתחול Firebase Auth ו-Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // מאזין ללחיצה על תמונת הלוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());
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

    // פונקציה לפתיחת לוח שנה
    private void showDatePickerDialog(String title, TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    targetTextView.setText("התאריך שנבחר: " + selectedDate);
                    targetTextView.setVisibility(View.VISIBLE);
                },
                year,
                month,
                day
        );

        datePickerDialog.setTitle(title);
        datePickerDialog.show();
    }
}
