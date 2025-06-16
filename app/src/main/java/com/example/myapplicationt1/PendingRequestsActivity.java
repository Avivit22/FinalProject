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

public class PendingRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<DocumentSnapshot> pendingList = new ArrayList<>();
    private RecyclerView recyclerView;
    private PendingRequestsAdapter adapter;
    private TextView tvEmptyMessage;
    private FirebaseAuth.AuthStateListener authListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.rvPending);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingRequestsAdapter(pendingList);
        recyclerView.setAdapter(adapter);

        ImageView logo = findViewById(R.id.logoImage);
        logo.setOnClickListener(v -> finish());

        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // האם המשתמש מחובר
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

        mAuth.addAuthStateListener(authListener);

    }


    private void loadPendingRequests(String uid) {
        db.collection("completions")
                .whereEqualTo("status", "pending")
                .whereEqualTo("submittedBy", uid)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    pendingList.clear();
                    List<DocumentSnapshot> docs = query.getDocuments();
                    Log.d("PendingActivity", "Found: " + docs.size() + " docs");

                    if (docs.isEmpty()) {
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyMessage.setVisibility(View.GONE);
                        pendingList.addAll(docs);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("PendingActivity", "שגיאה: " + e.getMessage(), e);
                    Toast.makeText(this, "שגיאה בטעינת הבקשות", Toast.LENGTH_SHORT).show();
                });
    }



}
