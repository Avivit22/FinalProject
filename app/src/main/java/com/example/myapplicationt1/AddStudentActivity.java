package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class AddStudentActivity extends AppCompatActivity {

    private Button birthDateButton;
    private Button joinDateButton;
    private ImageView profileImage;
    private Button uploadImageButton;
    private Button addButton;
    private EditText fullNameInput, activeNumberInput, gradeInput, phoneInput, addressInput, parent1Input, parent2Input, parentPhoneInput;
    private Spinner genderSpinner, dayOfWeekSpinner;
    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private Bitmap currentProfileBitmap = null;
    private static final String TAG = "AddStudentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_student);

        Log.d(TAG, "onCreate started");

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.i(TAG, "User is authenticated: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        } else {
            Log.w(TAG, "User is NOT authenticated! Data saving might be restricted by rules.");
            Toast.makeText(this, "שגיאה: יש להתחבר למערכת תחילה.", Toast.LENGTH_LONG).show();
        }

        birthDateButton = findViewById(R.id.birthDateButton);
        joinDateButton = findViewById(R.id.joinDateButton);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        addButton = findViewById(R.id.addButton);
        fullNameInput = findViewById(R.id.fullNameInput);
        activeNumberInput = findViewById(R.id.activeNumberInput);
        gradeInput = findViewById(R.id.grade);
        phoneInput = findViewById(R.id.phoneInput);
        addressInput = findViewById(R.id.addressInput);
        parent1Input = findViewById(R.id.parent1NameInput);
        parent2Input = findViewById(R.id.parent2NameInput);
        parentPhoneInput = findViewById(R.id.parentPhoneInput);
        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> {
            Intent intent = new Intent(AddStudentActivity.this, ManagerMainPageActivity.class);
            startActivity(intent);
            finish();
        });

        birthDateButton.setOnClickListener(v -> openDatePicker(birthDateButton));
        joinDateButton.setOnClickListener(v -> openDatePicker(joinDateButton));
        profileImage.setOnClickListener(v -> openGallery());
        uploadImageButton.setOnClickListener(v -> openGallery());

        addButton.setOnClickListener(v -> {
            Log.d(TAG, "Add button clicked");

            String fullName = fullNameInput.getText().toString().trim();
            String activeNumber = activeNumberInput.getText().toString().trim();
            String gradeVal = gradeInput.getText().toString().trim();
            String birthDateVal = birthDateButton.getText().toString();
            String joinDateVal = joinDateButton.getText().toString();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(activeNumber) || TextUtils.isEmpty(gradeVal)) {
                Toast.makeText(this, "נא למלא שם מלא, מספר פעיל וכיתה", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Validation failed: Core fields missing.");
                return;
            }
            if (birthDateVal.equals("בחר תאריך") || joinDateVal.equals("בחר תאריך")) {
                Toast.makeText(this, "נא לבחור תאריך לידה והצטרפות", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Validation failed: Dates not selected.");
                return;
            }

            addButton.setEnabled(false);
            Log.d(TAG, "Proceeding to saveStudentDataToFirestore with Base64 image");
            saveStudentDataToFirestore(currentProfileBitmap);
        });
        Log.d(TAG, "onCreate finished");
    }


    private void saveStudentDataToFirestore(Bitmap profileBitmapToSave) {
        Log.d(TAG, "saveStudentDataToFirestore called.");

        String fullName = fullNameInput.getText().toString().trim();
        String activeNumber = activeNumberInput.getText().toString().trim();
        String gradeVal = gradeInput.getText().toString().trim();
        String phoneVal = phoneInput.getText().toString().trim();
        String addressVal = addressInput.getText().toString().trim();
        String parent1NameVal = parent1Input.getText().toString().trim();
        String parent2NameVal = parent2Input.getText().toString().trim();
        String parentPhoneVal = parentPhoneInput.getText().toString().trim();
        String genderVal = genderSpinner.getSelectedItem().toString();
        String dayOfWeekVal = dayOfWeekSpinner.getSelectedItem().toString();
        String birthDateVal = birthDateButton.getText().toString();
        String joinDateVal = joinDateButton.getText().toString();

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("fullName", fullName);
        studentData.put("activeNumber", activeNumber);
        studentData.put("grade", gradeVal);
        studentData.put("phone", phoneVal);
        studentData.put("address", addressVal);
        studentData.put("parent1Name", parent1NameVal);
        studentData.put("parent2Name", parent2NameVal);
        studentData.put("parentPhone", parentPhoneVal);
        studentData.put("gender", genderVal);
        studentData.put("dayOfWeek", dayOfWeekVal);
        studentData.put("birthDate", birthDateVal);
        studentData.put("joinDate", joinDateVal);
        studentData.put("isActive", true);

        String base64Image = "";
        if (profileBitmapToSave != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 60;
                profileBitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                byte[] imageBytes = baos.toByteArray();


                int maxSizeBytesBeforeEncoding = 500 * 1024; // 500KB
                if (imageBytes.length < maxSizeBytesBeforeEncoding) {
                    base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                    Log.d(TAG, "Base64 image string created. Length: " + base64Image.length());
                } else {
                    Log.w(TAG, "Compressed image is still too large (" + imageBytes.length + " bytes). Not saving as Base64.");
                    Toast.makeText(this, "התמונה גדולה מדי לאחר כיווץ, לא נשמרה.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting Bitmap to Base64 string", e);
                Toast.makeText(this, "שגיאה בעיבוד התמונה.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "No profile bitmap to save.");
        }
        studentData.put("profileImageBase64", base64Image);

        Log.i(TAG, "Attempting to save student data to Firestore: " + studentData.toString().substring(0, Math.min(studentData.toString().length(), 300)) + "...");

        db.collection("students")
                .add(studentData)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Student data successfully saved to Firestore. Document ID: " + documentReference.getId());
                    Toast.makeText(AddStudentActivity.this, "החניך נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddStudentActivity.this, ManagerMainPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save student data to Firestore. Error: " + e.getMessage(), e);
                    Toast.makeText(AddStudentActivity.this, "שגיאה בשמירת נתוני החניך: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    addButton.setEnabled(true);
                });
    }

    private void openDatePicker(Button button) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthOfYear, dayOfMonth) ->
                        button.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + yearSelected),
                year, month, day);
        datePickerDialog.show();
    }

    private void openGallery() {
        Log.d(TAG, "openGallery called");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(Intent.createChooser(intent, "בחר תמונה"), PICK_IMAGE_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e(TAG, "No activity found to handle ACTION_GET_CONTENT for images.", ex);
            Toast.makeText(this, "לא נמצאה אפליקציה לבחירת תמונות.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called. RequestCode: " + requestCode + ", ResultCode: " + resultCode);
        currentProfileBitmap = null;
        imageUri = null;

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.i(TAG, "Image selected from gallery. URI: " + imageUri.toString());
            try {

                currentProfileBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                profileImage.setImageBitmap(currentProfileBitmap);
                Log.d(TAG, "Bitmap created and set to ImageView.");
            } catch (IOException e) {
                Log.e(TAG, "IOException when loading bitmap from URI: " + e.getMessage(), e);
                Toast.makeText(this, "שגיאה בהצגת התמונה שנבחרה", Toast.LENGTH_SHORT).show();
                currentProfileBitmap = null; // איפוס אם יש שגיאה
                imageUri = null;
            }
        } else {
            Log.w(TAG, "Image selection was cancelled or failed. ResultCode: " + resultCode);
            profileImage.setImageResource(R.drawable.default_profile); // החזרת תמונת ברירת מחדל אם הבחירה בוטלה
        }
    }
}