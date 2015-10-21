package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;

import com.sony.smarteyeglass.SmartEyeglassControl;
import com.sony.smarteyeglass.extension.util.CameraEvent;
import com.sony.smarteyeglass.extension.util.ControlCameraException;
import com.sony.smarteyeglass.extension.util.SmartEyeglassControlUtils;
import com.sony.smarteyeglass.extension.util.SmartEyeglassEventListener;
import com.sony.smarteyeglass.extension.util.ar.CylindricalRenderObject;
import com.sony.smarteyeglass.extension.util.ar.GlassesRenderObject;
import com.sony.smarteyeglass.extension.util.ar.RenderObject;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;

/**
 * Shows how to access the SmartEyeglass camera to capture pictures.
 * Demonstrates how to listen to camera events, process
 * camera data, display pictures, and store image data to external storage.
 */
public final class SampleCameraControl extends ControlExtension {

    /** The application context. */
    private final Context context;

    private static SampleCameraControl instance;

    /** Instance of the Control Utility class. */
    private final SmartEyeglassControlUtils utils;

    /** Uses SmartEyeglass API version*/
    private static final int SMARTEYEGLASS_API_VERSION = 3;

    private SonyCameraPublisher imagePublisher;

    enum State {LVL1, LVL1COMPLETE};
    State gameState = State.LVL1;

    private int width, height;
    boolean completedTask;

    public static int xtrans = 195, ytrans = 135;
    boolean movingX = true;

    public static SampleCameraControl getInstance() {
        return instance;
    }

    /**
     * Creates an instance of this control class.
     *
     * @param context               The context.
     * @param hostAppPackageName    Package name of host application.
     */
    public SampleCameraControl(final Context context,
                               final String hostAppPackageName) {
        super(context, hostAppPackageName);

        this.context = context;
        width = context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_width);
        height = context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_height);

        instance = this;
        imagePublisher = SonyCameraPublisher.getInstance();

        SmartEyeglassEventListener listener = new SmartEyeglassEventListener() {

            int skipper = 0;
            // When camera operation has succeeded
            // handle result according to current recording mode
            @Override
            public void onCameraReceived(final CameraEvent event) {
                if(skipper++ > 25) {
                    skipper = 0;
                    return;
                }
                imagePublisher.onNewRawImage(event.getData(), 320, 240);
                updateDisplay();
            }
            // Called when camera operation has failed
            // We just log the error
            @Override
            public void onCameraErrorReceived(final int error) {
                Log.d(Constants.LOG_TAG, "onCameraErrorReceived: " + error);
            }
            // When camera is set to record image to a file,
            // log the operation and clean up
            @Override
            public void onCameraReceivedFile(final String filePath) {
                Log.d(Constants.LOG_TAG, "onCameraReceivedFile: " + filePath);
                //mode.closeCamera(utils);
            }
        };

        utils = new SmartEyeglassControlUtils(hostAppPackageName, listener);
        utils.setRequiredApiVersion(SMARTEYEGLASS_API_VERSION);
        utils.activate(context);
        utils.setPowerMode(SmartEyeglassControl.Intents.POWER_MODE_HIGH);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(Constants.LOG_TAG, "Done setting up utils");
    }

    // Clean up data structures on termination.
    @Override
    public void onDestroy() {
        utils.deactivate();
    };

    // When app becomes visible, set up camera mode choices
    // and instruct user to begin camera operation
    @Override
    public void onResume() {
        // Keep the screen on for this demonstration.
        // Don't do this in a real app, it will drain the battery.
        setScreenState(Control.Intents.SCREEN_STATE_ON);

        Log.d(Constants.LOG_TAG, "Preparing to set camera mode ");

        utils.setCameraMode(1, 6, 2);
        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }

        Log.d(Constants.LOG_TAG, "Camera Mode set ");

        drawBox();
    }

    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }

        //movingX = !movingX;
        gameState = State.LVL1;
        //completedTask = false;
    }

    // Stop showing animation and listening for sensor data
    // when app is paused
    @Override
    public void onPause() {
        utils.stopCamera();
    }

    //int increment = 10;
    @Override
    public void onTouch(final ControlTouchEvent ev) {
        super.onTouch(ev);

//        if(ev.getAction() == Control.Intents.SWIPE_DIRECTION_LEFT) {
//            if(movingX) {
//                xtrans += increment;
//            } else {
//                ytrans += increment;
//            }
//        } else if(ev.getAction() == Control.Intents.SWIPE_DIRECTION_RIGHT) {
//            if(movingX) {
//                xtrans -= increment;
//            } else {
//                ytrans -= increment;
//            }
//        }

        //Log.d("cmon", "xtrans: " + xtrans + " ytrans: " + ytrans);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * Retrieve a registered bitmap by its resource ID.
     *      @param id   The resource ID.
     *      @return     The bitmap.
     */
    private Bitmap getBitmapResource(final int id) {
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), id);
        b.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return b;
    }

    public void updateDisplay() {
        switch(gameState) {
            case LVL1:
                drawBox();
                break;
            case LVL1COMPLETE:
                showText("Great Job!");
                break;
        }
    }

    public void drawBox() {
        Bitmap box_bmp = getBitmapResource(R.drawable.box);
        Bitmap bmp = Bitmap.createBitmap(width, height, box_bmp.getConfig());

        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(box_bmp, (int) BallMotionSubscriber.getInstance().getBall().getX() - box_bmp.getWidth()/2,
            (int) BallMotionSubscriber.getInstance().getBall().getY() - box_bmp.getWidth()/2, paint);

        showBitmap(bmp);
    }

    /**
     * draws text to the center of the screen
     * @param message
     */
    public void showText(String message) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(40);
        paint.setColor(Color.WHITE);

        //used to center text on the screen
        Rect bounds = new Rect();
        paint.getTextBounds(message, 0, message.length(), bounds);

        canvas.drawText(message, width / 2 - bounds.width() / 2, height / 2 - bounds.height() / 2, paint);

        showBitmap(bmp);
    }

    public void handleGesture(int pose_num) {
        if(pose_num == 1) {
            gameState = State.LVL1COMPLETE;
        }
    }
}