# راهنمای اتصال فایربیس به سامانه هوشمند نظمیار (NezamYar Firebase Integration Guide)

این راهنما مراحل اتصال پروژه نظمیار به سرویس‌های Firebase (مراحل ۲ تا ۶ نقشه راه) را تشریح می‌کند.

---

### ۱. ایجاد پروژه در کنسول فایربیس (Firebase Console)
1. به [Firebase Console](https://console.firebase.google.com) مراجعه فرمایید.
2. بر روی **Add Project** کلیک کرده و نام پروژه را `nezamyar-school` تعیین کنید.
3. یک اپلیکیشن اندروید با شناسه یکتای پکیج پروژه (`com.aistudio.nezamyar.sqvxpl`) اضافه نمایید.

---

### ۲. دریافت و افزودن فایل `google-services.json`
1. در تنظیمات پروژه فایربیس، فایل پیکربندی `google-services.json` را دانلود کنید.
2. فایل را در مسیر ریشه ماژول اپلیکیشن قرار دهید:
   ```text
   /app/google-services.json
   ```

---

### ۳. فعال‌سازی سرویس‌های فایربیس

#### الف) احراز هویت (Firebase Authentication)
- در منوی Authentication، ارائه‌دهنده‌های **Email/Password** و **Phone** را فعال نمایید.
- در Stage 3 با تغییر `AppContainer.authRepository` به `FirebaseAuthRepositoryImpl` احراز هویت متصل می‌گردد.

#### ب) پایگاه داده ابری (Cloud Firestore)
- پایگاه داده Firestore را در حالت Production فعال کنید.
- ساختار اسناد ابری با `schoolId`:
  ```text
  schools/{schoolId}
  users/{userId}
  students/{studentId}
  classes/{classId}
  behaviorRecords/{recordId}
  notifications/{notificationId}
  auditLogs/{logId}
  ```

#### ج) پیام‌رسانی ابری (Firebase Cloud Messaging - FCM)
- جهت ارسال اعلان‌های لحظه‌ای به اولیاء هنگام ثبت رفتار مثبت یا موارد انضباطی، توکن‌های دستگاه والدین در زیرمجموعه `users/{parentId}/fcmTokens` ثبت و Cloud Functions برای ارسال خودکار نوتیفیکیشن مستقر می‌شود.

---

### ۴. قوانین امنیتی فایراستور (Firestore Security Rules)

قوانین امنیتی نقش‌محور برای حفظ حریم خصوصی دانش‌آموزان:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }
    
    function isManager(schoolId) {
      return isAuthenticated() && 
             getUserData().schoolId == schoolId && 
             getUserData().role == 'MANAGER';
    }
    
    function isStaff(schoolId) {
      return isAuthenticated() && 
             getUserData().schoolId == schoolId && 
             (getUserData().role in ['MANAGER', 'VICE_PRINCIPAL', 'TEACHER', 'COUNSELOR']);
    }

    // Students collection
    match /students/{studentId} {
      allow read: if isAuthenticated() && (
        isStaff(resource.data.schoolId) ||
        (getUserData().role == 'PARENT' && request.auth.uid in resource.data.parentIds)
      );
      allow create, update, delete: if isAuthenticated() && isManager(request.resource.data.schoolId);
    }

    // Behavior records collection
    match /behaviorRecords/{recordId} {
      allow read: if isAuthenticated() && (
        isStaff(resource.data.schoolId) ||
        (getUserData().role == 'PARENT' && request.auth.uid in get(/databases/$(database)/documents/students/$(resource.data.studentId)).data.parentIds)
      );
      allow create: if isAuthenticated() && isStaff(request.resource.data.schoolId);
      allow update, delete: if isAuthenticated() && isManager(resource.data.schoolId);
    }
  }
}
```

---

### ۵. معماری جابجایی دمو به فایربیس (Zero UI Changes)
به لطف معماری چندلایه‌ای (`Domain Repositories` و `AppContainer`):
برای اتصال فایربیس هیچ تغییری در صفحات UI و ViewModels نیاز نیست. صرفاً کافیست پیاده‌سازی‌های Firebase جایگزین `DemoRepositoryImpl` در `AppContainer.kt` شوند.
