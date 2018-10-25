package net.flamily.irs.robot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.robot.speech.SpeechManager;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import net.flamily.irs.robot.image_capture.CaptureImageReceiver;
import net.flamily.irs.robot.robot_listenining.SpeechService;
import net.flamily.irs.robot.robot_listenining.VoiceRecorder;

import java.lang.ref.WeakReference;

import static android.content.Context.BIND_AUTO_CREATE;


public class PlatformAbstraction implements CaptureImageReceiver.ICaptureImageReceiver, SpeechManager.OnConnectListener {

    private final WeakReference<WebView> ref;
    private String TAG = "PlatformAbstraction";

    private String INTENT_CAPTURE_IMAGE = "ImageCaptureAction";

    private CaptureImageReceiver mCaptureImageReceiver;
    private SpeechManager mSpeechManager;


    private SpeechService mSpeechService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    public PlatformAbstraction(WeakReference<WebView> webViewReference) {
        this.ref = webViewReference;
    }

    @JavascriptInterface
    public void takePhoto() {
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
     * Unregisters broadcast receivers ( Image Capture)
     *
     * @param context current context you are working on
     */
    public void unregisterBroadCastReceiver(Context context) {
        Log.e(TAG, "Unregister services");
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
                        Log.e(TAG,"LOADING");
                        webView.loadUrl("javascript:irs_raw.photoSuccess('success')");
                        String encodedImage = Base64.encodeToString(data, Base64.DEFAULT);

                        //TESTING
                        String pageData = "<img width=\"100%%25\"  src=\"data:image/jpeg;base64," + encodedImage + "\" />";
                        //String photoData = "data:image/jpeg;base64," + encodedImage;
                        webView.loadDataWithBaseURL("fake://not/needed", pageData, "text/html", null, "");
                    }
                }
            });
        } else {
            webView.loadUrl("javascript:irs_raw.photoError('Unable to take image')");
        }

    }


    @JavascriptInterface
    public void say(String phrase) {
        Log.e(TAG, "say");
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }
        Context context = ref.get().getContext();

        if (mSpeechManager == null)
        {
            Log.e(TAG, "Registering");
            registerSpeech(context);
        }
        mSpeechManager.setChatEnable(true);
        Log.e(TAG, "getChatEnable: "+mSpeechManager.getChatEnable());
        mSpeechManager.setTtsListener(new SpeechManager.TtsListener() {
            @Override
            public void onBegin(int i) {
                Log.d(TAG,"On begin talking");
            }

            @Override
            public void onError(int i) {
                Log.d(TAG,"On error");
            }

            @Override
            public void onEnd(int i) {
                Log.d(TAG,"On end talking");
            }
        });
        mSpeechManager.setTtsEnable(true);
        mSpeechManager.startSpeaking(phrase,true, true);
    }

    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            Log.d(TAG, "onVoiceStart");
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            //Log.d(TAG,"onVoice");
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            Log.d(TAG, "onVoiceEnd");
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }
    };

    @JavascriptInterface
    public String identify() {
        return "robot";
    }



    @Override
    public void onConnect(boolean status) {
        Log.d(TAG, "RobotSpeech: onConnect");
        if (status) {
            Log.d(TAG,"speechManager init success!");
            mSpeechManager.setChatEnable(true);
        } else {
            Log.d(TAG,"speechManager init fail?");
        }
    }

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    final WebView webView = ref.get();
                    if (webView == null) {
                        return;
                    }
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    } else {
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:irs_raw.phraseSuccess('" + text + "')");

                            }
                        });
                    }

                }
            };

    // For some reason it's not recognizing that service - needs to be tested
    public void registerSpeech(Context context) {
        Log.e(TAG, "Register speech boi");

        if (mSpeechManager == null) {
            mSpeechManager = new SpeechManager(context, this);
        }
    }

    public void disableSpeech() {
        if (mSpeechManager != null) {
            mSpeechManager.shutdown();
        }
    }

    @JavascriptInterface
    public void listen(String phrases) {
        Log.e(TAG, "listen fun");
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }
        Context context = ref.get().getContext();

        // Prepare Cloud Speech API
        context.bindService(new Intent(context, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
        startVoiceRecorder();

    }

    public void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    public void stopVoiceRecorder(Context context) {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener);
        context.unbindService(mServiceConnection);
        mSpeechService = null;
    }

}
