package com.example.myapplicationt1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class AcceptRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_requests);

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
}
