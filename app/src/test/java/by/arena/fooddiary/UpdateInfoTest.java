package by.arena.fooddiary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.junit.Test;

/**
 * Проверка разбора манифеста обновления — той части автообновления,
 * которая решает, показывать ли пользователю предложение обновиться.
 */
public class UpdateInfoTest {

    private static final String VALID = "{"
            + "\"version_code\": 4,"
            + "\"version_name\": \"1.1.1\","
            + "\"url\": \"https://github.com/zigorminsk-debug/eat-control/releases/download/v1.1.1/food-diary-1.1.1.apk\","
            + "\"sha256\": \"0123456789abcdef\","
            + "\"size\": 64657,"
            + "\"notes\": \"Что-то новое\""
            + "}";

    @Test
    public void parsesAllManifestFields() throws JSONException {
        UpdateInfo info = UpdateInfo.parse(VALID);
        assertEquals(4, info.versionCode);
        assertEquals("1.1.1", info.versionName);
        assertEquals("https://github.com/zigorminsk-debug/eat-control/releases/download/v1.1.1/food-diary-1.1.1.apk", info.url);
        assertEquals("0123456789abcdef", info.sha256);
        assertEquals(64657L, info.size);
        assertEquals("Что-то новое", info.notes);
        assertEquals("1.1.1", info.describe());
    }

    @Test
    public void detectsNewerVersionByCode() throws JSONException {
        UpdateInfo info = UpdateInfo.parse(VALID);
        assertTrue(info.isNewerThan(3));
        assertFalse(info.isNewerThan(4));
        assertFalse(info.isNewerThan(5));
    }

    @Test
    public void rejectsManifestWithoutVersionCode() {
        try {
            UpdateInfo.parse("{\"url\": \"https://example.com/app.apk\"}");
            fail("ожидалась ошибка разбора");
        } catch (JSONException expected) {
            // так и должно быть
        }
    }

    @Test
    public void rejectsManifestWithoutUrl() {
        try {
            UpdateInfo.parse("{\"version_code\": 4}");
            fail("ожидалась ошибка разбора");
        } catch (JSONException expected) {
            // так и должно быть
        }
    }

    @Test
    public void rejectsEmptyResponse() {
        try {
            UpdateInfo.parse(null);
            fail("ожидалась ошибка разбора");
        } catch (JSONException expected) {
            // так и должно быть
        }
    }

    @Test
    public void fallsBackToCodeAsVersionName() throws JSONException {
        UpdateInfo info = UpdateInfo.parse("{\"version_code\": 7, \"url\": \"https://example.com/app.apk\"}");
        assertEquals("7", info.describe());
    }
}
