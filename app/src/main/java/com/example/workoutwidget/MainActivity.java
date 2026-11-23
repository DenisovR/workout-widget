package com.example.workoutwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Build;
import androidx.core.content.FileProvider;
import java.io.File;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private Button update;
  private long pendingDownload = -1;
  private BroadcastReceiver downloadReceiver;
  private static final String RELEASE_API = "https://api.github.com/repos/DenisovR/workout-widget/releases/latest";
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(40,60,40,40);
    root.setBackgroundColor(Color.rgb(18,26,32));
    TextView title = new TextView(this);
    title.setText("МОИ ТРЕНИРОВКИ");
    title.setTextSize(26); title.setTextColor(Color.WHITE);
    root.addView(title);
    TextView description = new TextView(this);
    description.setText("\nПонедельник · Плечи, грудь, ноги\n\nСреда · Спина и руки\n\nПятница · Всё тело\n\nДобавь виджет на рабочий стол, чтобы видеть ближайшую тренировку.");
    description.setTextSize(17); description.setTextColor(Color.LTGRAY);
    root.addView(description);
    update = new Button(this);
    update.setText("Проверить обновления");
    root.addView(update);
    update.setOnClickListener(v -> checkUpdates());
    TextView version = new TextView(this);
    version.setText("Установленная версия: " + BuildConfig.VERSION_NAME);
    version.setTextColor(Color.LTGRAY);
    version.setGravity(Gravity.CENTER_HORIZONTAL);
    root.addView(version);
    setContentView(root);
    downloadReceiver = new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if(id != pendingDownload) return;
        DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        android.database.Cursor cursor = dm.query(new DownloadManager.Query().setFilterById(id));
        boolean success = false;
        if(cursor != null) {try {if(cursor.moveToFirst()) success = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;} finally {cursor.close();}}
        if(!success) {new AlertDialog.Builder(MainActivity.this).setMessage("Не удалось скачать обновление.").setPositiveButton("ОК",null).show();return;}
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"workout-update.apk");
        Uri uri = FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",file);
        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {startActivity(install);}catch(Exception e){new AlertDialog.Builder(MainActivity.this).setMessage("Не удалось открыть установщик: "+e.getMessage()).setPositiveButton("ОК",null).show();}
      }
    };
    IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    if(Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(downloadReceiver,filter);
  }
  @Override protected void onDestroy(){if(downloadReceiver != null)unregisterReceiver(downloadReceiver);super.onDestroy();}
  private void downloadUpdate(String link) {
    try {
      File folder = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
      if(folder == null) throw new IllegalStateException("Нет доступной папки загрузок");
      DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
      request.setTitle("Обновление «Мои тренировки»");
      request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
      request.setDestinationUri(Uri.fromFile(new File(folder,"workout-update.apk")));
      File old = new File(folder,"workout-update.apk"); if(old.exists()) old.delete();
      pendingDownload = ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
    } catch(Exception e) {new AlertDialog.Builder(this).setMessage("Ошибка загрузки: "+e.getMessage()).setPositiveButton("ОК",null).show();}
  }
  private void checkUpdates() {
    update.setEnabled(false); update.setText("Проверяем GitHub…");
    new Thread(() -> {
      String message = null, url = null;
      boolean newer = false;
      try {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept","application/vnd.github+json");
        connection.setRequestProperty("User-Agent","WorkoutWidget-Android");
        int status = connection.getResponseCode();
        if(status != 200) throw new Exception(status == 404 ? "Обновления ещё не опубликованы." : "GitHub вернул ошибку "+status);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(InputStream in = connection.getInputStream()) {byte[] buf = new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
        JSONObject json = new JSONObject(out.toString("UTF-8"));
        String tag = json.optString("tag_name","");
        int remote = 0;
        if(tag.startsWith("v1.0.")) remote = Integer.parseInt(tag.substring(5));
        int local = BuildConfig.VERSION_CODE;
        newer = remote > local;
        JSONArray assets = json.optJSONArray("assets");
        if(assets != null) for(int i=0;i<assets.length();i++) {
          JSONObject asset = assets.getJSONObject(i);
          if(asset.optString("name").endsWith(".apk")) {url=asset.optString("browser_download_url");break;}
        }
        if(newer && url != null) message = "Доступна версия "+tag+". Скачать APK для установки?";
        else message = "У тебя установлена актуальная версия.";
      } catch(Exception e) { message = "Не удалось проверить обновления: "+e.getMessage(); }
      final String text=message, link=url;final boolean available=newer && url!=null;
      runOnUiThread(() -> {
        update.setEnabled(true);update.setText("Проверить обновления");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Обновления").setMessage(text).setNegativeButton("Закрыть",null);
        if(available) dialog.setPositiveButton("Обновить", (d,w) -> downloadUpdate(link));
        dialog.show();
      });
    }).start();
  }
}