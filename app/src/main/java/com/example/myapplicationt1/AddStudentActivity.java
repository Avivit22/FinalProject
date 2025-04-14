package com.example.myapplicationt1;

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
    private EditText fullNameInput, activeNumberInput, gradeInput, phoneInput, addressInput, parent1Input, parent2Input, parentPhoneInput; //הוספה
    private Spinner genderSpinner, dayOfWeekSpinner; //הוספה
    private FirebaseFirestore db; // מופע של Firestore הוספה

    // *** תוספת: משתנים עבור Storage ו-ProgressBar ***
    private FirebaseStorage storage; // תוספת
    private StorageReference storageReference; // תוספת
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
        studentData.put("joinDate", joinDate);
        // נוסיף גם את ה-URL של התמונה (כרגע ריק, כי לא טיפלנו בהעלאה)
        String profileImageUrl = (imageUri != null) ? imageUri.toString() : ""; // שמירת ה-URI כטקסט (לא אידיאלי, עדיף להעלות)
        studentData.put("profileImageUrl", profileImageUrl);


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
