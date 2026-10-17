package by.arena.fooddiary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Автообновление приложения.
 *
 * Схема работы:
 *  1. приложение скачивает update.json из последнего релиза на GitHub;
 *  2. сравнивает version_code с текущей версией BuildConfig.VERSION_CODE;
 *  3. если есть новая версия — предлагает скачать её;
 *  4. APK скачивается, сверяется SHA-256 и передаётся системному установщику.
 *
 * Данные дневника при этом никуда не отправляются: по сети уходит только
 * запрос манифеста версии и сам файл APK.
 */
public class UpdateManager {

    /** Манифест последнего релиза: releases/latest/download отдаёт редирект на свежий релиз. */
    public static final String MANIFEST_URL =
            "https://github.com/zigorminsk-debug/eat-control/releases/latest/download/update.json";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final String USER_AGENT = "FoodDiary-Android/" + BuildConfig.VERSION_NAME;
    private static final int BUFFER = 16 * 1024;

    private static final String PREFS = "local_diary_v1";
    private static final String KEY_LAST_CHECK = "update_check_ts";
    private static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;

    private static final int INK = 0xFF1A3133;
    private static final int MUTED = 0xFF637474;
    private static final int GREEN = 0xFF237063;
    private static final int GREEN_DARK = 0xFF123F3D;
    private static final int LINE = 0xFFDCE5E0;

    private final Handler main = new Handler(Looper.getMainLooper());

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressLabel;

    /**
     * Проверяет наличие обновления.
     *
     * @param interactive true — показывать сообщения о результате (кнопка «Проверить обновление»),
     *                    false — тихая проверка при запуске приложения.
     */
    public void check(final Activity activity, final boolean interactive) {
        if (activity == null || activity.isFinishing()) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                String raw = null;
                String error = null;
                try {
                    raw = httpGet(MANIFEST_URL);
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                final String body = raw;
                final String failure = error;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing()) return;
                        if (body == null) {
                            if (interactive) {
                                toast(activity, "Не удалось проверить обновления: " + failure);
                            }
                            return;
                        }
                        try {
                            handleManifest(activity, new JSONObject(body), interactive);
                        } catch (Exception e) {
                            if (interactive) toast(activity, "Не удалось разобрать ответ сервера");
                        }
                    }
                });
            }
        }).start();
    }

    private void handleManifest(Activity activity, JSONObject info, boolean interactive) {
        int remoteCode = info.optInt("version_code", 0);
        String remoteName = info.optString("version_name", "");
        String url = info.optString("url", "");
        String sha256 = info.optString("sha256", "");

        if (remoteCode <= BuildConfig.VERSION_CODE) {
            if (interactive) {
                toast(activity, "Установлена последняя версия (" + BuildConfig.VERSION_NAME + ")");
            }
            return;
        }
        if (TextUtils.isEmpty(url)) {
            if (interactive) toast(activity, "В манифесте нет ссылки на файл обновления");
            return;
        }

        final String notes = info.optString("notes", "");
        final String downloadUrl = url;
        final String checksum = sha256;

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 22), dp(activity, 18), dp(activity, 22), dp(activity, 4));

        TextView title = text(activity, "Доступно обновление", 17, INK, true);
        box.addView(title);
        TextView version = text(activity,
                "Установлена версия " + BuildConfig.VERSION_NAME
                        + " → будет установлена " + (remoteName.isEmpty() ? String.valueOf(remoteCode) : remoteName),
                13, GREEN_DARK, false);
        box.addView(version);
        if (!notes.isEmpty()) {
            TextView note = text(activity, notes, 12, MUTED, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(activity, 10);
            box.addView(note, lp);
        }

        new AlertDialog.Builder(activity)
                .setView(box)
                .setPositiveButton("Скачать и установить", (dialog, which) ->
                        download(activity, downloadUrl, checksum))
                .setNegativeButton("Позже", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Загрузка APK
    // ------------------------------------------------------------------

    private void download(final Activity activity, final String url, final String sha256) {
        showProgress(activity, "Загрузка обновления…", 0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String error = null;
                boolean ok = false;
                try {
                    File target = apkFile(activity);
                    if (target == null) throw new IOException("Нет доступа к каталогу приложения");
                    HttpURLConnection connection = open(url);
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    connection.setReadTimeout(READ_TIMEOUT_MS);
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    connection.setInstanceFollowRedirects(true);
                    connection.connect();

                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) {
                        throw new IOException("HTTP " + status);
                    }
                    long total = connection.getContentLength();
                    InputStream in = connection.getInputStream();
                    OutputStream out = new FileOutputStream(target);
                    try {
                        byte[] buffer = new byte[BUFFER];
                        int read;
                        long done = 0;
                        int percent = 0;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                            done += read;
                            if (total > 0) {
                                int next = (int) (done * 100L / total);
                                if (next != percent) {
                                    percent = next;
                                    final int shown = percent;
                                    main.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (progressBar != null) progressBar.setProgress(shown);
                                            if (progressLabel != null) {
                                                progressLabel.setText("Загрузка обновления… " + shown + "%");
                                            }
                                        }
                                    });
                                }
                            }
                        }
                        out.flush();
                    } finally {
                        closeQuietly(in);
                        closeQuietly(out);
                        connection.disconnect();
                    }
                    if (!TextUtils.isEmpty(sha256)) {
                        String actual = sha256(apkFile(activity));
                        if (actual == null || !actual.equalsIgnoreCase(sha256)) {
                            // Файл повреждён или подменён — не устанавливаем его.
                            deleteQuietly(apkFile(activity));
                            throw new IOException("Контрольная сумма не совпала");
                        }
                    }
                    ok = true;
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                final String failure = error;
                final boolean success = ok;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        if (activity.isFinishing()) return;
                        if (success) {
                            install(activity);
                        } else {
                            toast(activity, "Ошибка загрузки: " + failure);
                        }
                    }
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // Установка
    // ------------------------------------------------------------------

    private void install(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Нужно разрешение на установку")
                    .setMessage("Разрешите приложению «Еда и самочувствие» устанавливать обновления, "
                            + "затем повторите установку.")
                    .setPositiveButton("Открыть настройки", (dialog, which) -> openInstallPermission(activity))
                    .setNegativeButton("Отмена", null)
                    .show();
            return;
        }
        Uri uri = ApkProvider.apkUri();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("update.apk", uri));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(activity, "На устройстве нет программы для установки пакетов");
        }
    }

    private void openInstallPermission(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast(activity, "Откройте настройки приложения вручную");
            }
        }
    }

    // ------------------------------------------------------------------
    // Служебное
    // ------------------------------------------------------------------

    private void showProgress(Activity activity, String message, int percent) {
        hideProgress();
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 22), dp(activity, 18), dp(activity, 22), dp(activity, 6));

        progressLabel = text(activity, message, 13, INK, false);
        box.addView(progressLabel);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(percent);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity, 12);
        box.addView(progressBar, lp);

        progressDialog = new AlertDialog.Builder(activity)
                .setView(box)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            progressDialog = null;
        }
        progressBar = null;
        progressLabel = null;
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json");
        connection.setInstanceFollowRedirects(true);
        connection.connect();
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status);
            }
            InputStream in = connection.getInputStream();
            try {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
                return buffer.toString("UTF-8");
            } finally {
                closeQuietly(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        URL target = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setUseCaches(false);
        return connection;
    }

    private static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        if (file == null || !file.isFile()) return null;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream in = new java.io.FileInputStream(file);
        try {
            byte[] buffer = new byte[BUFFER];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        } finally {
            closeQuietly(in);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }

    private static File apkFile(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        if (dir == null) return null;
        return new File(dir, "update.apk");
    }

    private static void closeQuietly(java.io.Closeable stream) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static void toast(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    private TextView text(Activity activity, String value, float size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Тихая проверка обновления при запуске приложения.
     * Не чаще одного раза в сутки, чтобы не дёргать GitHub при каждом открытии.
     */
    public static void checkInBackground(final Activity activity) {
        if (activity == null) return;
        try {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            long last = prefs.getLong(KEY_LAST_CHECK, 0L);
            if (last > 0L && now - last < CHECK_INTERVAL_MS) return;
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply();
        } catch (Exception ignored) {
            // Не смогли прочитать настройки — просто проверим один раз.
        }
        new UpdateManager().check(activity, false);
    }
}
