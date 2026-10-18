package by.arena.fooddiary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewTransport;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Просмотр одной справочной страницы.
 *
 * Страницы лежат в app/src/main/assets/help/ и открываются офлайн через
 * file:///android_asset/... — разрешение на интернет для этого не нужно.
 * Внешние ссылки (источники, рекомендации) открываются в системном браузере.
 */
public class HelpArticleActivity extends Activity {

    /** Имя файла статьи внутри assets/help. */
    public static final String EXTRA_ASSET = "asset";
    /** Заголовок статьи для шапки экрана. */
    public static final String EXTRA_TITLE = "title";

    public static final String ASSET_GAS = "help/gazy-i-vzdutie.html";
    public static final String ASSET_GALLSTONE = "help/kamen-v-zhelchnom-puzyre.html";

    private static final String ASSET_PREFIX = "file:///android_asset/";

    private static final int GREEN_DARK = Color.rgb(18, 63, 61);
    private static final int INK = Color.rgb(26, 49, 51);
    private static final int SURFACE = Color.WHITE;
    private static final int LINE = Color.rgb(220, 229, 224);

    private WebView webView;
    private String pageUrl = ASSET_PREFIX + ASSET_GAS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String asset = getIntent().getStringExtra(EXTRA_ASSET);
        if (asset == null || asset.trim().isEmpty()) asset = ASSET_GAS;
        pageUrl = ASSET_PREFIX + asset.trim();

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title == null || title.trim().isEmpty()) title = "Помощь";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SURFACE);
        setContentView(root);

        root.addView(buildHeader(title), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        webView.setBackgroundColor(SURFACE);
        webView.getSettings().setJavaScriptEnabled(false);
        // Страница лежит внутри приложения, поэтому файловый доступ нужен только
        // для file:///android_asset. Доступ из самой страницы к файлам и
        // кросс-доменные запросы запрещены.
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setDomStorageEnabled(false);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new HelpWebViewClient());
        webView.setWebChromeClient(new HelpChromeClient());
        root.addView(webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(pageUrl);
        }
    }

    private View buildHeader(String title) {
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
        back.setOnClickListener(v -> onBackPressed());
        header.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView titleView = text(title, 15, INK, true);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setSingleLine(true);
        titleView.setPadding(dp(12), 0, 0, 0);
        header.addView(titleView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return header;
    }

    private class HelpWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || request.getUrl() == null) return false;
            return handleUrl(request.getUrl());
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) return false;
            return handleUrl(Uri.parse(url));
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request != null && request.isForMainFrame()) {
                Toast.makeText(HelpArticleActivity.this,
                        "Не удалось открыть справочную страницу", Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Внутренние якоря остаются в WebView, внешние ссылки уходят в браузер. */
    private boolean handleUrl(Uri uri) {
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            openExternal(uri);
            return true;
        }
        if ("file".equals(scheme)) {
            return false;
        }
        return true;
    }

    /** Открывает внешнюю ссылку в системном браузере. */
    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Нет приложения для открытия ссылки", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Ссылки в справочных страницах помечены target="_blank". Без этого
     * обработчика WebView не знает, куда их открывать, и переход молча
     * игнорируется — поэтому такие ссылки уводим в системный браузер.
     */
    private class HelpChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                     Message resultMsg) {
            WebViewTransport transport = (WebViewTransport) resultMsg.obj;
            if (transport == null) return false;

            WebView probe = new WebView(HelpArticleActivity.this);
            probe.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                    if (request != null && request.getUrl() != null) {
                        openExternal(request.getUrl());
                    }
                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, String url) {
                    if (url != null) openExternal(Uri.parse(url));
                    return true;
                }
            });
            transport.setWebView(probe);
            resultMsg.sendToTarget();
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.setWebViewClient(new WebViewClient());
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
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
