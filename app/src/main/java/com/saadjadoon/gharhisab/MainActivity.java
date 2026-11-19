package com.saadjadoon.gharhisab;

/* Hisaab Kitaab — Developed by Muhammad Saad Jadoon */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String CHANNEL_ID = "gharhisab_monthly";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setTextZoom(100);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                if (isFinishing()) { result.cancel(); return true; }
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                if (isFinishing()) { result.cancel(); return true; }
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
        });
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Month Summary", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Month-end expense summary notifications");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public class Bridge {
        /* Permanent key-value storage (survives instantly, used to mirror app data
           so progress and the one-time onboarding flag are never lost) */
        @JavascriptInterface
        public void setPref(String key, String value) {
            try {
                SharedPreferences p = getSharedPreferences("hisaabkitaab", MODE_PRIVATE);
                p.edit().putString(key, value).commit();
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public String getPref(String key) {
            try {
                SharedPreferences p = getSharedPreferences("hisaabkitaab", MODE_PRIVATE);
                return p.getString(key, "");
            } catch (Exception e) { return ""; }
        }

        private boolean writeDownload(String name, String mime, byte[] data) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentResolver cr = getContentResolver();
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HisaabKitaab");
                    Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) throw new Exception("no uri");
                    OutputStream os = cr.openOutputStream(uri);
                    os.write(data); os.close();
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), "HisaabKitaab");
                    if (!dir.exists()) dir.mkdirs();
                    FileOutputStream os = new FileOutputStream(new File(dir, name));
                    os.write(data); os.close();
                }
                return true;
            } catch (Exception e) {
                try {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    FileOutputStream os = new FileOutputStream(new File(dir, name));
                    os.write(data); os.close();
                    return true;
                } catch (Exception e2) { return false; }
            }
        }

        private void toast(final String m) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface
        public void createPdf(final String name, final String title, final String body) {
            new Thread(() -> {
                try {
                    PdfDocument doc = new PdfDocument();
                    Paint titlePaint = new Paint();
                    titlePaint.setTextSize(17); titlePaint.setFakeBoldText(true); titlePaint.setColor(0xFF102A43);
                    Paint bodyPaint = new Paint();
                    bodyPaint.setTextSize(9.5f); bodyPaint.setColor(0xFF222222);
                    bodyPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
                    String[] lines = body.split("\n", -1);
                    int perPage = 52, idx = 0, pageNo = 1;
                    while (idx < lines.length) {
                        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, pageNo).create();
                        PdfDocument.Page page = doc.startPage(info);
                        android.graphics.Canvas c = page.getCanvas();
                        int y = 52;
                        if (pageNo == 1) { c.drawText(title, 36, y, titlePaint); y += 26; }
                        for (int i = 0; i < perPage && idx < lines.length; i++, idx++) {
                            String line = lines[idx];
                            if (line.length() > 96) line = line.substring(0, 96);
                            c.drawText(line, 36, y, bodyPaint); y += 14;
                        }
                        doc.finishPage(page); pageNo++;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    doc.writeTo(bos); doc.close();
                    toast(writeDownload(name, "application/pdf", bos.toByteArray())
                            ? "PDF saved to Downloads/HisaabKitaab" : "Could not save the PDF");
                } catch (Exception e) { toast("Could not create the PDF"); }
            }).start();
        }

        @JavascriptInterface
        public void createXlsx(final String name, final String csv) {
            new Thread(() -> {
                try {
                    List<List<String>> rows = parseCsv(csv);
                    StringBuilder sheet = new StringBuilder();
                    sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                         .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                    for (List<String> row : rows) {
                        sheet.append("<row>");
                        for (String cell : row) {
                            String v = cell == null ? "" : cell.trim();
                            if (v.matches("-?\\d+(\\.\\d+)?")) sheet.append("<c><v>").append(v).append("</v></c>");
                            else sheet.append("<c t=\"inlineStr\"><is><t>").append(xmlEsc(v)).append("</t></is></c>");
                        }
                        sheet.append("</row>");
                    }
                    sheet.append("</sheetData></worksheet>");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ZipOutputStream z = new ZipOutputStream(bos);
                    zipAdd(z, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
                    zipAdd(z, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                    zipAdd(z, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Hisaab Kitaab\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                    zipAdd(z, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
                    zipAdd(z, "xl/worksheets/sheet1.xml", sheet.toString());
                    z.close();
                    toast(writeDownload(name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray())
                            ? "Excel file saved to Downloads/HisaabKitaab" : "Could not save the Excel file");
                } catch (Exception e) { toast("Could not create the Excel file"); }
            }).start();
        }

        private void zipAdd(ZipOutputStream z, String path, String content) throws Exception {
            z.putNextEntry(new ZipEntry(path));
            z.write(content.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }

        private String xmlEsc(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }

        private List<List<String>> parseCsv(String csv) {
            List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            StringBuilder cell = new StringBuilder();
            boolean inQ = false;
            for (int i = 0; i < csv.length(); i++) {
                char ch = csv.charAt(i);
                if (inQ) {
                    if (ch == '"') {
                        if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append('"'); i++; }
                        else inQ = false;
                    } else cell.append(ch);
                } else if (ch == '"') inQ = true;
                else if (ch == ',') { row.add(cell.toString()); cell.setLength(0); }
                else if (ch == '\n') { row.add(cell.toString()); cell.setLength(0); rows.add(row); row = new ArrayList<>(); }
                else if (ch != '\r') cell.append(ch);
            }
            if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
            return rows;
        }

        @JavascriptInterface
        public void notify(String title, String text) {
            try {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                android.app.Notification n = new android.app.Notification.Builder(MainActivity.this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) nm.notify((int) (System.currentTimeMillis() % 100000), n);
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void saveFile(String name, String content) {
            new Thread(() -> {
                String msg;
                try {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentResolver cr = getContentResolver();
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                        cv.put(MediaStore.Downloads.MIME_TYPE, name.endsWith(".csv") ? "text/csv" : "application/json");
                        cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HisaabKitaab");
                        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                        if (uri == null) throw new Exception("no uri");
                        OutputStream os = cr.openOutputStream(uri);
                        os.write(content.getBytes(StandardCharsets.UTF_8));
                        os.close();
                    } else {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS), "HisaabKitaab");
                        if (!dir.exists()) dir.mkdirs();
                        FileOutputStream os = new FileOutputStream(new File(dir, name));
                        os.write(content.getBytes(StandardCharsets.UTF_8));
                        os.close();
                    }
                    msg = "File saved to Downloads/HisaabKitaab";
                } catch (Exception e) {
                    try {
                        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        FileOutputStream os = new FileOutputStream(new File(dir, name));
                        os.write(content.getBytes(StandardCharsets.UTF_8));
                        os.close();
                        msg = "File saved to the app folder";
                    } catch (Exception e2) {
                        msg = "Could not save the file";
                    }
                }
                final String m = msg;
                runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
            }).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { android.webkit.CookieManager.getInstance().flush(); } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }
        /* Ask the app first: it goes back one step (or closes a popup/menu).
           Only when the app says it is at the first screen do we close. */
        try {
            webView.evaluateJavascript("window.__hkBack?window.__hkBack():'0'", value -> {
                if (value == null || !value.contains("1")) finish();
            });
        } catch (Exception e) {
            if (webView.canGoBack()) webView.goBack(); else finish();
        }
    }
}
