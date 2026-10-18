package by.arena.fooddiary;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Минимальный аналог FileProvider без сторонних зависимостей.
 *
 * Нужен, чтобы системный установщик пакетов прочитал скачанный APK:
 * Android 7+ запрещает передавать файлы приложения через file://.
 * Провайдер отдаёт ровно один файл — update.apk — из приватного каталога
 * приложения и больше ничего не раскрывает.
 */
public class ApkProvider extends ContentProvider {

    public static final String AUTHORITY = "by.arena.fooddiary.apk";
    private static final String FILE_NAME = "update.apk";

    /** content:// URI для установки уже скачанного APK. */
    public static Uri apkUri() {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(FILE_NAME)
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = apkFile();
        if (file == null || !file.isFile()) {
            throw new FileNotFoundException("Файл обновления не найден: " + uri);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File file = apkFile();
        MatrixCursor cursor = new MatrixCursor(
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        cursor.addRow(new Object[]{FILE_NAME, file == null ? 0L : file.length()});
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only provider");
    }

    private File apkFile() {
        Context context = getContext();
        if (context == null) return null;
        File dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        if (dir == null) return null;
        return new File(dir, FILE_NAME);
    }
}
