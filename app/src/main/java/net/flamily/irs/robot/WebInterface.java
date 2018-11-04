package net.flamily.irs.robot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.conscrypt.Conscrypt;

import java.lang.ref.WeakReference;
import java.security.Security;

public class WebInterface extends Activity {
    private WebView webView;

    //private static final int MY_PERMISSIONS_REQUEST_CAMERA = 101;
    private String TAG = "WebInterface";

    private PlatformAbstraction mPlatformAbstraction;

    //Lifecycle

    static {
        Security.insertProviderAt(Conscrypt.newProvider("GmsCore_OpenSSL"), 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_interface);
        buildWebView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        if (mPlatformAbstraction != null) {
            mPlatformAbstraction.registerImageBroadCastReceiver(this);
            mPlatformAbstraction.registerSpeech(this);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        if (mPlatformAbstraction != null) {
            mPlatformAbstraction.unregisterBroadCastReceiver(this);
            mPlatformAbstraction.disableSpeech();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        if (mPlatformAbstraction != null) {
            mPlatformAbstraction.unregisterBroadCastReceiver(this);
            mPlatformAbstraction.disableSpeech();
            mPlatformAbstraction.stopVoiceRecorder(this);
        }
    }

    @Override
    protected void onDestroy() {
        //Gotta kill the web view dead otherwise the garbage collector doesn't come
        if (webView != null) {
            ViewGroup vg = (ViewGroup) findViewById(R.id.web_view_container);
            if (vg != null) {
                vg.removeView(webView);
            }
            webView.destroy();
            webView = null;
        }

        if (mPlatformAbstraction != null) {
            mPlatformAbstraction.unregisterBroadCastReceiver(this);
            mPlatformAbstraction.disableSpeech();
            mPlatformAbstraction.stopVoiceRecorder(this);
        }

        super.onDestroy();
    }

    //Rotation

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            if (mPlatformAbstraction != null)
                mPlatformAbstraction.unregisterBroadCastReceiver(this);
            ViewGroup vg = (ViewGroup) findViewById(R.id.web_view_container);
            vg.removeView(webView);
        }
        super.onConfigurationChanged(newConfig); // Do the rotate
        setContentView(R.layout.web_interface); // Re-inflate another of the same view
        buildWebView(); // Recombobulate the web view.
        if (mPlatformAbstraction != null)
            mPlatformAbstraction.registerImageBroadCastReceiver(this);
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
        Log.e(TAG, "buildWebView");
        if (webView == null) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

            // Thinking 'dafuq?' Check this out: https://developer.android.com/guide/webapps/webview

            webView = new WebView(this);
            WeakReference<WebView> ref = new WeakReference<WebView>(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setSupportMultipleWindows(true);
            mPlatformAbstraction = new PlatformAbstraction(ref);
            webView.addJavascriptInterface(mPlatformAbstraction, "irs_raw");
            webView.loadUrl("file:///android_asset/index.html");
        }
        if (webView.getParent() == null) {
            //Add the web view to the view hierarchy. Sneaky this way so we can rotate later
            ViewGroup vg = (ViewGroup) findViewById(R.id.web_view_container);
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
