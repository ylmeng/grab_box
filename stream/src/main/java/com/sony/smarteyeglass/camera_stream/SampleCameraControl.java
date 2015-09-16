package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sony.smarteyeglass.extension.util.CameraEvent;
import com.sony.smarteyeglass.extension.util.ControlCameraException;
import com.sony.smarteyeglass.extension.util.SmartEyeglassControlUtils;
import com.sony.smarteyeglass.extension.util.SmartEyeglassEventListener;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;

import org.ros.android.view.camera.CompressedImagePublisher;

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
        imagePublisher = SonyCameraPublisher.getInstance();
        Log.d(Constants.LOG_TAG, "Starting up image handlers ");
        // Initialize listener for camera events
        SmartEyeglassEventListener listener = new SmartEyeglassEventListener() {
            // When camera operation has succeeded
            // handle result according to current recording mode
            @Override
            public void onCameraReceived(final CameraEvent event) {
                Log.d(Constants.LOG_TAG, "onCameraReceived: ");
                imagePublisher.onNewRawImage(event.getData(), 320,240);

                //mode.handleCameraEvent(event);
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
        Log.d(Constants.LOG_TAG, "Done setting up image handlers ");
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

        /*SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        // Track current camera mode parameters
        CameraModeFactory[] factories = CameraModeFactory.values();

        int recordMode = Integer.parseInt(prefs.getString(
                context.getString(R.string.preference_key_recordmode), "2"));
        if (recordMode < 0 || recordMode >= factories.length) {
            recordMode = 0;
        }
        // Get and show current recording mode
        mode = factories[recordMode].of(context);
        mode.setBitmapDisplay(new BitmapDisplay() {
            @Override
            public void displayBitmap(final Bitmap bitmap) {
                showBitmap(bitmap);
            }
        });
        // Get and show quality parameters
        int jpegQuality =  1;
                //Integer.parseInt(prefs.getString(
                //context.getString(R.string.preference_key_jpeg_quality), "1"));
        int preferenceId = mode.getPreferenceId();
        int resolution = Integer.parseInt(prefs.getString(
                context.getString(preferenceId), "6"));

        // Set the camera mode to match the setup
        utils.setCameraMode(jpegQuality, resolution, mode.getRecordingMode());
        // Display instruction for current recording mode
        mode.updateDisplay(); */
        Log.d(Constants.LOG_TAG, "Preparing to set camera mode ");

        utils.setCameraMode(1, 6, 3);
        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }
        Log.d(Constants.LOG_TAG, "Camera Mode set ");
    }

    // Clean up any open files and reset mode when app is paused.
    @Override
    public void onPause() {
        //mode.closeCamera(utils);
        //mode = null;
    }

    // Respond to tap on touch pad by switching camera modes.
    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }
       // mode.toggleState(utils);
    }
}
