package com.peets.socialplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import com.peets.socialplay.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import static org.opencv.core.Core.circle;
import static org.opencv.core.Core.rectangle;

/*
 * the main activity to host the webRTC connectivity
 */
public class GuidedPlayActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "GuidedPlayActivity";
    private Button hangupButton = null;
    private ImageView imageView = null;
    private String chatRoom = null;
    private TextView scoreView = null;
    private TextView timerView = null;
    private static TextView savedTimerView = null;
    private static MediaPlayer mp = null;

    private static CountDownTimer countDownTimer = null;
    private final long startTime = 60 * 1000;
    private final long interval = 1 * 1000;
    private int challengeCount = 1;
    private static int score = 0;
    private static int challengeLimit = 2;

    private BallDetectorView mOpenCvCameraView;
    private List<Camera.Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    private BallDetector ballDetector;

    private final int REDCOLOR = 1;
    private final int GREENCOLOR = 2;
    private final int BLUECOLOR = 3;
    private final int YELLOWCOLOR = 4;

    private static int foundInstanceCount = 0;
    private static int foundWrongInstanceCount = 0;
    private final int FIRMCOUNT = 5;

    private static final Scalar BLUE = new Scalar(0, 0, 255);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar YELLOW = new Scalar(255, 255, 0);
    private static final Scalar WHITE = new Scalar(255, 255, 255);
    private static final Scalar PURPLE = new Scalar(255, 0, 255);
    private int detectionAreaSize = 200;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(GuidedPlayActivity.this);
                    ballDetector = new BallDetector();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public GuidedPlayActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public class MyCountDownTimer extends CountDownTimer {
        private boolean isCancelled = false;

        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            challengeCount++;
            if (challengeCount <= challengeLimit) {
                playAudio();
                startTimer(startTime, interval);
            } else {
                savedTimerView.setText("");
//                savedTimerView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (!isCancelled) {
                Log.e(TAG, "onTick: millisUntilFinished = " + millisUntilFinished);
                savedTimerView.setText(R.string.remaining);
                savedTimerView.setText(savedTimerView.getText() + "" + millisUntilFinished / 1000);
                savedTimerView.setVisibility(View.VISIBLE);

                if (foundInstanceCount >= FIRMCOUNT) {
                    wonChallenge();

                    isCancelled = true;
                    this.cancel();
                    // force the current challenge to finish
                    onFinish();
                }

                if (foundWrongInstanceCount >= FIRMCOUNT) {
                    foundWrongInstanceCount = 0;
                    if (!mp.isPlaying())
                        playWrongColorInstance();
                }
            }
        }
    }

    private void playWrongColorInstance() {
        mp = MediaPlayer.create(this, R.raw.wrongcolor);
        long duration = (long) mp.getDuration() + 500;
        mp.start();
    }

    private void wonChallenge() {
        foundInstanceCount = 0;
        mp = MediaPlayer.create(this, R.raw.youwon);
        long duration = (long) mp.getDuration() + 500;
        mp.start();

        score += 10;
        displayScore();

        sleep(duration);
    }

    private void displayScore() {
        scoreView.setText(R.string.score);
        scoreView.setText(scoreView.getText() + "" + score);
    }

    /**
     * Called when the activity is first created. This is where we'll hook up
     * our views in XML layout files to our application.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // show a progress bar
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.e(TAG, "On create : " + (countDownTimer == null));
        setContentView(R.layout.guidedplay);

        mOpenCvCameraView = (BallDetectorView) findViewById(R.id.balldetectorview);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        hangupButton = (Button) findViewById(R.id.hangup_button);
        hangupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "hangupButton OnClick");

                // when clicked, it hang up the call and clean the webview
                clearView();
            }
        });


        scoreView = (TextView) findViewById(R.id.score);
        scoreView.setText(R.string.score);
        displayScore();
        timerView = (TextView) findViewById(R.id.timer);
        savedTimerView = timerView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            // Manage exception
            Log.e(TAG, "OpenCV initialization error!");
        } else {
            Log.i(TAG, "OpenCV loaded successfully");
            mOpenCvCameraView.enableView();
            mOpenCvCameraView.setOnTouchListener(this);
            ballDetector = new BallDetector();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        BallDetector.DetectionResult res = ballDetector.findBall(frame);
        if (res != null) {            // Draw the max circle
            circle(frame, new Point(res.center_x, res.center_y), 3, PURPLE, -1, 8, 0);
            int color = 0;
            // circle outline
            switch (res.color) {
                case COLOR_RED:
                    circle(frame, new Point(res.center_x, res.center_y), res.radius, RED, 3, 8, 0);
                    Core.putText(frame, "RED", new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 1, RED);
                    color = REDCOLOR;
                    break;
                case COLOR_GREEN:
                    circle(frame, new Point(res.center_x, res.center_y), res.radius, GREEN, 3, 8, 0);
                    Core.putText(frame, "GREEN", new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 1, GREEN);
                    color = GREENCOLOR;
                    break;
                case COLOR_BLUE:
                    circle(frame, new Point(res.center_x, res.center_y), res.radius, BLUE, 3, 8, 0);
                    Core.putText(frame, "BLUE", new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 1, BLUE);
                    color = BLUECOLOR;
                    break;
                case COLOR_YELLOW:
                    circle(frame, new Point(res.center_x, res.center_y), res.radius, YELLOW, 3, 8, 0);
                    Core.putText(frame, "YELLOW", new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 1, YELLOW);
                    color = YELLOWCOLOR;
                    break;
                default:
                    circle(frame, new Point(res.center_x, res.center_y), res.radius, WHITE, 3, 8, 0);
                    Core.putText(frame, "OTHER", new Point(100, 100), Core.FONT_HERSHEY_PLAIN, 1, WHITE);
                    break;
            }

            checkResult(color);
        }

        // Draw square
        rectangle(frame, new Point(frame.cols() / 2 - detectionAreaSize / 2, frame.rows() / 2 - detectionAreaSize / 2), new Point(frame.cols() / 2 + detectionAreaSize / 2, frame.rows() / 2 + detectionAreaSize / 2), GREEN);
        return frame;
    }

    private void checkResult(int color) {
        switch (challengeCount) {
            case 1:
                if (color == REDCOLOR) foundInstanceCount++;
                else foundWrongInstanceCount++;
                break;
            case 2:
                if (color == YELLOWCOLOR) foundInstanceCount++;
                else foundWrongInstanceCount++;
                break;
            default:
                foundWrongInstanceCount++;
                break;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int idx = 0;
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            //return true;
        } else {
            mColorEffectsMenu = menu.addSubMenu("Color Effect");
            mEffectMenuItems = new MenuItem[effects.size()];


            ListIterator<String> effectItr = effects.listIterator();
            while (effectItr.hasNext()) {
                String element = effectItr.next();
                mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
                idx++;
            }
        }
        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Camera.Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while (resolutionItr.hasNext()) {
            Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1) {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        } else if (item.getGroupId() == 2) {
            int id = item.getItemId();
            Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void playAudio() {
        mp = MediaPlayer.create(this, (challengeCount == 1) ? R.raw.challenge1
                : R.raw.challenge2);

        mp.start();
    }

    private void startTimer(long period, long interval) {
        foundInstanceCount = 0;
        foundWrongInstanceCount = 0;
        countDownTimer = new MyCountDownTimer(period, interval);
        countDownTimer.start();
    }

    /**
     * @param
     * @return void
     * @throws
     * @Title: clearView
     * @Description: Hang up method , clear webview cache
     */
    protected void clearView() {
        Log.e(TAG, "clearView will clear view");

        hangupButton.setEnabled(false);

        PlaydateActivity.previousChatRoom = chatRoom; // remember the previous chat room
        Log.e(TAG,
                "clearView preserve previous chat room: "
                        + PlaydateActivity.previousChatRoom);

        // stop the timer activity
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timerView.setText("");
            countDownTimer = null;
        }

        if (mp != null) {
            mp.pause();
            mp = null;
        }

        startActivity(new Intent(getApplicationContext(),
                PlaydateActivity.class));
    }

    /**
     * Called when the activity is coming to the foreground. This is where we
     * will check whether there's an incoming connection.
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.e(TAG, "on start: chatRoom = " + chatRoom);

        if (countDownTimer == null) {
            playAudio();
            startTimer(startTime, interval);
        }
    }

    /**
     * utility to do sleep
     *
     * @param milliseconds
     */
    public void sleep(long milliseconds) {
        try {
            Log.e(TAG, "will sleep " + milliseconds
                    + "milliseconds");
            Thread.sleep(milliseconds);
        } catch (Exception ex) {
            Log.e(TAG,
                    "sleep encounters exception: " + ex.getMessage());
        }
    }

}
