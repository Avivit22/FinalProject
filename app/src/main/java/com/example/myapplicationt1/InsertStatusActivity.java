package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class InsertStatusActivity extends AppCompatActivity {

    // משתנים עבור Firebase Auth ו-Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_status);

        // אתחול Firebase Auth ו-Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // מאזין ללחיצה על תמונת הלוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());


        // מאזין לכפתור "פרטי חניך" של נתן כהן
        Button detailsNathan = findViewById(R.id.detailsNathan);
        detailsNathan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStudentDetails("נתן כהן");
            }
        });

        // מאזין לכפתור "פרטי חניך" של אורי לוי
        Button detailsOri = findViewById(R.id.detailsOri);
        detailsOri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStudentDetails("אורי לוי");
            }
        });
    }

    /**
     * מעבר לעמוד פרטי החניך עם העברת שם החניך
     */
    private void openStudentDetails(String studentName) {
        Intent intent = new Intent(InsertStatusActivity.this, ShowUpdateStudentActivity.class);
        intent.putExtra("STUDENT_NAME", studentName); // שולח את שם החניך לעמוד הבא
        startActivity(intent);
    }

    //  פונקציה לקבלת סוג המשתמש והעברה למסך בית מנהל או מדריך בהתאם לסוג משתמש
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
                                intent = new Intent(InsertStatusActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(InsertStatusActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(InsertStatusActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(InsertStatusActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(InsertStatusActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

}
