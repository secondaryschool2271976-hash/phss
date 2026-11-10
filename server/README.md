# نظام رفع وتحميل المحاضرات والواجبات | Cloudflare R2 Object Storage
### تطبيق الفراعنة التعليمي لطلاب الجامعة

هذا الدليل يوضح خطوات تشغيل واستخدام خادم الـ Backend وواجهة الـ Frontend لرفع وتحميل ملفات المحاضرات والواجبات بصيغ (PDF, MP4, PPTX, DOCX, ZIP) مع التخزين السحابي المباشر على **Cloudflare R2 Object Storage**.

---

## 📌 1. بيانات الاتصال بحساب Cloudflare R2
تم ضبط الإعدادات مسبقاً في ملف `.env` كالتالي:
- **اسم الحاوية (Bucket Name):** `lecture-data`
- **رابط نقطة الاتصال (Endpoint URL):** `https://20fd1dbe0e11c7fcd435574c9c57835f.r2.cloudflarestorage.com`
- **مفتاح الوصول (Access Key ID):** `1ca95d8f0cef704a0e560894e94f53c7`
- **المفتاح السري (Secret Access Key):** `13e00719ee430a63f22daadfe752772be87661f90f75851f696c52bfd6161c0a`
- **النطاق العام للتحميل (Public Download Domain):** `https://pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev`

---

## 🛠️ 2. المتطلبات الأساسية
- تثبيت [Node.js](https://nodejs.org) (الإصدار 18 أو أحدث)
- مدير الحزم `npm`

---

## 🚀 3. خطوات التشغيل السريعة

### الخطوة 1: الدخول إلى مجلد السيرفر
```bash
cd server
```

### الخطوة 2: تثبيت الحزم البرمجية
```bash
npm install
```

### الخطوة 3: التحقق من ملف المتغيرات البيئية `.env`
الملف موجود بالفعل داخل مجلد `server/.env`. يمكنك تعديله إذا رغبت في تغيير المنفذ أو أي إعدادات أخرى:
```env
PORT=3000
R2_BUCKET_NAME=lecture-data
R2_ENDPOINT=https://20fd1dbe0e11c7fcd435574c9c57835f.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=1ca95d8f0cef704a0e560894e94f53c7
R2_SECRET_ACCESS_KEY=13e00719ee430a63f22daadfe752772be87661f90f75851f696c52bfd6161c0a
R2_PUBLIC_DOMAIN=https://pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev
```

### الخطوة 4: تشغيل السيرفر
```bash
npm start
```
أو في وضع التطوير والتحديث التلقائي:
```bash
npm run dev
```

---

## 🌐 4. الوصول إلى واجهة المستخدم (Web Frontend)
بمجرد تشغيل السيرفر، افتح متصفحك على:
👉 **`http://localhost:3000`**

### مميزات الواجهة:
1. **السحب والإفلات (Drag & Drop):** إمكانية إلقاء أي ملف (PDF, MP4, PPTX, DOCX, ZIP) مباشرة في خانة الرفع.
2. **الحقول الوصفية:**
   - اسم المحاضرة أو الواجب.
   - اسم المادة الدراسية.
   - الفرقة الدراسية (الأولى، الثانية، الثالثة، الرابعة).
   - القسم الدراسي (نظم معلومات إدارية، علوم حاسب، إدارة أعمال).
   - نوع الملف (محاضرة، واجب، شيت عملي، ملخص).
3. **شريط تقدم الرفع (Upload Progress Bar):** يظهر نسبة الرفع التفاعلية من `0%` حتى `100%` لحظياً.
4. **كارت الملف بعد الرفع:**
   - عرض اسم الملف، حجمه، نوعه، وتاريخ الرفع.
   - **زر نسخ الرابط:** لنسخ الرابط العام بنقرة واحدة إلى الحافظة.
   - **زر تحميل الملف مباشرة:** لفتح أو تنزيل الملف فورياً عبر الرابط العام.
5. **سجل الملفات:** عرض قائمة بالملفات المرفوعة مع إمكانية إعادة نسخ الرابط أو التحميل.

---

## 📡 5. واجهات برمجة التطبيقات (API Endpoints)

### 1. رفع ملف جديد
- **المسار:** `POST /api/upload`
- **نوع المحتوى:** `multipart/form-data`
- **الحقول:**
  - `file`: الملف المراد رفعه (PDF, MP4, PPTX, DOCX, ZIP).
  - `title`: اسم المحاضرة / الواجب.
  - `subjectName`: اسم المادة.
  - `yearGroup`: الفرقة الدراسية.
  - `department`: القسم (MIS, CS, BA).
  - `category`: التصنيف (محاضرة، واجب، شيت).

#### مثال استجابة النجاح (JSON Response):
```json
{
  "success": true,
  "message": "تم رفع الملف بنجاح إلى Cloudflare R2",
  "file": {
    "originalName": "ERP_Lecture_4.pdf",
    "fileName": "1727339200123_4a8b1c2d.pdf",
    "key": "uploads/1727339200123_4a8b1c2d.pdf",
    "fileSize": 2451920,
    "formattedSize": "2.34 MB",
    "fileType": "application/pdf",
    "extension": "pdf",
    "downloadUrl": "https://pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev/uploads/1727339200123_4a8b1c2d.pdf",
    "metadata": {
      "title": "المحاضرة 4 - تخطيط موارد المؤسسات",
      "subjectName": "تخطيط موارد المؤسسات (ERP)",
      "yearGroup": "الفرقة الثالثة",
      "department": "MIS",
      "category": "محاضرة",
      "uploadedAt": "2026-09-26T10:15:30.000Z"
    }
  }
}
```

### 2. جلب قائمة الملفات
- **المسار:** `GET /api/files`
- **الاستجابة:** قائمة بالملفات المتاحة في السحابة مع روابط التحميل المباشرة.

### 3. فحص صحة الاتصال بـ R2
- **المسار:** `GET /api/health`
- **الاستجابة:** حالة الاتصال وسلامة مفاتيح Cloudflare R2.
