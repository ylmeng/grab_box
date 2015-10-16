package com.sony.smarteyeglass.camera_stream;

import android.content.Context;
import android.util.AttributeSet;

import org.ros.android.view.camera.CameraPreviewView;
import org.ros.android.view.camera.CompressedImagePublisher;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

/**
 * Created by csrobot on 9/10/15.
 */
public class SonyCameraPreviewView extends CameraPreviewView implements NodeMain {

    public SonyCameraPreviewView(Context context) {
        super(context);
    }

    public SonyCameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SonyCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ros_camera_preview_view");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        setRawImageListener(new CompressedImagePublisher(connectedNode));
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
