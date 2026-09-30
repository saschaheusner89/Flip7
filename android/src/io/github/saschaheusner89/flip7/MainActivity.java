package io.github.saschaheusner89.flip7;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Flip 7 Score Tracker – schlanke WebView-Hülle um assets/index.html.
 *
 * Die Seite läuft unter https://appassets.androidplatform.net/ und wird komplett aus den
 * App-Assets geliefert (kein Internet nötig). Der feste https-Ursprung sorgt dafür, dass
 * localStorage (Spielstand, Medaillen, Statistik) dauerhaft erhalten bleibt.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final int BG = 0xFF0F0E17;

    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Display bleibt an, solange die App im Vordergrund ist (Handy liegt auf dem Tisch)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setBackgroundDrawable(new ColorDrawable(BG));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        web = new WebView(this);
        web.setBackgroundColor(BG);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage
        s.setTextZoom(100);                    // System-Schriftgröße soll das Layout nicht sprengen
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);

        web.setWebViewClient(new AssetClient());
        web.addJavascriptInterface(new NativeBridge(), "Flip7Native");   // "Teilen" für die Datensicherung
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Randlos-Darstellung (ab Android 15 Pflicht): Inhalt nicht unter Status-/Navigationsleiste.
        // Die Abstände werden hier nativ gesetzt und NICHT an die Seite weitergereicht,
        // damit CSS env(safe-area-inset-*) nicht doppelt Platz reserviert.
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (Build.VERSION.SDK_INT >= 30) return Api30.padAndConsume(v, insets);
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            }
        });

        web.loadUrl("https://" + HOST + "/index.html");
    }

    /** Zurück-Taste: erst offene Fenster in der App schließen, sonst App in den Hintergrund (Spiel bleibt). */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
                "(function(){try{return !!(window.flip7Back&&window.flip7Back())}catch(e){return false}})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String handled) {
                        if (!"true".equals(handled)) moveTaskToBack(true);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            ViewGroup parent = (ViewGroup) web.getParent();
            if (parent != null) parent.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Brücke für die Seite: Teilen-Menü (Datensicherung) und kurzes haptisches Feedback. */
    private class NativeBridge {
        /** "long" = lange gedrückt, sonst kurzer Tick (nutzt die System-Haptik, keine Berechtigung nötig). */
        @JavascriptInterface
        public void haptic(final String kind) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (web != null) web.performHapticFeedback("long".equals(kind)
                            ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.VIRTUAL_KEY);
                }
            });
        }

        @JavascriptInterface
        public void share(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_SUBJECT, "Flip 7 – Datensicherung");
                    send.putExtra(Intent.EXTRA_TEXT, text);
                    try {
                        startActivity(Intent.createChooser(send, "Sicherung teilen"));
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    /** Liefert die App-Dateien (und die Google-Fonts) aus den Assets statt aus dem Netz. */
    private class AssetClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            String host = u.getHost();
            if (host == null) return null;
            if (host.equals(HOST)) {
                String path = u.getPath();
                if (path == null || path.length() <= 1) path = "/index.html";
                WebResourceResponse r = asset(path.substring(1));
                return r != null ? r : new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                        new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
            }
            // Schriften (Syne, DM Mono) liegen in der App → sehen auch offline richtig aus
            if (host.equals("fonts.googleapis.com")) return asset("fonts/fonts.css");
            if (host.equals("fonts.gstatic.com")) return asset("fonts/" + u.getLastPathSegment());
            return null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (HOST.equals(u.getHost())) return false;
            // Externe Links im Browser öffnen statt in der App
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (Exception ignored) {
            }
            return true;
        }
    }

    private WebResourceResponse asset(String path) {
        if (path == null || path.contains("..")) return null;
        try {
            InputStream in = getAssets().open(path);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Access-Control-Allow-Origin", "*");   // Schriften werden per CORS geladen
            headers.put("Cache-Control", "no-cache");
            String mime = mime(path);
            return new WebResourceResponse(mime, mime.startsWith("text/") ? "utf-8" : null, 200, "OK", headers, in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String mime(String p) {
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".js")) return "text/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    /** API-30-Aufrufe in eigener Klasse, damit ältere Android-Versionen sie nie laden müssen. */
    private static final class Api30 {
        static WindowInsets padAndConsume(View v, WindowInsets insets) {
            android.graphics.Insets b = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsets.CONSUMED;
        }
    }
}
