package com.example.myapplicationt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ManagerMainPageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manager_main_page);

        //לחיצה על כפתור הוספת חניך תעביר לעמוד הוספת חניך
        // מציאת הכפתור לפי ה-ID
        Button btnAddStudent = findViewById(R.id.btnAddStudent);
        // מאזין ללחיצה על כפתור "הוספת חניך"
        btnAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // מעבר לעמוד add_student
                Intent intent = new Intent(ManagerMainPageActivity.this, AddStudentActivity.class);
                startActivity(intent);
            }
        });

        //לחיצה על כפתור הוספת מדריך תעביר לעמוד הוספת מדריך
        // מציאת הכפתור לפי ה-ID
        Button btnAddInstructor = findViewById(R.id.btnAddInstructor);

        // מאזין ללחיצה על כפתור "הוספת מדריך"
        btnAddInstructor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // מעבר לעמוד add_guide
                Intent intent = new Intent(ManagerMainPageActivity.this, AddGuideActivity.class);
                startActivity(intent);
            }
        });


        // לחיצה על כפתור רישום לשיעור השלמה תעביר לעמוד completion_class
        Button btnLessonRegistration = findViewById(R.id.btnLessonRegistration);
        btnLessonRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, CompletionClassActivity.class);
                startActivity(intent);
            }
        });

        //לחיצה על כפתור פרטי חניך תעביר לעמוד ShowUpdateStudent
        Button btnStudentDetails = findViewById(R.id.btnStudentDetails);
        btnStudentDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, ShowUpdateStudentActivity.class);
                startActivity(intent);
            }
        });

        //לחיצה על כפתור פרטי מדריך תעביר לעמוד ShowUpdateGuide
        Button btnInstructorDetails = findViewById(R.id.btnInstructorDetails);
        btnInstructorDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, ShowUpdateGuideActivity.class);
                startActivity(intent);
            }
        });


        // לחיצה על כפתור "רישום נוכחות לחוג של היום" תעבירה לעמוד insert_status.xml
        Button btnRegisterAttendance = findViewById(R.id.btnRegisterAttendance);
        btnRegisterAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // מעבר לעמוד InsertStatus (שמחובר ל-insert_status.xml)
                Intent intent = new Intent(ManagerMainPageActivity.this, InsertStatusActivity.class);
                startActivity(intent);
            }
        });

        //ניווט לעמוד צפייה בלוח זמנים
        Button btnShowSchedule = findViewById(R.id.btnShowSchedule);
        btnShowSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, ShowScheduleActivity.class);
                startActivity(intent);
            }
        });

        // ניווט לעמוד הפקת דוחות נוכחות report.xml
        Button btnAttendanceReport = findViewById(R.id.btnAttendanceReport);
        btnAttendanceReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, ReportActivity.class);
                startActivity(intent);
            }
        });

        // לחיצה על כפתור בקשות להשלמה תעביר לעמוד accept_requests
        Button btnRequests = findViewById(R.id.btnRequests);
        btnRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManagerMainPageActivity.this, AcceptRequestsActivity.class);
                startActivity(intent);
            }
        });

        //  לחיצה על כפתור "התנתק מהמערכת" תעביר לעמוד activity_main.xml
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ManagerMainPageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });




    }
}
