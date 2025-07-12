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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

/**
 * עמוד הבית של המדריך.
 * מאפשר ניווט מהיר לפונקציות עיקריות (נוכחות, שיעור השלמה, דוחות, לוח זמנים),
 */
public class GuideMainPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_main_page);
        // מציאת הפעמון
        ImageView bellIcon = findViewById(R.id.bellIcon);
        // מציאת ה-badge שמראה את מספר ההתראות
        TextView notificationBadge = findViewById(R.id.notificationBadge);

        // activity_notifications.xml לחיצה על הפעמון תעביר לעמוד
        bellIcon.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                String uid = currentUser.getUid();

                db.collection("completions")
                        .whereEqualTo("submittedBy", uid)
                        .whereEqualTo("seenByGuide", false)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                doc.getReference().update("seenByGuide", true);
                            }
                        });
            }

            // פותח את העמוד
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);
        });

        // קריאה ראשונית לבדוק האם יש התראות שלא נקראו
        checkNotifications();


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

        //  לחיצה על כפתור "התנתק מהמערכת" תעביר לעמוד activity_main.xml
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(GuideMainPageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

    }

    /**
     * פונקציה זו מתבצעת בכל פעם שחוזרים למסך (לדוג' אחרי חזרה מהתראות)
     * ומעדכנת מחדש את ה-badge של ההתראות
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkNotifications();
    }

    /**
     * פונקציה שבודקת התראות שלא נקראו עבור המדריך ומעדכנת את ה-badge
     */
    private void checkNotifications() {
        // יצירת חיבור ל-Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // קבלת המשתמש הנוכחי
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // מציאת ה-badge במסך
        TextView notificationBadge = findViewById(R.id.notificationBadge);

        if (currentUser != null) {
            String uid = currentUser.getUid();

            // חיפוש במסמך completions את כל מה שלא נקרא ע"י המדריך(seenByGuide = false)
            db.collection("completions")
                    .whereEqualTo("submittedBy", uid)
                    .whereEqualTo("seenByGuide", false)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int unreadCount = querySnapshot.size();
                        if (unreadCount > 0) {
                            // אם יש התראות שלא נקראו - מציגים מספר
                            notificationBadge.setText(String.valueOf(unreadCount));
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            // אם אין - מסתירים
                            notificationBadge.setVisibility(View.GONE);
                        }
                    });
        }
    }

}
