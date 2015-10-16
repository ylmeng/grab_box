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

    public static GestureSubscriber instance;
    public static int lastGesture = 0;

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
                lastGesture = message.getData();
                SampleCameraControl.getInstance().handleGesture(lastGesture);
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
