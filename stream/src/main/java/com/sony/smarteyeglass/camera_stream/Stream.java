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

public class Stream extends RosActivity implements View.OnTouchListener
{
    private int cameraId;
    private CompressedVideoView imageSubscriber;
    private SelectionSub boxSubscriber;
    private SonyCameraPublisher imagePublisher;

    public Stream() {
        super("SonyCamera", "SonyCamera"/*, URI.create("http://10.10.10.93:11311")*/);
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

        imageSubscriber.setOnTouchListener(this);
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
        imagePublisher = SonyCameraPublisher.getInstance();
        boxSubscriber = SelectionSub.getInstance();
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(imagePublisher, nodeConfiguration);
            nodeMainExecutor.execute(imageSubscriber, nodeConfiguration);
            nodeMainExecutor.execute(boxSubscriber, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        SampleCameraControl.moveSquare((int)(motionEvent.getX() / view.getWidth() * 400),
                (int)(motionEvent.getY() / view.getHeight() * 100));
        return false;
    }
}
