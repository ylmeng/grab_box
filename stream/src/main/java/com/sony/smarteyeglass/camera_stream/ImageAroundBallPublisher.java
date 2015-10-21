package com.sony.smarteyeglass.camera_stream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
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
public class ImageAroundBallPublisher implements NodeMain {
    private ConnectedNode connectedNode;
    private Publisher<CompressedImage> imagePublisher;
    private Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
    private byte[] rawImageBuffer;
    private int cropWidth = 200, cropHeight = 200;
    private ChannelBufferOutputStream stream;

    private static ImageAroundBallPublisher instance = null;

    private ImageAroundBallPublisher() {
    }

    public static ImageAroundBallPublisher getInstance() {
        if(instance == null) {
            synchronized (ImageAroundBallPublisher.class) {
                if (instance == null) {
                    instance = new ImageAroundBallPublisher();
                }
            }
        }
        return instance;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("SonyCamera/imageAroundBall");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild("imageAroundBall");
        imagePublisher =
                connectedNode.newPublisher(resolver.resolve("image/compressed"),
                        CompressedImage._TYPE);
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

    public void cropAndPublish(Bitmap full_scene) {

        Bitmap bmpAroundObj= Bitmap.createBitmap(cropWidth, cropHeight, full_scene.getConfig());
        new Canvas(bmpAroundObj).drawBitmap(full_scene, cropWidth/2 - Ball.XTRANS, cropHeight/2 - Ball.YTRANS, null);

        ByteArrayOutputStream local_stream = new ByteArrayOutputStream();
        bmpAroundObj.compress(Bitmap.CompressFormat.JPEG, 100, local_stream);
        rawImageBuffer = local_stream.toByteArray();

        bmpAroundObj.recycle();

        Time currentTime = connectedNode.getCurrentTime();
        String frameId = "camera";

        CompressedImage image = imagePublisher.newMessage();
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

        cameraInfo.setWidth(cropWidth);
        cameraInfo.setHeight(cropHeight);
        cameraInfoPublisher.publish(cameraInfo);
    }
}
