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
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowUpdateGuideActivity extends AppCompatActivity {

    private static final String TAG = "ShowUpdateGuide";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView searchGuideAutoComplete;
    // אין כפתור חיפוש נפרד
    private ScrollView detailsScrollView;

    private EditText fullNameEditText, activeNumberEditText, emailEditText, birthDateEditText,
            phoneEditText, joinDateEditText, addressEditText,
            parent1NameEditText, parent2NameEditText, parentPhoneEditText; // שדות אלו ישמשו כאנשי קשר
    private Spinner genderSpinner, dayOfWeekSpinner;
    private ImageView profileImage;
    private Button uploadImageButton, saveChangesButton, deleteGuideButton;

    // להסתרת שדות הורים אם לא רלוונטי
    private LinearLayout parent1NameLayout, parent2NameLayout, parentPhoneLayout;


    private Uri newImageUri = null;
    private Bitmap currentProfileBitmap = null;
    private String currentGuideDocumentId = null; // ישמור את ה-UID של המדריך שהוא גם ה-ID של המסמך
    private Guide currentGuide = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            newImageUri = result.getData().getData();
                            try {
                                currentProfileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), newImageUri);
                                profileImage.setImageBitmap(currentProfileBitmap);
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading new image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה חדשה", Toast.LENGTH_SHORT).show();
                                newImageUri = null;
                                currentProfileBitmap = null;
                                if (currentGuide != null && currentGuide.getProfileImageBase64() != null && !currentGuide.getProfileImageBase64().isEmpty()){
                                    loadBase64Image(currentGuide.getProfileImageBase64(), profileImage);
                                } else {
                                    profileImage.setImageBitmap(null);
                                }
                            }
                        } else {
                            Log.d(TAG, "New image selection cancelled or failed.");
                            newImageUri = null;
                            if (currentProfileBitmap != null) {
                                profileImage.setImageBitmap(currentProfileBitmap);
                            } else if (currentGuide != null && currentGuide.getProfileImageBase64() != null && !currentGuide.getProfileImageBase64().isEmpty()){
                                loadBase64Image(currentGuide.getProfileImageBase64(), profileImage);
                            } else {
                                profileImage.setImageBitmap(null);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_update_guide); // שימוש ב-XML של המדריך

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        setupListeners();
        loadGuideNamesForAutoComplete();
    }

    private void initializeUI() {
        searchGuideAutoComplete = findViewById(R.id.searchGuideAutoComplete);
        detailsScrollView = findViewById(R.id.detailsScrollView);

        fullNameEditText = findViewById(R.id.fullNameEditText);
        activeNumberEditText = findViewById(R.id.activeNumberEditText);
        emailEditText = findViewById(R.id.emailEditText); // שדה נוסף למדריך
        birthDateEditText = findViewById(R.id.birthDateEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);
        addressEditText = findViewById(R.id.addressEditText);

        // שדות שמשמשים כאנשי קשר
        parent1NameEditText = findViewById(R.id.parent1NameEditText);
        parent2NameEditText = findViewById(R.id.parent2NameEditText);
        parentPhoneEditText = findViewById(R.id.parentPhoneEditText);
        // Layouts של שדות הורים להסתרה אפשרית
        parent1NameLayout = findViewById(R.id.parent1NameLayout);
        parent2NameLayout = findViewById(R.id.parent2NameLayout);
        parentPhoneLayout = findViewById(R.id.parentPhoneLayout);

        // הסתרת שדות הורים כברירת מחדל (או אם תחליטי שהם לא רלוונטיים למדריך)
        // parent1NameLayout.setVisibility(View.GONE);
        // parent2NameLayout.setVisibility(View.GONE);
        // parentPhoneLayout.setVisibility(View.GONE);


        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveChangesButton = findViewById(R.id.saveChangesButton); // ודאי שה-ID ב-XML הוא saveChangesButton
        deleteGuideButton = findViewById(R.id.deleteGuideButton); // ודאי שה-ID ב-XML הוא deleteGuideButton

        detailsScrollView.setVisibility(View.GONE);
        clearGuideDetailsForm();
    }

    private void clearGuideDetailsForm() {
        fullNameEditText.setText("");
        activeNumberEditText.setText("");
        emailEditText.setText("");
        birthDateEditText.setText("");
        birthDateEditText.setHint("בחר תאריך");
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

        searchGuideAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            searchGuideByName(selectedName); // קריאה ישירה לחיפוש
        });

        birthDateEditText.setOnClickListener(v -> showDatePickerDialog(birthDateEditText));
        joinDateEditText.setOnClickListener(v -> showDatePickerDialog(joinDateEditText));
        uploadImageButton.setOnClickListener(v -> openGalleryForNewImage());
        saveChangesButton.setOnClickListener(v -> saveGuideChanges());
        deleteGuideButton.setOnClickListener(v -> {
            if (currentGuideDocumentId != null) {
                showDeletePopup();
            } else {
                Toast.makeText(this, "נא לבחור מדריך תחילה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGuideNamesForAutoComplete() {
        db.collection("users")
                .whereEqualTo("userType", "guide") // סינון מדריכים בלבד
                .whereEqualTo("isActive", true) // *** תוספת: שליפת מדריכים פעילים בלבד ***
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> guideNames = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Boolean isActive = document.getBoolean("isActive");
                            if (isActive == null || isActive) {
                                String name = document.getString("fullName");
                                if (name != null && !name.isEmpty()) {
                                    guideNames.add(name);
                                }
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, guideNames);
                        searchGuideAutoComplete.setAdapter(adapter);
                        Log.d(TAG, "Guide names loaded for autocomplete: " + guideNames.size());
                    } else {
                        Log.e(TAG, "Error getting guide names: ", task.getException());
                        Toast.makeText(this, "שגיאה בטעינת שמות מדריכים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchGuideByName(String guideNameToSearch) {
        if (TextUtils.isEmpty(guideNameToSearch)) {
            Toast.makeText(this, "נא לבחור שם מדריך מהרשימה", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Searching for guide: " + guideNameToSearch);
        clearGuideDetailsForm(); // ודאי שיש לך פונקציה כזו או נקי את השדות ידנית
        detailsScrollView.setVisibility(View.GONE);
        currentGuideDocumentId = null;
        currentGuide = null;

        db.collection("users")
                .whereEqualTo("userType", "guide")
                .whereEqualTo("fullName", guideNameToSearch)
                .whereEqualTo("isActive", true) // *** תוספת: חיפוש רק במדריכים פעילים ***
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            currentGuideDocumentId = document.getId();
                            currentGuide = document.toObject(Guide.class);
                            if (currentGuide != null) {
                                // בדיקה נוספת שהמדריך באמת פעיל
                                if (currentGuide.getIsActive() == null || currentGuide.getIsActive()) {
                                    populateGuideDetails(currentGuide); // ודאי שפונקציה זו קיימת וממלאה את כל השדות
                                    detailsScrollView.setVisibility(View.VISIBLE);
                                    Toast.makeText(ShowUpdateGuideActivity.this, "מדריך נמצא", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ShowUpdateGuideActivity.this, "המדריך אינו פעיל יותר", Toast.LENGTH_SHORT).show();
                                    detailsScrollView.setVisibility(View.GONE);
                                }
                            } else {
                                Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בהמרת נתוני מדריך", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ShowUpdateGuideActivity.this, "לא נמצא מדריך פעיל בשם זה", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error searching for guide: ", task.getException());
                        Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בחיפוש מדריך", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateGuideDetails(Guide guide) {
        if (guide == null) return;

        fullNameEditText.setText(guide.getFullName());
        activeNumberEditText.setText(guide.getActiveNumber());
        emailEditText.setText(guide.getEmail());
        birthDateEditText.setText(guide.getBirthDate());
        phoneEditText.setText(guide.getPhone());
        joinDateEditText.setText(guide.getJoinDate());
        addressEditText.setText(guide.getAddress());
        parent1NameEditText.setText(guide.getParent1Name()); // עבור איש קשר 1
        parent2NameEditText.setText(guide.getParent2Name()); // עבור איש קשר 2
        parentPhoneEditText.setText(guide.getParentPhone()); // טלפון איש קשר

        setSpinnerSelection(genderSpinner, guide.getGender(), getResources().getStringArray(R.array.gender_options));
        setSpinnerSelection(dayOfWeekSpinner, guide.getDayOfWeek(), getResources().getStringArray(R.array.days_of_week));

        if (guide.getProfileImageBase64() != null && !guide.getProfileImageBase64().isEmpty()) {
            loadBase64Image(guide.getProfileImageBase64(), profileImage);
        } else {
            profileImage.setImageBitmap(null);
            currentProfileBitmap = null;
        }
    }

    private void loadBase64Image(String base64String, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            currentProfileBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(currentProfileBitmap);
        } catch (Exception e) { // תפיסת Exception כללי יותר למקרה של Base64 פגום
            Log.e(TAG, "Error decoding/loading Base64 image", e);
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value, String[] array) {
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveGuideChanges() {
        if (currentGuideDocumentId == null || currentGuide == null) {
            Toast.makeText(this, "נא לבחור מדריך תחילה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("fullName", fullNameEditText.getText().toString().trim());
        updatedData.put("activeNumber", activeNumberEditText.getText().toString().trim());
        updatedData.put("email", emailEditText.getText().toString().trim()); // ולידציית מייל אם משתנה
        updatedData.put("birthDate", birthDateEditText.getText().toString());
        updatedData.put("phone", phoneEditText.getText().toString().trim());
        updatedData.put("joinDate", joinDateEditText.getText().toString());
        updatedData.put("address", addressEditText.getText().toString().trim());
        updatedData.put("parent1Name", parent1NameEditText.getText().toString().trim());
        updatedData.put("parent2Name", parent2NameEditText.getText().toString().trim());
        updatedData.put("parentPhone", parentPhoneEditText.getText().toString().trim());
        updatedData.put("gender", genderSpinner.getSelectedItem().toString());
        updatedData.put("dayOfWeek", dayOfWeekSpinner.getSelectedItem().toString());
        // userType ו-uid לא צריכים להתעדכן כאן בדרך כלל

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
                    Toast.makeText(this, "התמונה החדשה גדולה מדי ולא נשמרה.", Toast.LENGTH_LONG).show();
                    newBase64Image = (currentGuide.getProfileImageBase64() != null) ? currentGuide.getProfileImageBase64() : "";
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting updated Bitmap to Base64 for guide", e);
                newBase64Image = (currentGuide.getProfileImageBase64() != null) ? currentGuide.getProfileImageBase64() : "";
            }
        }
        updatedData.put("profileImageUrl", newBase64Image); // שימוש בשם השדה מהמחלקה Guide

        db.collection("users").document(currentGuideDocumentId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShowUpdateGuideActivity.this, "פרטי המדריך עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    detailsScrollView.setVisibility(View.GONE);
                    searchGuideAutoComplete.setText("");
                    clearGuideDetailsForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating guide data", e);
                    Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בעדכון פרטי המדריך: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                                intent = new Intent(ShowUpdateGuideActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(ShowUpdateGuideActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ShowUpdateGuideActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ShowUpdateGuideActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeletePopup() {
        // ... (ללא שינוי, אך מתייחס למחיקת מדריך) ...
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
                if (currentGuideDocumentId != null) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("leavingDate", departureDate);
                    update.put("isActive", false); // סימון המדריך כלא פעיל
                    db.collection("users").document(currentGuideDocumentId).update(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ShowUpdateGuideActivity.this, "תאריך עזיבת מדריך נשמר", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                detailsScrollView.setVisibility(View.GONE);
                                searchGuideAutoComplete.setText("");
                                clearGuideDetailsForm();
                                loadGuideNamesForAutoComplete(); //טעינה מחדש את רשימת ההשלמה האוטומטית
                            })
                            .addOnFailureListener(e -> Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בעדכון תאריך עזיבה", Toast.LENGTH_SHORT).show());

                }
            } else {
                Toast.makeText(ShowUpdateGuideActivity.this, "יש להזין תאריך עזיבה", Toast.LENGTH_SHORT).show();
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

