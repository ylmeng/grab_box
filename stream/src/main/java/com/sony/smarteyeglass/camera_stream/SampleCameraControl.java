package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
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

    /** Instance of the Control Utility class. */
    private final SmartEyeglassControlUtils utils;

    /** Uses SmartEyeglass API version*/
    private static final int SMARTEYEGLASS_API_VERSION = 3;

    /** The camera mode. */
    private SonyCameraPublisher imagePublisher;

    private static Point square_center = new Point(-1, -1);
    private final Bitmap square_bmp;

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
        imagePublisher = SonyCameraPublisher.getInstance();
        Log.d(Constants.LOG_TAG, "Starting up image handlers ");

        SmartEyeglassEventListener listener = new SmartEyeglassEventListener() {

            ////////////////////
            //Camera Listeners//
            ////////////////////

            long last_frame = 0;
            // When camera operation has succeeded
            // handle result according to current recording mode
            @Override
            public void onCameraReceived(final CameraEvent event) {

                if(event == null || event.getData() == null) {

                    Log.d("mySony", "Skipped a frame");
                    return;

                }

                Log.d(Constants.LOG_TAG, "onCameraReceived: ");
                imagePublisher.onNewRawImage(event.getData(), 320, 240);

                if (event.getTimestamp() - last_frame < 600) {
                    Log.d("mySony", "Skipped a glasses-frame");
                    return;
                }
                else {
                    last_frame = event.getTimestamp();
                }

                Log.d(Constants.LOG_TAG, "Updating background");
                // To enable camera feedback on glass
                Bitmap bitmap = Bitmap.createScaledBitmap( BitmapFactory.decodeByteArray(
                        event.getData(),0,event.getData().length), 100, 100, false); //419, 138, false);

                updateDisplay(bitmap, event.getTimestamp());

                Log.d(Constants.LOG_TAG, "exited onCameraReceived");
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
            }
        };

        square_bmp = getBitmapResource(R.drawable.sample_27);
        utils = new SmartEyeglassControlUtils(hostAppPackageName, listener);
        utils.setRequiredApiVersion(SMARTEYEGLASS_API_VERSION);
        utils.activate(context);
        utils.setPowerMode(SmartEyeglassControl.Intents.POWER_MODE_HIGH);

        Log.d(Constants.LOG_TAG, "Done setting up image/ handlers ");

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

        utils.setCameraMode(1, 6, 3);
        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }
        Log.d(Constants.LOG_TAG, "Camera Mode set ");
    }

    // Respond to tap on touch pad by switching camera modes.
    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }
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

    //Jordan's bitmap function
    private void updateDisplay(Bitmap bmp, long t)
    {
        Log.d("mySony", "updating the display, time change: " + (System.currentTimeMillis() - t) );
        if(bmp == null)
            return;

        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(16);
        paint.setColor(Color.WHITE);

        if (isSquareOn())
        {
            canvas.drawBitmap(square_bmp, square_center.x, square_center.y, paint);
        }

        Log.d("mySony", "updating the display, time change: " + (System.currentTimeMillis() - t) );
        showBitmap(bmp);
        Log.d("mySony", "updating the display, time change: " + (System.currentTimeMillis() - t));
        Log.d("mySony", "-");
    }

    public static boolean isSquareOn() {
        return (square_center.x > 0 && square_center.y > 0);
    }

    public static void moveSquare(int x, int y) {
        square_center.set(x, y);
    }

}

