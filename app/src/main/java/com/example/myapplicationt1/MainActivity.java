package com.example.myapplicationt1;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 *עמוד ההתחברות הראשי
 * עמוד זה מאפשר למשתמשים (מנהלים או מדריכים) להתחבר לאפליקציה.
 */
public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // אתחול Firebase Auth ו-Firestore
        mAuth = FirebaseAuth.getInstance(); //אתחול שמאפשר שירות בדיקת התחברות
        db = FirebaseFirestore.getInstance(); //אתחול שמאפשר שמירה ושליפת נתוני משתמש

        // איתור שדור ההקלדה במסך ההתחברות
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        // מציאת הכפתור להתחברות כמנהל
        Button loginButtonM = findViewById(R.id.loginButtonM);
        loginButtonM.setOnClickListener(v -> {
            String email = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "נא להזין מייל וסיסמא", Toast.LENGTH_SHORT).show();
                return;
            }//if

            // התחברות באמצעות Firebase Auth
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                // שליפת נתוני המשתמש מ-Firestore מאוסף "users"
                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String userType = documentSnapshot.getString("userType");
                                                if (userType != null && userType.equalsIgnoreCase("manager")) {
                                                    // אם המשתמש הוא מנהל, מציגים את חלון הפופאפ להזנת קוד
                                                    showAdminCodePopup();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "משתמש זה אינו מנהל", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(MainActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(MainActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });


        // מציאת הכפתור להתחברות כמדריך
        Button loginButtonG = findViewById(R.id.loginButtonG);

        // לחיצה על כפתור "התחבר כמדריך" – נבדוק שהמשתמש הוא מדריך
        loginButtonG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInAndCheckUserType("guide");
            }
        });

        // הוספת מאזין לכפתור "שכחתי סיסמא"
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordText);
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = usernameEditText.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(MainActivity.this, "נא להזין כתובת מייל", Toast.LENGTH_SHORT).show();
                    return;
                }
                // קריאה לפונקציה לשליחת מייל איפוס סיסמא
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "מייל לאיפוס סיסמא נשלח", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "שגיאה בשליחת מייל איפוס: "
                                        + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


    }

    /**
     *פונקציה לאימות המשתמש ובדיקת סוג המשתמש במסד הנתונים
     */
    private void signInAndCheckUserType(String expectedType) {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "נא להזין מייל וסיסמא", Toast.LENGTH_SHORT).show();
            return;
        }

        // התחברות באמצעות Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // אם ההתחברות הצליחה, שולפים את האובייקט שמייצג את המשתמש שמחובר כרגע
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            // שליפת נתוני המשתמש מ-Firestore מאוסף "users"
                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String userType = documentSnapshot.getString("userType");
                                            if (userType != null && userType.equalsIgnoreCase(expectedType)) {
                                                // אם המשתמש הוא מדריך, נעבור למסך הבית של המדריך
                                                Intent intent = new Intent(MainActivity.this, GuideMainPageActivity.class);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(MainActivity.this, "משתמש זה אינו מדריך", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(MainActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     *בדיקת פופאפ הזנת קוד מנהל
     */
    private void showAdminCodePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_admin_code, null);

        AlertDialog dialog = builder.setView(popupView).create();

        // קובע שהפופ-אפ יוצג קטן ובמרכז
        dialog.getWindow().setLayout(300, 200);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        //חיפוש תיבת הטקסט בה המשתמש מזין את קוד המנהל
        EditText adminCodeEditText = popupView.findViewById(R.id.adminCodeEditText);
        //חיפוש כפתור אישור הזנת הקוד- מוביל להתחברות כמנהל
        Button adminLoginButton = popupView.findViewById(R.id.adminLoginButton);

        adminLoginButton.setOnClickListener(v -> {
            String enteredCode = adminCodeEditText.getText().toString();
            if (enteredCode.equals("159357")) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, ManagerMainPageActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "קוד המנהל שהוכנס שגוי", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

}
