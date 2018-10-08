package net.flamily.irs.robot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class WebInterface extends Activity {
    private WebView webView;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 101;
    private String TAG = "WebInterface";

    private PlatformAbstraction mPlatformAbstraction;

    //Lifecycle

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_interface);
        buildWebView();
        checkPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        if (mPlatformAbstraction != null)
            mPlatformAbstraction.registerImageBroadCastReceiver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        if (mPlatformAbstraction != null)
            mPlatformAbstraction.unregisterBroadCastReceiver(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
        if (mPlatformAbstraction != null)
            mPlatformAbstraction.unregisterBroadCastReceiver(this);
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

        //TODO: unregister receiver
        if (mPlatformAbstraction != null)
            mPlatformAbstraction.unregisterBroadCastReceiver(this);

        super.onDestroy();
    }

    //Rotation

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            if (mPlatformAbstraction != null)
                mPlatformAbstraction.unregisterBroadCastReceiver(this);
            ViewGroup vg = findViewById(R.id.web_view_container);
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
            WeakReference<WebView> ref = new WeakReference<>(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setSupportMultipleWindows(true);
            mPlatformAbstraction = new PlatformAbstraction(ref);
            webView.addJavascriptInterface(mPlatformAbstraction, "irs_raw");
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


    private void checkPermissions() {
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.KITKAT) {
            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission();
            }
        }
    }

    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permissions required: quitting app", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    ////TODO: regex + match words
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    final ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    webView.post(new Runnable() {
                        @Override
                        public void run() {
                            // do something with those bytes now
                            webView.loadUrl("javascript:irs_raw.phraseSuccess('" + result.get(0) + "')");
                        }
                    });
                }
                break;
        }
    }

}
