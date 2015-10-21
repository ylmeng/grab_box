package com.sony.smarteyeglass.camera_stream;

import android.util.Log;

import geometry_msgs.Point;

/**
 * Created by chris on 10/20/15.
 */
public class Ball {
    public static final int XTRANS = 195, YTRANS = 135;
    private double x, y;
    //possibly velocity

    public Ball() {
        setX(0);
        setY(0);
    }

    public void setCenter(double x, double y) {
        setX(x);
        setY(y);
    }

    public void setCenter(geometry_msgs.Point point) {
        setCenter(point.getX(), point.getY());
    }

    public void moveCenterBy(double x, double y) {
        setCenter(3*x + this.x, 3*y + this.y);
    }

    public void moveCenterBy(geometry_msgs.Point point) {
        moveCenterBy(point.getX(), point.getY());
    }

    public boolean setX(double x) {
        if(x <= 319 && x >= 0) {
            this.x = x;
            return true;
        } else {
            Log.d("mySony", "Did not move ball's x because it reached a bound");
            return false;
        }
    }

    public boolean setY(double y) {
        if(y <= 118 && y >= 0) {
            this.y = y;
            return true;
        } else {
            Log.d("mySony", "Did not move ball's y because it reached a bound");
            return false;
        }
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }

    public double getXInImage() {
        return x + XTRANS;
    }
    public double getYInImage() {
        return y + YTRANS;
    }
}
