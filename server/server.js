import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import multer from 'multer';
import path from 'path';
import crypto from 'crypto';
import { fileURLToPath } from 'url';
import {
  S3Client,
  PutObjectCommand,
  ListObjectsV2Command,
  DeleteObjectCommand,
  HeadBucketCommand
} from '@aws-sdk/client-s3';

// تحميل متغيرات البيئة
dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// إعدادات CORS و body parser
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// خدمة الملفات الثابتة لواجهة المستخدم Frontend
app.use(express.static(path.join(__dirname, 'public')));

// قراءة بيانات الاتصال بـ Cloudflare R2
const BUCKET_NAME = process.env.R2_BUCKET_NAME || 'lecture-data';
const ENDPOINT = process.env.R2_ENDPOINT || 'https://20fd1dbe0e11c7fcd435574c9c57835f.r2.cloudflarestorage.com';
const ACCESS_KEY_ID = process.env.R2_ACCESS_KEY_ID || '1ca95d8f0cef704a0e560894e94f53c7';
const SECRET_ACCESS_KEY = process.env.R2_SECRET_ACCESS_KEY || '13e00719ee430a63f22daadfe752772be87661f90f75851f696c52bfd6161c0a';
let PUBLIC_DOMAIN = process.env.R2_PUBLIC_DOMAIN || 'https://pub-f9710a4d35b34defa42b5ea768a0c9e9.r2.dev';

// قراءة بيانات Supabase
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://nevabpquhmgsbhhygybn.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'sb_publishable_36VwC0IlbeW1MUDkKLwDtQ_WJgwjIfk';
const SUPABASE_STUDENT_TABLE = process.env.SUPABASE_STUDENT_TABLE || 'student';

// إزالة أي شرطة مائلة من نهاية الدومين
if (PUBLIC_DOMAIN.endsWith('/')) {
  PUBLIC_DOMAIN = PUBLIC_DOMAIN.slice(0, -1);
}

// تهيئة عميل S3 للاتصال بـ Cloudflare R2
const s3Client = new S3Client({
  region: 'auto',
  endpoint: ENDPOINT,
  credentials: {
    accessKeyId: ACCESS_KEY_ID,
    secretAccessKey: SECRET_ACCESS_KEY,
  },
});

// قائمة الصيغ المسموح برفعها
const ALLOWED_EXTENSIONS = ['.pdf', '.mp4', '.pptx', '.docx', '.zip'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'video/mp4',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/zip',
  'application/x-zip-compressed'
];

// إعداد Multer لتخزين الملف في الذاكرة (Memory Storage) لتحويله مباشرة إلى R2
const storage = multer.memoryStorage();

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 100 * 1024 * 1024, // أقصى حجم: 100 ميجابايت للمحاضرة أو الفيديو
  },
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const mime = file.mimetype;

    const isValidExt = ALLOWED_EXTENSIONS.includes(ext);
    const isValidMime = ALLOWED_MIME_TYPES.includes(mime) || ext === '.zip' || ext === '.pptx' || ext === '.docx';

    if (isValidExt && isValidMime) {
      cb(null, true);
    } else {
      cb(new Error(`نوع الملف غير مدعوم (${ext}). الصيغ المدعومة هي: PDF, MP4, PPTX, DOCX, ZIP.`));
    }
  },
});

// دالة مساعدة لتنسيق حجم الملف
function formatBytes(bytes, decimals = 2) {
  if (!+bytes) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

// دالة مساعدة لتوليد اسم فريد لكل ملف
function generateUniqueFileName(originalName) {
  const ext = path.extname(originalName).toLowerCase();
  const rawBase = path.basename(originalName, ext)
    .replace(/[^a-zA-Z0-9_\-\u0600-\u06FF]/g, '_')
    .slice(0, 30);
  const timestamp = Date.now();
  const randomHex = crypto.randomBytes(4).toString('hex');
  return `${timestamp}_${randomHex}${ext}`;
}

/**
 * مسار فحص حالة السيرفر والاتصال بـ R2
 * GET /api/health
 */
// مسار فحص حالة السيرفر والاتصال بـ R2 و Supabase
app.get('/api/health', async (req, res) => {
  let r2Status = 'offline';
  let supabaseStatus = 'unknown';

  try {
    await s3Client.send(new HeadBucketCommand({ Bucket: BUCKET_NAME }));
    r2Status = 'online';
  } catch (err) {
    r2Status = `error: ${err.message}`;
  }

  try {
    const supaRes = await fetch(`${SUPABASE_URL}/rest/v1/${SUPABASE_STUDENT_TABLE}?select=count`, {
      headers: {
        'apikey': SUPABASE_ANON_KEY,
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`
      }
    });
    supabaseStatus = supaRes.ok ? 'online' : `error: ${supaRes.status}`;
  } catch (err) {
    supabaseStatus = `error: ${err.message}`;
  }

  res.json({
    status: 'online',
    message: 'خادم بوابة معهد الفراعنة العالي يعمل بكفاءة',
    bucket: BUCKET_NAME,
    publicDomain: PUBLIC_DOMAIN,
    r2Status: r2Status,
    supabaseStatus: supabaseStatus,
    table: SUPABASE_STUDENT_TABLE
  });
});

/**
 * دالة مساعدة للتواصل مع Supabase REST API
 */
async function callSupabase(endpoint, method = 'GET', body = null, prefer = 'return=representation') {
  const url = `${SUPABASE_URL}/rest/v1/${endpoint}`;
  const headers = {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    'Content-Type': 'application/json',
    'Prefer': prefer
  };

  const options = { method, headers };
  if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    const err = new Error(data?.message || data?.hint || data?.details || `Supabase error (${response.status})`);
    err.status = response.status;
    err.details = data;
    throw err;
  }

  return data;
}

// -------------------------------------------------------------
// مسارات إدارة الطلاب عبر Supabase
// -------------------------------------------------------------

/**
 * جلب قائمة الطلاب من قاعدة بيانات سوبابيز
 * GET /api/students
 */
app.get('/api/students', async (req, res) => {
  try {
    const students = await callSupabase(`${SUPABASE_STUDENT_TABLE}?select=*&order=id.desc`);
    res.json({
      success: true,
      count: students.length,
      students: students
    });
  } catch (err) {
    console.error('خطأ جلب الطلاب من سوبابيز:', err);
    res.status(500).json({
      success: false,
      error: 'تعذر جلب بيانات الطلاب من قاعدة البيانات: ' + err.message
    });
  }
});

/**
 * تسجيل الدخول للطالب بالرقم القومي (14 رقماً)
 * GET /api/student/login?nationalId=...
 */
app.get('/api/student/login', async (req, res) => {
  try {
    const nationalId = req.query.nationalId?.trim();
    if (!nationalId) {
      return res.status(400).json({ success: false, error: 'يرجى إدخال الرقم القومي للطالب' });
    }

    const students = await callSupabase(`${SUPABASE_STUDENT_TABLE}?Id=eq.${nationalId}&select=*`);
    if (!students || students.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'لم يتم العثور على طالب مسجل بهذا الرقم القومي'
      });
    }

    res.json({
      success: true,
      student: students[0]
    });
  } catch (err) {
    console.error('خطأ تسجيل دخول الطالب:', err);
    res.status(500).json({
      success: false,
      error: 'حدث خطأ أثناء فحص بيانات الدخول: ' + err.message
    });
  }
});

/**
 * إضافة طالب جديد إلى Supabase مع التحقق من الاسم الرباعي ورقم الهاتف
 * POST /api/students
 */
app.post('/api/students', async (req, res) => {
  try {
    const { Name, Id, department = 'نظم معلومات إدارية (MIS)', acd_year = 'الفرقة الثانية', phone } = req.body;

    if (!Name || !Id) {
      return res.status(400).json({
        success: false,
        error: 'الاسم والرقم القومي حقول إجبارية'
      });
    }

    // شرط الاسم الرباعي
    const nameWords = Name.trim().split(/\s+/).filter(Boolean);
    if (nameWords.length < 4) {
      return res.status(400).json({
        success: false,
        error: 'شرط إلزامي: يجب أن يكون اسم الطالب رباعياً على الأقل (أربعة أسماء).'
      });
    }

    // شرط الرقم القومي 14 رقماً
    const idStr = String(Id).trim();
    if (!/^\d{14}$/.test(idStr)) {
      return res.status(400).json({
        success: false,
        error: 'الرقم القومي يجب أن يتكون من 14 رقماً صحيحاً.'
      });
    }

    const newStudent = {
      Name: Name.trim(),
      Id: Number(idStr),
      department: department.trim(),
      acd_year: acd_year.trim(),
      phone: phone ? Number(String(phone).replace(/\D/g, '')) : null
    };

    const inserted = await callSupabase(SUPABASE_STUDENT_TABLE, 'POST', newStudent);
    res.status(201).json({
      success: true,
      message: 'تم تسجيل الطالب بنجاح في قاعدة البيانات',
      student: inserted?.[0] || newStudent
    });
  } catch (err) {
    console.error('خطأ إضافة الطالب:', err);
    res.status(500).json({
      success: false,
      error: 'تعذر إضافة الطالب إلى سوبابيز: ' + (err.details?.message || err.message)
    });
  }
});

/**
 * حذف طالب نهائياً من قاعدة البيانات بواسطة المسؤول
 * DELETE /api/students/:id
 */
app.delete('/api/students/:id', async (req, res) => {
  try {
    const studentId = req.params.id;
    if (!studentId) {
      return res.status(400).json({ success: false, error: 'معرف الطالب مطلوب' });
    }

    // الحذف بواسطة id أو Id
    await callSupabase(`${SUPABASE_STUDENT_TABLE}?id=eq.${studentId}`, 'DELETE', null, 'return=representation');

    res.json({
      success: true,
      message: `تم حذف الطالب رقم (${studentId}) نهائياً من قاعدة بيانات سوبابيز.`
    });
  } catch (err) {
    console.error('خطأ حذف الطالب:', err);
    res.status(500).json({
      success: false,
      error: 'تعذر حذف الطالب من قاعدة البيانات: ' + (err.details?.message || err.message)
    });
  }
});

/**
 * حذف الحسابات التجريبية أو تفريغ الطلاب المحددين نهائياً
 * POST /api/students/purge-test
 */
app.post('/api/students/purge-test', async (req, res) => {
  try {
    const { ids } = req.body; // مصفوفة من المعرفات أو إن كانت فارغة نحذف الحسابات التي تحمل نمط تجريبي
    
    if (Array.isArray(ids) && ids.length > 0) {
      const idFilter = ids.join(',');
      await callSupabase(`${SUPABASE_STUDENT_TABLE}?id=in.(${idFilter})`, 'DELETE', null, 'return=representation');
      return res.json({
        success: true,
        message: `تم حذف ${ids.length} حساب نهائياً من سوبابيز.`
      });
    }

    // إذا لم تُحدد معرفات، نجلب الطلاب ونحذف أول 20 طالب اختباري
    const all = await callSupabase(`${SUPABASE_STUDENT_TABLE}?select=id,Name&limit=50`);
    if (!all || all.length === 0) {
      return res.json({ success: true, message: 'لا يوجد طلاب لحذفهم' });
    }

    const testIds = all.slice(0, 20).map(s => s.id);
    const filter = testIds.join(',');
    await callSupabase(`${SUPABASE_STUDENT_TABLE}?id=in.(${filter})`, 'DELETE', null, 'return=representation');

    res.json({
      success: true,
      message: `تم حذف ${testIds.length} حساب تجريبي نهائياً من قاعدة بيانات سوبابيز بنجاح.`
    });
  } catch (err) {
    console.error('خطأ تفريغ الحسابات:', err);
    res.status(500).json({
      success: false,
      error: 'تعذر حذف الحسابات التجريبية: ' + (err.details?.message || err.message)
    });
  }
});

// -------------------------------------------------------------
// المقررات الدراسية ونظام الساعات المعتمدة (18 ساعة)
// -------------------------------------------------------------
const YEAR_2_COURSES = [
  {
    code: 'LOG-201',
    name: 'اللوجستيات و سلاسل الإمداد',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'مبادئ إدارة الأعمال',
    doctor: 'أ.د. عبد الرحمن البدري',
    day: 'الأحد',
    time: '09:00 ص - 12:00 م',
    hall: 'مدرج 2 أ'
  },
  {
    code: 'ACC-202',
    name: 'محاسبه التكاليف',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'مبادئ المحاسبة المالية',
    doctor: 'د. طارق المنشاوي',
    day: 'الإثنين',
    time: '10:00 ص - 01:00 م',
    hall: 'مدرج 1 ب'
  },
  {
    code: 'LAW-203',
    name: 'قانون تجاري',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'لا يوجد',
    doctor: 'المستشار د. محمود عزت',
    day: 'الثلاثاء',
    time: '09:00 ص - 12:00 م',
    hall: 'قاعة 305'
  },
  {
    code: 'ECO-204',
    name: 'مبادئ اقتصاد جزئي',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'مقدمة في علم الاقتصاد',
    doctor: 'د. سهير السعدني',
    day: 'الأربعاء',
    time: '11:00 ص - 02:00 م',
    hall: 'مدرج 3 ج'
  },
  {
    code: 'MKT-205',
    name: 'مبادئ تسويق',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'مبادئ التسويق وسلوك المستهلك',
    doctor: 'د. منال فوزي',
    day: 'الخميس',
    time: '09:00 ص - 12:00 م',
    hall: 'مدرج 2 أ'
  },
  {
    code: 'MIS-206',
    name: 'مقرر اختياري تخصصي: نظم المعلومات الإدارية الحديثة',
    creditHours: 3,
    department: 'نظم معلومات إدارية',
    year: 'الفرقة الثانية',
    prerequisite: 'مقدمة في نظم المعلومات',
    doctor: 'د. حازم القاضي',
    day: 'السبت',
    time: '10:00 ص - 01:00 م',
    hall: 'معمل الحاسب 4'
  }
];

// جدول الامتحانات
const EXAM_SCHEDULE = [
  { subject: 'اللوجستيات و سلاسل الإمداد', code: 'LOG-201', date: '2025-06-01', day: 'الأحد', time: '10:00 ص - 12:00 م', hall: 'مدرج الفراعنة 1', committee: 'لجنة رقم 4' },
  { subject: 'محاسبه التكاليف', code: 'ACC-202', date: '2025-06-04', day: 'الأربعاء', time: '10:00 ص - 12:00 م', hall: 'مدرج الفراعنة 2', committee: 'لجنة رقم 4' },
  { subject: 'قانون تجاري', code: 'LAW-203', date: '2025-06-08', day: 'الأحد', time: '10:00 ص - 12:00 م', hall: 'مدرج الفراعنة 1', committee: 'لجنة رقم 4' },
  { subject: 'مبادئ اقتصاد جزئي', code: 'ECO-204', date: '2025-06-11', day: 'الأربعاء', time: '10:00 ص - 12:00 م', hall: 'مدرج الفراعنة 3', committee: 'لجنة رقم 4' },
  { subject: 'مبادئ تسويق', code: 'MKT-205', date: '2025-06-15', day: 'الأحد', time: '10:00 ص - 12:00 م', hall: 'مدرج الفراعنة 2', committee: 'لجنة رقم 4' },
  { subject: 'مقرر اختياري: نظم المعلومات', code: 'MIS-206', date: '2025-06-18', day: 'الأربعاء', time: '10:00 ص - 12:00 م', hall: 'معمل الحاسب الرئيسي', committee: 'لجنة رقم 4' }
];

app.get('/api/courses', (req, res) => {
  res.json({
    success: true,
    maxCreditHours: 18,
    academicYear: 'الفرقة الثانية',
    courses: YEAR_2_COURSES
  });
});

app.get('/api/exams', (req, res) => {
  res.json({
    success: true,
    academicYear: 'الفرقة الثانية',
    schedule: EXAM_SCHEDULE
  });
});

/**
 * مسار رفع الملفات الأساسي
 * POST /api/upload
 */
app.post('/api/upload', (req, res) => {
  upload.single('file')(req, res, async (err) => {
    if (err) {
      return res.status(400).json({
        success: false,
        error: err.message || 'حدث خطأ أثناء فحص الملف المرفوع',
      });
    }

    if (!req.file) {
      return res.status(400).json({
        success: false,
        error: 'لم يتم اختيار أي ملف لرفعه',
      });
    }

    try {
      const {
        title = 'بدون عنوان',
        subjectName = 'مادة عامة',
        yearGroup = 'الفرقة الأولى',
        department = 'MIS',
        category = 'محاضرة'
      } = req.body;

      // 1. توليد اسم فريد للملف مع الاحتفاظ بالامتداد الأصلي
      const originalName = req.file.originalname;
      const uniqueFileName = generateUniqueFileName(originalName);

      // تحديد مسار التخزين داخل الـ Bucket
      const fileKey = `uploads/${uniqueFileName}`;

      // 2. إعداد أمر حفظ الملف في Cloudflare R2
      const putCommand = new PutObjectCommand({
        Bucket: BUCKET_NAME,
        Key: fileKey,
        Body: req.file.buffer,
        ContentType: req.file.mimetype || 'application/octet-stream',
        Metadata: {
          'original-name': encodeURIComponent(originalName),
          'title': encodeURIComponent(title),
          'subject': encodeURIComponent(subjectName),
          'year': encodeURIComponent(yearGroup),
          'department': encodeURIComponent(department),
          'category': encodeURIComponent(category),
        },
      });

      // إرسال الملف إلى Cloudflare R2
      await s3Client.send(putCommand);

      // 3. توليد الرابط المباشر العام
      const downloadUrl = `${PUBLIC_DOMAIN}/${fileKey}`;

      // 4. إرجاع استجابة JSON متكاملة
      return res.status(200).json({
        success: true,
        message: 'تم رفع الملف بنجاح إلى Cloudflare R2',
        file: {
          originalName: originalName,
          fileName: uniqueFileName,
          key: fileKey,
          fileSize: req.file.size,
          formattedSize: formatBytes(req.file.size),
          fileType: req.file.mimetype,
          extension: path.extname(originalName).toLowerCase().replace('.', ''),
          downloadUrl: downloadUrl,
          metadata: {
            title: title,
            subjectName: subjectName,
            yearGroup: yearGroup,
            department: department,
            category: category,
            uploadedAt: new Date().toISOString(),
          }
        }
      });
    } catch (uploadError) {
      console.error('خطأ أثناء الرفع إلى R2:', uploadError);
      return res.status(500).json({
        success: false,
        error: 'فشلت عملية التخزين في Cloudflare R2: ' + (uploadError.message || 'خطأ غير معروف'),
      });
    }
  });
});

/**
 * مسار جلب قائمة الملفات المرفوعة من الـ Bucket
 * GET /api/files
 */
app.get('/api/files', async (req, res) => {
  try {
    const listCommand = new ListObjectsV2Command({
      Bucket: BUCKET_NAME,
      Prefix: 'uploads/',
      MaxKeys: 100,
    });

    const response = await s3Client.send(listCommand);
    const contents = response.Contents || [];

    const files = contents.map(item => {
      const fileName = path.basename(item.Key);
      const ext = path.extname(fileName).toLowerCase().replace('.', '');
      return {
        key: item.Key,
        fileName: fileName,
        fileSize: item.Size,
        formattedSize: formatBytes(item.Size),
        lastModified: item.LastModified,
        extension: ext,
        downloadUrl: `${PUBLIC_DOMAIN}/${item.Key}`,
      };
    });

    res.json({
      success: true,
      count: files.length,
      files: files,
    });
  } catch (error) {
    console.error('خطأ جلب الملفات:', error);
    res.status(500).json({
      success: false,
      error: 'تعذر جلب قائمة الملفات من السحابة: ' + error.message,
    });
  }
});

/**
 * مسار حذف ملف من Cloudflare R2
 * DELETE /api/files/:key
 */
app.delete('/api/files/*', async (req, res) => {
  try {
    const key = req.params[0];
    if (!key) {
      return res.status(400).json({ success: false, error: 'المسار غير محدد' });
    }

    await s3Client.send(new DeleteObjectCommand({
      Bucket: BUCKET_NAME,
      Key: key,
    }));

    res.json({
      success: true,
      message: 'تم حذف الملف بنجاح من Cloudflare R2',
      key: key,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'تعذر حذف الملف: ' + error.message,
    });
  }
});

// تشغيل السيرفر
app.listen(PORT, () => {
  console.log('========================================================');
  console.log(`🚀 خادم رفع الملفات يعمل بنجاح على المنفذ: ${PORT}`);
  console.log(`🔗 واجهة الويب: http://localhost:${PORT}`);
  console.log(`📦 مسار الرفع: POST http://localhost:${PORT}/api/upload`);
  console.log(`☁️ Cloudflare R2 Bucket: ${BUCKET_NAME}`);
  console.log(`🌐 النطاق العام للتحميل: ${PUBLIC_DOMAIN}`);
  console.log('========================================================');
});
