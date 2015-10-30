package com.sony.smarteyeglass.camera_stream;

import android.util.Log;

/**
 * Created by chris on 10/20/15.
 */
public class Ball {
    public static final int WIDTH = 32, HEIGHT = 32;
    public static final int XTRANS = 195, YTRANS = 135;
    public static final int SCREEN_WIDTH = 419, SCREEN_HEIGHT = 138;
    public static final int NOT_FOUND_COORD = -1337;
    public static final double SPEED = 45;

    /*the coordinates of center point of the object*/
    private double x, y, targetX, targetY;
    private boolean locked;

    public Ball() {
        x = SCREEN_WIDTH/2;
        y = SCREEN_HEIGHT/2;
        targetX = NOT_FOUND_COORD;
        targetY = NOT_FOUND_COORD;
        locked = false;
    }

    public Ball(int x, int y) {
        setX(x);
        setY(y);
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
        setCenter(xDiff + x, yDiff + y);
    }

    /**
     * locks position of ball
     */
    public void lock() {
        locked = true;
    }

    /**
     * unlocks position of ball
     */
    public void unlock() {
        locked = false;
    }

    public void updatePos() {

        if(y == targetY && x == targetX) {
            return;
        }

        double netY = targetY - y;
        double netX = targetX - x;
        double distance = Math.sqrt(Math.pow(netX, 2) + Math.pow(netY, 2));

        if(distance <= SPEED) {
            setX(targetX);
            setY(targetY);
            return;
        }

        double theta;
        if(netX != 0) {
            theta = Math.atan(netY / netX);
        } else {
            theta = Math.PI/2 * netY / Math.abs(netY);
        }

        if(netX < 0) {
            theta += Math.PI;
        }

        double moveX = Math.cos(theta) * SPEED, moveY = Math.sin(theta) * SPEED;

        moveCenterBy(moveX, moveY);
    }

    /**
     * simple setter for the x coordinate to ensure the resulting coordinate is within a proper range
     * @param x
     * @return a boolean describing if the argument was stored or not
     */
    public boolean setX(double x) {
        if (locked) {
            return false;
        }

        if(x > SCREEN_WIDTH) {
            this.x = SCREEN_WIDTH;
            return false;
        }

        if(x < 0) {
            this.x = 0;
            return false;
        }

        this.x = x;
        return true;
    }

    /**
     * simple setter for the y coordinate to ensure the resulting coordinate is within a proper range
     * @param y
     * @return a boolean describing if the argument was stored or not
     */
    public boolean setY(double y) {
        if(locked) {
            return false;
        }

        if(y > SCREEN_HEIGHT) {
            this.y = SCREEN_HEIGHT;
            return false;
        }

        if(y < 0) {
            this.y = 0;
            return false;
        }

        this.y = y;
        return true;
    }

    /**
     * simple setter for the targetX coordinate
     * @param x
     * @return a boolean describing if the argument was stored or not
     */
    public boolean setTargetX(double x) {
        if(locked)
            return false;

        targetX = x;
        return true;
    }

    /**
     * simple setter for the targetY coordinate to ensure the resulting coordinate is within a proper range
     * @param y
     * @return a boolean describing if the argument was stored or not
     */
    public boolean setTargetY(double y) {
        if(locked)
            return false;

        targetY = y;
        return true;
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
     * simple getter for targetX coord
     * @return
     */
    public double getTargetX() {
        return targetX;
    }

    /**
     * simple getter for targetY coord
     * @return
     */
    public double getTargetY() {
        return targetY;
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
