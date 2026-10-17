package by.arena.fooddiary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A small, offline-first food and symptom diary.
 * No network permission is declared: entries stay in the app's private preferences.
 */
public class MainActivity extends Activity {
    private static final String PREFS = "local_diary_v1";
    private static final String KEY_ENTRIES = "entries_json";
    private static final int REQUEST_EXPORT = 701;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int ANALYSIS_DAYS = 14;
    private static final int MATCH_WINDOW_HOURS = 12;
    private static final Locale RU = new Locale("ru", "RU");

    private static final int BG = Color.rgb(244, 246, 242);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(26, 49, 51);
    private static final int MUTED = Color.rgb(99, 116, 116);
    private static final int GREEN = Color.rgb(35, 112, 99);
    private static final int GREEN_DARK = Color.rgb(18, 63, 61);
    private static final int GREEN_SOFT = Color.rgb(231, 243, 237);
    private static final int GOLD = Color.rgb(229, 191, 113);
    private static final int LINE = Color.rgb(220, 229, 224);
    private static final int RED = Color.rgb(166, 68, 60);

    private SharedPreferences preferences;
    private JSONArray entries = new JSONArray();
    private LinearLayout root;
    private LinearLayout contentHost;
    private LinearLayout navHost;
    private String screen = "home";
    private boolean symptomMode = false;
    private long selectedTime = System.currentTimeMillis();

    // Current form fields
    private TextView timeButton;
    private EditText foodsField;
    private EditText portionField;
    private EditText noteField;
    private Spinner mealTypeSpinner;
    private Spinner stoolSpinner;
    private SeekBar bloatingBar;
    private SeekBar gasBar;
    private SeekBar painBar;
    private SeekBar stressBar;
    private CheckBox controlCheck;

    // Черновик формы: переживает переключение между «Еда» и «Симптомы»,
    // чтобы введённое не пропадало при смене вкладки.
    private String draftFoods = "";
    private String draftPortion = "";
    private String draftMealNote = "";
    private String draftSymptomNote = "";
    private int draftMealType = 0;
    private int draftStool = 0;
    private int draftBloating = 0;
    private int draftGas = 0;
    private int draftPain = 0;
    private int draftStress = 0;
    private boolean draftControl = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadEntries();

        getWindow().setStatusBarColor(GREEN_DARK);
        getWindow().setNavigationBarColor(SURFACE);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentHost = new LinearLayout(this);
        contentHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        navHost = new LinearLayout(this);
        navHost.setOrientation(LinearLayout.VERTICAL);
        root.addView(navHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));
        render();

        // Раз в сутки тихо проверяем, не вышла ли новая версия.
        UpdateManager.checkInBackground(this);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(11), dp(12), dp(11));
        header.setBackgroundColor(SURFACE);

        TextView mark = text("Ж", 15, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(GREEN_DARK, GREEN_DARK, 12));
        header.addView(mark, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(10), 0, dp(6), 0);
        // Заголовок всегда в одну строку: раньше он переносился и налезал
        // на бейдж «только на устройстве».
        TextView title = text("Еда и самочувствие", 14, INK, true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (Build.VERSION.SDK_INT >= 26) {
            title.setAutoSizeTextTypeUniformWithConfiguration(11, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        }
        titleBox.addView(title);
        titleBox.addView(text("личный дневник ЖКТ", 10, MUTED, false));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView privacy = text("● локально", 9, GREEN, true);
        privacy.setGravity(Gravity.CENTER);
        privacy.setSingleLine(true);
        privacy.setPadding(dp(8), dp(6), dp(8), dp(6));
        privacy.setBackground(round(GREEN_SOFT, GREEN_SOFT, 20));
        header.addView(privacy);

        TextView help = text("?", 15, GREEN_DARK, true);
        help.setGravity(Gravity.CENTER);
        help.setSingleLine(true);
        help.setContentDescription("О приложении и помощь");
        help.setBackground(round(SURFACE, LINE, 14));
        help.setClickable(true);
        help.setFocusable(true);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        helpParams.setMargins(dp(8), 0, 0, 0);
        header.addView(help, helpParams);
        help.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        return header;
    }

    private void render() {
        if (contentHost == null) return;
        contentHost.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(25));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentHost.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if ("home".equals(screen)) renderHome(page);
        else if ("add".equals(screen)) renderAdd(page);
        else if ("analysis".equals(screen)) renderAnalysis(page);
        else renderHistory(page);
        renderNavigation();
    }

    private void renderHome(LinearLayout page) {
        addText(page, formatDay(System.currentTimeMillis()).toUpperCase(RU), 11, GREEN, true);
        addGap(page, 6);
        addText(page, "Ваш пищевой дневник", 27, INK, true);
        addText(page, "Записи помогают заметить повторяющиеся связи между едой и самочувствием.", 14, MUTED, false);
        addGap(page, 15);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(19));
        hero.setBackground(round(GREEN_DARK, GREEN_DARK, 20));
        addText(hero, "НАБЛЮДЕНИЕ, НЕ ДИАГНОЗ", 10, GOLD, true);
        addGap(hero, 8);
        addText(hero, "Поймайте повторяющийся паттерн", 20, Color.WHITE, true);
        addGap(hero, 6);
        addText(hero, "Отмечайте продукты, время и симптомы. Анализ покажет совпадения в ваших записях — он не определяет аллергию, непереносимость или заболевание.", 13, Color.rgb(218, 233, 225), false);
        addGap(hero, 12);
        addText(hero, "Данные хранятся локально. В сеть уходят только проверка обновления и сам файл APK.", 11, Color.rgb(204, 224, 215), false);
        addCard(page, hero);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView addMeal = button("＋  Еда", GREEN, Color.WHITE, false);
        TextView addSymptom = button("◉  Симптом", Color.WHITE, GREEN_DARK, true);
        // Обе кнопки одной высоты: раньше вторая обрезала нижнюю строку подписи.
        LinearLayout.LayoutParams actionLeft = new LinearLayout.LayoutParams(0, dp(56), 1f);
        actionLeft.setMargins(0, 0, dp(8), 0);
        actions.addView(addMeal, actionLeft);
        actions.addView(addSymptom, new LinearLayout.LayoutParams(0, dp(56), 1f));
        addMeal.setOnClickListener(v -> openAdd(false));
        addSymptom.setOnClickListener(v -> openAdd(true));
        page.addView(actions);
        addGap(page, 17);

        int mealsToday = 0;
        int symptomNotesToday = 0;
        int maxBloating = 0;
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long dayStart = start.getTimeInMillis();
        for (JSONObject obj : sortedEntries()) {
            long ts = obj.optLong("ts", 0L);
            if (ts < dayStart) continue;
            if ("meal".equals(obj.optString("type"))) mealsToday++;
            else {
                symptomNotesToday++;
                maxBloating = Math.max(maxBloating, obj.optInt("bloating", 0));
            }
        }

        addText(page, "СЕГОДНЯ", 10, MUTED, true);
        addGap(page, 7);
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(dp(14), dp(15), dp(14), dp(15));
        stats.setBackground(round(SURFACE, LINE, 17));
        stats.addView(metric("" + mealsToday, "приёмов пищи"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(metric("" + symptomNotesToday, "отметок"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(metric(symptomNotesToday == 0 ? "—" : maxBloating + "/10", "макс. вздутие"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f));
        page.addView(stats);
        addGap(page, 20);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Последние записи", 18, INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView seeAll = text("Вся история  →", 12, GREEN, true);
        heading.addView(seeAll);
        seeAll.setOnClickListener(v -> { screen = "history"; render(); });
        page.addView(heading);
        addGap(page, 9);

        List<JSONObject> list = sortedEntries();
        if (list.isEmpty()) {
            addEmptyState(page, "Пока нет записей", "Добавьте первый приём пищи или отметьте самочувствие — дневник начнёт собирать вашу личную историю.");
        } else {
            for (int i = 0; i < Math.min(3, list.size()); i++) addEntryCard(page, list.get(i), false);
        }
        addGap(page, 5);
        addText(page, "Подсказка: записывайте еду ближе ко времени приёма, а симптом — когда он появился.", 11, MUTED, false);
    }

    private void renderAdd(LinearLayout page) {
        addText(page, "НОВАЯ ЗАПИСЬ", 10, GREEN, true);
        addGap(page, 5);
        addText(page, "Что хотите отметить?", 24, INK, true);
        addText(page, "Время записи можно изменить, если вносите данные позже.", 13, MUTED, false);
        addGap(page, 14);

        LinearLayout typeRow = new LinearLayout(this);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView mealTab = button("Еда", symptomMode ? SURFACE : GREEN_SOFT, symptomMode ? MUTED : GREEN_DARK, !symptomMode);
        TextView symptomTab = button("Симптомы", symptomMode ? GREEN_SOFT : SURFACE, symptomMode ? GREEN_DARK : MUTED, symptomMode);
        LinearLayout.LayoutParams tab1 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tab1.setMargins(0, 0, dp(8), 0);
        typeRow.addView(mealTab, tab1);
        typeRow.addView(symptomTab, new LinearLayout.LayoutParams(0, dp(46), 1f));
        mealTab.setOnClickListener(v -> { if (symptomMode) { stashForm(); symptomMode = false; render(); } });
        symptomTab.setOnClickListener(v -> { if (!symptomMode) { stashForm(); symptomMode = true; render(); } });
        page.addView(typeRow);
        addGap(page, 12);

        LinearLayout timeCard = card();
        timeCard.setOrientation(LinearLayout.HORIZONTAL);
        timeCard.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout timeCopy = new LinearLayout(this);
        timeCopy.setOrientation(LinearLayout.VERTICAL);
        timeCopy.addView(text("Время", 11, MUTED, false));
        timeCopy.addView(text("Укажите, когда это было", 14, INK, true));
        timeCard.addView(timeCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        timeButton = button(formatDateTime(selectedTime), GREEN_SOFT, GREEN_DARK, true);
        timeCard.addView(timeButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));
        timeButton.setOnClickListener(v -> chooseDateTime());
        addCard(page, timeCard);

        if (!symptomMode) {
            addFieldLabel(page, "Тип приёма пищи");
            mealTypeSpinner = spinner(new String[]{"Завтрак", "Обед", "Ужин", "Перекус", "Другое"});
            mealTypeSpinner.setSelection(Math.min(draftMealType, 4));
            page.addView(mealTypeSpinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            addGap(page, 13);

            addFieldLabel(page, "Что вы ели или пили? *");
            foodsField = input("Например: молоко, овсянка, яблоко", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 2);
            if (!draftFoods.isEmpty()) foodsField.setText(draftFoods);
            page.addView(foodsField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78)));
            addText(page, "Пишите отдельные продукты через запятую или с новой строки — так анализ сможет сгруппировать совпадения.", 11, MUTED, false);
            addGap(page, 12);

            addFieldLabel(page, "Порция (необязательно)");
            portionField = input("Например: чашка, 150 г, небольшая порция", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 1);
            if (!draftPortion.isEmpty()) portionField.setText(draftPortion);
            page.addView(portionField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            addGap(page, 13);

            addFieldLabel(page, "Комментарий (необязательно)");
            noteField = input("Способ приготовления, соус, напиток…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
            if (!draftMealNote.isEmpty()) noteField.setText(draftMealNote);
            noteField.setGravity(Gravity.TOP | Gravity.START);
            page.addView(noteField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(82)));
            addGap(page, 18);
        } else {
            addText(page, "Оцените выраженность сейчас", 16, INK, true);
            addText(page, "0 — нет, 10 — максимально выражено. Сохраняйте запись примерно в одно и то же время относительно появления симптома.", 12, MUTED, false);
            addGap(page, 12);

            controlCheck = new CheckBox(this);
            controlCheck.setText("Контрольная отметка: заметных симптомов сейчас нет");
            controlCheck.setTextColor(INK);
            controlCheck.setTextSize(13);
            if (Build.VERSION.SDK_INT >= 21) controlCheck.setButtonTintList(ColorStateList.valueOf(GREEN));
            controlCheck.setPadding(0, dp(5), 0, dp(5));
            page.addView(controlCheck);
            addGap(page, 5);

            bloatingBar = addSlider(page, "Вздутие / распирание", draftBloating);
            gasBar = addSlider(page, "Газы / отхождение газов", draftGas);
            painBar = addSlider(page, "Боль в животе", draftPain);
            stressBar = addSlider(page, "Стресс или напряжение · необязательно", draftStress);

            controlCheck.setOnCheckedChangeListener((buttonView, checked) -> {
                if (checked) {
                    bloatingBar.setProgress(0);
                    gasBar.setProgress(0);
                    painBar.setProgress(0);
                }
                bloatingBar.setEnabled(!checked);
                gasBar.setEnabled(!checked);
                painBar.setEnabled(!checked);
            });

            stoolSpinner = spinner(new String[]{"Стул не отмечаю", "Тип 1–2 · твёрдый / запор", "Тип 3–4 · оформленный", "Тип 5–7 · мягкий / жидкий"});
            stoolSpinner.setSelection(Math.min(draftStool, 3));
            addFieldLabel(page, "Стул (необязательно)");
            page.addView(stoolSpinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            addGap(page, 13);

            addFieldLabel(page, "Комментарий (необязательно)");
            noteField = input("Например: началось после еды, сколько длилось…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
            if (!draftSymptomNote.isEmpty()) noteField.setText(draftSymptomNote);
            noteField.setGravity(Gravity.TOP | Gravity.START);
            page.addView(noteField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(82)));
            addGap(page, 17);

            if (draftControl) controlCheck.setChecked(true);
        }

        TextView save = button("Сохранить запись", GREEN, Color.WHITE, false);
        page.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        save.setOnClickListener(v -> {
            if (symptomMode) saveSymptom();
            else saveMeal();
        });
        addGap(page, 10);
        addText(page, "Записи остаются в приложении на этом устройстве. Удаление приложения удалит локальный дневник.", 10, MUTED, false);
    }

    private void renderAnalysis(LinearLayout page) {
        addText(page, "ПОСЛЕДНИЕ 14 ДНЕЙ", 10, GREEN, true);
        addGap(page, 5);
        addText(page, "Наблюдения", 26, INK, true);
        addText(page, "Повторяющиеся совпадения в вашем дневнике — повод присмотреться, но не доказательство причины.", 13, MUTED, false);
        addGap(page, 14);

        long cutoff = System.currentTimeMillis() - ANALYSIS_DAYS * DAY_MS;
        int meals = 0;
        int symptomNotes = 0;
        int controls = 0;
        int strongNotes = 0;
        for (JSONObject obj : sortedEntries()) {
            if (obj.optLong("ts", 0L) < cutoff) continue;
            if ("meal".equals(obj.optString("type"))) meals++;
            else {
                symptomNotes++;
                if (obj.optBoolean("control", false)) controls++;
                else if (obj.optInt("bloating", 0) >= 4) strongNotes++;
            }
        }

        LinearLayout summary = card();
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(metric("" + meals, "приёмов пищи"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summary.addView(metric("" + symptomNotes, "отметок состояния"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summary.addView(metric("" + controls, "контрольных"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        addCard(page, summary);
        addGap(page, 17);

        addText(page, "Какие продукты повторялись перед симптомом", 17, INK, true);
        addText(page, "Считаются продукты из записей о еде за 12 часов до отметки вздутия 4/10 и выше. Контрольные отметки без симптомов показаны отдельно.", 12, MUTED, false);
        addGap(page, 9);

        Map<String, int[]> matches = new HashMap<>();
        for (JSONObject obj : sortedEntries()) {
            long ts = obj.optLong("ts", 0L);
            if (ts < cutoff || !"symptom".equals(obj.optString("type"))) continue;
            boolean control = obj.optBoolean("control", false);
            boolean symptomCase = !control && obj.optInt("bloating", 0) >= 4;
            if (!control && !symptomCase) continue;
            Set<String> foods = foodsInWindow(ts, MATCH_WINDOW_HOURS);
            for (String food : foods) {
                int[] counts = matches.get(food);
                if (counts == null) { counts = new int[2]; matches.put(food, counts); }
                if (symptomCase) counts[0]++;
                if (control) counts[1]++;
            }
        }

        if (matches.isEmpty()) {
            addEmptyState(page, "Пока недостаточно данных для совпадений", "Запишите несколько приёмов пищи, затем отмечайте выраженность вздутия. Для сравнения можно сохранять контрольные отметки в периоды без симптомов.");
        } else {
            List<String> names = new ArrayList<>(matches.keySet());
            Collections.sort(names, new Comparator<String>() {
                @Override public int compare(String a, String b) {
                    int diff = matches.get(b)[0] - matches.get(a)[0];
                    if (diff != 0) return diff;
                    return a.compareTo(b);
                }
            });
            LinearLayout listCard = card();
            listCard.setPadding(dp(15), dp(4), dp(15), dp(4));
            int shown = Math.min(12, names.size());
            for (int i = 0; i < shown; i++) {
                String name = names.get(i);
                int[] counts = matches.get(name);
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(12), 0, dp(12));
                TextView food = text(titleCase(name), 14, INK, true);
                row.addView(food, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                LinearLayout numbers = new LinearLayout(this);
                numbers.setOrientation(LinearLayout.VERTICAL);
                numbers.setGravity(Gravity.RIGHT);
                numbers.addView(text("симптом: " + counts[0], 11, counts[0] > 0 ? RED : MUTED, true));
                numbers.addView(text("контроль: " + counts[1], 10, MUTED, false));
                row.addView(numbers);
                listCard.addView(row);
                if (i < shown - 1) listCard.addView(divider());
            }
            addCard(page, listCard);
        }

        LinearLayout meaning = card();
        meaning.setBackground(round(GREEN_SOFT, GREEN_SOFT, 17));
        addText(meaning, "Как читать этот экран", 15, GREEN_DARK, true);
        addGap(meaning, 6);
        addText(meaning, "Это частота совместных записей, не вероятность и не диагноз. Приём пищи может содержать несколько ингредиентов, а симптомы — зависеть от порции, времени, запора, стресса и других факторов. Если совпадение повторяется 3 раза и более, это лишь повод аккуратно проверить его ещё раз, а не исключать продукт навсегда.", 12, Color.rgb(55, 89, 82), false);
        addGap(meaning, 8);
        addText(meaning, "Порог 4/10 и окно 12 часов — технические настройки дневника, а не медицинские нормативы. Без контрольных отметок сравнение неполное.", 11, Color.rgb(73, 103, 96), false);
        addCard(page, meaning);

        addGap(page, 15);
        TextView export = button("Экспортировать дневник в CSV", Color.WHITE, GREEN_DARK, true);
        page.addView(export, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        export.setOnClickListener(v -> exportCsv());
        addGap(page, 9);
        TextView add = button("＋  Добавить запись", GREEN, Color.WHITE, false);
        page.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        add.setOnClickListener(v -> openAdd(true));
    }

    private void renderHistory(LinearLayout page) {
        addText(page, "ВАШИ ДАННЫЕ", 10, GREEN, true);
        addGap(page, 5);
        addText(page, "История", 26, INK, true);
        List<JSONObject> list = sortedEntries();
        addText(page, list.size() + (list.size() == 1 ? " запись" : " записей") + " · хранится только на этом устройстве", 12, MUTED, false);
        addGap(page, 13);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView export = button("Экспорт CSV", SURFACE, GREEN_DARK, true);
        TextView clear = button("Удалить всё", Color.rgb(255, 240, 237), RED, true);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, dp(48), 1f);
        l.setMargins(0, 0, dp(8), 0);
        actions.addView(export, l);
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 1f));
        export.setOnClickListener(v -> exportCsv());
        clear.setOnClickListener(v -> confirmClearAll());
        page.addView(actions);
        addGap(page, 15);

        if (list.isEmpty()) {
            addEmptyState(page, "История пока пуста", "Нажмите «Запись», чтобы добавить приём пищи или симптом.");
        } else {
            for (JSONObject obj : list) addEntryCard(page, obj, true);
        }
    }

    private void renderNavigation() {
        navHost.removeAllViews();
        View line = new View(this);
        line.setBackgroundColor(LINE);
        navHost.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(7), dp(4), dp(7), dp(5));
        row.setBackgroundColor(SURFACE);
        String[] keys = {"home", "add", "analysis", "history"};
        String[] icons = {"●", "+", "◉", "≡"};
        String[] labels = {"Сегодня", "Запись", "Анализ", "История"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            boolean active = ("add".equals(screen) && "add".equals(key)) || screen.equals(key);
            TextView item = text(icons[i] + "\n" + labels[i], 10, active ? GREEN_DARK : MUTED, active);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(3), dp(4), dp(3), dp(2));
            item.setBackground(round(active ? GREEN_SOFT : SURFACE, active ? GREEN_SOFT : SURFACE, 13));
            item.setClickable(true);
            item.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(item, lp);
            item.setOnClickListener(v -> {
                if ("add".equals(key)) {
                    // Если уже открыт экран записи — не сбрасываем режим
                    // («Еда»/«Симптомы») и не выбрасываем введённое.
                    if (!"add".equals(screen)) openAdd(false);
                } else {
                    screen = key;
                    render();
                }
            });
        }
        navHost.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void openAdd(boolean forSymptom) {
        screen = "add";
        symptomMode = forSymptom;
        selectedTime = System.currentTimeMillis();
        render();
    }

    /** Сохраняет содержимое формы, чтобы переключение вкладок его не теряло. */
    private void stashForm() {
        if (foodsField != null) draftFoods = foodsField.getText().toString();
        if (portionField != null) draftPortion = portionField.getText().toString();
        if (mealTypeSpinner != null) draftMealType = mealTypeSpinner.getSelectedItemPosition();
        if (bloatingBar != null) draftBloating = bloatingBar.getProgress();
        if (gasBar != null) draftGas = gasBar.getProgress();
        if (painBar != null) draftPain = painBar.getProgress();
        if (stressBar != null) draftStress = stressBar.getProgress();
        if (stoolSpinner != null) draftStool = stoolSpinner.getSelectedItemPosition();
        if (controlCheck != null) draftControl = controlCheck.isChecked();
        if (noteField != null) {
            if (symptomMode) draftSymptomNote = noteField.getText().toString();
            else draftMealNote = noteField.getText().toString();
        }
    }

    private void clearDraft() {
        draftFoods = "";
        draftPortion = "";
        draftMealNote = "";
        draftSymptomNote = "";
        draftMealType = 0;
        draftStool = 0;
        draftBloating = 0;
        draftGas = 0;
        draftPain = 0;
        draftStress = 0;
        draftControl = false;
    }

    private void saveMeal() {
        String foods = foodsField == null ? "" : foodsField.getText().toString().trim();
        if (foods.isEmpty()) {
            if (foodsField != null) foodsField.setError("Укажите хотя бы один продукт");
            Toast.makeText(this, "Добавьте продукты или напитки", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", UUID.randomUUID().toString());
            obj.put("type", "meal");
            obj.put("ts", selectedTime);
            obj.put("mealType", mealTypeSpinner == null ? "Приём пищи" : String.valueOf(mealTypeSpinner.getSelectedItem()));
            obj.put("foods", foods);
            obj.put("portion", portionField == null ? "" : portionField.getText().toString().trim());
            obj.put("note", noteField == null ? "" : noteField.getText().toString().trim());
            entries.put(obj);
            persistEntries();
            hideKeyboard();
            clearDraft();
            screen = "home";
            render();
            Toast.makeText(this, "Приём пищи сохранён", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Не удалось сохранить запись", Toast.LENGTH_LONG).show();
        }
    }

    private void saveSymptom() {
        boolean control = controlCheck != null && controlCheck.isChecked();
        int bloating = control || bloatingBar == null ? 0 : bloatingBar.getProgress();
        int gas = control || gasBar == null ? 0 : gasBar.getProgress();
        int pain = control || painBar == null ? 0 : painBar.getProgress();
        if (!control && bloating == 0 && gas == 0 && pain == 0) {
            Toast.makeText(this, "Оцените симптом или отметьте контрольную запись", Toast.LENGTH_LONG).show();
            return;
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", UUID.randomUUID().toString());
            obj.put("type", "symptom");
            obj.put("ts", selectedTime);
            obj.put("control", control);
            obj.put("bloating", bloating);
            obj.put("gas", gas);
            obj.put("pain", pain);
            obj.put("stress", stressBar == null ? 0 : stressBar.getProgress());
            obj.put("stool", stoolSpinner == null ? "" : String.valueOf(stoolSpinner.getSelectedItem()));
            obj.put("note", noteField == null ? "" : noteField.getText().toString().trim());
            entries.put(obj);
            persistEntries();
            hideKeyboard();
            clearDraft();
            screen = "home";
            render();
            Toast.makeText(this, control ? "Контрольная отметка сохранена" : "Симптом сохранён", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Не удалось сохранить запись", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseDateTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedTime);
        DatePickerDialog dateDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(selectedTime);
            picked.set(Calendar.YEAR, year);
            picked.set(Calendar.MONTH, month);
            picked.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                picked.set(Calendar.HOUR_OF_DAY, hour);
                picked.set(Calendar.MINUTE, minute);
                picked.set(Calendar.SECOND, 0);
                picked.set(Calendar.MILLISECOND, 0);
                selectedTime = picked.getTimeInMillis();
                if (timeButton != null) timeButton.setText(formatShortDateTime(selectedTime));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private SeekBar addSlider(LinearLayout parent, String title, int initial) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(5), 0, dp(2));
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.addView(text(title, 13, INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text(initial + "/10", 12, GREEN, true);
        labelRow.addView(value);
        block.addView(labelRow);
        SeekBar seek = new SeekBar(this);
        seek.setMax(10);
        seek.setProgress(initial);
        if (Build.VERSION.SDK_INT >= 21) {
            seek.setProgressTintList(ColorStateList.valueOf(GREEN));
            seek.setThumbTintList(ColorStateList.valueOf(GREEN));
            seek.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        }
        seek.setContentDescription(title);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { value.setText(progress + "/10"); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        block.addView(seek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        parent.addView(block);
        return seek;
    }

    private Spinner spinner(String[] options) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(11), 0, dp(10), 0);
        spinner.setBackground(round(SURFACE, LINE, 12));
        spinner.setMinimumHeight(dp(48));
        return spinner;
    }

    private EditText input(String hint, int inputType, int minLines) {
        EditText edit = new EditText(this);
        edit.setTextSize(14);
        edit.setTextColor(INK);
        edit.setHintTextColor(Color.rgb(139, 153, 150));
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setPadding(dp(13), dp(10), dp(13), dp(10));
        edit.setBackground(round(Color.rgb(250, 252, 250), LINE, 12));
        if (minLines <= 1) {
            edit.setSingleLine(true);
        } else {
            edit.setMinLines(minLines);
            edit.setMaxLines(minLines + 2);
        }
        return edit;
    }

    private void addFieldLabel(LinearLayout parent, String title) {
        addGap(parent, 2);
        addText(parent, title, 12, INK, true);
        addGap(parent, 6);
    }

    private void addEntryCard(LinearLayout parent, JSONObject obj, boolean showDelete) {
        LinearLayout c = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        String type = obj.optString("type", "");
        String label = "meal".equals(type) ? "ЕДА · " + obj.optString("mealType", "Приём пищи").toUpperCase(RU) : (obj.optBoolean("control", false) ? "КОНТРОЛЬНАЯ ОТМЕТКА" : "САМОЧУВСТВИЕ");
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text(label, 10, GREEN, true));
        titleBox.addView(text(formatDateTime(obj.optLong("ts", 0L)), 11, MUTED, false));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (showDelete) {
            TextView delete = button("×", Color.rgb(248, 241, 239), RED, true);
            header.addView(delete, new LinearLayout.LayoutParams(dp(38), dp(38)));
            delete.setOnClickListener(v -> confirmDelete(obj.optString("id", "")));
        }
        c.addView(header);
        addGap(c, 10);

        if ("meal".equals(type)) {
            addText(c, obj.optString("foods", ""), 15, INK, true);
            String portion = obj.optString("portion", "");
            if (!portion.isEmpty()) addText(c, "Порция: " + portion, 12, MUTED, false);
        } else if (obj.optBoolean("control", false)) {
            addText(c, "Заметных симптомов нет", 14, GREEN_DARK, true);
        } else {
            addText(c, "Вздутие " + obj.optInt("bloating", 0) + "/10   ·   Газы " + obj.optInt("gas", 0) + "/10", 14, INK, true);
            addText(c, "Боль " + obj.optInt("pain", 0) + "/10   ·   Стресс " + obj.optInt("stress", 0) + "/10", 12, MUTED, false);
            String stool = obj.optString("stool", "");
            if (!stool.isEmpty() && !"Стул не отмечаю".equals(stool)) addText(c, stool, 12, MUTED, false);
        }
        String note = obj.optString("note", "");
        if (!note.isEmpty()) {
            addGap(c, 4);
            addText(c, note, 12, MUTED, false);
        }
        addCard(parent, c);
    }

    private void addEmptyState(LinearLayout parent, String title, String message) {
        LinearLayout empty = card();
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setPadding(dp(20), dp(24), dp(20), dp(24));
        TextView icon = text("○", 27, GREEN, true);
        icon.setGravity(Gravity.CENTER);
        empty.addView(icon);
        addGap(empty, 5);
        TextView heading = text(title, 16, INK, true);
        heading.setGravity(Gravity.CENTER);
        empty.addView(heading);
        addGap(empty, 6);
        TextView body = text(message, 12, MUTED, false);
        body.setGravity(Gravity.CENTER);
        empty.addView(body);
        addCard(parent, empty);
    }

    private LinearLayout metric(String number, String caption) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(3), dp(2), dp(3), dp(2));
        box.addView(text(number, 20, GREEN_DARK, true));
        box.addView(text(caption, 10, MUTED, false));
        return box;
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

    private void addGap(LinearLayout parent, int dpValue) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(dpValue)));
    }

    private void addText(LinearLayout parent, String value, float size, int color, boolean bold) {
        parent.addView(text(value, size, color, bold));
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView button(String label, int fill, int foreground, boolean outlined) {
        TextView b = text(label, 13, foreground, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.setMinHeight(dp(46));
        b.setMaxLines(2);
        b.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= 26) {
            b.setAutoSizeTextTypeUniformWithConfiguration(11, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        }
        b.setBackground(round(fill, outlined ? LINE : fill, 13));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        return v;
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

    private void loadEntries() {
        String raw = preferences.getString(KEY_ENTRIES, "[]");
        try { entries = new JSONArray(raw); }
        catch (JSONException e) { entries = new JSONArray(); }
    }

    private void persistEntries() {
        preferences.edit().putString(KEY_ENTRIES, entries.toString()).apply();
    }

    private List<JSONObject> sortedEntries() {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject obj = entries.optJSONObject(i);
            if (obj != null) list.add(obj);
        }
        Collections.sort(list, new Comparator<JSONObject>() {
            @Override public int compare(JSONObject a, JSONObject b) {
                return Long.compare(b.optLong("ts", 0L), a.optLong("ts", 0L));
            }
        });
        return list;
    }

    private Set<String> foodsInWindow(long symptomTime, int hours) {
        Set<String> result = new HashSet<>();
        long start = symptomTime - hours * 60L * 60L * 1000L;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject obj = entries.optJSONObject(i);
            if (obj == null || !"meal".equals(obj.optString("type"))) continue;
            long time = obj.optLong("ts", 0L);
            if (time < start || time > symptomTime) continue;
            String raw = obj.optString("foods", "").replace('\n', ',').replace(';', ',');
            String[] parts = raw.split(",");
            for (String part : parts) {
                String clean = part.trim().toLowerCase(RU).replaceAll("\\s+", " ");
                clean = clean.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
                if (clean.length() >= 2) result.add(clean);
            }
        }
        return result;
    }

    private String titleCase(String value) {
        if (value == null || value.length() == 0) return "";
        return value.substring(0, 1).toUpperCase(RU) + value.substring(1);
    }

    private String formatDateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        return new SimpleDateFormat("EEE, d MMM · HH:mm", RU).format(timestamp);
    }

    private String formatShortDateTime(long timestamp) {
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        return new SimpleDateFormat("d MMM · HH:mm", RU).format(timestamp);
    }

    private String formatDay(long timestamp) {
        return new SimpleDateFormat("EEEE, d MMMM", RU).format(timestamp);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private void confirmDelete(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить запись?")
                .setMessage("Эту запись нельзя будет восстановить.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    JSONArray kept = new JSONArray();
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject obj = entries.optJSONObject(i);
                        if (obj != null && !id.equals(obj.optString("id", ""))) kept.put(obj);
                    }
                    entries = kept;
                    persistEntries();
                    render();
                    Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmClearAll() {
        if (entries.length() == 0) {
            Toast.makeText(this, "Дневник уже пуст", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Удалить весь дневник?")
                .setMessage("Все записи на этом устройстве будут удалены без возможности восстановления.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить всё", (dialog, which) -> {
                    entries = new JSONArray();
                    persistEntries();
                    render();
                    Toast.makeText(this, "Дневник очищен", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void exportCsv() {
        if (entries.length() == 0) {
            Toast.makeText(this, "Нет записей для экспорта", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "food-symptom-diary.csv");
        try { startActivityForResult(intent, REQUEST_EXPORT); }
        catch (Exception e) { Toast.makeText(this, "Не удалось открыть выбор файла", Toast.LENGTH_LONG).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        // Поток открывается первым: если файл недоступен, openOutputStream
        // вернёт null, и без проверки конструктор OutputStreamWriter упал бы
        // с NullPointerException вместо понятного сообщения.
        OutputStream out = getContentResolver().openOutputStream(uri);
        if (out == null) {
            Toast.makeText(this, "Не удалось открыть файл для записи", Toast.LENGTH_LONG).show();
            return;
        }
        try (OutputStream stream = out;
             OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writer.write("дата;тип;приём пищи;продукты;порция;вздутие_0-10;газы_0-10;боль_0-10;стул;стресс_0-10;комментарий\r\n");
            for (JSONObject obj : sortedEntries()) {
                boolean meal = "meal".equals(obj.optString("type"));
                String[] cells = new String[]{
                        formatDateTime(obj.optLong("ts", 0L)),
                        meal ? "еда" : (obj.optBoolean("control", false) ? "контроль" : "симптом"),
                        meal ? obj.optString("mealType", "") : "",
                        meal ? obj.optString("foods", "") : "",
                        meal ? obj.optString("portion", "") : "",
                        meal ? "" : String.valueOf(obj.optInt("bloating", 0)),
                        meal ? "" : String.valueOf(obj.optInt("gas", 0)),
                        meal ? "" : String.valueOf(obj.optInt("pain", 0)),
                        meal ? "" : obj.optString("stool", ""),
                        meal ? "" : String.valueOf(obj.optInt("stress", 0)),
                        obj.optString("note", "")
                };
                for (int i = 0; i < cells.length; i++) {
                    if (i > 0) writer.write(';');
                    writer.write(csvCell(cells[i]));
                }
                writer.write("\r\n");
            }
            writer.flush();
            Toast.makeText(this, "CSV-файл сохранён", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csvCell(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    @Override
    public void onBackPressed() {
        if (!"home".equals(screen)) {
            screen = "home";
            render();
        } else {
            super.onBackPressed();
        }
    }
}
