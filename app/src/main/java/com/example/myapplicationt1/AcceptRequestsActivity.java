package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AcceptRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CompletionRequestAdapter adapter;
    private final List<CompletionRequest> requestList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_requests);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView); // תוודא שקיים ב-XML
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CompletionRequestAdapter(this, requestList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.logoImage).setOnClickListener(v -> {
            startActivity(new Intent(this, ManagerMainPageActivity.class));
        });

        loadRequestsFromFirestore();

        // מאזין ללחיצה על הלוגו, יעביר לעמוד הראשי של המנהל
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AcceptRequestsActivity.this, ManagerMainPageActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadRequestsFromFirestore() {
        db.collection("completions")
                .whereEqualTo("status", "pending")
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
