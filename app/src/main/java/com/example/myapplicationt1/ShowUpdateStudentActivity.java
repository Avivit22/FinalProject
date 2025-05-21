package com.example.myapplicationt1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowUpdateStudentActivity extends AppCompatActivity {

    private static final String TAG = "ShowUpdateStudent";  //תג עבור לוגים- עוזר לזהות מאיזו מחלקה הודעות הלוג הגיעו

    //גישה לשירותי FIREBASE
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView searchStudentAutoComplete; //רכיב UI
    private ScrollView detailsScrollView;  //רכיב UI

    //שדות קלט לעריכת פרטי חניך
    private EditText fullNameEditText, organizationIdEditText, birthDateEditText,
            gradeEditText, phoneEditText, joinDateEditText, addressEditText,
            parent1NameEditText, parent2NameEditText, parentPhoneEditText;
    private Spinner genderSpinner, dayOfWeekSpinner; //ספינרים לבחירת מגדר ויום חוג
    private ImageView profileImage; //תמונת חניך
    private Button uploadImageButton, saveChangesButton, deleteStudentButton, convertToGuideButton;

    //משתניפ לניהול תמונת חניך
    private Uri newImageUri = null;
    private Bitmap currentProfileBitmap = null;

    //משתנים לזיהוי וניהול חניך
    private String currentStudentDocumentId = null; //מזהה מסמך של חניך הFIRESTORE
    private Student currentStudent = null; //פרטי חניך ספציפי

    //טיפול בבחירת תמונה מהגלריה
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {  //בדיקה את המשתמש בחר תמונה והבחירה בוצעה בהצלחה
                            Uri selectedImageUri = result.getData().getData(); //קבלת URI של התמונה שנבחרה
                            try {
                                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                                profileImage.setImageBitmap(selectedBitmap);
                                newImageUri = selectedImageUri; //שמירת URI של התמונה החדשה
                                currentProfileBitmap = selectedBitmap;
                                Log.d(TAG, "New image selected and bitmap created."); //רישום ללוג
                            } catch (IOException e) {  //טיפול בשגיאה אם טעינת התמונה נכשלה
                                Log.e(TAG, "Error loading new image bitmap", e);
                                Toast.makeText(this, "שגיאה בטעינת תמונה חדשה", Toast.LENGTH_SHORT).show();
                                newImageUri = null; //איפוס משתנה אם טעינת התמונה נכשלה
                                currentProfileBitmap = null;
                                if (currentStudent != null && currentStudent.getProfileImageBase64() != null && !currentStudent.getProfileImageBase64().isEmpty()){
                                    loadBase64Image(currentStudent.getProfileImageBase64(), profileImage);
                                } else { //אם לא הייתה תמונה קודמת, מנקים תצוגה ומאפסים את BITMAP
                                    profileImage.setImageBitmap(null);
                                    currentProfileBitmap = null;
                                }
                            }
                        } else { //אם המשתמש ביטל את בחירת התמונה או שהבחירה נכשלה
                            Log.d(TAG, "New image selection cancelled or failed.");
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_update_student); //טעינת קובץ XML

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI(); //אתחול רכיבי UI
        setupListeners();  //הגדרת מאזינים לאירועים
        loadStudentNamesForAutoComplete(); //טעינת שמות חניכים לשדה החיפוש
    }

    //פונקציה לאתחול כל רכיבי הUI שמוגדרים בקובץ הXML
    private void initializeUI() {
        searchStudentAutoComplete = findViewById(R.id.search_student);
        detailsScrollView = findViewById(R.id.detailsScrollView);

        fullNameEditText = findViewById(R.id.fullNameEditText);
        organizationIdEditText = findViewById(R.id.organizationIdEditText);
        birthDateEditText = findViewById(R.id.birthDateEditText);
        gradeEditText = findViewById(R.id.gradeEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        joinDateEditText = findViewById(R.id.joinDateEditText);
        addressEditText = findViewById(R.id.addressEditText);
        parent1NameEditText = findViewById(R.id.parent1NameEditText);
        parent2NameEditText = findViewById(R.id.parent2NameEditText);
        parentPhoneEditText = findViewById(R.id.parentPhoneEditText);

        genderSpinner = findViewById(R.id.genderSpinner);
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveChangesButton = findViewById(R.id.saveChangesButton);
        deleteStudentButton = findViewById(R.id.deleteStudentButton);
        convertToGuideButton = findViewById(R.id.convertToGuideButton);

        detailsScrollView.setVisibility(View.GONE);  //בפתיחת המסך החלק בו מוצגים פרטי החניך מוסתר
        clearStudentDetailsForm();  //ניקוי השדות בטופס
    }

    //פונקציה לניקוי כל השדות בטופס ואיפוס משתני התמונה
    private void clearStudentDetailsForm() {
        fullNameEditText.setText("");
        organizationIdEditText.setText("");
        birthDateEditText.setText("");
        birthDateEditText.setHint("בחר תאריך");
        gradeEditText.setText("");
        phoneEditText.setText("");
        joinDateEditText.setText("");
        joinDateEditText.setHint("בחר תאריך");
        addressEditText.setText("");
        parent1NameEditText.setText("");
        parent2NameEditText.setText("");
        parentPhoneEditText.setText("");
        profileImage.setImageBitmap(null); //ניקוי תמונה
        currentProfileBitmap = null;
        newImageUri = null;
        if (genderSpinner.getAdapter() != null && genderSpinner.getAdapter().getCount() > 0) genderSpinner.setSelection(0);
        if (dayOfWeekSpinner.getAdapter() != null && dayOfWeekSpinner.getAdapter().getCount() > 0) dayOfWeekSpinner.setSelection(0);
    }


    //פונקציה להגדרת מאזינים לרכיבי UI
    private void setupListeners() {
        ImageView logoImage = findViewById(R.id.logoImage);
        logoImage.setOnClickListener(v -> routeUserBasedOnType()); //לחיצה על הלוגו תנתב על המשתמש לפי הסוג שלו

        //המשתמש בוחר שם מהרשימה הנפתחת בחיפוש
        searchStudentAutoComplete.setOnItemClickListener((parent, view, position, id) -> searchStudentByName());

        //לחיצה על שדות התאריך פותחת דיאלוג לבחירת תאריך
        birthDateEditText.setOnClickListener(v -> showDatePickerDialog(birthDateEditText));
        joinDateEditText.setOnClickListener(v -> showDatePickerDialog(joinDateEditText));

        uploadImageButton.setOnClickListener(v -> openGalleryForNewImage()); //לחיצה על "שנה תמונה" פותחת גלריה
        saveChangesButton.setOnClickListener(v -> saveStudentChanges()); //לחיצה על "שמור שינויים" שומרת נתונים עדכניים

        deleteStudentButton.setOnClickListener(v -> {  //לחיצה על כפתור מחיקה מהמערכת
            if (currentStudentDocumentId != null) { //בדיקה אם נבחר חניך
                showDeletePopup();  //הצגת פופאפ לאישור מחיקה מהמערכת והזנת תאריך עזיבה
            } else {
                Toast.makeText(this, "נא לבחור חניך תחילה", Toast.LENGTH_SHORT).show();
            }
        });
        /*convertToGuideButton.setOnClickListener(v -> {
            Toast.makeText(this, "פונקציית המרה למדריך עוד לא מומשה", Toast.LENGTH_SHORT).show();
        });*/
    }

    //פונקציה לטעינת שמות כל החניכים מFIRESTORE והצגתם בשדה החיפוש עם השלמה אוטומטית
    private void loadStudentNamesForAutoComplete() {
        db.collection("students")  //גישה לcollection students בתוך FIRESTORE
                .get()  //קבלת כל המסמכים שבתוך collections students
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> studentNames = new ArrayList<>(); //רשימת עבור שמות החניכים
                        for (QueryDocumentSnapshot document : task.getResult()) { //מעבר על כל מסמך בFIRESTORE
                            String name = document.getString("fullName");  //קבלת שם החניך מהמסמך
                            if (name != null && !name.isEmpty()) {
                                studentNames.add(name);
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, studentNames);  //יצירת adapter להצגת שמות
                        searchStudentAutoComplete.setAdapter(adapter);
                    } else { //טיפול בשגיאה אם טעינת השמות נכשלה
                        Log.e(TAG, "Error getting student names: ", task.getException());
                    }
                });
    }

    //פונקציה לחיפוש חניך לפי שם שהוזן בשדה החיפוש
    private void searchStudentByName() {
        String studentNameToSearch = searchStudentAutoComplete.getText().toString().trim();
        if (TextUtils.isEmpty(studentNameToSearch)) {  //בדיקה אם הוזן שם חניך לחיפוש
            Toast.makeText(this, "נא להזין שם חניך לחיפוש", Toast.LENGTH_SHORT).show();
            return;
        }
        //ניקוי הטופס והסתרת החלק בו מוצגים פרטי חניך לפני שיש חיפוש חדש
        clearStudentDetailsForm();
        detailsScrollView.setVisibility(View.GONE);
        currentStudentDocumentId = null;
        currentStudent = null;

        //שאילתא לFIRESTORE לחיפוש שם ספציפי
        db.collection("students")
                .whereEqualTo("fullName", studentNameToSearch)  //סינון לפי שדה שם מלא
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            currentStudentDocumentId = document.getId();
                            currentStudent = document.toObject(Student.class);
                            if (currentStudent != null) {
                                populateStudentDetails(currentStudent); //מילוי שדות בטופס עם פרטי החניך
                                detailsScrollView.setVisibility(View.VISIBLE); //הצגת החלק במסך שמראה פרטי חניך
                                Toast.makeText(ShowUpdateStudentActivity.this, "חניך נמצא", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בהמרת נתוני חניך", Toast.LENGTH_SHORT).show();
                            }
                        } else { //אם אין חניך בשם הרצוי
                            Toast.makeText(ShowUpdateStudentActivity.this, "לא נמצא חניך בשם זה", Toast.LENGTH_SHORT).show();
                        }
                    } else {  //טיפול בשגיאה אם החיפוש נכשל
                        Log.e(TAG, "Error searching for student: ", task.getException());
                        Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בחיפוש חניך", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //פונקציה למילוי שדות הטופס עם פרטי חניך שהתקבלו
    private void populateStudentDetails(Student student) {
        if (student == null) {
            Log.e(TAG, "populateStudentDetails called with null student object.");
            return;
        }

        Log.d(TAG, "Populating details for student: " + student.getFullName());
        Log.d(TAG, "Student object from Firestore (after potential mapping): " + student.toString());

        //מילוי כל שדות הטקסט
        fullNameEditText.setText(student.getFullName());
        organizationIdEditText.setText(student.getActiveNumber());
        birthDateEditText.setText(student.getBirthDate());
        gradeEditText.setText(student.getGrade());
        phoneEditText.setText(student.getPhone());
        joinDateEditText.setText(student.getJoinDate());
        addressEditText.setText(student.getAddress());
        parent1NameEditText.setText(student.getParent1Name());
        parent2NameEditText.setText(student.getParent2Name());
        parentPhoneEditText.setText(student.getParentPhone());

        //הגדרת הבחירות בספינרים
        setSpinnerSelection(genderSpinner, student.getGender(), getResources().getStringArray(R.array.gender_options));
        setSpinnerSelection(dayOfWeekSpinner, student.getDayOfWeek(), getResources().getStringArray(R.array.days_of_week));

        //טעינת תמונה אם יש
        if (student.getProfileImageBase64() != null && !student.getProfileImageBase64().isEmpty()) {
            loadBase64Image(student.getProfileImageBase64(), profileImage);
        } else {  //אם אין תמונה
            profileImage.setImageBitmap(null);
            currentProfileBitmap = null;
            Log.d(TAG, "No profileImageBase64 found for student or it's empty.");
        }
    }

    //פונקציה לטעינת תמונה ממחרוזת Base64 אל ImageView ועדכון currentProfileBitmap
    private void loadBase64Image(String base64String, ImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            Log.w(TAG, "loadBase64Image called with null or empty string.");
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
            return;
        }
        Log.d(TAG, "Attempting to decode Base64 string of length: " + base64String.length());
        try {  //המרת מחרוזת Base64 למערך של בתים
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            if (decodedBytes == null || decodedBytes.length == 0) {
                Log.e(TAG, "Base64.decode returned null or empty byte array.");
                imageView.setImageBitmap(null);
                currentProfileBitmap = null;
                return;
            }
            Log.d(TAG, "Decoded byte array length: " + decodedBytes.length);

            currentProfileBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length); //המרת מערך הבתים לBITMAP
            if (currentProfileBitmap != null) { //בדיקה שההמרה לBITMAP הצליחה
                imageView.setImageBitmap(currentProfileBitmap);
                Log.i(TAG, "Base64 image loaded successfully into ImageView.");
            } else {  //אם ההמרה לBITMAP נכשלה
                Log.e(TAG, "BitmapFactory.decodeByteArray returned null. Invalid image data in Base64?");
                imageView.setImageBitmap(null);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException while decoding Base64 image. String might be malformed.", e);
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
        } catch (OutOfMemoryError e) {  //טיפול בשגיאה אם התמונה גדולה מדי ואין מקום בזיכרון
            Log.e(TAG, "OutOfMemoryError while decoding Base64 image. Image might be too large.", e);
            Toast.makeText(this, "התמונה גדולה מדי להצגה (נגמר הזיכרון)", Toast.LENGTH_LONG).show();
            imageView.setImageBitmap(null);
            currentProfileBitmap = null;
        }
    }

    //פונקציית עזר להגדרת הבחירה בספינר לפי ערך נתון
    private void setSpinnerSelection(Spinner spinner, String value, String[] array) {
        if (value != null && array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    spinner.setSelection(i); //הגדרת הערך הנבחר בספינר
                    break;
                }
            }
        }
    }

    //פונקציה לפתיחת גלריה לבחירת תמונה
    private void openGalleryForNewImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    //פונקציה לשמירת שינויים בפרטי חניך לFIRESTORE
    private void saveStudentChanges() {
        if (currentStudentDocumentId == null || currentStudent == null) {
            Toast.makeText(this, "נא לבחור חניך תחילה", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>(); //אחסון הנתונים המעודכנים
        //הכנסת כל ערכי השדות מהטופס למפה
        updatedData.put("fullName", fullNameEditText.getText().toString().trim());
        updatedData.put("activeNumber", organizationIdEditText.getText().toString().trim());
        updatedData.put("birthDate", birthDateEditText.getText().toString());
        updatedData.put("grade", gradeEditText.getText().toString().trim());
        updatedData.put("phone", phoneEditText.getText().toString().trim());
        updatedData.put("joinDate", joinDateEditText.getText().toString());
        updatedData.put("address", addressEditText.getText().toString().trim());
        updatedData.put("parent1Name", parent1NameEditText.getText().toString().trim());
        updatedData.put("parent2Name", parent2NameEditText.getText().toString().trim());
        updatedData.put("parentPhone", parentPhoneEditText.getText().toString().trim());
        updatedData.put("gender", genderSpinner.getSelectedItem().toString());
        updatedData.put("dayOfWeek", dayOfWeekSpinner.getSelectedItem().toString());

        String newBase64Image = "";  //מחרוזת לשמירת Base64 של התמונה
        if (newImageUri != null && currentProfileBitmap != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                currentProfileBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] imageBytes = baos.toByteArray();
                //בדירת גודל התמונה כדי לא לחרוג במגבלת הגודל
                int maxSizeBytesBeforeEncoding = 750 * 1024;
                if (imageBytes.length < maxSizeBytesBeforeEncoding) {
                    newBase64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                    Log.d(TAG, "New image (from newImageUri) converted to Base64 and will be saved.");
                } else { //אם התמונה גדולה מדי
                    Log.w(TAG, "New selected image is too large after compression. Not saving new image.");
                    Toast.makeText(this, "התמונה החדשה גדולה מדי ולא נשמרה. נשמרת התמונה הקודמת (אם הייתה).", Toast.LENGTH_LONG).show();

                    if (currentStudent.getProfileImageBase64() != null) { //אם הייתה תמונה לפני כן אז נשמור את הקודמת
                        newBase64Image = currentStudent.getProfileImageBase64();
                    } else {
                        newBase64Image = ""; //אם לא הייתה תמונה ישנה נגדיר מחרוזת ריקה
                    }
                }
            } catch (Exception e) {  //טיפול בשגיאה אם ההמרה לBase64 נכשלה
                Log.e(TAG, "Error converting new Bitmap (from newImageUri) to Base64", e);
                Toast.makeText(this, "שגיאה בעיבוד התמונה החדשה. נשמרת התמונה הקודמת (אם הייתה).", Toast.LENGTH_SHORT).show();
                if (currentStudent.getProfileImageBase64() != null) { //אם הייתה תמונה ישנה אז נשמור אותה (במצב שיש שגיאה)
                    newBase64Image = currentStudent.getProfileImageBase64();
                } else {
                    newBase64Image = "";
                }
            }
        }

        //אם לא נבחרה תמונה חדשה או שבחירת התמונה נכשלה
        else if (newImageUri == null) {
            if (currentStudent.getProfileImageBase64() != null) { //שמירת תמונה קיימת אם יש כזו
                newBase64Image = currentStudent.getProfileImageBase64();
                Log.d(TAG, "No new image selected. Retaining existing student image (if any).");
            } else {  //אם לא נבחרה תמונה חדשה וגם לא הייתה תמונה ישנה
                newBase64Image = "";
                Log.d(TAG, "No new image selected and no existing student image. Saving empty string for image.");
            }
        }


        updatedData.put("profileImageBase64", newBase64Image);

        //שמירת שדות נוספים אם הם קיימים באובייקט החניך הספציפי
        if (currentStudent.getClassId() != null) {
            updatedData.put("classId", currentStudent.getClassId());
        } else {
            updatedData.put("classId", "");
        }
        if (currentStudent.getLeavingDate() != null) {
            updatedData.put("leavingDate", currentStudent.getLeavingDate());
        } else {
            updatedData.put("leavingDate", "");
        }

        Log.d(TAG, "Attempting to update student data for doc ID: " + currentStudentDocumentId);
        db.collection("students").document(currentStudentDocumentId)  //עדכון מסמך החניך בFIRESTORE עם הנתונים החדשים
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShowUpdateStudentActivity.this, "פרטי החניך עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    loadStudentNamesForAutoComplete();  //טעינה מחדש של שמות החניכים כדי לעדכן את רשימת החיפוש
                    detailsScrollView.setVisibility(View.GONE);  //הסתרת החלק בו מוצגים פרטי חניך
                    searchStudentAutoComplete.setText("");  //ניקוי שדה החיפוש
                    searchStudentAutoComplete.clearFocus();
                    clearStudentDetailsForm();
                    //איפוס משתני חניך נוכחי
                    currentStudent = null;
                    currentStudentDocumentId = null;
                })
                .addOnFailureListener(e -> {  //אם העדכון נכשל
                    Log.e(TAG, "Error updating student data", e);
                    Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בעדכון פרטי החניך: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // פונקציה לניתוב המשתמש למסך הראשי המתאים לפי סוג המשתמש (מנהל או מדריך)
    private void routeUserBasedOnType() {
        FirebaseUser user = mAuth.getCurrentUser(); // קבלת המשתמש הנוכחי שמחובר
        if (user != null) {
            String uid = user.getUid();

            // קריאה ל-Firestore לקבלת סוג המשתמש
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType"); //קבלת סוג משתמש
                            Intent intent;
                            if (userType != null && userType.equalsIgnoreCase("manager")) {  // בדיקה אם המשתמש הוא מנהל
                                intent = new Intent(ShowUpdateStudentActivity.this, ManagerMainPageActivity.class);
                            } else {  //אם המשתמש הוא מדריך
                                intent = new Intent(ShowUpdateStudentActivity.this, GuideMainPageActivity.class);
                            }
                            startActivity(intent);  //פתיחת מסך
                            finish(); // סגירת המסך הנוכחי
                        } else {
                            Toast.makeText(ShowUpdateStudentActivity.this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(ShowUpdateStudentActivity.this, "לא קיים משתמש מחובר", Toast.LENGTH_SHORT).show();
        }
    }

    // פונקציה להצגת פופאפ לאישור מחיקה מהאפליקציה והשארה בFIRABASE וגם הזנה ושמירת תאריך עזיבה
    private void showDeletePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.popup_delete, null);
        final EditText departureDateEditText = popupView.findViewById(R.id.departureDateEditText);
        Button calendarButton = popupView.findViewById(R.id.calendarButton);

        // לחיצה על כפתור לוח שנה פותחת דיאלוג לבחירת תאריך
        calendarButton.setOnClickListener(v -> showDatePickerDialog(departureDateEditText));
        Button deleteConfirmButton = popupView.findViewById(R.id.deleteConfirmButton);
        ImageView closeButton = popupView.findViewById(R.id.closePopupButton);
        AlertDialog dialog = builder.setView(popupView).create();

        deleteConfirmButton.setOnClickListener(v -> {
            String departureDate = departureDateEditText.getText().toString();
            if (!departureDate.isEmpty()) {  // בדיקה שהוזן תאריך עזיבה
                if (currentStudentDocumentId != null) { //בדיקה שיש שיוך לחניך
                    Map<String, Object> update = new HashMap<>();
                    update.put("leavingDate", departureDate);

                    // עדכון המסמך של החניך עם תאריך העזיבה
                    db.collection("students").document(currentStudentDocumentId).update(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ShowUpdateStudentActivity.this, "תאריך עזיבה נשמר", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();  // סגירת הפופאפ
                                detailsScrollView.setVisibility(View.GONE);
                                searchStudentAutoComplete.setText("");
                                clearStudentDetailsForm();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ShowUpdateStudentActivity.this, "שגיאה בעדכון תאריך עזיבה", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(ShowUpdateStudentActivity.this, "יש להזין תאריך עזיבה", Toast.LENGTH_SHORT).show();
            }
        });
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();  // הצגת הפופאפ
    }

    // פונקציה להצגת דיאלוג לבחירת תאריך
    private void showDatePickerDialog(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();  //יצירת מופע של לוח שנה
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // יצירת דיאלוג לבחירת תאריך
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);  // עיצוב התאריך הנבחר למחרוזת dd/MM/yyyy
                    targetEditText.setText(selectedDate);  // הצבת התאריך הנבחר בשדה הטקסט המתאים
                },
                year, month, day
        );
        datePickerDialog.show();  //הצגת הדיאלוג
    }
}
