package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
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
    private Set<String> blockedDates = new HashSet<>();
    private TextWatcher searchWatcher;



    interface OnDateSelected {
        void onDateSelected(String date);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completion_class);

        setFormEnabled(false); // ← השבתת הטופס כבר בהתחלה

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
            checkIfFormIsValid();
        });

        noButton.setOnClickListener(v -> {
            requiresManagerApproval = !isManager;

            // הסתרת תאריך החיסור
            findViewById(R.id.missing_date_label).setVisibility(View.GONE);
            findViewById(R.id.missing_date_button).setVisibility(View.GONE);
            missingDateSelected.setVisibility(View.GONE);
            missingDate = "";
            missingDateSelected.setText("");

            if (!isManager) {
                Toast.makeText(this, "הבקשה תישלח לאישור מנהל", Toast.LENGTH_SHORT).show();
                saveCompletion(); // מדריך - שמירה מיידית
            } else {
                Toast.makeText(this, "שיעור נרשם כמנהל - אושר אוטומטית", Toast.LENGTH_SHORT).show();
                saveCompletion(); // גם אצל מנהל - שמירה מיידית
            }
        });

        completionDateButton.setOnClickListener(v -> {
            showMaterialDatePicker((date) -> {
                completionDate = date;
                completionDateSelected.setText("התאריך שנבחר: " + date);
                completionDateSelected.setVisibility(View.VISIBLE);
                checkIfFormIsValid();
            });
        });

        missingDateButton.setOnClickListener(v -> {
            showMissingDatePicker((date) -> {
                missingDate = date;
                missingDateSelected.setText("התאריך שנבחר: " + date);
                missingDateSelected.setVisibility(View.VISIBLE);
                checkIfFormIsValid();
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

        searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    searchStudentView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0);
                } else {
                    searchStudentView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    resetForm();
                    setFormEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        searchStudentView.addTextChangedListener(searchWatcher);


        searchStudentView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = searchStudentView.getCompoundDrawables()[2]; // ימני
                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();
                    int touchX = (int) event.getX();
                    int viewWidth = searchStudentView.getWidth();
                    int paddingEnd = searchStudentView.getPaddingEnd();

                    if (touchX >= (viewWidth - paddingEnd - drawableWidth)) {
                        // לחצו על ה-X
                        searchStudentView.setText(""); // אופציונלי
                        resetForm();
                        setFormEnabled(false);
                        return true;
                    }
                }
            }
            return false;
        });








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
                        setFormEnabled(true); // ← הפעלת הטופס
                        checkIfFormIsValid();
                    });
                });

    }

    private void setFormEnabled(boolean enabled) {
        LinearLayout formContainer = findViewById(R.id.formContainer);
        formContainer.setAlpha(enabled ? 1f : 0.4f);
        setViewAndChildrenEnabled(formContainer, enabled);
    }

    private void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setViewAndChildrenEnabled(group.getChildAt(i), enabled);
            }
        }
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

                        loadBlockedDatesForStudent(studentName); // ⬅️ הוספה חשובה
                    }
                });
    }

    private void loadBlockedDatesForStudent(String studentName) {
        blockedDates.clear();

        // Load from completions
        db.collection("completions")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot doc : docs) {
                        Timestamp ts = doc.getTimestamp("completionDate");
                        if (ts != null) {
                            String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
                            blockedDates.add(formatted);
                        }
                    }

                    // Load from schedule
                    db.collection("schedule")
                            .whereEqualTo("studentName", studentName)
                            .get()
                            .addOnSuccessListener(docs2 -> {
                                for (DocumentSnapshot doc : docs2) {
                                    Timestamp ts = doc.getTimestamp("date");
                                    if (ts != null) {
                                        String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
                                        blockedDates.add(formatted);
                                    }
                                }
                            });
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



    private void showMaterialDatePicker(OnDateSelected listener) {
        CalendarConstraints.DateValidator validator = new CustomDateValidator(regularDay, blockedDates);

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(validator)
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("בחר תאריך שיעור השלמה")
                .setCalendarConstraints(constraints)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);
            String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
            listener.onDateSelected(formatted);
        });

        datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }




    private static String normalizeHebrewDay(String day) {
        return day.replace("יום ", "").trim();  // "יום חמישי" -> "חמישי"
    }

    private void showMissingDatePicker(OnDateSelected listener) {
        CalendarConstraints.DateValidator validator = new CalendarConstraints.DateValidator() {
            @Override
            public boolean isValid(long dateMillis) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(dateMillis);

                String dayOfWeek = new SimpleDateFormat("EEEE", new Locale("he", "IL")).format(cal.getTime());
                String normalizedDay = normalizeHebrewDay(dayOfWeek);

                // מאשרים רק אם זה היום הקבוע של החניך או מופיע כבר בלוח כשיעור השלמה
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());

                return normalizeHebrewDay(normalizedDay).equals(normalizeHebrewDay(regularDay)) ||
                        blockedDates.contains(formatted);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {}
        };

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(validator)
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("בחר תאריך חיסור")
                .setCalendarConstraints(constraints)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selection);
            String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
            listener.onDateSelected(formatted);
        });

        datePicker.show(getSupportFragmentManager(), "MISSING_DATE_PICKER");
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
        searchStudentView.removeTextChangedListener(searchWatcher);
        searchStudentView.setText("");
        searchStudentView.addTextChangedListener(searchWatcher);

        regularDayText.setText("");
        completionDateSelected.setText("");
        completionDateSelected.setVisibility(View.GONE);
        missingDateSelected.setText("");
        missingDateSelected.setVisibility(View.GONE);

        findViewById(R.id.missing_date_selected).setVisibility(View.GONE);
        findViewById(R.id.missing_date_button).setVisibility(View.GONE);
        findViewById(R.id.missing_date_label).setVisibility(View.GONE);

        setFormEnabled(false);

        searchStudentView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

    }

    //הפעלת הכפתור שמור רק כשיש גם תלמיד וגם תאריך
    private void checkIfFormIsValid() {
        boolean valid = !selectedStudentName.isEmpty()
                && (!requiresManagerApproval ? !missingDate.isEmpty() : true)
                && !completionDate.isEmpty();
        saveButton.setEnabled(valid);
        Button noButton = findViewById(R.id.no_button);
        noButton.setEnabled(!completionDate.isEmpty()); //  נועל את הכפתור "לא" עד שיש תאריך השלמה
    }

    public static class CustomDateValidator implements CalendarConstraints.DateValidator {
        private final String regularDay;
        private final Set<String> blockedDates;

        public CustomDateValidator(String regularDay, Set<String> blockedDates) {
            this.regularDay = regularDay;
            this.blockedDates = blockedDates;
        }

        protected CustomDateValidator(Parcel in) {
            regularDay = in.readString();
            blockedDates = new HashSet<>();
            in.readStringList(new ArrayList<>(blockedDates));
        }

        @Override
        public boolean isValid(long dateMillis) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dateMillis);

            String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());

            // חסימת תאריכי עבר
            Calendar today = Calendar.getInstance();
            if (cal.before(today)) return false;

            // חסימת ימים קבועים
            String dayOfWeek = new SimpleDateFormat("EEEE", new Locale("he", "IL")).format(cal.getTime());
            if (normalizeHebrewDay(dayOfWeek).equals(normalizeHebrewDay(regularDay))) return false;

            // חסימת תאריכים שכבר תפוסים
            return !blockedDates.contains(formattedDate);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(regularDay);
            dest.writeStringList(new ArrayList<>(blockedDates));
        }

        public static final Creator<CustomDateValidator> CREATOR = new Creator<CustomDateValidator>() {
            @Override
            public CustomDateValidator createFromParcel(Parcel in) {
                return new CustomDateValidator(in);
            }

            @Override
            public CustomDateValidator[] newArray(int size) {
                return new CustomDateValidator[size];
            }
        };
    }
}
