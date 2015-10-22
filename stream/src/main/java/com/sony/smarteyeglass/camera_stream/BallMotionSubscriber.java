package com.sony.smarteyeglass.camera_stream;

import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

/**
 * Created by chris on 9/18/15.
 */

public class BallMotionSubscriber implements NodeMain {

    private static BallMotionSubscriber instance;
    private Ball ball = new Ball();

    public static BallMotionSubscriber getInstance() {
        if(instance == null) {
            synchronized (BallMotionSubscriber.class) {
                if (instance == null) {
                    instance = new BallMotionSubscriber();
                }
            }
        }
        return instance;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<std_msgs.Bool> subscriber = connectedNode.newSubscriber("/ball_mover/isHandPresent", std_msgs.Bool._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.Bool>() {
            @Override
            public void onNewMessage(std_msgs.Bool message) {

                //ball was not set so we cannot move it yet
                if(ball == null) {
                    return;
                }

                //if the hand is present & the correct gesture is being made
                if(message.getData() && GestureSubscriber.getInstance().lastGesture == GestureSubscriber.LSHAPEDSTRETCH) {
                    SampleCameraControl.getInstance().proceed();
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
        return GraphName.of("sony_ball_motion_sub");
    }

    public void setBall(Ball b) {
        ball = b;
    }

    public Ball getBall() {
        return ball;
    }
}
