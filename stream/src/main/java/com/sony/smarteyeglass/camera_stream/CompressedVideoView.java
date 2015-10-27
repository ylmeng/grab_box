package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sensor_msgs.CompressedImage;

/**
 * Created by csrobot on 9/14/15.
 */
public class CompressedVideoView extends SurfaceView implements SurfaceHolder.Callback, NodeMain {
    private ConnectedNode connectedNode;
    private static final String LOG_TAG = "CompressedVideoView";
    private String rootNodeName, topicName;
    private ExecutorService pushThread;
    private Subscriber<CompressedImage> cameraSub;

    public static Bitmap bitmap;

    public CompressedVideoView(Context context) {
        super(context);
        initialize();
    }

    public CompressedVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        cameraSub = null;
        rootNodeName = "/ball_mover";
        topicName="/ball_mover/warped_compressed/compressed";

        pushThread = new ThreadPoolExecutor(1, 1,                        // use only a single thread
        30,TimeUnit.MILLISECONDS,    // timeout after 30mS (33mS is the max time of a single frame at 30fps)
        new ArrayBlockingQueue<Runnable>(10));
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("jordan", "SURFACE CREATED");

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("jordan", "SURFACE CHANGED");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("jordan", "SURFACE DESTROYED");

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("/rosjava/compressed_image_subscriber");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        cameraSub = connectedNode.newSubscriber(topicName, CompressedImage._TYPE);
        cameraSub.addMessageListener(new MessageListener<CompressedImage>() {
            @Override
            public void onNewMessage(final CompressedImage compressedImage) {
                try {
                    pushThread.submit(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mySony", "got an image");
                            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
                            final SurfaceHolder holder = getHolder();

                            Canvas c = holder.lockCanvas();
                            if (holder != null && c != null) {

                                BitmapFromCompressedImage bf = new BitmapFromCompressedImage();
                                bitmap = bf.call(compressedImage);

                                //SampleCameraControl.getInstance().drawBitmap(bitmap.copy(bitmap.getConfig(), true));

                                Rect dest = new Rect(0, 0, getWidth(), getHeight());
                                c.drawBitmap(bitmap, null, dest, null);

                                holder.unlockCanvasAndPost(c);
                            }
                        }
                    });
                } catch (RejectedExecutionException rejectedExecutionException) {
                }
            }
        });
    }


    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }
}
