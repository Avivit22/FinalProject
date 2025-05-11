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
                            profileImage.setImageResource(R.drawable.default_profile); // החזרת ברירת מחדל
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
        guideData.put("dayOfWeek", dayOfWeek); // שדה זה רלוונטי למדריך?
        guideData.put("joinDate", joinDate);
        guideData.put("userType", "guide");
        // הוספת שדות הורים אם הם רלוונטיים למדריך
        guideData.put("parent1Name", parent1Name);
        guideData.put("parent2Name", parent2Name);
        guideData.put("parentPhone", parentPhone);


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
        guideData.put("profileImageBase64", base64Image); // שמירת התמונה כ-Base64

        // קריאה ליצירת משתמש ב-Auth ושמירת הנתונים
        createUserAndSaveData(guideData);
    }


    // --- פונקציה ליצירת משתמש ב-Auth ושמירת הנתונים ב-Firestore ---
    // (בדיוק כמו קודם, כי היא לא מטפלת ישירות בתמונה אלא מקבלת אותה ב-guideData)
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
    // (בדיוק כמו קודם, כי היא לא מטפלת ישירות בתמונה אלא מקבלת אותה ב-guideData)
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
                    // שקלי למחוק את משתמש ה-Auth אם השמירה ב-Firestore נכשלה
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



/*package com.example.myapplicationt1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap; // עדיין נחוץ להצגת התמונה
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore; // עדיין נחוץ להצגת התמונה
import android.text.TextUtils;
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

// --- ייבואים עבור Firebase (כולל Storage) ---
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
// --- סוף ייבואי Firebase ---

import java.io.IOException; // עדיין נחוץ להצגת התמונה
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // נחוץ ליצירת שמות קבצים ייחודיים

public class AddGuideActivity extends AppCompatActivity {

    // --- UI Elements ---
    private Button birthDateButton;
    private Button joinDateButton;
    private ImageView profileImage;
    private Button uploadImageButton;
    private Button addButton; // כפתור ההוספה
    private EditText fullNameInput, activeNumberInput, emailInput, phoneInput, addressInput, parent1NameInput, parent2NameInput, parentPhoneInput;
    private Spinner genderSpinner, dayOfWeekSpinner;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage; // הוספנו חזרה
    private StorageReference storageReference; // הוספנו חזרה

    // --- Data ---
    private Uri imageUri; // ה-URI של התמונה שנבחרה
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String TAG = "AddGuide";

    // בחירת תמונה מהגלריה (קוד מקורי עם שיפורים קלים בלוגים)
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            imageUri = result.getData().getData();
                            Log.d(TAG, "Image selected: " + imageUri.toString());
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                profileImage.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                                imageUri = null; // איפוס אם יש שגיאה
                            }
                        } else {
                            Log.d(TAG, "Image selection cancelled or failed.");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_guide); // שימוש ב-XML המקורי שלך

        // --- אתחול Firebase (כולל Storage) ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(); // הוספנו חזרה
        storageReference = storage.getReference(); // הוספנו חזרה
        // --- סוף אתחול ---

        // --- קבלת רפרנסים ל-UI Elements ---
        fullNameInput = findViewById(R.id.fullNameInput);
        activeNumberInput = findViewById(R.id.activeNumberInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        addressInput = findViewById(R.id.addressInput);
        parent1NameInput = findViewById(R.id.parent1NameInput);
        parent2NameInput = findViewById(R.id.parent2NameInput);
        parentPhoneInput = findViewById(R.id.parentPhoneInput);
        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        addButton = findViewById(R.id.addButton);
        birthDateButton = findViewById(R.id.birthDateButton);
        joinDateButton = findViewById(R.id.joinDateButton);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        // --- סוף קבלת רפרנסים ---

        // לחיצה על לוגו (קוד מקורי עם שיפור בניווט)
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> {
            Intent intent = new Intent(AddGuideActivity.this, ManagerMainPageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // --- Listeners (קוד מקורי נשמר) ---
        birthDateButton.setOnClickListener(v -> openDatePickerDialog(birthDateButton));
        joinDateButton.setOnClickListener(v -> openDatePickerDialog(joinDateButton));
        View.OnClickListener imagePickerListener = v -> openGallery();
        profileImage.setOnClickListener(imagePickerListener);
        uploadImageButton.setOnClickListener(imagePickerListener);

        // Listener לכפתור הוספה ראשי
        addButton.setOnClickListener(v -> {
            Log.d(TAG, "Add button clicked");
            startGuideAdditionProcess(); // קריאה לפונקציה שמתחילה את התהליך
        });
        // סוף Listener
    }

    // --- פונקציה שמתחילה את תהליך ההוספה ---
    private void startGuideAdditionProcess() {
        // 1. איסוף נתונים מהטופס
        String fullName = fullNameInput.getText().toString().trim();
        String activeNumber = activeNumberInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String parent1Name = parent1NameInput.getText().toString().trim();
        String parent2Name = parent2NameInput.getText().toString().trim();
        String parentPhone = parentPhoneInput.getText().toString().trim();
        String gender = "";
        if (genderSpinner.getSelectedItem() != null) { gender = genderSpinner.getSelectedItem().toString(); }
        String dayOfWeek = "";
        if (dayOfWeekSpinner.getSelectedItem() != null) { dayOfWeek = dayOfWeekSpinner.getSelectedItem().toString(); }
        String birthDate = birthDateButton.getText().toString();
        String joinDate = joinDateButton.getText().toString();

        // 2. ולידציה בסיסית
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

        // 3. הכנת ה-Map עם הנתונים
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
        guideData.put("parent1Name", parent1Name);
        guideData.put("parent2Name", parent2Name);
        guideData.put("parentPhone", parentPhone);
        guideData.put("userType", "guide");

        // --- השבתת כפתור ---
        addButton.setEnabled(false);

        // 4. בדיקה אם נבחרה תמונה והמשך בהתאם
        if (imageUri != null) {
            Log.d(TAG, "Image URI found, starting upload process.");
            // קריאה לפונקציה שמעלה תמונה ואז יוצרת משתמש
            uploadImageAndCreateUser(guideData);
        } else {
            Log.d(TAG, "No image URI found, proceeding without upload.");
            guideData.put("profileImageUrl", ""); // שדה ריק
            // קריאה ישירה ליצירת משתמש ושמירה (ללא תמונה)
            createUserAndSaveData(guideData);
        }
    }

    // --- פונקציה להעלאת תמונה, ואז יצירת משתמש ושמירה ---
    private void uploadImageAndCreateUser(Map<String, Object> guideData) {
        String filename = "guide_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = storageReference.child(filename);
        Log.d(TAG, "Uploading guide image to: " + filename);
        UploadTask uploadTask = fileRef.putFile(imageUri);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    // ההעלאה הצליחה, ממשיכים לקבל את ה-URL
                    Log.d(TAG, "Image upload successful (putFile complete). Getting download URL...");
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Log.d(TAG, "Successfully retrieved download URL: " + downloadUri.toString());
                        String imageUrl = downloadUri.toString();
                        guideData.put("profileImageUrl", imageUrl); // הוספת ה-URL
                        createUserAndSaveData(guideData); // המשך ליצירת משתמש ושמירה
                    }).addOnFailureListener(e -> {
                        // שגיאה בקבלת ה-URL (כמו הבעיה הקודמת)
                        Log.e(TAG, "Error getting download URL AFTER successful upload: ", e);
                        String errorMsg = "שגיאה בקבלת כתובת התמונה";
                        if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            errorMsg = "שגיאה: הקובץ לא נמצא לאחר ההעלאה. (בדוק הרשאות Storage!)";
                        }
                        Toast.makeText(AddGuideActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        guideData.put("profileImageUrl", ""); // שמירה בלי URL
                        createUserAndSaveData(guideData); // ננסה להמשיך בלעדיו
                    });
                } else {
                    // ההעלאה עצמה נכשלה (putFile נכשל)
                    Log.e(TAG, "Image upload failed (putFile failed): ", task.getException());
                    String errorMessage = getStorageErrorMessage(task.getException()); // שימוש בפונקציית עזר
                    Toast.makeText(AddGuideActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    enableAddButton(); // הפעל מחדש את הכפתור
                }
            }
        });
        // אין כאן addOnProgressListener
    }


    // --- פונקציה ליצירת משתמש ב-Auth ושמירת הנתונים ב-Firestore ---
    private void createUserAndSaveData(Map<String, Object> guideData) {
        String email = (String) guideData.get("email");
        if (email == null) {
            Log.e(TAG, "Email is null, cannot create user.");
            Toast.makeText(this, "שגיאה: כתובת המייל חסרה.", Toast.LENGTH_LONG).show();
            enableAddButton();
            return;
        }

        Log.d(TAG, "Creating user in Firebase Auth for email: " + email);
        mAuth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                Log.d(TAG, "Firebase Auth user created successfully. UID: " + userId);
                                Toast.makeText(AddGuideActivity.this, "משתמש נוצר ב-Auth", Toast.LENGTH_SHORT).show();
                                guideData.put("uid", userId); // הוספת ה-UID לנתונים
                                saveDataToFirestore(userId, guideData); // שמירת הנתונים
                            } else {
                                Log.e(TAG, "Auth user creation successful, but getCurrentUser is null!");
                                Toast.makeText(AddGuideActivity.this, "שגיאה בקבלת פרטי משתמש לאחר יצירה", Toast.LENGTH_LONG).show();
                                enableAddButton();
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String authErrorMsg = "יצירת משתמש נכשלה.";
                            if (task.getException() != null) {
                                authErrorMsg += " סיבה: " + task.getException().getMessage();
                            }
                            Toast.makeText(AddGuideActivity.this, authErrorMsg, Toast.LENGTH_LONG).show();
                            enableAddButton();
                            // אם הגענו לכאן אחרי ניסיון העלאת תמונה, אולי כדאי למחוק אותה
                            // String imageUrlToDelete = (String) guideData.get("profileImageUrl");
                            // if (imageUrlToDelete != null && !imageUrlToDelete.isEmpty()) { deleteImageFromStorage(imageUrlToDelete); }
                        }
                    }
                });
    }


    // --- פונקציה לשמירת הנתונים הסופיים ב-Firestore ---
    private void saveDataToFirestore(String userId, Map<String, Object> guideData) {
        Log.d(TAG, "Saving final guide data to Firestore for UID: " + userId);
        Log.d(TAG, "Data: " + guideData.toString());
        db.collection("users").document(userId) // שמירה ב-"users"
                .set(guideData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Guide data successfully written to Firestore!");
                    Toast.makeText(AddGuideActivity.this, "פרטי המדריך נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    // מעבר למסך הראשי וסגירת הנוכחי
                    Intent intent = new Intent(AddGuideActivity.this, ManagerMainPageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing guide data to Firestore", e);
                    Toast.makeText(AddGuideActivity.this, "שגיאה בשמירת פרטים ב-Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    enableAddButton();
                    // אם השמירה נכשלת, כדאי לשקול למחוק את המשתמש שנוצר ב-Auth ואת התמונה אם הועלתה
                    // FirebaseUser userToDelete = mAuth.getCurrentUser(); if (userToDelete != null && userToDelete.getUid().equals(userId)) { userToDelete.delete(); }
                    // String imageUrlToDelete = (String) guideData.get("profileImageUrl"); if (imageUrlToDelete != null && !imageUrlToDelete.isEmpty()) { deleteImageFromStorage(imageUrlToDelete); }
                });
    }


    // --- פונקציית עזר להפעלת הכפתור ---
    private void enableAddButton() {
        if (addButton != null) {
            addButton.setEnabled(true);
        }
    }


    // --- פונקציות קיימות (DatePicker, Gallery) - ללא שינוי מהותי ---
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
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // --- פונקציית עזר לתרגום שגיאות Storage ---
    private String getStorageErrorMessage(Exception exception) {
        String errorMessage = "שגיאה בהעלאת התמונה.";
        if (exception instanceof StorageException) {
            StorageException se = (StorageException) exception;
            switch (se.getErrorCode()) {
                case StorageException.ERROR_OBJECT_NOT_FOUND: errorMessage = "שגיאת העלאה: אובייקט לא נמצא."; break;
                case StorageException.ERROR_BUCKET_NOT_FOUND: errorMessage = "שגיאת העלאה: דלי האחסון לא נמצא."; break;
                case StorageException.ERROR_PROJECT_NOT_FOUND: errorMessage = "שגיאת העלאה: הפרויקט לא נמצא."; break;
                case StorageException.ERROR_QUOTA_EXCEEDED: errorMessage = "שגיאת העלאה: חריגה ממכסת האחסון."; break;
                case StorageException.ERROR_NOT_AUTHENTICATED: errorMessage = "שגיאת העלאה: נדרש אימות משתמש."; break;
                case StorageException.ERROR_NOT_AUTHORIZED: errorMessage = "שגיאת העלאה: אין הרשאה לבצע את הפעולה."; break; // חשוב לבדוק את זה!
                case StorageException.ERROR_RETRY_LIMIT_EXCEEDED: errorMessage = "שגיאת העלאה: חריגה ממגבלת ניסיונות (רשת?)."; break;
                default: if(se.getMessage() != null) errorMessage += " " + se.getMessage(); break;
            }
        } else if (exception != null && exception.getMessage() != null){
            errorMessage += " " + exception.getMessage();
        }
        Log.e(TAG, "Storage Error Details: " + errorMessage); // הדפסה מפורטת יותר ללוג
        return errorMessage;
    }


    // --- פונקציה למחיקת תמונה (לשימוש עתידי אם צריך) ---
    /*
    private void deleteImageFromStorage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        try {
            StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
            photoRef.delete().addOnSuccessListener(aVoid ->
                Log.d(TAG, "Successfully deleted image from storage: " + imageUrl)
            ).addOnFailureListener(exception ->
                Log.e(TAG, "Failed to delete image from storage: " + imageUrl, exception)
            );
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid image URL format, cannot delete: " + imageUrl, e);
        }
    }


} // סוף המחלקה AddGuide









/*
package com.example.myapplicationt1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap; // עדיין נחוץ להצגת התמונה
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore; // עדיין נחוץ להצגת התמונה
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// --- ייבואים עבור Firebase Auth ו-Firestore בלבד ---
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener; // עדיין נחוץ לטיפול בשגיאות
import com.google.android.gms.tasks.OnSuccessListener; // עדיין נחוץ לטיפול בשגיאות
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
// --- סוף הסרה ---

import java.io.IOException; // עדיין נחוץ להצגת התמונה
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
// import java.util.UUID; // *** הסרה: אין צורך ב-UUID ללא העלאת קבצים ***


public class AddGuide extends AppCompatActivity {

    // --- UI Elements ---
    private Button birthDateButton, joinDateButton, uploadImageButton, addButton;
    private ImageView profileImage;
    private EditText fullNameInput, activeNumberInput, emailInput, phoneInput, addressInput, parent1NameInput, parent2NameInput, parentPhoneInput;
    private Spinner genderSpinner, dayOfWeekSpinner;

    // --- Firebase Auth & Firestore בלבד ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    // *** הסרה: משתני Storage הוסרו ***
    // private FirebaseStorage storage;
    // private StorageReference storageReference;

    // --- Data ---
    private Uri imageUri; // עדיין שומרים את ה-URI אם המשתמש בחר תמונה לתצוגה
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String TAG = "AddGuide";

    // בחירת תמונה מהגלריה (נשאר כדי שהמשתמש יוכל לבחור ולראות תמונה)
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            imageUri = result.getData().getData();
                            Log.d(TAG, "Image selected (for display only): " + imageUri.toString());
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                profileImage.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                Log.e(TAG, "Error loading image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                                imageUri = null;
                            }
                        } else {
                            Log.d(TAG, "Image selection cancelled or failed.");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_guide); // שימוש ב-XML המקורי

        // --- אתחול Firebase Auth & Firestore ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // *** הסרה: אתחול Storage הוסר ***
        // storage = FirebaseStorage.getInstance();
        // storageReference = storage.getReference();

        // --- קבלת רפרנסים ---
        // ... (כל ה-findViewById נשארים זהים לקוד הקודם) ...
        fullNameInput = findViewById(R.id.fullNameInput); activeNumberInput = findViewById(R.id.activeNumberInput); emailInput = findViewById(R.id.emailInput); phoneInput = findViewById(R.id.phoneInput); addressInput = findViewById(R.id.addressInput); parent1NameInput = findViewById(R.id.parent1NameInput); parent2NameInput = findViewById(R.id.parent2NameInput); parentPhoneInput = findViewById(R.id.parentPhoneInput); genderSpinner = findViewById(R.id.genderSpinner); dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner); addButton = findViewById(R.id.addButton); birthDateButton = findViewById(R.id.birthDateButton); joinDateButton = findViewById(R.id.joinDateButton); profileImage = findViewById(R.id.profileImage); uploadImageButton = findViewById(R.id.uploadImageButton);
*/

        // --- Listeners ---
        // ... (כל ה-Listeners נשארים זהים לקוד הקודם) ...
 /*     ImageView logoImage = findViewById(R.id.logoImage); logoImage.setOnClickListener(v -> {  finish(); });
      /*  birthDateButton.setOnClickListener(v -> openDatePickerDialog(birthDateButton)); joinDateButton.setOnClickListener(v -> openDatePickerDialog(joinDateButton));
        View.OnClickListener imagePickerListener = v -> openGallery(); profileImage.setOnClickListener(imagePickerListener); uploadImageButton.setOnClickListener(imagePickerListener);

        addButton.setOnClickListener(v -> {
            Log.d(TAG, "Add button clicked - attempting direct user creation and data save."); // שינוי בלוג
            // *** שינוי: קריאה ישירה לפונקציה שיוצרת משתמש ושומרת נתונים ***
            collectValidateAndSaveGuideData();
        });
    }

    // --- פונקציה חדשה: אוספת, מאמתת וקוראת ליצירת משתמש ושמירת נתונים ---
    private void collectValidateAndSaveGuideData() {
        // 1. איסוף נתונים מהטופס
        String fullName = fullNameInput.getText().toString().trim();
        String activeNumber = activeNumberInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String parent1Name = parent1NameInput.getText().toString().trim();
        String parent2Name = parent2NameInput.getText().toString().trim();
        String parentPhone = parentPhoneInput.getText().toString().trim();
        String gender = ""; if (genderSpinner.getSelectedItem() != null) { gender = genderSpinner.getSelectedItem().toString(); }
        String dayOfWeek = ""; if (dayOfWeekSpinner.getSelectedItem() != null) { dayOfWeek = dayOfWeekSpinner.getSelectedItem().toString(); }
        String birthDate = birthDateButton.getText().toString();
        String joinDate = joinDateButton.getText().toString();

        // 2. ולידציה בסיסית
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(activeNumber) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || birthDate.equals("בחר תאריך") || joinDate.equals("בחר תאריך") || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // אפשר להחזיר את ה-Toast הספציפיים אם רוצים
            Toast.makeText(this, "נא למלא את כל השדות בצורה תקינה", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. הכנת ה-Map עם הנתונים
        Map<String, Object> guideData = new HashMap<>();
        guideData.put("fullName", fullName); guideData.put("activeNumber", activeNumber); guideData.put("email", email);
        guideData.put("phone", phone); guideData.put("address", address); guideData.put("gender", gender);
        guideData.put("birthDate", birthDate); guideData.put("dayOfWeek", dayOfWeek); guideData.put("joinDate", joinDate);
        guideData.put("parent1Name", parent1Name); guideData.put("parent2Name", parent2Name); guideData.put("parentPhone", parentPhone);
        guideData.put("userType", "guide");
        // *** הסרה: אין טיפול בתמונה, אז שומרים שדה ריק (או לא שומרים בכלל - עדיף ריק לעקביות) ***
        guideData.put("profileImageUrl", ""); // שומרים מחרוזת ריקה

        // --- השבתת כפתור ---
        addButton.setEnabled(false);

        // 4. קריאה ישירה ליצירת משתמש ב-Auth ושמירת הנתונים
        createUserAndSaveData(guideData);
    }


    // --- פונקציה ליצירת משתמש ב-Auth ושמירת הנתונים ב-Firestore ---
    // (זהה לקוד הקודם, כי היא לא טיפלה בהעלאת תמונה)
    private void createUserAndSaveData(Map<String, Object> guideData) {
        String email = (String) guideData.get("email");
        if (email == null) {
            Log.e(TAG, "Email is null, cannot create user.");
            Toast.makeText(this, "שגיאה: כתובת המייל חסרה.", Toast.LENGTH_LONG).show();
            enableAddButton();
            return;
        }

        Log.d(TAG, "Creating user in Firebase Auth for email: " + email);
        mAuth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                Log.d(TAG, "Firebase Auth user created successfully. UID: " + userId);
                                Toast.makeText(AddGuide.this, "משתמש נוצר ב-Auth", Toast.LENGTH_SHORT).show();
                                guideData.put("uid", userId);
                                saveDataToFirestore(userId, guideData); // שמירת הנתונים
                            } else {
                                Log.e(TAG, "Auth user creation successful, but getCurrentUser is null!");
                                Toast.makeText(AddGuide.this, "שגיאה בקבלת פרטי משתמש לאחר יצירה", Toast.LENGTH_LONG).show();
                                enableAddButton();
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            // נציג הודעה קצת יותר ברורה למשתמש
                            String authErrorMsg = "יצירת משתמש נכשלה.";
                            if (task.getException() != null) {
                                // כאן אפשר לנתח את סוג השגיאה מ-FirebaseAuthException אם רוצים, למשל מייל קיים
                                authErrorMsg += " סיבה: " + task.getException().getMessage();
                            }
                            Toast.makeText(AddGuide.this, authErrorMsg, Toast.LENGTH_LONG).show();
                            enableAddButton();
                        }
                    }
                });
    }


    // --- פונקציה לשמירת הנתונים הסופיים ב-Firestore ---
    // (זהה לקוד הקודם)
    private void saveDataToFirestore(String userId, Map<String, Object> guideData) {
        Log.d(TAG, "Saving final guide data to Firestore for UID: " + userId);
        Log.d(TAG, "Data: " + guideData.toString());
        db.collection("users").document(userId)
                .set(guideData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Guide data successfully written to Firestore!");
                    Toast.makeText(AddGuide.this, "פרטי המדריך נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                    // מעבר למסך הראשי וסגירת הנוכחי
                    Intent intent = new Intent(AddGuide.this, ManagerMainPage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing guide data to Firestore", e);
                    Toast.makeText(AddGuide.this, "שגיאה בשמירת פרטים ב-Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    enableAddButton();
                    // אם השמירה נכשלת, כדאי לשקול למחוק את המשתמש שנוצר ב-Auth
                    // FirebaseUser userToDelete = mAuth.getCurrentUser(); if (userToDelete != null && userToDelete.getUid().equals(userId)) { userToDelete.delete(); }
                });
    }

    // --- פונקציית עזר להפעלת הכפתור ---
    private void enableAddButton() {
        if (addButton != null) {
            addButton.setEnabled(true);
        }
    }

    // --- פונקציות קיימות (DatePicker, Gallery) נשארות זהות ---
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
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // --- פונקציית עזר לתרגום שגיאות Storage - *** כבר לא נחוצה והוסרה *** ---
    // private String getStorageErrorMessage(Exception exception) { ... }

    // --- פונקציה למחיקת תמונה - *** כבר לא נחוצה והוסרה *** ---
    // private void deleteImageFromStorage(String imageUrl) { ... }

} // סוף המחלקה AddGuide


"""

*/