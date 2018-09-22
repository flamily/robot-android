package net.flamily.irs.robot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebView;

import net.flamily.irs.robot.BuildConfig;

import java.lang.ref.WeakReference;

public class WebInterface extends Activity {
    private WebView webView;

    //Lifecycle

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_interface);
        buildWebView();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        //Gotta kill the web view dead otherwise the garbage collector doesn't come
        if (webView != null) {
            ViewGroup vg = findViewById(R.id.web_view_container);
            if (vg != null) {
                vg.removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    //Rotation

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            ViewGroup vg = findViewById(R.id.web_view_container);
            vg.removeView(webView);
        }
        super.onConfigurationChanged(newConfig); // Do the rotate
        setContentView(R.layout.web_interface); // Re-inflate another of the same view
        buildWebView(); // Recombobulate the web view.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    //WebView

    @SuppressLint("SetJavascriptEnabled")
    private void buildWebView() {
        if (webView == null) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

            // Thinking 'dafuq?' Check this out: https://developer.android.com/guide/webapps/webview

            webView = new WebView(this);
            WeakReference<WebView> ref = new WeakReference<>(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setSupportMultipleWindows(true);
            webView.addJavascriptInterface(new PlatformAbstraction(ref), "irs_raw");
            webView.loadUrl("file:///android_asset/index.html");
        }
        if (webView.getParent() == null) {
            //Add the web view to the view hierarchy. Sneaky this way so we can rotate later

            ViewGroup vg = findViewById(R.id.web_view_container);
            vg.addView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    // Back navigation

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
