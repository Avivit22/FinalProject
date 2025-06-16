package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class GuideMainPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_main_page);
        ImageView bellIcon = findViewById(R.id.bellIcon);

        // activity_notifications.xml לחיצה על הפעמון תעביר לעמוד
        bellIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);
        });

        //  לחיצה על כפתור "רישום נוכחות לחוג של היום" תעביר לעמוד insert_status.xml
        Button btnRegisterAttendance = findViewById(R.id.btnRegisterAttendance);
        btnRegisterAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideMainPageActivity.this, InsertStatusActivity.class);
                startActivity(intent);
            }
        });

        //  לחיצה על כפתור "רישום לשיעור השלמה" תעביר לעמוד completion_class.xml
        Button btnLessonRegistration = findViewById(R.id.btnLessonRegistration);
        btnLessonRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideMainPageActivity.this, CompletionClassActivity.class);
                startActivity(intent);
            }
        });

        //  לחיצה על כפתור "לוח זמנים חודשי" תעביר לעמוד activity_show_schedule.xml
        Button btnShowSchedule = findViewById(R.id.btnShowSchedule);
        btnShowSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideMainPageActivity.this, ShowScheduleActivity.class);
                startActivity(intent);
            }
        });

        //  לחיצה על כפתור "דו"ח נוכחות" תעביר לעמוד report.xml
        Button btnAttendanceReport = findViewById(R.id.btnAttendanceReport);
        btnAttendanceReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GuideMainPageActivity.this, ReportActivity.class);
                startActivity(intent);
            }
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        TextView notificationBadge = findViewById(R.id.notificationBadge);

        if (currentUser != null) {
            String uid = currentUser.getUid();

            db.collection("completions")
                    .whereEqualTo("submittedBy", uid)
                    .whereEqualTo("seenByGuide", false)
                    .whereIn("status", Arrays.asList("approved", "rejected"))
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int unreadCount = querySnapshot.size();
                        if (unreadCount > 0) {
                            notificationBadge.setText(String.valueOf(unreadCount));
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }
                    });
        }
    }
}
