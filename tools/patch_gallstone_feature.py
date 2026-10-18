#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Добавляет в MainActivity учёт симптомов желчного пузыря."""
from pathlib import Path

P = Path(__file__).resolve().parents[1] / "app/src/main/java/by/arena/fooddiary/MainActivity.java"
s = P.read_text(encoding="utf-8")


def rep(old, new, count=1):
    global s
    assert s.count(old) >= 1, "НЕ НАЙДЕНО:\n" + old[:200]
    s = s.replace(old, new, count)


# --- 1. Константы -----------------------------------------------------------
rep(
    """    private static final int ANALYSIS_DAYS = 14;
    private static final int MATCH_WINDOW_HOURS = 12;""",
    """    private static final int ANALYSIS_DAYS = 14;
    private static final int MATCH_WINDOW_HOURS = 12;
    // Окно и порог для желчнокаменной части дневника: технические настройки,
    // а не медицинские нормативы.
    private static final int GALL_WINDOW_HOURS = 6;
    private static final int GALL_PAIN_THRESHOLD = 4;
    // Признаки жирной пищи: в анализе такие продукты помечаются отдельно.
    private static final String[] FATTY_HINTS = {
            "масло", "жирн", "сало", "бекон", "сыр", "сметан", "слив", "жарен",
            "свинин", "баранин", "утин", "гусин", "печен", "яйц", "творог",
            "майонез", "колбас", "сосиск", "ветчин", "холодец", "тушен", "пицц",
            "бургер", "картофел", "орех", "арахис", "авокадо", "сельд",
            "скумбри", "консерв", "сливочн", "топлен", "подсолнечн", "оливков"
    };""",
)

# --- 2. Поле режима вместо булева флага -------------------------------------
rep(
    """    private String screen = "home";
    private boolean symptomMode = false;""",
    """    private String screen = "home";
    // Режим экрана записи: "meal" — еда, "symptom" — самочувствие, "gall" — желчный пузырь.
    private String entryMode = MODE_MEAL;""",
)
rep(
    """    private static final Locale RU = new Locale("ru", "RU");""",
    """    private static final Locale RU = new Locale("ru", "RU");

    private static final String MODE_MEAL = "meal";
    private static final String MODE_SYMPTOM = "symptom";
    private static final String MODE_GALL = "gall";""",
)

# --- 3. Поля ввода для желчного режима -------------------------------------
rep(
    """    private SeekBar stressBar;
    private CheckBox controlCheck;""",
    """    private SeekBar stressBar;
    private CheckBox controlCheck;
    private SeekBar gallPainBar;
    private SeekBar gallNauseaBar;
    private SeekBar gallBloatingBar;
    private CheckBox gallAfterMealCheck;
    private CheckBox gallFattyCheck;
    private CheckBox gallJaundiceCheck;
    private CheckBox gallFeverCheck;
    private CheckBox gallControlCheck;""",
)

# --- 4. Черновик формы для желчного режима ---------------------------------
rep(
    """    private int draftStress = 0;
    private boolean draftControl = false;""",
    """    private int draftStress = 0;
    private boolean draftControl = false;
    private int draftGallPain = 0;
    private int draftGallNausea = 0;
    private int draftGallBloating = 0;
    private boolean draftGallAfterMeal = false;
    private boolean draftGallFatty = false;
    private boolean draftGallJaundice = false;
    private boolean draftGallFever = false;
    private boolean draftGallControl = false;
    private String draftGallNote = "";""",
)

# --- 5. Статистика «Сегодня» -----------------------------------------------
rep(
    """        int mealsToday = 0;
        int symptomNotesToday = 0;
        int maxBloating = 0;""",
    """        int mealsToday = 0;
        int symptomNotesToday = 0;
        int maxBloating = 0;
        int gallAttacksToday = 0;""",
)
rep(
    """            if ("meal".equals(obj.optString("type"))) mealsToday++;
            else {
                symptomNotesToday++;
                maxBloating = Math.max(maxBloating, obj.optInt("bloating", 0));
            }""",
    """            String type = obj.optString("type");
            if (MODE_MEAL.equals(type)) {
                mealsToday++;
            } else if (MODE_GALL.equals(type)) {
                if (!obj.optBoolean("control", false) && obj.optInt("pain", 0) >= GALL_PAIN_THRESHOLD) {
                    gallAttacksToday++;
                }
            } else {
                symptomNotesToday++;
                maxBloating = Math.max(maxBloating, obj.optInt("bloating", 0));
            }""",
)
rep(
    """        stats.addView(metric(symptomNotesToday == 0 ? "—" : maxBloating + "/10", "макс. вздутие"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f));""",
    """        stats.addView(metric(symptomNotesToday == 0 ? "—" : maxBloating + "/10", "макс. вздутие"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f));
        stats.addView(metric("" + gallAttacksToday, "приступов"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.95f));""",
)

# --- 6. Вкладки выбора типа записи -----------------------------------------
rep(
    """        TextView mealTab = button("Еда", symptomMode ? SURFACE : GREEN_SOFT, symptomMode ? MUTED : GREEN_DARK, !symptomMode);
        TextView symptomTab = button("Симптомы", symptomMode ? GREEN_SOFT : SURFACE, symptomMode ? GREEN_DARK : MUTED, symptomMode);
        LinearLayout.LayoutParams tab1 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tab1.setMargins(0, 0, dp(8), 0);
        typeRow.addView(mealTab, tab1);
        typeRow.addView(symptomTab, new LinearLayout.LayoutParams(0, dp(46), 1f));
        mealTab.setOnClickListener(v -> { if (symptomMode) { stashForm(); symptomMode = false; render(); } });
        symptomTab.setOnClickListener(v -> { if (!symptomMode) { stashForm(); symptomMode = true; render(); } });""",
    """        TextView mealTab = button("Еда", MODE_MEAL.equals(entryMode) ? GREEN_SOFT : SURFACE, MODE_MEAL.equals(entryMode) ? GREEN_DARK : MUTED, !MODE_MEAL.equals(entryMode));
        TextView symptomTab = button("Симптомы", MODE_SYMPTOM.equals(entryMode) ? GREEN_SOFT : SURFACE, MODE_SYMPTOM.equals(entryMode) ? GREEN_DARK : MUTED, !MODE_SYMPTOM.equals(entryMode));
        TextView gallTab = button("Желчный", MODE_GALL.equals(entryMode) ? GREEN_SOFT : SURFACE, MODE_GALL.equals(entryMode) ? GREEN_DARK : MUTED, !MODE_GALL.equals(entryMode));
        LinearLayout.LayoutParams tab1 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tab1.setMargins(0, 0, dp(6), 0);
        typeRow.addView(mealTab, tab1);
        LinearLayout.LayoutParams tab2 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tab2.setMargins(0, 0, dp(6), 0);
        typeRow.addView(symptomTab, tab2);
        typeRow.addView(gallTab, new LinearLayout.LayoutParams(0, dp(46), 1f));
        mealTab.setOnClickListener(v -> { if (!MODE_MEAL.equals(entryMode)) { stashForm(); entryMode = MODE_MEAL; render(); } });
        symptomTab.setOnClickListener(v -> { if (!MODE_SYMPTOM.equals(entryMode)) { stashForm(); entryMode = MODE_SYMPTOM; render(); } });
        gallTab.setOnClickListener(v -> { if (!MODE_GALL.equals(entryMode)) { stashForm(); entryMode = MODE_GALL; render(); } });""",
)

# --- 7. Тело формы: режим еды / самочувствия / желчного ---------------------
rep(
    """        if (!symptomMode) {
            addFieldLabel(page, "Тип приёма пищи");""",
    """        if (MODE_MEAL.equals(entryMode)) {
            addFieldLabel(page, "Тип приёма пищи");""",
)
rep(
    """            if (draftControl) controlCheck.setChecked(true);
        }""",
    """            if (draftControl) controlCheck.setChecked(true);
        } else {
            addText(page, "Боль в правом подреберье", 16, INK, true);
            addText(page, "Отметьте, что было и когда это было. 0 — нет, 10 — максимально выражено.", 12, MUTED, false);
            addGap(page, 12);

            gallPainBar = addSlider(page, "Боль справа под рёбрами", draftGallPain);
            gallNauseaBar = addSlider(page, "Тошнота", draftGallNausea);
            gallBloatingBar = addSlider(page, "Вздутие и газы", draftGallBloating);
            addGap(page, 4);

            gallAfterMealCheck = check("После еды (в первые 6 часов)", draftGallAfterMeal);
            page.addView(gallAfterMealCheck);
            gallFattyCheck = check("После жирной пищи", draftGallFatty);
            page.addView(gallFattyCheck);
            gallJaundiceCheck = check("Желтуха, тёмная моча или светлый стул", draftGallJaundice);
            page.addView(gallJaundiceCheck);
            gallFeverCheck = check("Жар или озноб", draftGallFever);
            page.addView(gallFeverCheck);
            addGap(page, 6);

            gallControlCheck = check("Контрольная отметка: симптомов не было", false);
            page.addView(gallControlCheck);
            gallControlCheck.setOnCheckedChangeListener((buttonView, checked) -> {
                if (checked) {
                    gallPainBar.setProgress(0);
                    gallNauseaBar.setProgress(0);
                    gallBloatingBar.setProgress(0);
                }
                gallPainBar.setEnabled(!checked);
                gallNauseaBar.setEnabled(!checked);
                gallBloatingBar.setEnabled(!checked);
            });
            if (draftGallControl) gallControlCheck.setChecked(true);
            addGap(page, 6);

            addFieldLabel(page, "Комментарий (необязательно)");
            noteField = input("Например: после жареного мяса, отдавало в правую лопатку…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
            if (!draftGallNote.isEmpty()) noteField.setText(draftGallNote);
            noteField.setGravity(Gravity.TOP | Gravity.START);
            page.addView(noteField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(82)));
            addGap(page, 10);
            addText(page, "Желтуха, жар или боль, которая не проходит дольше 4–6 часов, — повод обратиться за медицинской помощью, а не ждать.", 11, RED, false);
            addGap(page, 8);
        }""",
)

# --- 8. Кнопка сохранения ---------------------------------------------------
rep(
    """        save.setOnClickListener(v -> {
            if (symptomMode) saveSymptom();
            else saveMeal();
        });""",
    """        save.setOnClickListener(v -> {
            if (MODE_SYMPTOM.equals(entryMode)) saveSymptom();
            else if (MODE_GALL.equals(entryMode)) saveGall();
            else saveMeal();
        });""",
)

# --- 9. openAdd -------------------------------------------------------------
rep(
    """    private void openAdd(boolean forSymptom) {
        screen = "add";
        symptomMode = forSymptom;""",
    """    private void openAdd(String mode) {
        screen = "add";
        entryMode = mode == null ? MODE_MEAL : mode;""",
)
rep("""        addMeal.setOnClickListener(v -> openAdd(false));
        addSymptom.setOnClickListener(v -> openAdd(true));""",
    """        addMeal.setOnClickListener(v -> openAdd(MODE_MEAL));
        addSymptom.setOnClickListener(v -> openAdd(MODE_SYMPTOM));""")
rep("""        add.setOnClickListener(v -> openAdd(true));""",
    """        add.setOnClickListener(v -> openAdd(MODE_SYMPTOM));""")
rep("""                    if (!"add".equals(screen)) openAdd(false);""",
    """                    if (!"add".equals(screen)) openAdd(MODE_MEAL);""")

# --- 10. Сохранение записи о желчном ---------------------------------------
rep(
    """    private void chooseDateTime() {""",
    """    private void saveGall() {
        boolean control = gallControlCheck != null && gallControlCheck.isChecked();
        int pain = control || gallPainBar == null ? 0 : gallPainBar.getProgress();
        int nausea = control || gallNauseaBar == null ? 0 : gallNauseaBar.getProgress();
        int bloating = control || gallBloatingBar == null ? 0 : gallBloatingBar.getProgress();
        boolean jaundice = !control && gallJaundiceCheck != null && gallJaundiceCheck.isChecked();
        boolean fever = !control && gallFeverCheck != null && gallFeverCheck.isChecked();
        if (!control && pain == 0 && nausea == 0 && bloating == 0 && !jaundice && !fever) {
            Toast.makeText(this, "Отметьте боль, тошноту или другой признак — либо сохраните контрольную отметку", Toast.LENGTH_LONG).show();
            return;
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", UUID.randomUUID().toString());
            obj.put("type", MODE_GALL);
            obj.put("ts", selectedTime);
            obj.put("control", control);
            obj.put("pain", pain);
            obj.put("nausea", nausea);
            obj.put("bloating", bloating);
            obj.put("afterMeal", !control && gallAfterMealCheck != null && gallAfterMealCheck.isChecked());
            obj.put("fatty", !control && gallFattyCheck != null && gallFattyCheck.isChecked());
            obj.put("jaundice", jaundice);
            obj.put("fever", fever);
            obj.put("note", noteField == null ? "" : noteField.getText().toString().trim());
            entries.put(obj);
            persistEntries();
            hideKeyboard();
            clearDraft();
            screen = "home";
            render();
            Toast.makeText(this, control ? "Контрольная отметка сохранена" : "Запись о желчном сохранена", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Не удалось сохранить запись", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseDateTime() {""",
)

# --- 11. Черновик формы: желчный режим -------------------------------------
rep(
    """        if (noteField != null) {
            if (symptomMode) draftSymptomNote = noteField.getText().toString();
            else draftMealNote = noteField.getText().toString();
        }
    }""",
    """        if (noteField != null) {
            if (MODE_SYMPTOM.equals(entryMode)) draftSymptomNote = noteField.getText().toString();
            else if (MODE_GALL.equals(entryMode)) draftGallNote = noteField.getText().toString();
            else draftMealNote = noteField.getText().toString();
        }
        if (gallPainBar != null) draftGallPain = gallPainBar.getProgress();
        if (gallNauseaBar != null) draftGallNausea = gallNauseaBar.getProgress();
        if (gallBloatingBar != null) draftGallBloating = gallBloatingBar.getProgress();
        if (gallAfterMealCheck != null) draftGallAfterMeal = gallAfterMealCheck.isChecked();
        if (gallFattyCheck != null) draftGallFatty = gallFattyCheck.isChecked();
        if (gallJaundiceCheck != null) draftGallJaundice = gallJaundiceCheck.isChecked();
        if (gallFeverCheck != null) draftGallFever = gallFeverCheck.isChecked();
        if (gallControlCheck != null) draftGallControl = gallControlCheck.isChecked();
    }""",
)
rep(
    """        draftStress = 0;
        draftControl = false;
    }""",
    """        draftStress = 0;
        draftControl = false;
        draftGallPain = 0;
        draftGallNausea = 0;
        draftGallBloating = 0;
        draftGallAfterMeal = false;
        draftGallFatty = false;
        draftGallJaundice = false;
        draftGallFever = false;
        draftGallControl = false;
        draftGallNote = "";
    }""",
)

# --- 12. Карточка записи в истории -----------------------------------------
rep(
    """        String label = "meal".equals(type) ? "ЕДА · " + obj.optString("mealType", "Приём пищи").toUpperCase(RU) : (obj.optBoolean("control", false) ? "КОНТРОЛЬНАЯ ОТМЕТКА" : "САМОЧУВСТВИЕ");""",
    """        String label;
        if (MODE_MEAL.equals(type)) {
            label = "ЕДА · " + obj.optString("mealType", "Приём пищи").toUpperCase(RU);
        } else if (MODE_GALL.equals(type)) {
            label = obj.optBoolean("control", false) ? "ЖЕЛЧНЫЙ · КОНТРОЛЬ" : "ЖЕЛЧНЫЙ";
        } else {
            label = obj.optBoolean("control", false) ? "КОНТРОЛЬНАЯ ОТМЕТКА" : "САМОЧУВСТВИЕ";
        }""",
)
rep(
    """        if ("meal".equals(type)) {
            addText(c, obj.optString("foods", ""), 15, INK, true);
            String portion = obj.optString("portion", "");
            if (!portion.isEmpty()) addText(c, "Порция: " + portion, 12, MUTED, false);
        } else if (obj.optBoolean("control", false)) {""",
    """        if (MODE_MEAL.equals(type)) {
            addText(c, obj.optString("foods", ""), 15, INK, true);
            String portion = obj.optString("portion", "");
            if (!portion.isEmpty()) addText(c, "Порция: " + portion, 12, MUTED, false);
        } else if (MODE_GALL.equals(type)) {
            if (obj.optBoolean("control", false)) {
                addText(c, "Желчный: симптомов не было", 14, GREEN_DARK, true);
            } else {
                addText(c, "Боль справа " + obj.optInt("pain", 0) + "/10"
                        + (obj.optInt("nausea", 0) > 0 ? "   ·   Тошнота " + obj.optInt("nausea", 0) + "/10" : ""), 14, INK, true);
                StringBuilder flags = new StringBuilder();
                if (obj.optBoolean("afterMeal", false)) flags.append("После еды");
                if (obj.optBoolean("fatty", false)) flags.append(flags.length() > 0 ? " · После жирной пищи" : "После жирной пищи");
                if (obj.optInt("bloating", 0) > 0) flags.append(flags.length() > 0 ? " · Вздутие " + obj.optInt("bloating", 0) + "/10" : "Вздутие " + obj.optInt("bloating", 0) + "/10");
                if (obj.optBoolean("jaundice", false)) flags.append(flags.length() > 0 ? " · Желтуха" : "Желтуха");
                if (obj.optBoolean("fever", false)) flags.append(flags.length() > 0 ? " · Жар" : "Жар");
                if (flags.length() > 0) addText(c, flags.toString(), 12, MUTED, false);
            }
        } else if (obj.optBoolean("control", false)) {""",
)

# --- 13. Вспомогательный чекбокс --------------------------------------------
rep(
    """    private void addFieldLabel(LinearLayout parent, String title) {""",
    """    private CheckBox check(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(INK);
        box.setTextSize(13);
        box.setButtonTintList(ColorStateList.valueOf(GREEN));
        box.setPadding(0, dp(5), 0, dp(5));
        box.setChecked(checked);
        return box;
    }

    private void addFieldLabel(LinearLayout parent, String title) {""",
)

# --- 14. Раздел анализа по желчному ----------------------------------------
rep(
    """        addGap(page, 15);
        TextView export = button("Экспортировать дневник в CSV", Color.WHITE, GREEN_DARK, true);""",
    """        renderGallAnalysis(page);

        addGap(page, 15);
        TextView export = button("Экспортировать дневник в CSV", Color.WHITE, GREEN_DARK, true);""",
)

rep(
    """    private void renderHistory(LinearLayout page) {""",
    """    /** Анализ по желчнокаменной симптоматике: что ели за 6 часов до приступа. */
    private void renderGallAnalysis(LinearLayout page) {
        long cutoff = System.currentTimeMillis() - ANALYSIS_DAYS * DAY_MS;
        int notes = 0;
        int attacks = 0;
        int controls = 0;
        for (JSONObject obj : sortedEntries()) {
            if (obj.optLong("ts", 0L) < cutoff) continue;
            if (!MODE_GALL.equals(obj.optString("type"))) continue;
            notes++;
            if (obj.optBoolean("control", false)) {
                controls++;
            } else if (obj.optInt("pain", 0) >= GALL_PAIN_THRESHOLD) {
                attacks++;
            }
        }

        addGap(page, 17);
        addText(page, "ЖЕЛЧНЫЙ ПУЗЫРЬ", 10, GREEN, true);
        addGap(page, 7);
        addText(page, "Что ели перед приступом справа", 17, INK, true);
        addText(page, "Считаются продукты из записей о еде за " + GALL_WINDOW_HOURS + " часов до отметки с болью справа "
                + GALL_PAIN_THRESHOLD + "/10 и выше. Контрольные отметки без симптомов показаны отдельно.", 12, MUTED, false);
        addGap(page, 9);

        LinearLayout summary = card();
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(metric("" + notes, "отметок"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summary.addView(metric("" + attacks, "приступов"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summary.addView(metric("" + controls, "контрольных"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        addCard(page, summary);
        addGap(page, 12);

        Map<String, int[]> matches = new HashMap<>();
        for (JSONObject obj : sortedEntries()) {
            long ts = obj.optLong("ts", 0L);
            if (ts < cutoff || !MODE_GALL.equals(obj.optString("type"))) continue;
            boolean control = obj.optBoolean("control", false);
            boolean attack = !control && obj.optInt("pain", 0) >= GALL_PAIN_THRESHOLD;
            if (!control && !attack) continue;
            for (String food : foodsInWindow(ts, GALL_WINDOW_HOURS)) {
                int[] counts = matches.get(food);
                if (counts == null) { counts = new int[2]; matches.put(food, counts); }
                if (attack) counts[0]++;
                if (control) counts[1]++;
            }
        }

        if (matches.isEmpty()) {
            addEmptyState(page, "Пока недостаточно данных",
                    "Отметьте приступ в разделе «Запись» → «Желчный», а в спокойные дни сохраняйте контрольные отметки — без них сравнение неполное.");
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

                LinearLayout nameBox = new LinearLayout(this);
                nameBox.setOrientation(LinearLayout.HORIZONTAL);
                nameBox.setGravity(Gravity.CENTER_VERTICAL);
                nameBox.addView(text(titleCase(name), 14, INK, true),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (isFattyFood(name)) {
                    TextView tag = text("жирное", 9, GREEN_DARK, true);
                    tag.setGravity(Gravity.CENTER);
                    tag.setPadding(dp(7), dp(3), dp(7), dp(3));
                    tag.setBackground(round(GREEN_SOFT, GREEN_SOFT, 10));
                    LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tagParams.setMargins(dp(6), 0, 0, 0);
                    nameBox.addView(tag, tagParams);
                }
                row.addView(nameBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                LinearLayout numbers = new LinearLayout(this);
                numbers.setOrientation(LinearLayout.VERTICAL);
                numbers.setGravity(Gravity.RIGHT);
                numbers.addView(text("приступ: " + counts[0], 11, counts[0] > 0 ? RED : MUTED, true));
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
        addText(meaning, "Это частота совместных записей, а не причина. Желчная колика действительно часто приходит "
                + "после жирной еды, но совпадение в дневнике — повод обсудить ситуацию с врачом, а не основание "
                + "исключать продукт навсегда. Большинство людей с камнями никогда не испытывают приступов.", 12,
                Color.rgb(55, 89, 82), false);
        addGap(meaning, 8);
        addText(meaning, "Окно " + GALL_WINDOW_HOURS + " часов и порог " + GALL_PAIN_THRESHOLD + "/10 — технические настройки "
                + "дневника, а не медицинские нормативы.", 11, Color.rgb(73, 103, 96), false);
        addCard(page, meaning);

        addGap(page, 12);
        TextView add = button("＋  Отметить приступ", GREEN, Color.WHITE, false);
        page.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        add.setOnClickListener(v -> openAdd(MODE_GALL));
    }

    /** Похоже ли название продукта на жирную пищу — такие продукты помечаются в анализе. */
    private boolean isFattyFood(String name) {
        if (name == null) return false;
        String value = name.toLowerCase(RU);
        for (String hint : FATTY_HINTS) {
            if (value.contains(hint)) return true;
        }
        return false;
    }

    private void renderHistory(LinearLayout page) {""",
)

# --- 15. Экспорт CSV: новые колонки ----------------------------------------
rep(
    """            writer.write("дата;тип;приём пищи;продукты;порция;вздутие_0-10;газы_0-10;боль_0-10;стул;стресс_0-10;комментарий\\r\\n");""",
    """            writer.write("дата;тип;приём пищи;продукты;порция;вздутие_0-10;газы_0-10;боль_0-10;стул;стресс_0-10;боль_справа_0-10;тошнота_0-10;желтуха;после_жирной_еды;комментарий\\r\\n");""",
)
rep(
    """            for (JSONObject obj : sortedEntries()) {
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
                };""",
    """            for (JSONObject obj : sortedEntries()) {
                boolean meal = MODE_MEAL.equals(obj.optString("type"));
                boolean gall = MODE_GALL.equals(obj.optString("type"));
                String[] cells = new String[]{
                        formatDateTime(obj.optLong("ts", 0L)),
                        meal ? "еда" : (gall ? "желчный" : (obj.optBoolean("control", false) ? "контроль" : "симптом")),
                        meal ? obj.optString("mealType", "") : "",
                        meal ? obj.optString("foods", "") : "",
                        meal ? obj.optString("portion", "") : "",
                        (meal || gall) ? "" : String.valueOf(obj.optInt("bloating", 0)),
                        (meal || gall) ? "" : String.valueOf(obj.optInt("gas", 0)),
                        (meal || gall) ? "" : String.valueOf(obj.optInt("pain", 0)),
                        (meal || gall) ? "" : obj.optString("stool", ""),
                        (meal || gall) ? "" : String.valueOf(obj.optInt("stress", 0)),
                        gall ? String.valueOf(obj.optInt("pain", 0)) : "",
                        gall ? String.valueOf(obj.optInt("nausea", 0)) : "",
                        gall && obj.optBoolean("jaundice", false) ? "да" : "",
                        gall && obj.optBoolean("fatty", false) ? "да" : "",
                        obj.optString("note", "")
                };""",
)

P.write_text(s, encoding="utf-8")
print("MainActivity обновлён")
