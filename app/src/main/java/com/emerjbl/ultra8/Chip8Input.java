package com.emerjbl.ultra8;

public class Chip8Input {

    boolean[] keys = new boolean[16];
    public boolean reset;
    public boolean pause;
    public boolean hurry = false;

    public boolean keyPressed(int x) {
        return keys[x];
    }

    public synchronized void reset() {
        reset = true;
        notify();
    }

    public void clear() {
        reset = false;
    }

    public void pause() {
        pause = true;
    }

    public void resume() {
        notify();
        pause = false;
    }

    public synchronized void setKey(int x) {
        keys[x] = true;
        notify();
    }

    public void resetKey(int x) {
        keys[x] = false;
    }

    public int checkForPress() {
        for (int i = 15; i >= 0; i--) {
            if (keys[i]) {
                return i;
            }
        }
        return -1;
    }


}
