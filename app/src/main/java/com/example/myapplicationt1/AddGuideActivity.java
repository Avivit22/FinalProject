package com.example.myapplicationt1;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// --- ייבואים עבור Firebase (Auth ו-Firestore בלבד) ---
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
// *** הסרה: ייבואים של Storage הוסרו ***
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageException;
// import com.google.firebase.storage.StorageReference;
// import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddGuideActivity extends AppCompatActivity {

    // --- UI Elements ---
    private Button birthDateButton;
    private Button joinDateButton;
    private ImageView profileImage;
    private Button uploadImageButton;
    private Button addButton;
    private EditText fullNameInput, activeNumberInput, emailInput, phoneInput, addressInput, parent1NameInput, parent2NameInput, parentPhoneInput;
    private Spinner genderSpinner, dayOfWeekSpinner;

    // --- Firebase (Auth ו-Firestore בלבד) ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    // *** הסרה: משתני Storage הוסרו ***
    // private FirebaseStorage storage;
    // private StorageReference storageReference;

    // --- Data ---
    private Uri imageUri; // ה-URI של התמונה שנבחרה מהגלריה (עדיין שימושי לקבלת ה-Bitmap)
    private Bitmap currentProfileBitmap = null; // *** תוספת: לשמור את ה-Bitmap הנבחר ***
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String TAG = "AddGuideActivity"; // שינוי קל בתג

    // בחירת תמונה מהגלריה (נשאר כדי שהמשתמש יוכל לבחור ולראות תמונה)
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        currentProfileBitmap = null; // איפוס לפני כל בחירה
                        imageUri = null;

                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            imageUri = result.getData().getData();
                            Log.d(TAG, "Image selected: " + imageUri.toString());
                            try {
                                currentProfileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                profileImage.setImageBitmap(currentProfileBitmap);
                                Log.d(TAG, "Bitmap created and set to ImageView for guide.");
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading image bitmap for guide", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                                currentProfileBitmap = null;
                                imageUri = null;
                            }
                        } else {
                            Log.d(TAG, "Image selection cancelled or failed for guide.");
                            profileImage.setImageResource(R.drawable.default_profile);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_guide);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // *** הסרה: אתחול Storage הוסר ***

        // --- קבלת רפרנסים ל-UI Elements --- (ללא שינוי)
        fullNameInput = findViewById(R.id.fullNameInput);
        activeNumberInput = findViewById(R.id.activeNumberInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        addressInput = findViewById(R.id.addressInput);
        parent1NameInput = findViewById(R.id.parent1NameInput); // שדות אלו קיימים ב-XML של המדריך? אם לא, יש להתאים
        parent2NameInput = findViewById(R.id.parent2NameInput); // שדות אלו קיימים ב-XML של המדריך? אם לא, יש להתאים
        parentPhoneInput = findViewById(R.id.parentPhoneInput); // שדות אלו קיימים ב-XML של המדריך? אם לא, יש להתאים
        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        addButton = findViewById(R.id.addButton);
        birthDateButton = findViewById(R.id.birthDateButton);
        joinDateButton = findViewById(R.id.joinDateButton);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);

        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> {
            Intent intent = new Intent(AddGuideActivity.this, ManagerMainPageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        birthDateButton.setOnClickListener(v -> openDatePickerDialog(birthDateButton));
        joinDateButton.setOnClickListener(v -> openDatePickerDialog(joinDateButton));
        View.OnClickListener imagePickerListener = v -> openGallery();
        profileImage.setOnClickListener(imagePickerListener);
        uploadImageButton.setOnClickListener(imagePickerListener);

        addButton.setOnClickListener(v -> {
            Log.d(TAG, "Add guide button clicked");
            // *** שינוי: קריאה לפונקציה שמתחילה את התהליך כולו ***
            startGuideAdditionProcess();
        });
    }

    // --- פונקציה שמתחילה את תהליך ההוספה ---
    private void startGuideAdditionProcess() {
        String fullName = fullNameInput.getText().toString().trim();
        String activeNumber = activeNumberInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        // שדות הורים - ודאי שהם רלוונטיים למדריך ואספי אותם אם כן
        String parent1Name = parent1NameInput.getText().toString().trim();
        String parent2Name = parent2NameInput.getText().toString().trim();
        String parentPhone = parentPhoneInput.getText().toString().trim();
        String gender = (genderSpinner.getSelectedItem() != null) ? genderSpinner.getSelectedItem().toString() : "";
        String dayOfWeek = (dayOfWeekSpinner.getSelectedItem() != null) ? dayOfWeekSpinner.getSelectedItem().toString() : "";
        String birthDate = birthDateButton.getText().toString();
        String joinDate = joinDateButton.getText().toString();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(activeNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "נא למלא שם מלא, מספר פעיל, מייל וטלפון", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "כתובת מייל אינה תקינה", Toast.LENGTH_SHORT).show();
            emailInput.requestFocus();
            return;
        }
        if (birthDate.equals("בחר תאריך") || joinDate.equals("בחר תאריך")) {
            Toast.makeText(this, "נא לבחור תאריך לידה והצטרפות", Toast.LENGTH_SHORT).show();
            return;
        }

        addButton.setEnabled(false);

        // הכנת ה-Map עם הנתונים, כולל התמונה כ-Base64
        Map<String, Object> guideData = new HashMap<>();
        guideData.put("fullName", fullName);
        guideData.put("activeNumber", activeNumber);
        guideData.put("email", email);
        guideData.put("phone", phone);
        guideData.put("address", address);
        guideData.put("gender", gender);
        guideData.put("birthDate", birthDate);
        guideData.put("dayOfWeek", dayOfWeek);
        guideData.put("joinDate", joinDate);
        guideData.put("userType", "guide");
        guideData.put("parent1Name", parent1Name);
        guideData.put("parent2Name", parent2Name);
        guideData.put("parentPhone", parentPhone);
        guideData.put("isActive", true); //ווידוא שהמדריך פעיל


        String base64Image = "";
        if (currentProfileBitmap != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 60; // התאימי לפי הצורך
                currentProfileBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                byte[] imageBytes = baos.toByteArray();
                int maxSizeBytesBeforeEncoding = 500 * 1024; // 500KB

                if (imageBytes.length < maxSizeBytesBeforeEncoding) {
                    base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                    Log.d(TAG, "Guide Base64 image string created. Length: " + base64Image.length());
                } else {
                    Log.w(TAG, "Guide compressed image is still too large (" + imageBytes.length + " bytes). Not saving as Base64.");
                    Toast.makeText(this, "התמונה גדולה מדי לאחר כיווץ, לא נשמרה.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting guide Bitmap to Base64 string", e);
                Toast.makeText(this, "שגיאה בעיבוד התמונה של המדריך.", Toast.LENGTH_SHORT).show();
            }
        }
        guideData.put("profileImageBase64", base64Image);

        // קריאה ליצירת משתמש ב-Auth ושמירת הנתונים
        createUserAndSaveData(guideData);
    }


    // --- פונקציה ליצירת משתמש ב-Auth ושמירת הנתונים ב-Firestore ---
    private void createUserAndSaveData(Map<String, Object> guideData) {
        String email = (String) guideData.get("email");
        if (email == null) {
            Log.e(TAG, "Email is null, cannot create user for guide.");
            Toast.makeText(this, "שגיאה: כתובת המייל חסרה.", Toast.LENGTH_LONG).show();
            enableAddButton();
            return;
        }

        Log.d(TAG, "Creating guide user in Firebase Auth for email: " + email);
        mAuth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                Log.d(TAG, "Firebase Auth guide user created successfully. UID: " + userId);
                                Toast.makeText(AddGuideActivity.this, "משתמש מדריך נוצר", Toast.LENGTH_SHORT).show();
                                guideData.put("uid", userId);
                                saveDataToFirestore(userId, guideData);
                            } else {
                                Log.e(TAG, "Auth guide user creation successful, but getCurrentUser is null!");
                                Toast.makeText(AddGuideActivity.this, "שגיאה בקבלת פרטי משתמש מדריך לאחר יצירה", Toast.LENGTH_LONG).show();
                                enableAddButton();
                            }
                        } else {
                            Log.w(TAG, "Guide createUserWithEmail:failure", task.getException());
                            String authErrorMsg = "יצירת משתמש מדריך נכשלה.";
                            if (task.getException() != null) {
                                authErrorMsg += " סיבה: " + task.getException().getMessage();
                            }
                            Toast.makeText(AddGuideActivity.this, authErrorMsg, Toast.LENGTH_LONG).show();
                            enableAddButton();
                        }
                    }
                });
    }

    // --- פונקציה לשמירת הנתונים הסופיים ב-Firestore ---
    private void saveDataToFirestore(String userId, Map<String, Object> guideData) {
        Log.d(TAG, "Saving final guide data to Firestore for UID: " + userId);
        Log.d(TAG, "Guide Data to save: " + guideData.toString().substring(0, Math.min(guideData.toString().length(), 300)) + "...");

        db.collection("users").document(userId) // עדיין שמירה ב-"users"
                .set(guideData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Guide data successfully written to Firestore!");
                    Toast.makeText(AddGuideActivity.this, "פרטי המדריך נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddGuideActivity.this, ManagerMainPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing guide data to Firestore", e);
                    Toast.makeText(AddGuideActivity.this, "שגיאה בשמירת פרטי המדריך ב-Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    enableAddButton();
                });
    }

    // --- פונקציית עזר להפעלת הכפתור ---
    private void enableAddButton() {
        if (addButton != null) {
            addButton.setEnabled(true);
        }
    }

    // --- פונקציות DatePicker ו-Gallery (ללא שינוי מהקוד שלך) ---
    private void openDatePickerDialog(Button button) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR); int month = calendar.get(Calendar.MONTH); int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format("%02d/%02d/%d", selectedDay, (selectedMonth + 1), selectedYear);
                    button.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // ודאי שזה קיים כדי שה-Chooser ידע מה להציג
        imagePickerLauncher.launch(intent);
    }
}


