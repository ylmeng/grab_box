package com.sony.smarteyeglass.camera_stream;

import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import std_msgs.Int32;

/**
 * Created by chris on 9/18/15.
 */

public class GestureSubscriber implements NodeMain {

    /* static reference to the only gesture subscriber that should be in use */
    public static GestureSubscriber instance;

    /* contains the last pose number read from myo */
    public int lastGesture = 0;
    public long lastGestureTime = -1;
    public static int GRASPING = 1;

    public static GestureSubscriber getInstance() {
        if(instance == null) {
            synchronized (GestureSubscriber.class) {
                if (instance == null) {
                    instance = new GestureSubscriber();
                }
            }
        }
        return instance;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<std_msgs.Int32> subscriber = connectedNode.newSubscriber("/myo_raw/gesture_num", std_msgs.Int32._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.Int32>() {
            @Override
            public void onNewMessage(std_msgs.Int32 message) {

                int prevLastGesture = lastGesture;
                lastGesture = message.getData();

                if (lastGesture != 0 && prevLastGesture != 0) {
                    lastGestureTime = System.nanoTime();
                }

                if(SampleCameraControl.getInstance() != null &&
                        SampleCameraControl.getInstance().gameState == SampleCameraControl.State.INITIAL
                        && lastGesture == 1) {
                    SampleCameraControl.getInstance().gameState = SampleCameraControl.State.LVL1;
                }

                Log.d(Constants.LOG_TAG, "I heard: \"" + message + "\"");
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

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sony_gesture_sub");
    }
}