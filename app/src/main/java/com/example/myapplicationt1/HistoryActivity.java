package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplicationt1.CompletionRequest;
import com.example.myapplicationt1.CompletionRequestAdapter;
import com.example.myapplicationt1.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * מסך היסטוריית בקשות לשיעורים נוספים.
 * מציג רשימת בקשות שאושרו או נדחו, עם אפשרות לצפות בהן ולעדכן סטטוס.
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CompletionRequestAdapter adapter;
    private List<CompletionRequest> allRequests = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // יצירת אדאפטר במצב היסטוריה (isHistoryMode = true)
        adapter = new CompletionRequestAdapter(this, allRequests, true); // מצב היסטוריה
        recyclerView.setAdapter(adapter);

        // אתחול Firestore
        db = FirebaseFirestore.getInstance();

        // טעינת הבקשות הקודמות ממסד הנתונים
        loadHistoryRequests();

        // מאזין ללחיצה על הלוגו, יעביר לעמוד אישור בקשות לשיעור נוסף
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryActivity.this, AcceptRequestsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * טעינת כל הבקשות להיסטוריה מה-Firestore.
     * מביא רק בקשות עם סטטוס מאושר (approved) או נדחה (rejected), מסוג "שיעור נוסף".
     */
    private void loadHistoryRequests() {
        db.collection("completions")
                .whereIn("status", Arrays.asList("approved", "rejected"))
                .whereEqualTo("type", "שיעור נוסף")
                .orderBy("submittedAt") // מיון לפי שעת השליחה
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        CompletionRequest req = doc.toObject(CompletionRequest.class);
                        if (req != null) {
                            req.setId(doc.getId()); // שמירת מזהה המסמך
                            allRequests.add(req);
                        }
                    }
                    adapter.notifyDataSetChanged(); // עדכון הרשימה בתצוגה
                });
    }
}
