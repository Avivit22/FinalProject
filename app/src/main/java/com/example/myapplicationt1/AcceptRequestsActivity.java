package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ניהול חניך המאפשר חיפוש, צפייה ועדכון פרטים, מחיקת חניך או המרתו למדריך.
 * כולל טיפול בתמונה, ניהול דיאלוגים וחיבור ל-Firebase.
 */
public class AcceptRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView; // רכיב תצוגת הרשימה
    private CompletionRequestAdapter adapter; // אדפטר לרשימת הבקשות
    private final List<CompletionRequest> requestList = new ArrayList<>(); // רשימת הבקשות
    private FirebaseFirestore db; // חיבור למסד הנתונים (Firestore


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_requests);

        // אתחול Firestore
        db = FirebaseFirestore.getInstance();

        // מציאת רכיב ה-RecyclerView מה-XML והגדרת תצוגה ליניארית (רשימה אנכית)
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // יצירת אדפטר והגדרתו לרשימה
        adapter = new CompletionRequestAdapter(this, requestList, false);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.logoImage).setOnClickListener(v -> {
            startActivity(new Intent(this, ManagerMainPageActivity.class));
        });

        // מאזין ללחיצה על הלוגו, יעביר לעמוד הראשי של המנהל
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AcceptRequestsActivity.this, ManagerMainPageActivity.class);
                startActivity(intent);
            }
        });

        // כפתור מעבר לעמוד היסטורית בקשות
        Button historyButton = findViewById(R.id.history_button);
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(AcceptRequestsActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // טעינת בקשות השלמה מ-Firestore
        loadRequestsFromFirestore();
    }

    /**
     * פונקציה לטעינת בקשות לשיעור השלמה מ-Firestore
     * מביאה רק בקשות עם סטטוס "pending" וממיינת לפי זמן שליחה
     */
    private void loadRequestsFromFirestore() {
        db.collection("completions")
                .whereEqualTo("status", "pending")
                .orderBy("submittedAt") // מיון לפי שעת השליחה
                .get()
                .addOnSuccessListener(query -> {
                    requestList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        CompletionRequest request = doc.toObject(CompletionRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                            requestList.add(request);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
