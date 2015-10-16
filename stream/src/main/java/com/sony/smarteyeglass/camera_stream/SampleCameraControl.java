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

    private int width = 419, height = 138;
    boolean completedTask;

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

        instance = this;

        utils = new SmartEyeglassControlUtils(hostAppPackageName, new SmartEyeglassEventListener());
        utils.setRequiredApiVersion(SMARTEYEGLASS_API_VERSION);
        utils.activate(context);
        utils.setPowerMode(SmartEyeglassControl.Intents.POWER_MODE_NORMAL);

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

        /* enable for camera
        utils.setCameraMode(1, 6, 3);
        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }
        */
        Log.d(Constants.LOG_TAG, "Camera Mode set ");

        drawBox();
    }

    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }

        completedTask = false;
        drawBox();
    }

    // Stop showing animation and listening for sensor data
    // when app is paused
    @Override
    public void onPause() {
        utils.stopCamera();
    }

    @Override
    public void onTouch(final ControlTouchEvent ev) {
        super.onTouch(ev);

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

    void updateDisplay() {
        if(!completedTask) {
            drawBox();
        }
    }

    public void drawBox() {
        Bitmap box_bmp = getBitmapResource(R.drawable.box);
        Bitmap bmp = Bitmap.createBitmap(width, height, box_bmp.getConfig());

        Log.d("handle", box_bmp.getWidth() + " " + box_bmp.getHeight());

        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(box_bmp, width/2 - box_bmp.getWidth()/2, height / 2 - box_bmp.getHeight() / 2, paint);

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
            completedTask = true;
        }

        if(completedTask) {
            showText("Great job!");
        } else {
            drawBox();
        }
    }
}

