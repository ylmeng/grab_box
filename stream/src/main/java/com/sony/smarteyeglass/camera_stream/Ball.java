package com.sony.smarteyeglass.camera_stream;

import android.util.Log;

/**
 * Created by chris on 10/20/15.
 */
public class Ball {
    public static final int XTRANS = 195, YTRANS = 135;

    /*the coordinates of center point of the object*/
    private double x, y;

    public Ball() {
        setX(190);
        setY(20);
    }

    /**
     * sets center location of the cube in reference to the coordinate system
     * displayed within the glass
     * @param x
     * @param y
     */
    public void setCenter(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Moves the coordinates of the box by a provided amount
     * @param xDiff the change in x
     * @param yDiff the change in y
     */
    public void moveCenterBy(double xDiff, double yDiff) {
        setCenter(3*xDiff + x, 3*yDiff + y); //Because xDiff and yDiff are typically 0-1 I am multiplying by 3
    }                                        //to increase the speed at which the object moves

    /**
     * simple setter for the x coordinate to ensure the resulting coordinate is within a proper range
     * @param x
     * @return a boolean describing if the argument provided was too large/small
     */
    public boolean setX(double x) {
        if(x <= 190 && x >= 0) {
            this.x = x;
            return true;
        } else {
            Log.d("mySony", "Did not move ball's x because it reached a bound");
            return false;
        }
    }

    /**
     * simple setter for the y coordinate to ensure the resulting coordinate is within a proper range
     * @param y
     * @return a boolean describing if the argument provided was too large/small
     */
    public boolean setY(double y) {
        if(y <= 118 && y >= 0) {
            this.y = y;
            return true;
        } else {
            Log.d("mySony", "Did not move ball's y because it reached a bound");
            return false;
        }
    }

    /**
     * simple getter for x coord
     * @return
     */
    public double getX() {
        return x;
    }

    /**
     * simple getter for y coord
     * @return
     */
    public double getY() {
        return y;
    }

    /**
     * 
     * @return
     */
    public double getXInImage() {
        return x + XTRANS;
    }

    /**
     * simple getter
     * @return
     */
    public double getYInImage() {
        return y + YTRANS;
    }
}
