package net.flamily.irs.robot;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

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

        if (mCaptureImageReceiver != null)
            sendIntent(INTENT_CAPTURE_IMAGE);
    }

    /**
     * Register ImageCapture broadcast receiver
     *
     * @param context current context you are workign on
     */
    public void registerImageBroadCastReceiver(Context context) {
        mCaptureImageReceiver = new CaptureImageReceiver();
        //Assign this
        //Declare the cb interface static in your activity
        CaptureImageReceiver.ICaptureImageReceiver iCaptureImageReceiver = this;
        mCaptureImageReceiver.registerCallback(iCaptureImageReceiver);

        IntentFilter filter = new IntentFilter(INTENT_CAPTURE_IMAGE);
        context.registerReceiver(mCaptureImageReceiver, filter);
    }

    private void sendIntent(String intent_action) {
        Log.e(TAG, "Sending Intent");
        Intent intent = new Intent(intent_action);
        intent.setAction(intent_action);
        ref.get().getContext().sendBroadcast(intent);
    }

    /**
     * Unregisters broadcast receivers
     *
     * @param context current context you are working on
     */
    public void unregisterBroadCastReceiver(Context context) {
        Log.e(TAG, "unregisterBroadCastReceiver");
        try {
            context.unregisterReceiver(mCaptureImageReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered");
        }

    }

    @Override
    public void sendImage(final byte[] data, final boolean success) {
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }

        if (success) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    // do something with those bytes now
                    if (data.length > 0) {
                        webView.loadUrl("javascript:irs_raw.photoSuccess('received bytes')");
                    }
                }
            });
        } else {
            Toast.makeText(webView.getContext(), "Unable to take image", Toast.LENGTH_LONG).show();
        }

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
