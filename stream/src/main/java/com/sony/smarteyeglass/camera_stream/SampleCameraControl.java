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

    enum State {
        INITIAL, SQUEEZE, LVL1, LVLCOMPLETE;
    };

    State gameState = State.INITIAL;
    private int transitionAnimationTimer = 60;
    private static int TRANSITION_LENGTH = 60;

    private int width, height;

    private static final int PADDING = 10;
    private Ball[] objective = { new Ball(Ball.WIDTH/2 + PADDING, 138/2), new Ball(419 - Ball.WIDTH/2 - PADDING, 138/2),
            new Ball(419/2, 138 - Ball.WIDTH/2 - PADDING), new Ball(419/2, Ball.WIDTH/2 + PADDING) };
    private int objective_cursor = 0;

    public static int xtrans = -550, ytrans = -605, focus = 6, option = 0;
    public static double xmag = 3.75, ymag = 3.02;

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

            int skipFrame = 0;
            // When camera operation has succeeded
            // handle result according to current recording mode
            @Override
            public void onCameraReceived(final CameraEvent event) {
                updateDisplay();

                if(gameState != State.LVL1 || transitionAnimationTimer < TRANSITION_LENGTH) {
                    return;
                }

                if(skipFrame++ > 2) {
                    skipFrame = 0;
                    return;
                }

                TipPointSubscriber.getInstance().getBall().updatePos();

                imagePublisher.onNewRawImage(event.getData(), 320, 240);
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

        utils = new SmartEyeglassControlUtils(hostAppPackageName, listener);
        utils.setRequiredApiVersion(SMARTEYEGLASS_API_VERSION);
        utils.activate(context);
        utils.setScreenDepth(focus); //sets the projection distance to ~1 meter
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


        utils.setCameraMode(
                SmartEyeglassControl.Intents.CAMERA_JPEG_QUALITY_STANDARD,
                SmartEyeglassControl.Intents.CAMERA_RESOLUTION_QVGA,
                SmartEyeglassControl.Intents.CAMERA_MODE_JPG_STREAM_LOW_RATE);

        try {
            utils.startCamera();
        } catch (ControlCameraException e) {
            e.printStackTrace();
        }

        Log.d(Constants.LOG_TAG, "Camera Mode set ");

    }

    @Override
    public void onTap(final int action, final long timeStamp) {
        if (action != Control.TapActions.SINGLE_TAP) {
            return;
        }

        if (++option > 4) {
            option = 0;
        }

        Log.d("mySony", "xmag: " + xmag + "ymag: " + ymag + "xtrans: " + xtrans + "ytrans: " + ytrans);
        gameState = State.INITIAL;
    }

    // Stop showing animation and listening for sensor data
    // when app is paused
    @Override
    public void onPause() {
        utils.stopCamera();
    }

    /**
     * adjusts display parameters when device is swiped
     * more specifically, adjusts the projection distance of images displayed in the glass
     * @param ev contains information about the touch event
     */
    @Override
    public void onTouch(final ControlTouchEvent ev) {
        super.onTouch(ev);
    }

    @Override
    public void onSwipe(int direction) {

        switch(option) {
            case 0:
                if (direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
                    xmag -= .025;
                } else if (direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
                    xmag += .025;
                }
                break;
            case 1:
                if (direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
                    ymag -= .025;
                } else if (direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
                    ymag += .025;
                }
                break;
            case 2:
                if (direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
                    xtrans -= 5;
                } else if (direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
                    xtrans += 5;
                }
                break;
            case 3:
                if (direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
                    ytrans -= 5;
                } else if (direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
                    ytrans += 5;
                }
                break;
            case 4:
                if (direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
                    focus--;
                } else if (direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
                    focus++;
                }
                break;
        }

        Log.d("mySony", "setting screen depth to: " + focus);

        utils.setScreenDepth(focus);
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
            case INITIAL:
                showText("Squeeze to begin.");
                break;

            case LVL1:
                drawBoard();
                break;

            case SQUEEZE:
                showText("Squeeze!");
                break;

            case LVLCOMPLETE:
                showText("Great Job!");
                break;
        }
    }

    /**
     * used to draw any bitmap to the screen,
     * used to be used to draw the warped perspective for parameter
     * calibration, currently unused

    public void drawBitmap() {
        Log.d("mySony", "drawing bitmap");
        Bitmap bmp = CompressedVideoView.bitmap;

        if(bmp == null) { return; }

        Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * xmag), (int)(bmp.getHeight() * ymag), false);
        Bitmap background = Bitmap.createBitmap(width, height, bmp.getConfig());

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        new Canvas(background).drawBitmap(scaled, xtrans, ytrans, paint);
        showBitmap(background);

    }
     */

    public void drawBoard() {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        drawBoardBorders(canvas);

        if (transitionAnimationTimer >= TRANSITION_LENGTH || transitionAnimationTimer / 10 % 2 != 0) {
            drawObjective(canvas);
            drawBall(canvas);
        }

        showBitmap(bmp);
    }

    public void drawBoardBorders(Canvas canvas) {
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);

        canvas.drawLine(0, 0, 0, height, paint);
        canvas.drawLine(0, height, width, height, paint);
        canvas.drawLine(width, height, width, 0, paint);
        canvas.drawLine(width, 0, 0, 0, paint);
    }

    public void drawObjective(Canvas canvas) {
        Bitmap objective_bmp = getBitmapResource(R.drawable.objective);

        int x = (int)objective[objective_cursor].getX(),
            y = (int)objective[objective_cursor].getY();

        Log.d("mySony", "objective_bitmap_width: " + objective_bmp.getWidth()
                + " : " + objective_bmp.getScaledWidth(canvas));

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(objective_bmp, x - objective_bmp.getScaledWidth(canvas) / 2,
                y - objective_bmp.getScaledHeight(canvas) / 2, paint);
    }

    public void drawBall(Canvas canvas) {
        Bitmap ball_bmp = getBitmapResource(R.drawable.ball);

        //int warpedX = (int)(TipPointSubscriber.getInstance().getBall().getX() * xmag + xtrans);
        //int warpedY = (int)(TipPointSubscriber.getInstance().getBall().getY() * ymag + ytrans);
        Ball b = TipPointSubscriber.getInstance().getBall();
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(ball_bmp, (int) b.getX() - ball_bmp.getScaledWidth(canvas) / 2,
                (int) b.getY() - ball_bmp.getScaledHeight(canvas) / 2, paint);
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

    public void processEndPoint(int x, int y) {
        if(gameState == State.LVL1) {
            int xNet = (int) Math.abs(x - objective[objective_cursor].getX());
            int yNet = (int) Math.abs(y - objective[objective_cursor].getY());

            Log.d("mySony", "xNet: " + xNet + " n " + yNet);

            //if the ball is within (about) a 20 pixel radius
            if(xNet < 20 && yNet < 20) {
                TipPointSubscriber.getInstance().getBall().lock();
                objectiveReachedAnimation();
                TipPointSubscriber.getInstance().getBall().unlock();

                //iterates to the next objective, if it was the last objective...
                if(++objective_cursor >= objective.length) {
                    gameState = State.LVLCOMPLETE; //finish the level
                    objective_cursor = 0;
                }
            }
        }
    }

    public void objectiveReachedAnimation() {
        for(transitionAnimationTimer = 0; transitionAnimationTimer < TRANSITION_LENGTH; transitionAnimationTimer++) {
            drawBoard();
        }
    }
}