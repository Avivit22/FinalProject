package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class GuideMainPageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_main_page);

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
    }
}
