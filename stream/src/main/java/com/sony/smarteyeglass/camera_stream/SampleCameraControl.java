package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Shows how to access the SmartEyeglass camera to capture pictures.
 * Demonstrates how to listen to camera events, process
 * camera data, display pictures, and store image data to external storage.
 */
public final class SampleCameraControl extends ControlExtension {

    /**
     * Set constants
     * Constant for 3Pi
     */
    private static final float PI_3 = (float) (Math.PI * 3.0);

    /** Degrees of a full rotation */
    private static final int FULL_ROTATION_DEGREE = 360;

    /** Degrees of a half rotation */
    private static final int HALF_ROTATION_DEGREE = FULL_ROTATION_DEGREE / 2;

    /** The entire angle of the vertical range to consider */
    private static final float VER_RANGE = 6.0f;

    /** */
    private int baseDeg;

    /** */
    private boolean isFirst = true;

    /** */
    private int count = 0;

    /** */
    private static final int OBJECT_ID = 1;
    /** */
//    private CylindricalRenderObject renderObj;
    private GlassesRenderObject renderObj, backgroundObj;

    /** Sensor management objects*/
    private AccessorySensor sensor = null;

    /** */
    private AccessorySensorManager sensorManager;

    /** Animation interval time */
    private static final int ANIMATION_INTERVAL_TIME = 66;

    /** Animation bitmap sequence */
    private SparseArray<Bitmap> imageMap = new SparseArray<Bitmap>();

    /** Animation starts disabled */
    private boolean isAnimation = false;

    /** Animation frame counter */
    private int frameCount = 0;

    /** Animation timer */
    private Timer timer;

    /** Handlers for AR events */
    private SmartEyeglassEventListener listener;


    ///////////////////////////

    /** The application context. */
    private final Context context;

    /** Instance of the Control Utility class. */
    private final SmartEyeglassControlUtils utils;

    /** Uses SmartEyeglass API version*/
    private static final int SMARTEYEGLASS_API_VERSION = 3;

    /** The camera mode. */
    private AbstractCameraMode mode;
    private SonyCameraPublisher imagePublisher;

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
        sensorManager = new AccessorySensorManager(context, hostAppPackageName);
        imagePublisher = SonyCameraPublisher.getInstance();
        Log.d(Constants.LOG_TAG, "Starting up image handlers ");

        listener = new SmartEyeglassEventListener() {

            ////////////////
            //AR Listeners//
            ////////////////

            // Log successful registrations of an AR object
            @Override
            public void onARRegistrationResult(
                    final int result, final int objectId) {
                Log.d(Constants.LOG_TAG,
                        "onARRegistrationResult() result=" + result
                                + " objectId=" + objectId);
                if (result != SmartEyeglassControl.Intents.AR_RESULT_OK) {
                    Log.d(Constants.LOG_TAG,
                            "AR registre object failed! errorcode = " + result);
                    return;
                }
            }
            // Find bitmap to render when requested by AR engine
            @Override
            public void onARObjectRequest(final int objectId) {
                Log.d(Constants.LOG_TAG,
                        "onLocalRenderingObjectRequest() "
                                + " objectId=" + objectId);
                // send bitmap
                utils.sendARObjectResponse(renderObj, 0);
            }

            ////////////////////
            //Camera Listeners//
            ////////////////////

            // When camera operation has succeeded
            // handle result according to current recording mode
            @Override
            public void onCameraReceived(final CameraEvent event) {
                Log.d(Constants.LOG_TAG, "onCameraReceived: ");
                imagePublisher.onNewRawImage(event.getData(), 320, 240);

            // To enable camera feedback on glass
            /*    Bitmap bitmap = Bitmap.createScaledBitmap( BitmapFactory.decodeByteArray(event.getData(),0,event.getData().length), 100,100, false);

                utils.sendARAnimationObject(20, bitmap);

                mode.handleCameraEvent(event);
            */}

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
        Log.d(Constants.LOG_TAG, "Done setting up image/ handlers ");

        setAnimationResource();
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

        CameraModeFactory[] factories = CameraModeFactory.values();

        mode = factories[3].of(context);
        mode.setBitmapDisplay(new BitmapDisplay() {
            @Override
            public void displayBitmap(final Bitmap bitmap) {
                showBitmap(bitmap);
            }
        });
        Log.d(Constants.LOG_TAG, "Preparing to set camera mode ");

        utils.setCameraMode(1, 6, 3);
        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }
        Log.d(Constants.LOG_TAG, "Camera Mode set ");

        baseDeg = 0;
        sensorStart();
    }

    // Respond to tap on touch pad by switching camera modes.
    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }
       // mode.toggleState(utils);
    }


    /** Start the rendering operation. */
    private void renderStart() {
        utils.setRenderMode(SmartEyeglassControl.Intents.MODE_AR);
        utils.changeARCylindricalVerticalRange(VER_RANGE);
        loadResource();
        startARAnimation();
    }

    /**
     * Create render object to place at cylindrical-coordinate position.
     * and register it with the AR engine.
     */
    private void loadResource() {

        //backgroundObj = new GlassesRenderObject(20, null, 0,
        //        0, 0, SmartEyeglassControl.Intents.AR_OBJECT_TYPE_ANIMATED_IMAGE);

        //Fixed position object
        renderObj = new GlassesRenderObject(OBJECT_ID,
                getBitmapResource(R.drawable.sample_00), 0,
                SelectionSub.boxPos.x, SelectionSub.boxPos.y,
                SmartEyeglassControl.Intents.AR_OBJECT_TYPE_ANIMATED_IMAGE);

/*
        final float v = 0.0f;
        float h = baseDeg;
        Log.d(Constants.LOG_TAG, "baseDeg: " + baseDeg);
        SelectionSub.boxPos.x = baseDeg;
        SelectionSub.boxPos.y = 0;

        renderObj = new CylindricalRenderObject(OBJECT_ID,
                getBitmapResource(R.drawable.sample_00), 0,
                SmartEyeglassControl.Intents.AR_OBJECT_TYPE_ANIMATED_IMAGE,
                h, v);
        */
        //registerObject(backgroundObj);
        registerObject(renderObj);
    }

    /**
     * Load the bitmap image for the current animation frame.
     */
    private void setAnimationResource() {
        int[] id = AnimationResources.ID_RESOURCE_LIST;
        for (int frame = 0; frame < AnimationResources.MAX_FRAME; frame++) {
            imageMap.put(frame, getBitmapResource(id[frame]));
        }
    }

    /**
     * Register a render object with the AR engine.
     *      @param obj The render object.
     */
    private void registerObject(final RenderObject obj) {
        Log.d(Constants.LOG_TAG, "registerObject " + obj);
        this.utils.registerARObject(obj);
    }

    /**
     * Convert sensor data to an orientation.
     *      @param x The horizontal coordinate.
     *      @param y The vertical coordinate.
     *      @return The heading value.
     */
    private static int getHeading(final float x, final float y) {
        double heading = 0;
        if (x == 0 && y < 0) {
            heading = Math.PI / 2.0;
        }
        if (x == 0 && y > 0) {
            heading = PI_3 / 2.0;
        }
        if (x < 0) {
            heading = Math.PI - Math.atan(y / x);
        }
        if (x > 0 && y < 0) {
            heading = -Math.atan(y / x);
        }
        if (x > 0 && y > 0) {
            heading = 2.0 * Math.PI - Math.atan(y / x);
        }
        int d = (int) (heading * HALF_ROTATION_DEGREE / Math.PI);
        if (d < 0) {
            d += FULL_ROTATION_DEGREE;
        }
        return d;
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

    /**
     * Start the sensors.
     */
    private void sensorStart() {
        count = 0;
        sensor = sensorManager.getSensor(Registration.SensorTypeValue.MAGNETIC_FIELD);
        isFirst = true;
        // Start listening for sensor updates.
        if (sensor == null) {
            Log.d(Constants.LOG_TAG, "No such sensor type: "
                    + Registration.SensorTypeValue.MAGNETIC_FIELD);
            return;
        }
        try {
            sensor.registerListener(new AccessorySensorEventListener() {
                @Override
                public void onSensorEvent(final AccessorySensorEvent ev) {
                    updateDirection(ev);
                }
            }, Sensor.SensorRates.SENSOR_DELAY_NORMAL, 0);
        } catch (AccessorySensorException e) {
            Log.d(Constants.LOG_TAG, "Failed to register listener");
        }
    }

    /**
     * Update the heading direction from sensor data.
     * @param ev The sensor event.
     */
    private void updateDirection(final AccessorySensorEvent ev) {
        float[] v = ev.getSensorValues();
        int headDirection = getHeading(v[0], v[1]);
        ++count;
        if (count > 2 && isFirst) {
            baseDeg = headDirection;
            renderStart();
            isFirst = false;
        }
    }

    // Stop showing animation and listening for sensor data
    // when app is paused
    @Override
    public void onPause() {
        // Stop animation
        stopARAnimation();

        // Stop sensors
        if (sensor == null) {
            return;
        }
        // Stop listening for sensor data
        sensor.unregisterListener();
        sensor = null;

        //mode.closeCamera(utils);
        //mode = null;
    }

    @Override
    public void onTouch(final ControlTouchEvent ev) {
        super.onTouch(ev);
    }

    @Override
    public void onStop() {
        super.onStop();
        imageMap.clear();
    }

    /**
     * Get next frame of the animation and send it with same object ID
     */
    private void updateAnimationimage() {
        if (!isAnimation) {
            return;
        }

        if(renderObj.getPosition().x != SelectionSub.boxPos.x
                || renderObj.getPosition().y != SelectionSub.boxPos.y)
        {
            renderObj.setPositon(new Point(SelectionSub.boxPos.x, SelectionSub.boxPos.y));
            utils.moveARObject(renderObj);
        }

        Bitmap bitmap = imageMap.get(frameCount);
        if (bitmap != null) {
            utils.sendARAnimationObject(OBJECT_ID, bitmap);
            Log.d(Constants.LOG_TAG, "update animation image: " + frameCount);
            frameCount++;
        }
        if (frameCount >= AnimationResources.MAX_FRAME) {
            frameCount = 0;
        }
    }

    /** Start animation rendering operation */
    private void startARAnimation() {
        utils.enableARAnimationRequest();

        // Clear timer variable, if it is already holding a Timer object
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Create a new instance of Timer and set it to run with interval
        // time value
        timer = new Timer();
        timer.schedule(new TimerTask() {
            private Handler mHandler = new Handler();
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        updateAnimationimage();
                    }
                });
            }
        }, 0, ANIMATION_INTERVAL_TIME);

        Log.d(Constants.LOG_TAG, "start animation");
        isAnimation = true;
    }

    /** Stop animation rendering operation */
    private void stopARAnimation() {
        // stop timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        utils.disableARAnimationRequest();

        Log.d(Constants.LOG_TAG, "stop animation");
        isAnimation = true;
    }
}
