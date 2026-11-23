package com.example.workoutwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
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
        if(available) dialog.setPositiveButton("Скачать", (d,w) -> startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link))));
        dialog.show();
      });
    }).start();
  }
}