package com.example.myapplicationt1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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

// Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// Java utility imports
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך עדכון מדריך: מאפשר חיפוש, צפייה, עדכון ומחיקת פרטי מדריכים ומנהלים.
 * כולל תמיכה בתמונת פרופיל, ניהול תאריכים וחיבור ל-Firebase.
 */
public class ShowUpdateGuideActivity extends AppCompatActivity {

    private static final String TAG = "ShowUpdateGuideActivity"; //תג ללוג

    //מופע של שירות FIREBASE
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView searchGuideAutoComplete; //שדה חיפוש עם השלמה אוטומטית
    private ScrollView detailsGuideScrollView;  //אזור גלילה
    private ImageView guideProfileImage; //תמונת מדריך
    private EditText guideFullNameEditText, guideActiveNumberEditText, guideEmailEditText,
            guideBirthDateEditText, guidePhoneEditText, guideJoinDateEditText,
            guideAddressEditText, guideParent1NameEditText, guideParent2NameEditText,
            guideParentPhoneEditText;
    private Spinner guideGenderSpinner, guideDayOfWeekSpinner;  //הגדרת ספינרים
    private Button uploadGuideImageButton, saveGuideChangesButton, deleteGuideButton;

    private LinearLayout guideDayOfWeekLayout, guideParent1NameLayout, guideParent2NameLayout, guideParentPhoneLayout;

    //משתנים לניהול תמונה שנבחרה
    private Uri newGuideImageUri = null;
    private Bitmap currentGuideProfileBitmap = null;

    private Object currentUserData = null;  //עבור שמירה של אובייקט מנהל או מדריך
    private String currentUserDocumentId = null; //מזהה משתמש נוכחי

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        //בדיקה אם משתמש בחר תמונה והפעולה הצליחה
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            Uri selectedImageUri = result.getData().getData();
                            try {  //המרת הURI לBITMAP
                                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                                guideProfileImage.setImageBitmap(selectedBitmap);
                                newGuideImageUri = selectedImageUri;
                                currentGuideProfileBitmap = selectedBitmap;
                                Log.d(TAG, "New staff image selected and bitmap created.");
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading new staff image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה חדשה", Toast.LENGTH_SHORT).show();
                                newGuideImageUri = null;

                                String oldBase64 = "";
                                if (currentUserData instanceof Guide) {
                                    oldBase64 = ((Guide) currentUserData).getProfileImageBase64();
                                } else if (currentUserData instanceof Manager) {
                                    oldBase64 = ((Manager) currentUserData).getProfileImageBase64();
                                }

                                if (oldBase64 != null && !oldBase64.isEmpty()) {
                                    loadBase64ImageToView(oldBase64, guideProfileImage, true);
                                } else {
                                    guideProfileImage.setImageResource(R.drawable.default_profile);
                                    currentGuideProfileBitmap = null;
                                }
                            }
                        } else {
                            Log.d(TAG, "New staff image selection cancelled or failed.");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_update_guide);

        //אתחול מופעיל של שירות FIREBASE
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI(); //אתחול כל רכיבי הXML
        setupListeners();  //הגדרת מאזינים לאירועים
        loadStaffNamesForAutoComplete(); //טעינת שמות מדריכים לשדה החיפוש והשלמה אוטומטית

        String activeNumberFromIntent = getIntent().getStringExtra("guideActiveNumber");
        if (activeNumberFromIntent != null && !activeNumberFromIntent.isEmpty()) {
            searchStaffByActiveNumber(activeNumberFromIntent);
        }

    }

    /**
     *פונקציה לאתחול כל רכיבי הממשק מקובץ הXML
     */
    private void initializeUI() {

        //חיבור לרכיבי הXML
        searchGuideAutoComplete = findViewById(R.id.search_staff_autocomplete);
        detailsGuideScrollView = findViewById(R.id.detailsScrollView_guide);
        guideProfileImage = findViewById(R.id.profileImage_guide);

        guideFullNameEditText = findViewById(R.id.fullNameEditText_guide);
        guideActiveNumberEditText = findViewById(R.id.activeNumberEditText_guide);
        guideEmailEditText = findViewById(R.id.emailEditText_guide);
        guideBirthDateEditText = findViewById(R.id.birthDateEditText_guide);
        guidePhoneEditText = findViewById(R.id.phoneEditText_guide);
        guideJoinDateEditText = findViewById(R.id.joinDateEditText_guide);
        guideAddressEditText = findViewById(R.id.addressEditText_guide);
        guideParent1NameEditText = findViewById(R.id.parent1NameEditText_guide);
        guideParent2NameEditText = findViewById(R.id.parent2NameEditText_guide);
        guideParentPhoneEditText = findViewById(R.id.parentPhoneEditText_guide);

        guideGenderSpinner = findViewById(R.id.genderSpinner_guide);
        guideDayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner_guide);

        uploadGuideImageButton = findViewById(R.id.uploadImageButton_guide);
        saveGuideChangesButton = findViewById(R.id.saveChangesButton_guide);
        deleteGuideButton = findViewById(R.id.deleteButton_guide);

        guideDayOfWeekLayout = findViewById(R.id.dayOfWeekLayout_guide);
        guideParent1NameLayout = findViewById(R.id.parent1NameLayout_guide);
        guideParent2NameLayout = findViewById(R.id.parent2NameLayout_guide);
        guideParentPhoneLayout = findViewById(R.id.parentPhoneLayout_guide);

        if (detailsGuideScrollView != null) {
            detailsGuideScrollView.setVisibility(View.GONE); //הסתרת אזור פרטי המדריך עד שייבחר מדריך
        } else {
            Log.e(TAG, "CRITICAL: detailsGuideScrollView is null. Check ID R.id.detailsScrollView_guide in XML.");
            // טיפול במקרה שה-ScrollView לא נמצא
        }
        clearGuideDetailsForm(); // ניקוי הטופס
    }


    /**
     *פונקציה לניקוי כל שדות הקלט בטופס, איפוס תמונה ומשתנים שקשורים אליה
     */
    private void clearGuideDetailsForm() {

        if (guideFullNameEditText != null) guideFullNameEditText.setText("");
        if (guideActiveNumberEditText != null) guideActiveNumberEditText.setText("");
        if (guideEmailEditText != null) guideEmailEditText.setText("");
        if (guideBirthDateEditText != null) {
            guideBirthDateEditText.setText("");
            guideBirthDateEditText.setHint("בחר תאריך");
        }
        if (guidePhoneEditText != null) guidePhoneEditText.setText("");
        if (guideJoinDateEditText != null) {
            guideJoinDateEditText.setText("");
            guideJoinDateEditText.setHint("בחר תאריך");
        }
        if (guideAddressEditText != null) guideAddressEditText.setText("");
        if (guideParent1NameEditText != null) guideParent1NameEditText.setText("");
        if (guideParent2NameEditText != null) guideParent2NameEditText.setText("");
        if (guideParentPhoneEditText != null) guideParentPhoneEditText.setText("");

        if (guideProfileImage != null) guideProfileImage.setImageResource(R.drawable.default_profile);
        currentGuideProfileBitmap = null;
        newGuideImageUri = null;


        if (guideGenderSpinner != null && guideGenderSpinner.getAdapter() != null && guideGenderSpinner.getAdapter().getCount() > 0)
            guideGenderSpinner.setSelection(0);
        if (guideDayOfWeekSpinner != null && guideDayOfWeekSpinner.getAdapter() != null && guideDayOfWeekSpinner.getAdapter().getCount() > 0)
            guideDayOfWeekSpinner.setSelection(0);


        if (guideDayOfWeekLayout != null) guideDayOfWeekLayout.setVisibility(View.VISIBLE);
        if (guideParent1NameLayout != null) guideParent1NameLayout.setVisibility(View.VISIBLE);
        if (guideParent2NameLayout != null) guideParent2NameLayout.setVisibility(View.VISIBLE);
        if (guideParentPhoneLayout != null) guideParentPhoneLayout.setVisibility(View.VISIBLE);
    }

    /**
     *פונקציה להגדרת מאזינים לרכיבי ממשק שונים
     */
    private void setupListeners() {

        ImageView logoImageView = findViewById(R.id.logoImage_guide);
        logoImageView.setOnClickListener(v -> routeUserBasedOnType());

        //מאזין לשדה החיפוש
        if (searchGuideAutoComplete != null) {
            searchGuideAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                searchStaffByName(selectedName);
            });
        }

        // מאזינים לשדות תאריך
        if (guideBirthDateEditText != null) {
            guideBirthDateEditText.setOnClickListener(v -> showDatePickerDialog(guideBirthDateEditText));
        }
        if (guideJoinDateEditText != null) {
            guideJoinDateEditText.setOnClickListener(v -> showDatePickerDialog(guideJoinDateEditText));
        }
        // מאזין לכפתור העלאת תמונה
        if (uploadGuideImageButton != null) {
            uploadGuideImageButton.setOnClickListener(v -> openImagePicker());
        }
        // מאזין לכפתור שמירת שינויים
        if (saveGuideChangesButton != null) {
            saveGuideChangesButton.setOnClickListener(v -> saveStaffChanges());
        }
        // מאזין לכפתור תאריך עזיבה
        if (deleteGuideButton != null) {
            deleteGuideButton.setOnClickListener(v -> {
                if (currentUserDocumentId != null) {
                    showDeleteStaffPopup();
                } else {
                    Toast.makeText(this, "נא לבחור איש צוות תחילה", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     *    פונקציה לטעינת שמות כל המדריכים הפעילים מהFIRESTORE והצגתם בשדה החיפוש עם השלמה אוטומטית
     */
    private void loadStaffNamesForAutoComplete() {
        db.collection("users")  //גישה לcollection של users מתוך FIRESTORE
                .whereIn("userType", Arrays.asList("guide", "manager"))  //סינון להצגת מדריכים ומנהלים בלבד
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> staffNames = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String leavingDate = document.getString("leavingDate");

                            //הוספה לרשימה רק אם למדריך אין תאריך עזיבה כלומר המדריך פעיל
                            if (leavingDate == null || leavingDate.isEmpty()) {
                                String name = document.getString("fullName");
                                if (name != null && !name.isEmpty()) {
                                    staffNames.add(name);
                                }
                            }
                        }

                        //יצירת ADAPTER להצגת שמות מדריכים בשדה החיפוש
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ShowUpdateGuideActivity.this,
                                android.R.layout.simple_dropdown_item_1line, staffNames);
                        if (searchGuideAutoComplete != null) {
                            searchGuideAutoComplete.setAdapter(adapter);
                        }
                        Log.d(TAG, "Active staff names loaded: " + staffNames.size());
                    } else {
                        Log.e(TAG, "Error getting staff names: ", task.getException());
                    }
                });
    }

    /**
     * פונקציה לחיפוש מדריך או מנהל לפי שם שהוקלד
     */
    private void searchStaffByName(String staffName) {
        if (TextUtils.isEmpty(staffName)) {
            Toast.makeText(this, "נא להזין שם לחיפוש", Toast.LENGTH_SHORT).show();
            return;
        }
        clearGuideDetailsForm(); //ניקוי הטופס לפני הצגת תוצאות חדשות
        if (detailsGuideScrollView != null) detailsGuideScrollView.setVisibility(View.GONE);
        currentUserDocumentId = null;
        currentUserData = null;

        db.collection("users")
                .whereEqualTo("fullName", staffName)
                .whereIn("userType", Arrays.asList("guide", "manager"))
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);


                        String leavingDate = document.getString("leavingDate");
                        if (leavingDate != null && !leavingDate.isEmpty()) {
                            Toast.makeText(ShowUpdateGuideActivity.this, "מדריך זה אינו פעיל.", Toast.LENGTH_LONG).show();
                            if (detailsGuideScrollView != null) detailsGuideScrollView.setVisibility(View.GONE);
                            return;
                        }

                        currentUserDocumentId = document.getId();
                        String userType = document.getString("userType");


                        if ("guide".equals(userType)) {
                            currentUserData = document.toObject(Guide.class);
                            if (currentUserData != null) ((Guide)currentUserData).setUid(currentUserDocumentId);
                        } else if ("manager".equals(userType)) {
                            currentUserData = document.toObject(Manager.class);
                            if (currentUserData != null) ((Manager)currentUserData).setUid(currentUserDocumentId);
                        }

                        if (currentUserData != null) {
                            populateStaffDetails();
                            if (detailsGuideScrollView != null) detailsGuideScrollView.setVisibility(View.VISIBLE);
                            Toast.makeText(ShowUpdateGuideActivity.this, "מדריך נמצא", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בהמרת נתוני מדריך", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error searching for staff or staff not found: ", task.getException());
                        Toast.makeText(ShowUpdateGuideActivity.this, "לא נמצא מדריך פעיל בשם זה", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * פונקציה למילוי שדות הטופס עם פרטי האדם שנבחר
     */
    private void populateStaffDetails() {
        if (currentUserData == null) return;


        String uid = "", fullName = "", activeNumber = "", email = "", gender = "", birthDate = "",
                dayOfWeek = "", phone = "", joinDate = "", address = "",
                p1Name = "", p2Name = "", pPhone = "", base64Img = "";

        boolean isGuide = currentUserData instanceof Guide;
        boolean isManager = currentUserData instanceof Manager;


        if (isGuide) {
            Guide guide = (Guide) currentUserData;
            uid = guide.getUid(); fullName = guide.getFullName(); activeNumber = guide.getActiveNumber();
            email = guide.getEmail(); gender = guide.getGender(); birthDate = guide.getBirthDate();
            dayOfWeek = guide.getDayOfWeek(); phone = guide.getPhone(); joinDate = guide.getJoinDate();
            address = guide.getAddress(); p1Name = guide.getParent1Name(); p2Name = guide.getParent2Name();
            pPhone = guide.getParentPhone(); base64Img = guide.getProfileImageBase64();


            if (guideDayOfWeekLayout != null) guideDayOfWeekLayout.setVisibility(View.VISIBLE);
            if (guideParent1NameLayout != null) guideParent1NameLayout.setVisibility(View.VISIBLE);
            if (guideParent2NameLayout != null) guideParent2NameLayout.setVisibility(View.VISIBLE);
            if (guideParentPhoneLayout != null) guideParentPhoneLayout.setVisibility(View.VISIBLE);
        } else if (isManager) {
            Manager manager = (Manager) currentUserData;
            uid = manager.getUid(); fullName = manager.getFullName(); activeNumber = manager.getActiveNumber();
            email = manager.getEmail(); gender = manager.getGender(); birthDate = manager.getBirthDate();
            dayOfWeek = manager.getDayOfWeek(); phone = manager.getPhone(); joinDate = manager.getJoinDate();
            address = manager.getAddress(); p1Name = manager.getParent1Name(); p2Name = manager.getParent2Name();
            pPhone = manager.getParentPhone(); base64Img = manager.getProfileImageBase64();


            if (guideDayOfWeekLayout != null) guideDayOfWeekLayout.setVisibility(View.GONE);
            if (guideParent1NameLayout != null) guideParent1NameLayout.setVisibility(View.GONE);
            if (guideParent2NameLayout != null) guideParent2NameLayout.setVisibility(View.GONE);
            if (guideParentPhoneLayout != null) guideParentPhoneLayout.setVisibility(View.GONE);
        }


        if (guideFullNameEditText != null) guideFullNameEditText.setText(fullName);
        if (guideActiveNumberEditText != null) guideActiveNumberEditText.setText(activeNumber);
        if (guideEmailEditText != null) guideEmailEditText.setText(email);
        if (guideBirthDateEditText != null) guideBirthDateEditText.setText(birthDate);
        if (guidePhoneEditText != null) guidePhoneEditText.setText(phone);
        if (guideJoinDateEditText != null) guideJoinDateEditText.setText(joinDate);
        if (guideAddressEditText != null) guideAddressEditText.setText(address);
        if (guideParent1NameEditText != null) guideParent1NameEditText.setText(p1Name);
        if (guideParent2NameEditText != null) guideParent2NameEditText.setText(p2Name);
        if (guideParentPhoneEditText != null) guideParentPhoneEditText.setText(pPhone);


        if (guideGenderSpinner != null) setSpinnerSelection(guideGenderSpinner, gender, getResources().getStringArray(R.array.gender_options));
        if (guideDayOfWeekSpinner != null && guideDayOfWeekLayout != null && guideDayOfWeekLayout.getVisibility() == View.VISIBLE) {
            setSpinnerSelection(guideDayOfWeekSpinner, dayOfWeek, getResources().getStringArray(R.array.days_of_week));
        }


        if (guideProfileImage != null) {
            if (base64Img != null && !base64Img.isEmpty()) {
                loadBase64ImageToView(base64Img, guideProfileImage, true);
            } else {
                guideProfileImage.setImageResource(R.drawable.default_profile);
                currentGuideProfileBitmap = null;
            }
        }
    }

    /**
     *פונקציה לטעינת תמונה ממחרוזת base64 אל imageView ועדכון של bitmap
     */
    private void loadBase64ImageToView(String base64String, ImageView imageView, boolean updateCurrentBitmapVar) {
        if (imageView == null) return;
        if (base64String == null || base64String.isEmpty()) {
            imageView.setImageResource(R.drawable.default_profile);
            if (updateCurrentBitmapVar) currentGuideProfileBitmap = null;
            return;
        }
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (decodedBitmap != null) {
                imageView.setImageBitmap(decodedBitmap);
                if (updateCurrentBitmapVar) currentGuideProfileBitmap = decodedBitmap;
            } else {
                Log.e(TAG, "BitmapFactory.decodeByteArray returned null for staff image.");
                imageView.setImageResource(R.drawable.default_profile);
                if (updateCurrentBitmapVar) currentGuideProfileBitmap = null;
            }
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "Error decoding Base64 for staff image - IllegalArgumentException: " + iae.getMessage(), iae);
            imageView.setImageResource(R.drawable.default_profile);
            if (updateCurrentBitmapVar) currentGuideProfileBitmap = null;
            Toast.makeText(this, "שגיאה בפורמט התמונה", Toast.LENGTH_SHORT).show();
        } catch (OutOfMemoryError oome) {
            Log.e(TAG, "Error decoding Base64 for staff image - OutOfMemoryError: " + oome.getMessage(), oome);
            imageView.setImageResource(R.drawable.default_profile);
            if (updateCurrentBitmapVar) currentGuideProfileBitmap = null;
            Toast.makeText(this, "התמונה גדולה מדי להצגה (נגמר הזיכרון)", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Generic error decoding Base64 for staff image: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.default_profile);
            if (updateCurrentBitmapVar) currentGuideProfileBitmap = null;
            Toast.makeText(this, "שגיאה לא ידועה בטעינת התמונה", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *פונקציה לשמירת שינויים בFIRESTORE
     */
    private void saveStaffChanges() {
        if (currentUserDocumentId == null || currentUserData == null) {
            Toast.makeText(this, "נא לבחור מדריך תחילה", Toast.LENGTH_SHORT).show();
            return;
        }


        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("uid", currentUserDocumentId);


        if (guideFullNameEditText != null) updatedData.put("fullName", guideFullNameEditText.getText().toString().trim());
        if (guideActiveNumberEditText != null) updatedData.put("activeNumber", guideActiveNumberEditText.getText().toString().trim());
        if (guideEmailEditText != null) updatedData.put("email", guideEmailEditText.getText().toString().trim());
        if (guideGenderSpinner != null) updatedData.put("gender", guideGenderSpinner.getSelectedItem().toString());
        if (guideBirthDateEditText != null) updatedData.put("birthDate", guideBirthDateEditText.getText().toString());
        if (guidePhoneEditText != null) updatedData.put("phone", guidePhoneEditText.getText().toString().trim());
        if (guideJoinDateEditText != null) updatedData.put("joinDate", guideJoinDateEditText.getText().toString());
        if (guideAddressEditText != null) updatedData.put("address", guideAddressEditText.getText().toString().trim());
        if (guideParent1NameEditText != null) updatedData.put("parent1Name", guideParent1NameEditText.getText().toString().trim());
        if (guideParent2NameEditText != null) updatedData.put("parent2Name", guideParent2NameEditText.getText().toString().trim());
        if (guideParentPhoneEditText != null) updatedData.put("parentPhone", guideParentPhoneEditText.getText().toString().trim());


        if (currentUserData instanceof Guide) {
            updatedData.put("userType", "guide");
            if(guideDayOfWeekLayout != null && guideDayOfWeekLayout.getVisibility() == View.VISIBLE && guideDayOfWeekSpinner != null) {
                updatedData.put("dayOfWeek", guideDayOfWeekSpinner.getSelectedItem().toString());
            }
        } else if (currentUserData instanceof Manager) {
            updatedData.put("userType", "manager");

        }


        String finalBase64Image = "";
        String existingBase64 = "";
        if (currentUserData instanceof Guide) existingBase64 = ((Guide)currentUserData).getProfileImageBase64();
        else if (currentUserData instanceof Manager) existingBase64 = ((Manager)currentUserData).getProfileImageBase64();

        if (newGuideImageUri != null && currentGuideProfileBitmap != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                currentGuideProfileBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] imageBytes = baos.toByteArray();
                if (imageBytes.length < 750 * 1024) {
                    finalBase64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                } else {
                    Toast.makeText(this, "התמונה החדשה גדולה מדי. נשמרת התמונה הקודמת.", Toast.LENGTH_LONG).show();
                    finalBase64Image = existingBase64;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting staff image to Base64", e);
                Toast.makeText(this, "שגיאה בעיבוד התמונה. נשמרת התמונה הקודמת.", Toast.LENGTH_SHORT).show();
                finalBase64Image = existingBase64;
            }
        } else {
            finalBase64Image = existingBase64;
        }
        updatedData.put("profileImageBase64", finalBase64Image != null ? finalBase64Image : "");


        String currentLeavingDate = "";
        if (currentUserData instanceof Guide) currentLeavingDate = ((Guide) currentUserData).getLeavingDate();
        else if (currentUserData instanceof Manager) currentLeavingDate = ((Manager) currentUserData).getLeavingDate();

        if (currentLeavingDate != null && !currentLeavingDate.isEmpty()) {
            updatedData.put("leavingDate", currentLeavingDate);
        } else {
            updatedData.put("leavingDate", "");
        }


        db.collection("users").document(currentUserDocumentId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShowUpdateGuideActivity.this, "פרטי מדריך עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    loadStaffNamesForAutoComplete();
                    if (detailsGuideScrollView != null) detailsGuideScrollView.setVisibility(View.GONE);
                    if (searchGuideAutoComplete != null) {
                        searchGuideAutoComplete.setText("");
                        searchGuideAutoComplete.clearFocus();
                    }
                    clearGuideDetailsForm();

                    currentUserData = null;
                    currentUserDocumentId = null;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating staff data", e);
                    Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בעדכון פרטי מדריך: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     *פונקציה להצגת פופאפ של הזנת תאריך עזיבה
     */
    private void showDeleteStaffPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.popup_delete, null);
        final EditText departureDateEditText = popupView.findViewById(R.id.departureDateEditText);
        Button calendarButton = popupView.findViewById(R.id.calendarButton);

        if (calendarButton != null && departureDateEditText != null) {
            calendarButton.setOnClickListener(v -> showDatePickerDialog(departureDateEditText));
        }

        Button deleteConfirmButton = popupView.findViewById(R.id.deleteConfirmButton);
        ImageView closeButton = popupView.findViewById(R.id.closePopupButton);
        AlertDialog dialog = builder.setView(popupView).create();

        if (deleteConfirmButton != null && departureDateEditText != null) {
            deleteConfirmButton.setOnClickListener(v -> {
                String departureDate = departureDateEditText.getText().toString();
                if (!departureDate.isEmpty()) {
                    if (currentUserDocumentId != null) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("leavingDate", departureDate);

                        db.collection("users").document(currentUserDocumentId).update(update)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ShowUpdateGuideActivity.this, "תאריך עזיבה נשמר", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();

                                    if (detailsGuideScrollView != null) detailsGuideScrollView.setVisibility(View.GONE);
                                    if (searchGuideAutoComplete != null) {
                                        searchGuideAutoComplete.setText("");
                                        searchGuideAutoComplete.clearFocus();
                                    }
                                    clearGuideDetailsForm();
                                    loadStaffNamesForAutoComplete();
                                    currentUserData = null;
                                    currentUserDocumentId = null;
                                })
                                .addOnFailureListener(e -> Toast.makeText(ShowUpdateGuideActivity.this, "שגיאה בעדכון תאריך עזיבה", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(ShowUpdateGuideActivity.this, "יש להזין תאריך עזיבה", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }


    /**
     *פונקציה לניתוב המשתמש למסך הראשי בהתאם לסוג שלו- מדריך או מנהל
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
                            if ("manager".equalsIgnoreCase(userType)) {
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

    /**
     *פונקציה לפתיחת גלריה בנייד לבחירת תמונה חדשה
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     *פונקציה להצגת דיאלוג לבחירת תאריך ועדכון שדה טקסט עם התאריך שנבחר
     */
    private void showDatePickerDialog(EditText targetEditText) {
        if (targetEditText == null) return;
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

    /**
     *פונקציית עזר להגדרת בחירה בספינר
     */
    private void setSpinnerSelection(Spinner spinner, String value, String[] array) {
        if (spinner == null) return;
        if (value != null && array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }

        if (spinner.getAdapter() != null && spinner.getAdapter().getCount() > 0) {
            spinner.setSelection(0);
        }
    }

    /**
     * מחפשת מדריך לפי מספר פעיל, טוענת ומציגה את פרטיו אם נמצא.
     */
    private void searchStaffByActiveNumber(String activeNumber) {
        clearGuideDetailsForm();
        detailsGuideScrollView.setVisibility(View.GONE);
        currentUserDocumentId = null;
        currentUserData = null;

        db.collection("users")
                .whereEqualTo("activeNumber", activeNumber)
                .whereEqualTo("userType", "guide")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        currentUserDocumentId = document.getId();
                        currentUserData = document.toObject(Guide.class);
                        if (currentUserData != null) ((Guide) currentUserData).setUid(currentUserDocumentId);

                        populateStaffDetails();
                        detailsGuideScrollView.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "המדריך החדש נטען אוטומטית", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "לא נמצא מדריך עם מספר פעיל זה", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "שגיאה בחיפוש מדריך לפי מספר פעיל", e);
                    Toast.makeText(this, "שגיאה בחיפוש מדריך חדש", Toast.LENGTH_SHORT).show();
                });
    }

}
