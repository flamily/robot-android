package net.flamily.irs.robot;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;

public class PlatformAbstraction {

    private final WeakReference<WebView> ref;

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

        //get the photo
        final String photo = "bingo";

        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:irs_raw.photoSuccess('" + photo + "')");
            }
        });


    }

    @JavascriptInterface
    public void listen(String phrases) throws JSONException {
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
