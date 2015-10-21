package com.sony.smarteyeglass.camera_stream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import sensor_msgs.CompressedImage;

/**
 * Created by csrobot on 9/10/15.
 */
public class SonyCameraPublisher implements NodeMain {
    private ConnectedNode connectedNode;
    private Publisher<CompressedImage> imagePublisher;
    private Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
    private byte[] rawImageBuffer;
    private Camera.Size rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;
    private ImageAroundBallPublisher imageAroundBallPub;

    private static SonyCameraPublisher instance = null;

    private SonyCameraPublisher() {
        imageAroundBallPub = ImageAroundBallPublisher.getInstance();
    }

    public static SonyCameraPublisher getInstance() {
        if(instance == null) {
            synchronized (SonyCameraPublisher.class) {
                if (instance == null) {
                    instance = new SonyCameraPublisher();
                }
            }

        }
        return instance;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("SonyCamera/test");
    }


    @Override
    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild("camera");
        imagePublisher =
                connectedNode.newPublisher(resolver.resolve("image/compressed"),
                        sensor_msgs.CompressedImage._TYPE);
        cameraInfoPublisher =
                connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    @Override
    public void onShutdown(Node node) {
        imagePublisher.shutdown();
        cameraInfoPublisher.shutdown();
    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    public void onNewRawImage(byte[] data, int width, int height) {
        Preconditions.checkNotNull(data);
        //Preconditions.checkNotNull(size);

        if (data != rawImageBuffer /*|| !size.equals(rawImageSize)*/) {
            rawImageBuffer = data;
            //rawImageSize = size;
            //yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, width, height, null);
            //rect = new Rect(0, 0, width, height);

            drawSquareOnRaw();
        }
        
        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "camera";

        sensor_msgs.CompressedImage image = imagePublisher.newMessage();
        image.setFormat("jpeg");
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(frameId);

        try {
            for (int i =0; i < rawImageBuffer.length; i++) {
                stream.writeByte(rawImageBuffer[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
        image.setData(stream.buffer().copy());
        stream.buffer().clear();

        imagePublisher.publish(image);

        sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(currentTime);
        cameraInfo.getHeader().setFrameId(frameId);

        cameraInfo.setWidth(width);
        cameraInfo.setHeight(height);
        cameraInfoPublisher.publish(cameraInfo);
    }

    void drawSquareOnRaw() {

        Bitmap bmp = BitmapFactory.decodeByteArray(rawImageBuffer, 0, rawImageBuffer.length);
        Bitmap mutableBitmap = bmp.copy(Bitmap.Config.RGB_565, true);

        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(2);
        paint.setColor(Color.WHITE);

        Rect center = new Rect(SampleCameraControl.xtrans - 10, SampleCameraControl.ytrans - 10,
                SampleCameraControl.xtrans + 10, SampleCameraControl.ytrans + 10);

        canvas.drawRect(center, paint);

        imageAroundBallPub.cropAndPublish(mutableBitmap);

        ByteArrayOutputStream local_stream = new ByteArrayOutputStream();
        mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, local_stream);
        rawImageBuffer = local_stream.toByteArray();

        mutableBitmap.recycle();
    }
}
