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
app.get('/api/health', async (req, res) => {
  try {
    await s3Client.send(new HeadBucketCommand({ Bucket: BUCKET_NAME }));
    res.json({
      status: 'online',
      message: 'السيرفر متصل بنجاح مع Cloudflare R2',
      bucket: BUCKET_NAME,
      publicDomain: PUBLIC_DOMAIN,
    });
  } catch (error) {
    res.status(500).json({
      status: 'error',
      message: 'تعذر الاتصال بـ Cloudflare R2',
      error: error.message,
    });
  }
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
