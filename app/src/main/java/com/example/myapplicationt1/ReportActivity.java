package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.content.Intent;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // מאזין ללחיצה על תמונת הלוגו
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());
    }

    // פונקציה לשאיבת סוג המשתמש מ-Firestore והעברה בהתאם לעמוד הבית של מנהל או מדריך
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
                                intent = new Intent(ReportActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(ReportActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ReportActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ReportActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ReportActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }


}
