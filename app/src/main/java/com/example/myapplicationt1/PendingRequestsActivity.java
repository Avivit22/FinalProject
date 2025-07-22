package com.example.myapplicationt1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * מחלקה שמציגה את כל הבקשות הממתינות לאישור עבור המדריך הנוכחי.
 * במסך זה המדריך יכול לצפות בבקשות שעדיין לא אושרו או נדחו,
 * כאשר כל בקשה מוצגת ברשימת RecyclerView.
 */
public class PendingRequestsActivity extends AppCompatActivity {

    // אובייקטי Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // רשימת מסמכים שמייצגת בקשות ממתינות
    private List<DocumentSnapshot> pendingList = new ArrayList<>();

    // רכיבי UI
    private RecyclerView recyclerView;
    private PendingRequestsAdapter adapter;
    private TextView tvEmptyMessage;
    private FirebaseAuth.AuthStateListener authListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // הגדרת RecyclerView להצגת הבקשות
        recyclerView = findViewById(R.id.rvPending);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingRequestsAdapter(pendingList);
        recyclerView.setAdapter(adapter);

        // כפתור לוגו לחזרה למסך הקודם
        ImageView logo = findViewById(R.id.logoImage);
        logo.setOnClickListener(v -> finish());

        // הודעה שמוצגת כשאין בקשות ממתינות
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // מאזין לחיבור המשתמש, כדי לוודא שהוא מחובר
        authListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                String uid = firebaseAuth.getCurrentUser().getUid();
                Log.d("PendingActivity", "Loaded for UID: " + uid);
                loadPendingRequests(uid);
            } else {
                Toast.makeText(this, "נדרש להתחבר", Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        // הוספת מאזין האימות
        mAuth.addAuthStateListener(authListener);

    }


    /**
     * טוען את כל הבקשות הממתינות שהוגשו ע"י המדריך הנוכחי
     * @param uid מזהה המשתמש המחובר
     */
    private void loadPendingRequests(String uid) {
        db.collection("completions")
                .whereEqualTo("status", "pending")      // רק בקשות במצב ממתין
                .whereEqualTo("submittedBy", uid)            // רק של המשתמש הנוכחי
                .orderBy("submittedAt", Query.Direction.DESCENDING)   // סדר לפי זמן שליחה
                .get()
                .addOnSuccessListener(query -> {
                    pendingList.clear();
                    List<DocumentSnapshot> docs = query.getDocuments();
                    Log.d("PendingActivity", "Found: " + docs.size() + " docs");

                    if (docs.isEmpty()) {
                        tvEmptyMessage.setVisibility(View.VISIBLE);  // מציג הודעה אם אין בקשות
                    } else {
                        tvEmptyMessage.setVisibility(View.GONE);
                        pendingList.addAll(docs);  // מוסיף לרשימה המקומית
                    }

                    adapter.notifyDataSetChanged();  // עדכון התצוגה
                })
                .addOnFailureListener(e -> {
                    Log.e("PendingActivity", "שגיאה: " + e.getMessage(), e);
                    Toast.makeText(this, "שגיאה בטעינת הבקשות", Toast.LENGTH_SHORT).show();
                });
    }



}
