package by.arena.fooddiary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Раздел «Помощь»: список справочных страниц, вшитых в приложение.
 *
 * Все статьи лежат в assets/help и работают без интернета. Здесь же —
 * короткая памятка о том, как вести дневник при желчнокаменной симптоматике.
 */
public class HelpActivity extends Activity {

    private static final int BG = Color.rgb(244, 246, 242);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(26, 49, 51);
    private static final int MUTED = Color.rgb(99, 116, 116);
    private static final int GREEN = Color.rgb(35, 112, 99);
    private static final int GREEN_DARK = Color.rgb(18, 63, 61);
    private static final int GREEN_SOFT = Color.rgb(231, 243, 237);
    private static final int LINE = Color.rgb(220, 229, 224);

    private static final String[][] ARTICLES = {
            {
                    "Газы и вздутие",
                    "Причины газообразования, обследование и методы лечения по рекомендациям гастроэнтерологических обществ.",
                    HelpArticleActivity.ASSET_GAS
            },
            {
                    "Камни в желчном пузыре",
                    "Симптомы желчной колики, что вызывает приступ, какие обследования нужны и когда требуется срочная помощь.",
                    HelpArticleActivity.ASSET_GALLSTONE
            }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(25));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addText(page, "СПРАВОЧНЫЕ СТРАНИЦЫ", 10, GREEN, true);
        addGap(page, 6);
        addText(page, "Помощь", 25, INK, true);
        addText(page, "Разборы, которые открываются без интернета и остаются в приложении.", 13, MUTED, false);
        addGap(page, 16);

        for (String[] article : ARTICLES) {
            addCard(page, articleCard(article[0], article[1], article[2]));
        }

        LinearLayout diary = card();
        diary.setBackground(round(GREEN_SOFT, GREEN_SOFT, 17));
        addText(diary, "Как вести дневник при желчнокаменной симптоматике", 15, GREEN_DARK, true);
        addGap(diary, 6);
        addText(diary, "В разделе «Запись» → «Желчный» отметьте боль справа под рёбрами, тошноту и вздутие, "
                + "а также связь с едой: «после еды» и «после жирной пищи». Отдельно отмечайте желтуху, "
                + "тёмную мочу или светлый стул и жар — это признаки, при которых нужна врачебная помощь. "
                + "В спокойные дни сохраняйте контрольные отметки: без них сравнение в анализе неполное.",
                12, Color.rgb(55, 89, 82), false);
        addGap(diary, 8);
        addText(diary, "На экране «Анализ» появится раздел «Желчный пузырь»: какие продукты встречались "
                + "в записях о еде за 6 часов до приступа. Это частота совместных записей, а не причина.",
                11, Color.rgb(73, 103, 96), false);
        addCard(page, diary);

        addGap(page, 8);
        addText(page, "Справочные материалы не ставят диагноз и не заменяют врача. При желтухе, жаре, "
                + "сильной боли справа или рвоте обращайтесь за медицинской помощью.", 11, MUTED, false);
    }

    private View articleCard(String title, String subtitle, final String asset) {
        LinearLayout box = card();
        addText(box, title, 17, INK, true);
        addGap(box, 6);
        addText(box, subtitle, 12, MUTED, false);
        addGap(box, 12);
        TextView open = button("Открыть", GREEN, Color.WHITE, false);
        box.addView(open, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        open.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpArticleActivity.class);
            intent.putExtra(HelpArticleActivity.EXTRA_ASSET, asset);
            intent.putExtra(HelpArticleActivity.EXTRA_TITLE, title);
            startActivity(intent);
        });
        return box;
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

        TextView title = text("Помощь", 15, INK, true);
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
