package net.flamily.irs.robot;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import net.flamily.irs.robot.image_capture.CaptureImageReceiver;

import java.lang.ref.WeakReference;


public class PlatformAbstraction implements CaptureImageReceiver.ICaptureImageReceiver {

    private final WeakReference<WebView> ref;
    private String TAG = "PlatformAbstraction";

    private String INTENT_CAPTURE_IMAGE = "ImageCaptureAction";

    private CaptureImageReceiver mCaptureImageReceiver;


    public PlatformAbstraction(WeakReference<WebView> webViewReference) {
        this.ref = webViewReference;
    }

    @JavascriptInterface
    public void takePhoto() {
        /*
        Success:
        -call 'irs_raw.photoSuccess(%base64 jpeg%)'
        Error:
        -call 'irs_raw.photoError(%error%)'
        --Errors:
        ---User Errors:
        camera permission: 'permission'
        no camera hardware: 'hardware_missing'
         */
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }

        startImageCaptureBroadCastReceiver();
        sendIntent(INTENT_CAPTURE_IMAGE);
    }


    private void startImageCaptureBroadCastReceiver() {
        mCaptureImageReceiver = new CaptureImageReceiver();
        //Assign this
        //Declare the cb interface static in your activity
        CaptureImageReceiver.ICaptureImageReceiver iCaptureImageReceiver = this;
        mCaptureImageReceiver.registerCallback(iCaptureImageReceiver);

        IntentFilter filter = new IntentFilter(INTENT_CAPTURE_IMAGE);
        ref.get().getContext().registerReceiver(mCaptureImageReceiver, filter);
    }

    private void sendIntent(String intent_action) {
        Log.e(TAG, "Sending Intent");
        Intent intent = new Intent(intent_action);
        intent.setAction(intent_action);
        ref.get().getContext().sendBroadcast(intent);
    }

    //TODO: FIX the unregister
    private void unregisterBroadCastReciver(Context context) {
        context.unregisterReceiver(mCaptureImageReceiver);
    }


    @Override
    public void sendImage(final byte[] data) {
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }

        webView.post(new Runnable() {
            @Override
            public void run() {
                // do something with those bytes now
                if (data.length > 0) {
                    webView.loadUrl("javascript:irs_raw.photoSuccess('received bytes')");
                }
            }
        });

        unregisterBroadCastReciver(webView.getContext());
    }

    @JavascriptInterface
    public void listen(String phrases) {
        /*
        Success:
        -call 'irs_raw.phraseSuccess(%index%)'
        Error:
        -call 'irs_raw.phraseError(%error%)'
        --Errors:
        ---Parameter Errors:
        phrase spelling (may only contain characters [a-z], separated by commas. No capitals, no spaces, no symbols: 'spelling'
        parameter formatting error: 'parameter_format'
        ---User Errors:
        timeout reached: 'timeout'
         */


        // read all the phrases

        //regex

        //start listening for words

        //match words
    }

    @JavascriptInterface
    public void say(String phrase) {
        /*
        --Errors:
        ---Parameter Errors:
        phrase spelling (may only contain characters [a-z] or spaces. No capitals, no symbols
        parameter formatting error
         */
    }

    @JavascriptInterface
    public String identify() {
        return "robot";
    }

}
