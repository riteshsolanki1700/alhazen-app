package com.ziyaanpatel.alhazen;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Wraps the Alhazen web app (assets/index.html) in a full-screen, offline WebView. */
public class MainActivity extends Activity {
    private static final String HOME = "https://appassets.androidplatform.net/assets/index.html";
    private static final int PICK = 42;
    private WebView web;
    private ValueCallback<Uri[]> pending;

    // Makes <a download> (Save diagram, Backup) write real files into Downloads.
    private static final String HOOK =
        "(function(){if(window.__alhHook)return;window.__alhHook=1;var c=HTMLAnchorElement.prototype.click;" +
        "HTMLAnchorElement.prototype.click=function(){var h=this.href,n=this.download;" +
        "if(n&&h&&(h.indexOf('blob:')==0||h.indexOf('data:')==0)){fetch(h).then(function(r){return r.blob()})" +
        ".then(function(b){var f=new FileReader();f.onload=function(){var s=f.result;" +
        "AlhazenApp.save(n,s.substring(s.indexOf(',')+1),b.type||'application/octet-stream')};f.readAsDataURL(b)});return}" +
        "return c.call(this)}})();";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);      // localStorage: progress, theme, login
        s.setDatabaseEnabled(true);        // IndexedDB: Ray Silo diagrams
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);

        // Serve the assets folder from a secure https origin so storage and IndexedDB work.
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();

        web.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }
            @Override
            public void onPageFinished(WebView v, String url) { v.evaluateJavascript(HOOK, null); }
        });

        // File chooser for Ray Silo "Camera" and "Upload" buttons. alert()/confirm() use default dialogs.
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pending != null) pending.onReceiveValue(null);
                pending = cb;
                try {
                    Intent i = p.createIntent();
                    if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE)
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(i, PICK);
                } catch (Exception e) { pending = null; return false; }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AlhazenApp");
        if (state != null) web.restoreState(state); else web.loadUrl(HOME);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && pending != null) {
            Uri[] out = null;
            if (res == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    out = new Uri[n];
                    for (int k = 0; k < n; k++) out[k] = data.getClipData().getItemAt(k).getUri();
                } else if (data.getData() != null) out = new Uri[]{ data.getData() };
            }
            pending.onReceiveValue(out);
            pending = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); web.saveState(b); }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void save(String name, String b64, String mime) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(bytes); }
                } else {
                    File f = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
                    try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                }
                toast("Saved to Downloads: " + name);
            } catch (Exception e) { toast("Could not save file"); }
        }
    }

    private void toast(final String m) {
        runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
    }
}
