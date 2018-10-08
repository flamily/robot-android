package net.flamily.irs.robot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import net.flamily.irs.robot.image_capture.CaptureImageReceiver;

import java.lang.ref.WeakReference;
import java.util.Locale;


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
     * @param context current working context
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
                        webView.loadUrl("javascript:irs_raw.photoSuccess(' | received " + data.length + "')");
                    }
                }
            });
        } else {
            webView.loadUrl("javascript:irs_raw.photoError('Unable to take image')");
        }

    }

    @JavascriptInterface
    public void listen(String phrases) {
        Log.e(TAG, "listen fun");
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }
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

        //TODO: timeout ?

        Context context = ref.get().getContext();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            ((Activity) context).startActivityForResult(intent, 10);
        } else {
            webView.loadUrl("javascript:irs_raw.phraseError('Your Device Don't Support Speech Input')");
        }

        // read all the phrases
        //regex

        //start listening for words

        //match words


    }


    @JavascriptInterface
    public void say(String phrase) {
        Log.e(TAG, "say");
        //TODO: robot says something
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
