package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.widget.EditText;
import java.util.Calendar;
import androidx.appcompat.app.AlertDialog;
import android.widget.*;
import android.provider.MediaStore;
import android.net.Uri;

//  לתמיכה ב-Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class ShowUpdateGuideActivity extends AppCompatActivity {

    private EditText birthDateEditText, joinDateEditText;

    // משתנים עבור Firebase Auth ו-Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //תמיכה בהעלאת תמונה מהגלריה
    private ImageView profileImage;
    private static final int PICK_IMAGE_REQUEST = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_update_guide);

        // אתחול Firebase Auth ו-Firestore (השורות החדשות, נוספו מתחת לשורת setContentView)
        mAuth = FirebaseAuth.getInstance(); // הוספנו שורה זו
        db = FirebaseFirestore.getInstance(); // הוספנו שורה זו

        // לחיצה על לוגו מחזירה לעמוד ראשי מנהל
        // מציאת התמונה לפי ה-ID
        ImageView logoImage = findViewById(R.id.logoImage);

        logoImage.setOnClickListener(v -> routeUserBasedOnType());


        // מאזין לכפתור "מחיקה מהמערכת"
        Button deleteButton = findViewById(R.id.addButton1);
        deleteButton.setOnClickListener(v -> showDeletePopup());

        // *** הוספת התמיכה בלוח שנה עבור "תאריך לידה" ו-"תאריך הצטרפות" ***
        birthDateEditText = findViewById(R.id.birthDateEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);


        // מאזינים ללחיצה על השדות לפתיחת לוח שנה
        birthDateEditText.setOnClickListener(v -> showDatePickerDialog(birthDateEditText));
        joinDateEditText.setOnClickListener(v -> showDatePickerDialog(joinDateEditText));

        //חיבור משתנים לקובץ XML
        profileImage = findViewById(R.id.profileImage);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);

        // הוספת Spinner עבור מגדר
        Spinner genderSpinner = findViewById(R.id.genderSpinner);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ShowUpdateGuideActivity.this, "המגדר שנבחר: " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // הוספת Spinner עבור יום בשבוע
        Spinner dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        dayOfWeekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ShowUpdateGuideActivity.this, "היום שנבחר: " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //  העלאת תמונה
        uploadImageButton.setOnClickListener(v -> openImagePicker());


    }//onCreate

    //לחיצה על לוגו בודקת את מנהל מעבירה לעמוד בית מנהל ואם מדריך מועבר לעמוד בית מדריך
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


    //  פתיחת בורר התמונות
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    //  הצגת התמונה שנבחרה
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            profileImage.setImageURI(selectedImage);
        }
    }


    private void showDeletePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.popup_delete, null);

        // תיבת טקסט להזנת תאריך עזיבה
        final EditText departureDateEditText = popupView.findViewById(R.id.departureDateEditText);

        // כפתור לוח שנה
        Button calendarButton = popupView.findViewById(R.id.calendarButton);
        calendarButton.setOnClickListener(v -> showDatePickerDialog(departureDateEditText));

        // כפתור למחיקה
        Button deleteConfirmButton = popupView.findViewById(R.id.deleteConfirmButton);
        deleteConfirmButton.setOnClickListener(v -> {
            String departureDate = departureDateEditText.getText().toString();
            if (!departureDate.isEmpty()) {
                Toast.makeText(ShowUpdateGuideActivity.this, "המדריך נמחק בהצלחה", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ShowUpdateGuideActivity.this, "יש להזין תאריך עזיבה", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור לסגירת הפופ-אפ
        ImageView closeButton = popupView.findViewById(R.id.closePopupButton);

        // יצירת הדיאלוג
        AlertDialog dialog = builder.setView(popupView).create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePickerDialog(EditText targetEditText) {
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
