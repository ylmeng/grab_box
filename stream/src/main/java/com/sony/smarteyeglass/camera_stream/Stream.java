package com.sony.smarteyeglass.camera_stream;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import org.ros.android.MasterChooser;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;

public class Stream extends RosActivity
{
    private GestureSubscriber gestureSub;
    private BallMotionSubscriber ballMotionSub;
    private CompressedVideoView imageSubscriber;
    private SonyCameraPublisher imagePublisher;
    private ImageAroundBallPublisher imageAroundBallPub;

    public Stream() {
        super("SonyCamera", "SonyCamera");
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        imageSubscriber = (CompressedVideoView) findViewById(R.id.ros_camera_preview_view);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopService(new Intent(this, SampleExtensionService.class));
        stopService(new Intent(this, ExtensionReceiver.class));
        stopService(new Intent(this, MasterChooser.class));
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        //instantiates ros objects
        gestureSub = GestureSubscriber.getInstance();
        ballMotionSub = BallMotionSubscriber.getInstance();
        imagePublisher = SonyCameraPublisher.getInstance();
        imageAroundBallPub = ImageAroundBallPublisher.getInstance();

        try {

            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(gestureSub, nodeConfiguration);
            nodeMainExecutor.execute(ballMotionSub, nodeConfiguration);
            nodeMainExecutor.execute(imagePublisher, nodeConfiguration);
            nodeMainExecutor.execute(imageAroundBallPub, nodeConfiguration);
            nodeMainExecutor.execute(imageSubscriber, nodeConfiguration);

        } catch (IOException e) {
            // Socket problem
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }
    }
}

