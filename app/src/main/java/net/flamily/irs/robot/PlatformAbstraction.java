package net.flamily.irs.robot;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Environment;
import android.robot.speech.SpeechManager;
import android.robot.speech.SpeechService;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import net.flamily.irs.robot.image_capture.CaptureImageReceiver;
import net.flamily.irs.robot.robot_listenining.WavRecorder;

import java.io.IOException;
import java.lang.ref.WeakReference;


public class PlatformAbstraction implements CaptureImageReceiver.ICaptureImageReceiver, SpeechManager.OnConnectListener {

    private final WeakReference<WebView> ref;
    private String TAG = "PlatformAbstraction";

    private String INTENT_CAPTURE_IMAGE = "ImageCaptureAction";

    private CaptureImageReceiver mCaptureImageReceiver;
    private SpeechManager mSpeechManager;

    private static String mFileName = null;
    private MediaRecorder mMediaRecorder;
    private boolean recording = true;
    private WavRecorder wavRecorder;
    private SpeechService mSpeechService;

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
    public void listen(String phrases) {
        Log.e(TAG, "listen fun");
        final WebView webView = ref.get();
        if (webView == null) {
            return;
        }
        Context context = ref.get().getContext();

        mFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/audio_file.3gp";
        //adb pull /mnt/internal_sd/Music/Recording.mp4 E:\
        try {
            //mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recording.3gp";

            Log.d(TAG, "DIR: " + mFileName);
            if (recording) {
                startRecording();
            } else {
                stopRecording(context);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


        recording = !recording;


        //Audio au = new Audio();
        //TODO: implement timeout

        //Standard Android Speech to Text
        /*Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            ((Activity) context).startActivityForResult(intent, 10);
        } else {
            //Doesn't support standard input, use robot input
            Log.e(TAG, "no support, plan B");
            if (mSpeechManager == null)
            {
                registerSpeech(context);
            }
        }*/

    }

    //@SuppressLint("WrongConstant")
    // For some reason it's not recognizing that service - needs to be tested
    public void registerSpeech(Context context){
        Log.e(TAG, "Register speech boi");

        if (mSpeechManager == null) {
            mSpeechManager = new SpeechManager(context, this);
        }
    }


    public void disableSpeech()
    {
        if(mSpeechManager != null)
        {
            mSpeechManager.shutdown();
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

    private void startRecording() {
        Log.e(TAG, "started recording");
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(mFileName);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        mMediaRecorder.start();
    }

    private void stopRecording(Context context) {
        Log.e(TAG, "stopped recording");
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }


}
