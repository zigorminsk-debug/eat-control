package by.arena.fooddiary;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Манифест новой версии — файл update.json, который приложение скачивает
 * из последнего релиза на GitHub.
 *
 * <pre>
 * {
 *   "version_code": 4,
 *   "version_name": "1.1.1",
 *   "url": "https://.../food-diary-1.1.1.apk",
 *   "sha256": "…",
 *   "size": 64657
 * }
 * </pre>
 *
 * Класс намеренно не зависит от Android — так его разбор проверяется
 * обычным модульным тестом на JVM.
 */
public class UpdateInfo {

    public final int versionCode;
    public final String versionName;
    public final String url;
    public final String sha256;
    public final long size;
    public final String notes;

    private UpdateInfo(int versionCode, String versionName, String url,
                       String sha256, long size, String notes) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.url = url;
        this.sha256 = sha256;
        this.size = size;
        this.notes = notes;
    }

    /** Разбирает ответ сервера. Бросает JSONException, если ответ нечитаемый. */
    public static UpdateInfo parse(String json) throws JSONException {
        if (json == null) throw new JSONException("Пустой ответ сервера");
        JSONObject obj = new JSONObject(json);
        int versionCode = obj.optInt("version_code", 0);
        String versionName = obj.optString("version_name", "");
        String url = obj.optString("url", "");
        String sha256 = obj.optString("sha256", "");
        long size = obj.optLong("size", 0L);
        String notes = obj.optString("notes", "");
        if (versionCode <= 0) throw new JSONException("В манифесте нет корректного version_code");
        if (url == null || url.trim().isEmpty()) throw new JSONException("В манифесте нет ссылки на APK");
        return new UpdateInfo(versionCode, versionName, url.trim(), sha256.trim(), size, notes);
    }

    /** Новее ли эта версия, чем установленная. */
    public boolean isNewerThan(int currentVersionCode) {
        return versionCode > currentVersionCode;
    }

    /** Короткое описание для интерфейса. */
    public String describe() {
        return versionName == null || versionName.trim().isEmpty()
                ? String.valueOf(versionCode)
                : versionName.trim();
    }
}
