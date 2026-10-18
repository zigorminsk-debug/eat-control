package by.arena.fooddiary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Экран «О приложении»: версия, справка, проверка обновления и условия
 * использования. Открывается по кнопке «?» в шапке главного экрана.
 */
public class AboutActivity extends Activity {

    private static final String SOURCE_URL = "https://github.com/zigorminsk-debug/eat-control";

    private static final int BG = Color.rgb(244, 246, 242);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(26, 49, 51);
    private static final int MUTED = Color.rgb(99, 116, 116);
    private static final int GREEN = Color.rgb(35, 112, 99);
    private static final int GREEN_DARK = Color.rgb(18, 63, 61);
    private static final int GREEN_SOFT = Color.rgb(231, 243, 237);
    private static final int LINE = Color.rgb(220, 229, 224);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(25));
        scroll.addView(page, new android.widget.ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addText(page, "О ПРИЛОЖЕНИИ", 10, GREEN, true);
        gap(page, 6);
        addText(page, "Еда и самочувствие", 25, INK, true);
        addText(page, "Личный дневник питания и симптомов ЖКТ. "
                + "Версия " + BuildConfig.VERSION_NAME + " (сборка " + BuildConfig.VERSION_CODE + ")", 13, MUTED, false);
        gap(page, 16);

        // --- Помощь ---
        LinearLayout help = card();
        addText(help, "Помощь", 16, GREEN_DARK, true);
        gap(help, 5);
        addText(help, "Внутри приложения есть справочные страницы «Газы и вздутие» и «Камни в желчном "
                + "пузыре: симптомы, обследование и лечение». Они открываются без интернета.", 12,
                Color.rgb(55, 89, 82), false);
        gap(help, 11);
        TextView helpButton = button("Открыть справку", GREEN, Color.WHITE, false);
        help.addView(helpButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        helpButton.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        addCard(page, help);

        // --- Обновление ---
        LinearLayout update = card();
        addText(update, "Обновления", 16, GREEN_DARK, true);
        gap(update, 5);
        addText(update, "Приложение умеет само проверять новые версии и предлагать обновление. "
                + "Новые сборки подписываются тем же постоянным ключом, поэтому обновление "
                + "устанавливается поверх уже установленной версии, а записи дневника сохраняются.", 12,
                Color.rgb(55, 89, 82), false);
        gap(update, 11);
        TextView checkButton = button("Проверить обновление", Color.WHITE, GREEN_DARK, true);
        update.addView(checkButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        checkButton.setOnClickListener(v -> new UpdateManager().check(this, true));
        addCard(page, update);

        // --- Приватность ---
        LinearLayout privacy = card();
        privacy.setBackground(round(GREEN_SOFT, GREEN_SOFT, 17));
        addText(privacy, "Приватность", 16, GREEN_DARK, true);
        gap(privacy, 5);
        addText(privacy, "Записи дневника хранятся только на этом устройстве и никуда не отправляются. "
                + "По сети приложение обращается только к GitHub: за проверкой обновления и за самим файлом APK. "
                + "Удаление приложения удаляет локальный дневник — при необходимости заранее экспортируйте CSV.",
                12, Color.rgb(55, 89, 82), false);
        addCard(page, privacy);

        // --- Источник ---
        LinearLayout source = card();
        addText(source, "Исходный код", 16, GREEN_DARK, true);
        gap(source, 5);
        addText(source, SOURCE_URL, 12, MUTED, false);
        gap(source, 11);
        TextView sourceButton = button("Открыть репозиторий", Color.WHITE, GREEN_DARK, true);
        source.addView(sourceButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        sourceButton.setOnClickListener(v -> openSource());
        addCard(page, source);

        gap(page, 8);
        addText(page, "Приложение не ставит диагноз и не заменяет врача. "
                + "Экран анализа показывает совпадения в ваших записях, а не причину симптомов.",
                11, MUTED, false);
    }

    private void openSource() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Нет приложения для открытия ссылки", Toast.LENGTH_LONG).show();
        }
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), dp(10), dp(16), dp(10));
        header.setBackgroundColor(SURFACE);

        TextView back = text("‹  Назад", 13, GREEN_DARK, true);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(8), dp(7), dp(10), dp(7));
        back.setBackground(round(SURFACE, LINE, 12));
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("О приложении", 15, INK, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return header;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(15), dp(16), dp(15));
        l.setBackground(round(SURFACE, LINE, 17));
        return l;
    }

    private void addCard(LinearLayout parent, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        parent.addView(card, lp);
    }

    private void addText(LinearLayout parent, String value, float size, int color, boolean bold) {
        parent.addView(text(value, size, color, bold));
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView button(String label, int fill, int foreground, boolean outlined) {
        TextView b = text(label, 13, foreground, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.setMinHeight(dp(46));
        b.setMaxLines(1);
        b.setIncludeFontPadding(false);
        b.setBackground(round(fill, outlined ? LINE : fill, 13));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void gap(LinearLayout parent, int dpValue) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(dpValue)));
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != fill) d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
