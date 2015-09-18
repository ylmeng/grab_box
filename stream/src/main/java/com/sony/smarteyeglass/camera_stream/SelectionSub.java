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
public class SelectionSub implements NodeMain {

    public static SelectionSub instance;
    public static android.graphics.Point boxPos = new android.graphics.Point(200, 10);

    public static SelectionSub getInstance() {
        if(instance == null) {
            synchronized (SonyCameraPublisher.class) {
                if (instance == null) {
                    instance = new SelectionSub();
                }
            }

        }
        return instance;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Point> subscriber = connectedNode.newSubscriber("sony_box_selection_topic", geometry_msgs.Point._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Point>() {
            @Override
            public void onNewMessage(geometry_msgs.Point message) {
                boxPos.x = (int)message.getX();
                boxPos.y = (int)message.getY();
                Log.d(Constants.LOG_TAG, "I heard: \"" + message.getX() + " y: "+ message.getY() + "\"");
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
        return GraphName.of("sony_selection_box");
    }
}
