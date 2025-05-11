package com.example.myapplicationt1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowUpdateStudentActivity extends AppCompatActivity {

    private static final String TAG = "ShowUpdateStudent";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView searchStudentAutoComplete;
    //private Button searchButton;
    private ScrollView detailsScrollView;


    private EditText fullNameEditText, organizationIdEditText, birthDateEditText,
            gradeEditText, phoneEditText, joinDateEditText, addressEditText,
            parent1NameEditText, parent2NameEditText, parentPhoneEditText;
    private Spinner genderSpinner, dayOfWeekSpinner;
    private ImageView profileImage;
    private Button uploadImageButton, saveChangesButton, deleteStudentButton, convertToGuideButton;

    private Uri newImageUri = null;
    private Bitmap currentProfileBitmap = null;
    private String currentStudentDocumentId = null;
    private Student currentStudent = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            newImageUri = result.getData().getData();
                            try {
                                currentProfileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), newImageUri);
                                profileImage.setImageBitmap(currentProfileBitmap);
                                Log.d(TAG, "New image selected and bitmap created.");
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading new image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה חדשה", Toast.LENGTH_SHORT).show();
                                newImageUri = null;
                                currentProfileBitmap = null;
                                // החזרת התמונה הקודמת אם הייתה
                                if (currentStudent != null && currentStudent.getProfileImageBase64() != null && !currentStudent.getProfileImageBase64().isEmpty()){
                                    loadBase64Image(currentStudent.getProfileImageBase64(), profileImage);
                                } else {
                                    profileImage.setImageBitmap(null);
                                }
                            }
                        } else {
                            Log.d(TAG, "New image selection cancelled or failed.");
                            newImageUri = null;
                            if (currentProfileBitmap != null) {
                                profileImage.setImageBitmap(currentProfileBitmap);
                            } else if (currentStudent != null && currentStudent.getProfileImageBase64() != null && !currentStudent.getProfileImageBase64().isEmpty()){
                                loadBase64Image(currentStudent.getProfileImageBase64(), profileImage);
                            } else {
                                profileImage.setImageBitmap(null);
                            }
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_update_student);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        setupListeners();
        loadStudentNamesForAutoComplete();
    }

    private void initializeUI() {
        searchStudentAutoComplete = findViewById(R.id.search_student);
        //searchButton = findViewById(R.id.searchButton);
        detailsScrollView = findViewById(R.id.detailsScrollView);

        fullNameEditText = findViewById(R.id.fullNameEditText);
        organizationIdEditText = findViewById(R.id.organizationIdEditText);
        birthDateEditText = findViewById(R.id.birthDateEditText);
        gradeEditText = findViewById(R.id.gradeEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);
        addressEditText = findViewById(R.id.addressEditText);
        parent1NameEditText = findViewById(R.id.parent1NameEditText);
        parent2NameEditText = findViewById(R.id.parent2NameEditText);
        parentPhoneEditText = findViewById(R.id.parentPhoneEditText);

        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveChangesButton = findViewById(R.id.saveChangesButton);
        deleteStudentButton = findViewById(R.id.deleteStudentButton);
        convertToGuideButton = findViewById(R.id.convertToGuideButton);

        detailsScrollView.setVisibility(View.GONE);
        clearStudentDetailsForm();
    }

    private void clearStudentDetailsForm() {
        fullNameEditText.setText("");
        organizationIdEditText.setText("");
        birthDateEditText.setText("");
        birthDateEditText.setHint("בחר תאריך");
        gradeEditText.setText("");
        phoneEditText.setText("");
        joinDateEditText.setText("");
        joinDateEditText.setHint("בחר תאריך");
        addressEditText.setText("");
        parent1NameEditText.setText("");
        parent2NameEditText.setText("");
        parentPhoneEditText.setText("");
        profileImage.setImageBitmap(null);
        currentProfileBitmap = null;
        newImageUri = null;
        if (genderSpinner.getAdapter() != null && genderSpinner.getAdapter().getCount() > 0) genderSpinner.setSelection(0);
        if (dayOfWeekSpinner.getAdapter() != null && dayOfWeekSpinner.getAdapter().getCount() > 0) dayOfWeekSpinner.setSelection(0);
    }


    private void setupListeners() {
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());

        searchStudentAutoComplete.setOnItemClickListener((parent, view, position, id) -> searchStudentByName());

        birthDateEditText.setOnClickListener(v -> showDatePickerDialog(birthDateEditText));
        joinDateEditText.setOnClickListener(v -> showDatePickerDialog(joinDateEditText));
        uploadImageButton.setOnClickListener(v -> openGalleryForNewImage());
        saveChangesButton.setOnClickListener(v -> saveStudentChanges());

        deleteStudentButton.setOnClickListener(v -> {
            if (currentStudentDocumentId != null) {
                showDeletePopup();
            } else {
                Toast.makeText(this, "נא לבחור חניך תחילה", Toast.LENGTH_SHORT).show();
            }
        });
        convertToGuideButton.setOnClickListener(v -> {
            Toast.makeText(this, "פונקציית המרה למדריך עוד לא מומשה", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadStudentNamesForAutoComplete() {
        db.collection("students")
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> studentNames = new ArrayList<>();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Boolean isActive = document.getBoolean("isActive");
                                if (isActive != null && isActive) { // רק אם השדה קיים והוא true
                                    String name = document.getString("fullName");
                                    if (name != null && !name.isEmpty()) {
                                        studentNames.add(name);
                                    }
                                }
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, studentNames);
                        searchStudentAutoComplete.setAdapter(adapter);
                    } else {
                        Log.e(TAG, "Error getting student names: ", task.getException());
                    }
                });
    }


    private void searchStudentByName() {
        String studentNameToSearch = searchStudentAutoComplete.getText().toString().trim();
        if (TextUtils.isEmpty(studentNameToSearch)) {
            Toast.makeText(this, "נא להזין שם חניך לחיפוש", Toast.LENGTH_SHORT).show();
            return;
        }
        clearStudentDetailsForm();
        detailsScrollView.setVisibility(View.GONE);
        currentStudentDocumentId = null;
        currentStudent = null;

        Log.d(TAG, "Searching for student: " + studentNameToSearch);

        db.collection("students") // חיפוש ב-collection students
                .whereEqualTo("fullName", studentNameToSearch)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            currentStudentDocumentId = document.getId();
                            currentStudent = document.toObject(Student.class);
                            if (currentStudent != null) {
                                //  בדיקה אם החניך פעיל לפני הצגת הפרטים
                                if (currentStudent.getIsActive() == null || currentStudent.getIsActive()) {
                                    populateStudentDetails(currentStudent);
                                    detailsScrollView.setVisibility(View.VISIBLE);
                                    Toast.makeText(ShowUpdateStudentActivity.this, "חניך נמצא", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ShowUpdateStudentActivity.this, "החניך אינו פעיל יותר", Toast.LENGTH_LONG).show();
                                    detailsScrollView.setVisibility(View.GONE);
                                    currentStudentDocumentId = null;
                                    currentStudent = null;
                                }
                            } else {
                                Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בהמרת נתוני חניך", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ShowUpdateStudentActivity.this, "לא נמצא חניך בשם זה", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error searching for student: ", task.getException());
                        Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בחיפוש חניך", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateStudentDetails(Student student) {
        if (student == null) {
            Log.e(TAG, "populateStudentDetails called with null student object.");
            return;
        }

        Log.d(TAG, "Populating details for student: " + student.getFullName());
        Log.d(TAG, "Student object from Firestore (after potential mapping): " + student.toString());

        fullNameEditText.setText(student.getFullName());
        organizationIdEditText.setText(student.getActiveNumber());
        birthDateEditText.setText(student.getBirthDate());
        gradeEditText.setText(student.getGrade());
        phoneEditText.setText(student.getPhone());
        joinDateEditText.setText(student.getJoinDate());
        addressEditText.setText(student.getAddress());
        parent1NameEditText.setText(student.getParent1Name());
        parent2NameEditText.setText(student.getParent2Name());
        parentPhoneEditText.setText(student.getParentPhone());

        setSpinnerSelection(genderSpinner, student.getGender(), getResources().getStringArray(R.array.gender_options));
        setSpinnerSelection(dayOfWeekSpinner, student.getDayOfWeek(), getResources().getStringArray(R.array.days_of_week));

        // *** שימוש ב-getter המעודכן ממחלקת Student ***
        if (student.getProfileImageBase64() != null && !student.getProfileImageBase64().isEmpty()) {
            loadBase64Image(student.getProfileImageBase64(), profileImage);
        } else {
            profileImage.setImageBitmap(null);
            currentProfileBitmap = null;
            Log.d(TAG, "No profileImageBase64 found for student or it's empty.");
        }
    }

    private void loadBase64Image(String base64String, ImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            Log.w(TAG, "loadBase64Image called with null or empty string.");
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
            return;
        }
        Log.d(TAG, "Attempting to decode Base64 string of length: " + base64String.length());
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            if (decodedBytes == null || decodedBytes.length == 0) {
                Log.e(TAG, "Base64.decode returned null or empty byte array.");
                imageView.setImageBitmap(null);
                currentProfileBitmap = null;
                return;
            }
            Log.d(TAG, "Decoded byte array length: " + decodedBytes.length);

            currentProfileBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (currentProfileBitmap != null) {
                imageView.setImageBitmap(currentProfileBitmap);
                Log.i(TAG, "Base64 image loaded successfully into ImageView.");
            } else {
                Log.e(TAG, "BitmapFactory.decodeByteArray returned null. Invalid image data in Base64?");
                imageView.setImageBitmap(null);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException while decoding Base64 image. String might be malformed.", e);
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError while decoding Base64 image. Image might be too large.", e);
            Toast.makeText(this, "התמונה גדולה מדי להצגה (נגמר הזיכרון)", Toast.LENGTH_LONG).show();
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value, String[] array) {
        // ... (ללא שינוי) ...
        if (value != null && array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void openGalleryForNewImage() {
        // ... (ללא שינוי) ...
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveStudentChanges() {
        if (currentStudentDocumentId == null || currentStudent == null) {
            Toast.makeText(this, "נא לבחור חניך תחילה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("fullName", fullNameEditText.getText().toString().trim());
        updatedData.put("activeNumber", organizationIdEditText.getText().toString().trim());
        updatedData.put("birthDate", birthDateEditText.getText().toString());
        updatedData.put("grade", gradeEditText.getText().toString().trim());
        updatedData.put("phone", phoneEditText.getText().toString().trim());
        updatedData.put("joinDate", joinDateEditText.getText().toString());
        updatedData.put("address", addressEditText.getText().toString().trim());
        updatedData.put("parent1Name", parent1NameEditText.getText().toString().trim());
        updatedData.put("parent2Name", parent2NameEditText.getText().toString().trim());
        updatedData.put("parentPhone", parentPhoneEditText.getText().toString().trim());
        updatedData.put("gender", genderSpinner.getSelectedItem().toString());
        updatedData.put("dayOfWeek", dayOfWeekSpinner.getSelectedItem().toString());

        String newBase64Image = "";
        if (currentProfileBitmap != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 60;
                currentProfileBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                byte[] imageBytes = baos.toByteArray();
                int maxSizeBytesBeforeEncoding = 500 * 1024;

                if (imageBytes.length < maxSizeBytesBeforeEncoding) {
                    newBase64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                } else {
                    Log.w(TAG, "Updated image is still too large. Not saving new image data.");
                    Toast.makeText(this, "התמונה החדשה גדולה מדי ולא נשמרה.", Toast.LENGTH_LONG).show();
                    // שמירת התמונה הישנה אם הייתה
                    newBase64Image = (currentStudent != null && currentStudent.getProfileImageBase64() != null) ? currentStudent.getProfileImageBase64() : "";
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting updated Bitmap to Base64", e);
                newBase64Image = (currentStudent != null && currentStudent.getProfileImageBase64() != null) ? currentStudent.getProfileImageBase64() : "";
            }
        }
        // *** שינוי: שימוש בשם השדה הנכון לתמונה כפי שהוא ב-Firestore (וב-Student.java) ***
        updatedData.put("profileImageUrl", newBase64Image); // היה profileImageBase64

        Log.d(TAG, "Attempting to update student data for doc ID: " + currentStudentDocumentId);
        db.collection("students").document(currentStudentDocumentId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShowUpdateStudentActivity.this, "פרטי החניך עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    detailsScrollView.setVisibility(View.GONE);
                    searchStudentAutoComplete.setText("");
                    clearStudentDetailsForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating student data", e);
                    Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בעדכון פרטי החניך: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void routeUserBasedOnType() {
        // ... (ללא שינוי) ...
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            Intent intent;
                            if (userType != null && userType.equalsIgnoreCase("manager")) {
                                intent = new Intent(ShowUpdateStudentActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(ShowUpdateStudentActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ShowUpdateStudentActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ShowUpdateStudentActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeletePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.popup_delete, null);
        final EditText departureDateEditText = popupView.findViewById(R.id.departureDateEditText);
        Button calendarButton = popupView.findViewById(R.id.calendarButton);
        calendarButton.setOnClickListener(v -> showDatePickerDialog(departureDateEditText));
        Button deleteConfirmButton = popupView.findViewById(R.id.deleteConfirmButton);
        ImageView closeButton = popupView.findViewById(R.id.closePopupButton);
        AlertDialog dialog = builder.setView(popupView).create();

        deleteConfirmButton.setOnClickListener(v -> {
            String departureDate = departureDateEditText.getText().toString();
            if (!departureDate.isEmpty()) {
                if (currentStudentDocumentId != null) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("leavingDate", departureDate);
                    update.put("isActive", false);
                    db.collection("students").document(currentStudentDocumentId).update(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ShowUpdateStudentActivity.this, "תאריך עזיבה נשמר", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                detailsScrollView.setVisibility(View.GONE);
                                searchStudentAutoComplete.setText("");
                                clearStudentDetailsForm();
                                loadStudentNamesForAutoComplete();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בעדכון תאריך עזיבה", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(ShowUpdateStudentActivity.this, "יש להזין תאריך עזיבה", Toast.LENGTH_SHORT).show();
            }
        });
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDatePickerDialog(EditText targetEditText) {
        // ... (ללא שינוי) ...
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    targetEditText.setText(selectedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }
}
