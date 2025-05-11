/*package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.IOException;
import java.util.Calendar;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.text.TextUtils;
// *** תוספת: הוספות עבור Firebase Storage ***
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AddStudentActivity extends AppCompatActivity {

    private Button birthDateButton;
    private Button joinDateButton;
    private ImageView profileImage;
    private Button uploadImageButton;
    private Button addButton; // כפתור הוספה ראשי
    private EditText fullNameInput, activeNumberInput, gradeInput, phoneInput, addressInput, parent1Input, parent2Input, parentPhoneInput;
    private Spinner genderSpinner, dayOfWeekSpinner;
    private FirebaseFirestore db;

    // *** תוספת: משתנים עבור Storage ו-ProgressBar ***
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private static final String TAG = "AddStudent"; //משתנה סטטי לזיהוי לוגים עבור הוספת החניך לענן



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_student); // חיבור המסך לקובץ XML

        // --- אתחול Firebase ---
        db = FirebaseFirestore.getInstance();

        // --- קבלת רפרנסים ל-UI Elements פעם אחת ---
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

        // Spinner של מגדר
        //Spinner genderSpinner = findViewById(R.id.genderSpinner);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // לחיצה על לוגו מחזירה לעמוד ראשי מנהל
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddStudentActivity.this, ManagerMainPageActivity.class);
                startActivity(intent);
                finish(); // סוגר את המסך הנוכחי הוספה
            }
        });

        // לחיצה על כפתור "תאריך לידה"
        //birthDateButton = findViewById(R.id.birthDateButton);
        birthDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker(birthDateButton);
            }
        });

        // לחיצה על כפתור "תאריך הצטרפות"
        //joinDateButton = findViewById(R.id.joinDateButton);
        joinDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker(joinDateButton);
            }
        });

        // הוספת תמונת פרופיל
        //profileImage = findViewById(R.id.profileImage);
        //uploadImageButton = findViewById(R.id.uploadImageButton);

        // מאזין ללחיצה על התמונה כדי לבחור תמונה מהגלריה
        profileImage.setOnClickListener(v -> openGallery());

        // מאזין ללחיצה על כפתור "בחר תמונה"
        uploadImageButton.setOnClickListener(v -> openGallery());


        // כפתור "הוספה למערכת" – שמירת נתוני החניך ב-Firestore
        //Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Log.d(TAG, "Add button clicked");

            addStudentToFirestore(); // קריאה לפונקציה מסודרת
            // *** תוספת: שינוי - קריאה לפונקציה שמתחילה את התהליך (כולל העלאת תמונה פוטנציאלית) ***
            //startStudentAdditionProcess(); // תוספת

        });

    }//onCreate

    // --- פונקציה לאיסוף, ולידציה ושמירה ---
    private void addStudentToFirestore() {
        // איסוף נתונים מהטופס (משתמש במשתני המחלקה)
        String fullName = fullNameInput.getText().toString().trim();
        String activeNumber = activeNumberInput.getText().toString().trim();
        String grade = gradeInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String parent1 = parent1Input.getText().toString().trim();
        String parent2 = parent2Input.getText().toString().trim();
        String parentPhone = parentPhoneInput.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String dayOfWeek = dayOfWeekSpinner.getSelectedItem().toString();
        String birthDate = birthDateButton.getText().toString();
        String joinDate = joinDateButton.getText().toString();

        // --- ולידציה בסיסית ---
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(activeNumber) || TextUtils.isEmpty(grade)) {
            Toast.makeText(this, "נא למלא שם מלא, מספר פעיל וכיתה", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Required fields missing.");
            return; // עצירת הפונקציה אם חסרים שדות חובה
        }
        if (birthDate.equals("בחר תאריך") || joinDate.equals("בחר תאריך")) {
            Toast.makeText(this, "נא לבחור תאריך לידה והצטרפות", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: Dates not selected.");
            return;
        }
        // ניתן להוסיף עוד ולידציות לפי הצורך (למשל, בדיקת spinner position 0)



        // הדפסה של כל הנתונים שנאספו (כמו בקוד המקורי)
        Log.d(TAG, "Full Name: " + fullName);
        Log.d(TAG, "Active Number: " + activeNumber);
        Log.d(TAG, "Grade: " + grade);
        Log.d(TAG, "Phone: " + phone);
        Log.d(TAG, "Address: " + address);
        Log.d(TAG, "Parent1: " + parent1);
        Log.d(TAG, "Parent2: " + parent2);
        Log.d(TAG, "Parent Phone: " + parentPhone);
        Log.d(TAG, "Gender: " + gender);
        Log.d(TAG, "Day of Week: " + dayOfWeek);
        Log.d(TAG, "Birth Date: " + birthDate);
        Log.d(TAG, "Join Date: " + joinDate);

        // בניית מפה עם הנתונים
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("fullName", fullName);
        // studentData.put("email", email); // *** הוסר - אין שדה מייל לחניך ***
        studentData.put("activeNumber", activeNumber);
        studentData.put("grade", grade);
        studentData.put("phone", phone);
        studentData.put("address", address);
        studentData.put("parent1Name", parent1);
        studentData.put("parent2Name", parent2);
        studentData.put("parentPhone", parentPhone);
        studentData.put("gender", gender);
        studentData.put("dayOfWeek", dayOfWeek);
        studentData.put("birthDate", birthDate);
        studentData.put("joinDate", joinDate);*/
        // נוסיף גם את ה-URL של התמונה (כרגע ריק, כי לא טיפלנו בהעלאה)
        //////////////////////////////////////////////////////////////////////
        /*String profileImageUrl = (imageUri != null) ? imageUri.toString() : ""; // שמירת ה-URI כטקסט (לא אידיאלי, עדיף להעלות)
        studentData.put("profileImageUrl", profileImageUrl);*/

/*

        Log.d(TAG, "Student data map created: " + studentData.toString());

        // --- השבתת הכפתור למניעת לחיצות כפולות ---
        addButton.setEnabled(false);
        // כאן אפשר להציג ProgressBar אם רוצים

        // שמירת הנתונים ב-Firestore באוסף "STUDENTS" (אותיות גדולות - תקני לפי הצורך!)
        db.collection("students") // *** שימי לב לשם ה-Collection! ***
                .add(studentData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added successfully with ID: " + documentReference.getId());
                        Toast.makeText(AddStudentActivity.this, "החניך נוסף בהצלחה", Toast.LENGTH_SHORT).show();

                        // --- הפעלת הכפתור מחדש והסתרת ProgressBar ---
                        // addButton.setEnabled(true); // אין צורך אם עוברים מסך

                        // מעבר למסך הראשי של המנהל *רק* לאחר הצלחה
                        Intent intent = new Intent(AddStudentActivity.this, ManagerMainPageActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // סגירת המסך הנוכחי
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding document to Firestore", e); // הדפסת השגיאה המלאה ל-Logcat
                        Toast.makeText(AddStudentActivity.this, "שגיאה בהוספת החניך: " + e.getMessage(), Toast.LENGTH_LONG).show(); // הצגת הודעה ברורה יותר

                        // --- הפעלת הכפתור מחדש והסתרת ProgressBar במקרה של כישלון ---
                        addButton.setEnabled(true);
                        // כאן אפשר להסתיר ProgressBar
                    }
                });
        Log.d(TAG, "Firestore add() called."); // לוודא שהקריאה ל-Firestore מתבצעת
    }

    private void openDatePicker(Button button) {
        // קבלת התאריך הנוכחי
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // יצירת לוח שנה לבחירת תאריך
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                // עדכון טקסט הכפתור עם התאריך שנבחר
                String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                button.setText(selectedDate);
            }
        }, year, month, day);

        // הצגת לוח השנה
        datePickerDialog.show();
    }

    // פתיחת גלריה לבחירת תמונה
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "בחר תמונה"), PICK_IMAGE_REQUEST);
    }

    // קבלת התמונה שנבחרה מהגלריה
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.d(TAG, "Image selected: " + imageUri.toString()); // תוספת
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Error loading bitmap from gallery", e); // תוספת
                Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show(); // תוספת
                imageUri = null; // תוספת: איפוס ה-URI אם יש שגיאה
            }//catch
        }//if
        else {
            Log.d(TAG, "Image selection cancelled or failed."); // תוספת
        }//else
    }
}
*/


package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // *** תוספת ***
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64; // *** תוספת ***
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.ByteArrayOutputStream; // *** תוספת ***
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
// import java.util.UUID; // לא נחוץ יותר לשמות קבצים ב-Storage

import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import android.text.TextUtils;

// אין צורך ב-importים של Storage יותר
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageException;
// import com.google.firebase.storage.StorageReference;
// import com.google.firebase.storage.UploadTask;

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
    // אין צורך במשתני Storage
    // private FirebaseStorage storage;
    // private StorageReference storageRootReference;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri; // URI של התמונה מהגלריה, נשתמש בו כדי לקבל Bitmap
    private Bitmap currentProfileBitmap = null; // *** תוספת: לשמור את ה-Bitmap הנבחר ***
    private static final String TAG = "AddStudentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_student);

        Log.d(TAG, "onCreate started");

        db = FirebaseFirestore.getInstance();
        // אין צורך לאתחל Storage
        // storage = FirebaseStorage.getInstance();
        // storageRootReference = storage.getReference();

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
            // אין צורך לבדוק imageUri כאן, הבדיקה תהיה ב-saveStudentDataToFirestore

            addButton.setEnabled(false);
            Log.d(TAG, "Proceeding to saveStudentDataToFirestore with Base64 image");
            // *** שינוי: קוראים ישירות לפונקציה ששומרת ב-Firestore עם ה-Bitmap ***
            saveStudentDataToFirestore(currentProfileBitmap);
        });
        Log.d(TAG, "onCreate finished");
    }

    // *** שינוי: הפונקציה מקבלת Bitmap וממירה ל-Base64 ***
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

        String base64Image = ""; // ברירת מחדל אם אין תמונה או יש שגיאה
        if (profileBitmapToSave != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // --- כיווץ התמונה ---
                // נסי להתאים את הערכים האלה. איכות נמוכה יותר = קובץ קטן יותר.
                // כדאי גם לשקול הקטנת רזולוציה (שינוי גודל ה-Bitmap) לפני הדחיסה.
                int quality = 60; // התחילי עם ערך נמוך יחסית לבדיקה
                profileBitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                byte[] imageBytes = baos.toByteArray();

                // בדיקה גסה לגודל מקסימלי (למשל, ~500KB לפני Base64, שיהפוך לכ ~665KB אחרי)
                // המגבלה של Firestore היא 1MB לכל המסמך!
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
        studentData.put("profileImageBase64", base64Image); // שדה לשמירת התמונה כ-Base64

        Log.i(TAG, "Attempting to save student data to Firestore: " + studentData.toString().substring(0, Math.min(studentData.toString().length(), 300)) + "..."); // הדפסה חלקית למניעת Log ארוך מדי

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
        currentProfileBitmap = null; // איפוס לפני כל בחירה
        imageUri = null; // איפוס ה-URI גם

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.i(TAG, "Image selected from gallery. URI: " + imageUri.toString());
            try {
                // המרה ל-Bitmap מייד לאחר הבחירה
                currentProfileBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                profileImage.setImageBitmap(currentProfileBitmap); // הצגת התמונה שנבחרה
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