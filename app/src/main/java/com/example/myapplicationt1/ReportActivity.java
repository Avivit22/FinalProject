package com.example.myapplicationt1;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class ReportActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // משתנים לניהול סינון ומיון
    private String selectedFilter = "all"; // "all", "name", "date", "toran", "month"
    private String selectedSort = "name";  // "name", "date"

    private List<AttendanceRecord> filteredData = new ArrayList<>();
    private String selectedValue = "";
    private boolean isNameAsc = true;
    private boolean isDateAsc = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // לוגו לחזרה לדף הראשי
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType());

        // כפתורי סינון ומיון
        Button filterByNameButton = findViewById(R.id.filterByNameButton);
        Button filterByDateButton = findViewById(R.id.filterByDateButton);
        Button filterByTorButton = findViewById(R.id.filterByCleanlinessButton);
        Button filterByMonthButton = findViewById(R.id.btnMonthFilter);
        Button sortByNameButton = findViewById(R.id.sortByNameButton);
        Button sortByDateButton = findViewById(R.id.sortByDateButton);

        // לחיצה על "סנן לפי שם" עם הצעות של שמות קיימים
        filterByNameButton.setOnClickListener(v -> {
            // נבנה רשימת כל השמות הקיימים
            db.collection("attendance")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<String> allNames = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String name = doc.getString("studentName");
                            if (name != null && !allNames.contains(name)) {
                                allNames.add(name);
                            }
                        }
                        showNameFilterDialog(allNames);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת שמות", Toast.LENGTH_SHORT).show());
        });


        // לחיצה על "סנן לפי תאריך"
        filterByDateButton.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        // לחיצה על "סנן לפי תורן"
        filterByTorButton.setOnClickListener(v -> {
            selectedFilter = "toran";
            fetchAttendanceData();
        });

        // לחיצה על "סנן לפי חודש"
        filterByMonthButton.setOnClickListener(v -> {
            showMonthPickerDialog();
        });

        // מיון לפי שם
        sortByNameButton.setOnClickListener(v -> {
            selectedSort = "name";

            // נהפוך את כיוון המיון בכל לחיצה
            isNameAsc = !isNameAsc;
            fetchAttendanceData();
        });

        // מיון לפי תאריך
        sortByDateButton.setOnClickListener(v -> {
            selectedSort = "date";

            // נהפוך את כיוון המיון בכל לחיצה
            isDateAsc = !isDateAsc;
            fetchAttendanceData();
        });


        // טעינת נתונים ראשונית
        fetchAttendanceData();

        // כפתור ייצוא לאקסל
        Button exportToExcelButton = findViewById(R.id.exportToExcelButton);
        exportToExcelButton.setOnClickListener(v -> {
            if (!filteredData.isEmpty()) {
                exportToExcel(filteredData);
            } else {
                Toast.makeText(this, "אין נתונים לייצוא", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור אפס סינון
        Button clearFilterButton = findViewById(R.id.clearFilterButton);
        clearFilterButton.setOnClickListener(v -> {
            selectedFilter = "all";
            selectedValue = "";
            fetchAttendanceData();
            Toast.makeText(this, "סינון אופס!", Toast.LENGTH_SHORT).show();
        });

    }

    // טעינת הנתונים מה-DB עם סינון ומיון
    private void fetchAttendanceData() {
        db.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredData.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("studentName");
                        String date = doc.getString("date");
                        String status = doc.getString("status");
                        boolean toran = doc.getBoolean("toran") != null ? doc.getBoolean("toran") : false;
                        String activeNumber = doc.getString("activeNumber") != null ? doc.getString("activeNumber") : "";

                        AttendanceRecord record = new AttendanceRecord(name, activeNumber, date, status, toran);

                        // סינון לפי הכפתור שנבחר
                        if (selectedFilter.equals("name") && !name.contains(selectedValue)) continue;
                        if (selectedFilter.equals("date") && !date.equals(selectedValue)) continue;
                        if (selectedFilter.equals("month")) {
                            // date בפיירבייס כתוב בפורמט 17-05-2025
                            // נוציא את החודש והשנה מהתאריך
                            String[] parts = record.date.split("-");
                            String recordMonthYear = parts[1] + "." + parts[2]; // לדוג' "05.2025"

                            if (!recordMonthYear.equals(selectedValue)) continue;
                        }

                        if (selectedFilter.equals("toran") && !toran) continue;

                        filteredData.add(record);
                    }

                    // מיון לפי שם או תאריך
                    if (selectedSort.equals("name")) {
                        Collator collator = Collator.getInstance(new Locale("he", "IL"));

                        if (isNameAsc) {
                            filteredData.sort((a, b) -> collator.compare(a.studentName, b.studentName));
                        } else {
                            filteredData.sort((a, b) -> collator.compare(b.studentName, a.studentName));
                        }
                    }
                    else if (selectedSort.equals("date")) {
                        if (isDateAsc) {
                            filteredData.sort(Comparator.comparing(a -> parseDate(a.date)));
                        } else {
                            filteredData.sort((a, b) -> parseDate(b.date).compareTo(parseDate(a.date)));
                        }
                    }


                    // עדכון הטבלה במסך
                    updateTable();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // עדכון הטבלה במסך לפי הבחירות
    private void updateTable() {
        TableLayout tableLayout = findViewById(R.id.attendanceTable);
        tableLayout.removeAllViews();

        for (int i = 0; i < filteredData.size(); i++) {
            AttendanceRecord record = filteredData.get(i);
            TableRow row = new TableRow(this);
            //row.setMinimumHeight(100);
            row.setPadding(8, 8, 8, 8);

            // צבע מתחלף
            int bgColor = (i % 2 == 0) ? Color.parseColor("#F8F8F8") : Color.parseColor("#E0E0E0");
            row.setBackgroundColor(bgColor);

            // עמודות
            // תא תורן
            TextView toranView = new TextView(this);
            if (record.toran) {
                toranView.setText("✔");
                toranView.setTextColor(Color.parseColor("#008000")); // ירוק
            } else {
                toranView.setText("");
            }
            toranView.setGravity(Gravity.CENTER);
            toranView.setPadding(4, 4, 4, 4); // padding פנימי
            toranView.setMinHeight(dpToPx(48));
            row.addView(toranView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));

            // תא סטטוס
            TextView statusView = new TextView(this);
            String statusText = record.status != null ? record.status : "";
            statusView.setText(statusText);
            statusView.setGravity(Gravity.CENTER);
            statusView.setPadding(4, 4, 4, 4); // padding פנימי
            statusView.setMinHeight(dpToPx(48));
            if (statusText.equals("נוכח")) {
                statusView.setTextColor(Color.parseColor("#008000"));
            } else if (statusText.equals("חיסור")) {
                statusView.setTextColor(Color.RED);
            } else if (statusText.equals("השלמה")) {
                statusView.setTextColor(Color.parseColor("#FF9800")); // כתום
            }
            row.addView(statusView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // תא תאריך
            TextView dateView = new TextView(this);
            String dateText = record.date != null ? shortenDate(record.date) : "";
            dateView.setText(dateText);
            dateView.setGravity(Gravity.CENTER);
            dateView.setPadding(4, 4, 4, 4); // padding פנימי
            dateView.setMinHeight(dpToPx(48));
            row.addView(dateView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // תא מספר פעיל
            TextView activeNumberView = new TextView(this);
            String activeNumberText = record.activeNumber != null ? record.activeNumber : "";
            activeNumberView.setText(activeNumberText);
            activeNumberView.setGravity(Gravity.CENTER);
            activeNumberView.setPadding(4, 4, 4, 4); // padding פנימי
            activeNumberView.setMinHeight(dpToPx(48));
            row.addView(activeNumberView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // תא שם
            TextView nameView = new TextView(this);
            String nameText = record.studentName != null ? record.studentName : "";
            nameView.setText(nameText);
            nameView.setGravity(Gravity.CENTER);
            nameView.setPadding(4, 4, 4, 4); // padding פנימי
            nameView.setMinHeight(dpToPx(48));
            row.addView(nameView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // הוסף לשורה
            tableLayout.addView(row);
        }
    }

    // ניווט לדף מתאים לפי סוג משתמש
    private void routeUserBasedOnType() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            Intent intent;
                            if (userType != null && userType.equalsIgnoreCase("manager")) {
                                intent = new Intent(ReportActivity.this, ManagerMainPageActivity.class);
                            } else {
                                intent = new Intent(ReportActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ReportActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(ReportActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ReportActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

    // מחלקת עזר לאחסון שורה
    public static class AttendanceRecord {
        String studentName;
        String activeNumber;
        String date;
        String status;
        boolean toran;

        public AttendanceRecord(String studentName, String activeNumber, String date, String status, boolean toran) {
            this.studentName = studentName;
            this.activeNumber = activeNumber;
            this.date = date;
            this.status = status;
            this.toran = toran;
        }
    }

    // פונקציית ייצוא לאקסל
    private void exportToExcel(List<AttendanceRecord> records) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        // כותרות
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Name", "Active Number", "Date", "Status", "Toran"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        //  תוכן - כתיבת הנתונים
        int rowNum = 1;
        for (AttendanceRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(record.studentName != null ? record.studentName : "");
            row.createCell(1).setCellValue(record.activeNumber != null ? record.activeNumber : "");
            row.createCell(2).setCellValue(record.date != null ? record.date : "");
            row.createCell(3).setCellValue(record.status != null ? record.status : "");
            row.createCell(4).setCellValue(record.toran ? "✔" : "");
        }

        try {
            // שם ייחודי לפי תאריך ושעה
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String fileName = "attendance_report_" + timeStamp + ".xlsx";

            // תיקיית files הפנימית של האפליקציה
            File dir = getFilesDir();
            File file = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            Toast.makeText(this, "קובץ נשמר: " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה ביצוא", Toast.LENGTH_SHORT).show();
        }
    }

    // דיאלוג בחירת תאריך
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // עדכון התאריך הנבחר
                    selectedFilter = "date";
                    selectedValue = String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear);
                    fetchAttendanceData();
                }, year, month, day);

        datePickerDialog.show();
    }

    // דיאלוג של פופ אפ לבחירת חודש ב"סינון לפי"
    private void showMonthPickerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout dialogView = (LinearLayout) inflater.inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        // הגדרות חודש
        String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(Calendar.getInstance().get(Calendar.MONTH) + 1);

        // הגדרות שנה
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);

        //הגדרת כותרת
        TextView title = new TextView(this);
        title.setText("בחר חודש ושנה");
        title.setPadding(0, 40, 0, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setCustomTitle(title)
                .setView(dialogView)
                .setPositiveButton("אישור", (dialog, which) -> {
                    String selectedMonth = String.format("%02d", monthPicker.getValue());
                    String selectedYear = String.valueOf(yearPicker.getValue());
                    selectedFilter = "month";
                    selectedValue = selectedMonth + "." + selectedYear;
                    fetchAttendanceData();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }





    // פונקציה לקיצור התאריך שיכנס לטבלה
    private String shortenDate(String date) {
        // אם התאריך בפורמט dd-MM-yyyy
        if (date.length() == 10 && date.charAt(2) == '-' && date.charAt(5) == '-') {
            String day = date.substring(0, 2);
            String month = date.substring(3, 5);
            String year = date.substring(8, 10); // רק שתי ספרות אחרונות
            return day + "." + month + "." + year;
        }
        return date;
    }

    // פונקציה לעיבוד תאריך ממחרוזת
    private java.util.Date parseDate(String dateStr) {
        try {
            return new java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.Date(0); // תחזיר תאריך ישן מאוד במקרה של כשל
        }
    }


    // דיאלוג סינון עם הצעות לשמות קיימים
    private void showNameFilterDialog(List<String> allNames) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        // כותרת ממורכזת
        TextView title = new TextView(this);
        title.setText("הזן שם חניך");
        title.setPadding(0, 40, 0, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        // שדה AutoComplete
        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allNames);
        input.setAdapter(adapter);
        input.setThreshold(1); // אחרי תו אחד מתחיל להציע

        builder.setCustomTitle(title);
        builder.setView(input);

        builder.setPositiveButton("סנן", (dialog, which) -> {
            selectedFilter = "name";
            selectedValue = input.getText().toString().trim();
            fetchAttendanceData();
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


}
