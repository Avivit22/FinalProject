package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * מסך התראות עבור מדריך.
 * מציג למדריך רשימת בקשות להשלמה/שיעור נוסף שאושרו או נדחו,
 * ומעדכן אותן כ"נצפו" ב-Firestore.
 */
public class NotificationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private NotificationAdapter adapter;
    private List<DocumentSnapshot> notifications = new ArrayList<>();
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // אתחול RecyclerView והתאמת האדאפטר
        recyclerView = findViewById(R.id.rvNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications);
        recyclerView.setAdapter(adapter);

        // כפתור חזרה באמצעות לוגו
        ImageView logo = findViewById(R.id.logoImage);
        logo.setOnClickListener(v -> finish());

        // כפתור מעבר לעמוד בקשות ממתינות
        TextView btnGoToPending = findViewById(R.id.btnGoToPending);
        btnGoToPending.setOnClickListener(v -> {
            Intent intent = new Intent(this, PendingRequestsActivity.class);
            startActivity(intent);
        });

        /**
         * מאזין לשינויים בחיבור המשתמש (אם יצא/נכנס).
         * ברגע שמזהה משתמש מחובר, טוען את ההתראות שלו.
         */
        authListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                String uid = firebaseAuth.getCurrentUser().getUid();
                loadNotifications(uid);
            } else {
                Toast.makeText(this, "נדרש להתחבר", Toast.LENGTH_SHORT).show();
                finish();
            }
        };
    }

    /**
     * הרשמה למאזין האימות בתחילת הפעילות.
     */
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authListener);
    }

    /**
     * הסרה של המאזין כאשר הפעילות נעצרת כדי למנוע נזילות זיכרון.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            mAuth.removeAuthStateListener(authListener);
        }
    }

    /**
     * טעינת ההתראות ממסד הנתונים עבור המשתמש הנוכחי.
     * כולל סימון ההתראות כ"נצפו" בשדה seenByGuide.
     * @param uid מזהה המשתמש
     */
    private void loadNotifications(String uid) {
        db.collection("completions")
                .whereEqualTo("submittedBy", uid)
                .whereIn("status", Arrays.asList("approved", "rejected"))
                .orderBy("decisionAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    notifications.clear();
                    for (DocumentSnapshot doc : query) {
                        notifications.add(doc);
                        // עדכון ההתראה כ"נצפתה"
                        doc.getReference().update("seenByGuide", true);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בטעינת התראות", Toast.LENGTH_SHORT).show());
    }
}

