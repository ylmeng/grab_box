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

public class TipPointSubscriber implements NodeMain {

    private static TipPointSubscriber instance;
    private Ball ball = new Ball();

    public static TipPointSubscriber getInstance() {
        if(instance == null) {
            synchronized (TipPointSubscriber.class) {
                if (instance == null) {
                    instance = new TipPointSubscriber();
                }
            }
        }
        return instance;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Point> subscriber = connectedNode.newSubscriber("/ball_mover/tip_point", geometry_msgs.Point._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Point>() {
            @Override
            public void onNewMessage(geometry_msgs.Point message) {

                //do not update ball's position if the ball is not found
                if(ball == null || message.getX() == Ball.NOT_FOUND_COORD
                        || message.getY() == Ball.NOT_FOUND_COORD
                        || GestureSubscriber.getInstance().lastGesture != GestureSubscriber.GRASPING) {
                    return;
                }

                ball.setTargetX(message.getX() * SampleCameraControl.xmag + SampleCameraControl.xtrans);
                ball.setTargetY(message.getY() * SampleCameraControl.ymag + SampleCameraControl.ytrans);

                SampleCameraControl.getInstance().processEndPoint(
                        (int) ball.getX(), (int) ball.getY());

                Log.d(Constants.LOG_TAG, "I heard: " + message.getX() + " " + message.getY());
                Log.d(Constants.LOG_TAG, "ball is: " + ball.getX() + " " + ball.getY());
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
