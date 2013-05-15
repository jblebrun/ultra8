package com.emerjbl.ultra8;

import android.content.Context;

import android.util.Log;

import android.view.FocusFinder;
import android.view.MotionEvent;
import android.view.ViewGroup;

import android.widget.Button;

public class MTButton extends Button {

    public MTButton(Context c) {
        super(c);
    }

    public boolean onTouchEvent(MotionEvent ev)
    {
        return false;
        /*
        int action = ev.getAction();
        Log.i("noisy",ev.toString());
        if(action == MotionEvent.ACTION_MOVE) {
            return true;
        }

        ViewGroup vg = (ViewGroup)this.getParent().getParent();
        int x = (int)ev.getX();
        int y = (int)ev.getY();
        Log.i("ultra8-motioncoords", x+","+y);
        Log.i("ultra8-layoutvg", vg.toString());
        MTButton b = (MTButton)(FocusFinder.getInstance().findNearestTouchable(vg, x, y, FOCUS_DOWN, null));
        if(b != null) {
            Log.i("ultra8-layoutmotion", Integer.toString((Integer)b.getTag()));
        }
        Log.i("ultra8-motionevent", Integer.toString(action)+": "+this);
        if(action == MotionEvent.ACTION_POINTER_1_DOWN) {
            int val = (Integer)getTag();
            setPressed(true);
            Log.i("ultra8-p1down", Integer.toString(val));
            if(b != null) {
                b.setPressed(true);
            }
        }
        if(action== MotionEvent.ACTION_POINTER_2_DOWN) {
            int val = (Integer)getTag();
            Log.i("ultra8-p2down", Integer.toString(val));
            setPressed(true);
            if(b != null) {
                b.setPressed(true);
            }
        }
        if(action == MotionEvent.ACTION_POINTER_2_UP) {
            int val = (Integer)getTag();
            setPressed(false);
            Log.i("ultra8-p2up", Integer.toString(val));
            if(b != null) {
                b.setPressed(false);
            }
        }
        if(action == MotionEvent.ACTION_POINTER_2_DOWN) {
            int val = (Integer)getTag();
            Log.i("ultra8-p2down", Integer.toString(val));
            setPressed(true);
            if(b != null) {
                b.setPressed(true);
            }
        }

        if(action == MotionEvent.ACTION_DOWN) {
            int val = (Integer)getTag();
            Log.i("ultra8-buttondown", Integer.toString(val));
            setPressed(true);
            if(b != null) {
                b.setPressed(true);
            }
        }
        if(action == MotionEvent.ACTION_UP) {
            int val = (Integer)getTag();
            Log.i("ultra8-buttonup", Integer.toString(val));
            setPressed(false);
            if(b != null) {
                b.setPressed(false);
            }
        }
        return true;
        */
    }



}
