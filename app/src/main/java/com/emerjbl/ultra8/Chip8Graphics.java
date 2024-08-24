package com.emerjbl.ultra8;

import java.util.Arrays;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

public class Chip8Graphics {
    static final int WIDTH = 64;
    static final int HEIGHT = 32;
    static final int fblen = WIDTH * HEIGHT;
    static final int HWIDTH = 128;
    static final int HHEIGHT = 64;
    static final int hfblen = HWIDTH * HHEIGHT;
    static final int[] masks = {0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01};
    static final int FADERATE = 0x08000000;
    static final int SETCOLOR = 0xFF00FF00;
    static final long lmask = 0xFFFFFFFFL;
    static final long FADEFLOOR = SETCOLOR - 2 * FADERATE;
    StringBuilder outString = new StringBuilder();
    Bitmap b;
    Bitmap hb;
    ImageView v;
    boolean stopped = true;

    static final int[] framebuffer = new int[WIDTH * HEIGHT];
    static final int[] hframebuffer = new int[HWIDTH * HHEIGHT];
    long lastDraw = SystemClock.uptimeMillis();
    boolean hires = false;
    Handler h;
    Runnable r;

    public void rscroll() {

    }

    public void lscroll() {

    }

    public void scrolldown(int n) {

    }

    class DrawRunnable implements Runnable {
        int counter = 0;
        int frameTime = 0;

        public void run() {
            long start = SystemClock.uptimeMillis();
            Bitmap bitmap;
            int[] fb;
            int len, width, height;
            if (hires) {
                len = hfblen;
                fb = hframebuffer;
                bitmap = hb;
                width = HWIDTH;
                height = HHEIGHT;

            } else {
                bitmap = b;
                fb = framebuffer;
                len = fblen;
                width = WIDTH;
                height = HEIGHT;
            }

            for (int i = 0; i < len; i++) {
                if (fb[i] != SETCOLOR && fb[i] != 0) {
                    if ((fb[i] & lmask) > (FADEFLOOR & lmask)) {
                        fb[i] -= FADERATE;
                    } else {
                        fb[i] = 0;
                    }
                }
            }
            bitmap.setPixels(fb, 0, width, 0, 0, width, height);
            v.setImageBitmap(bitmap);

            v.postInvalidate();
            lastDraw = lastDraw + 15;
            if (!stopped) {
                h.postAtTime(this, lastDraw + 15);
            }
            long end = SystemClock.uptimeMillis();
            frameTime += (end - start);
            if (counter++ > 100) {
                Log.i("ultra8", "still drawing... " + frameTime / 100 + " ms/frame");
                counter = 0;
                frameTime = 0;
            }
        }
    }

    public void stop() {
        stopped = true;
    }

    public void start() {
        stopped = false;
        h.postAtTime(r, lastDraw + 50);
    }

    public Chip8Graphics(ImageView v) {
        this.v = v;
        b = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        hb = Bitmap.createBitmap(HWIDTH, HHEIGHT, Bitmap.Config.ARGB_8888);

        b.setDensity(4);
        hb.setDensity(2);
        Log.i("Ultra8", "made bitmap " + b.getHeight() + "x" + b.getWidth());

        h = new Handler();
        r = new DrawRunnable();
    }

    boolean putSprite(int xbase, int ybase, int[] data, int offset, int lines) {
        boolean unset = false;
        int row;
        int height, width;
        int bytesPerRow = 1;
        int[] fb;
        if (hires) {
            height = HHEIGHT;
            width = HWIDTH;
            fb = hframebuffer;
        } else {
            height = HEIGHT;
            width = WIDTH;
            fb = framebuffer;
        }
        if (lines == 0) {
            lines = 16;
            bytesPerRow = 2;
        }
        for (int yoffset = 0; yoffset < lines; yoffset += bytesPerRow) {
            for (int bpr = 0; bpr < bytesPerRow; bpr++) {
                row = data[offset + yoffset + bpr];
                //Log.i("ultra8","doing sprite byte "+Integer.toHexString(row)+" from "+(offset+yoffset+bpr));
                for (int xoffset = 0; xoffset < 8; xoffset++) {
                    if ((row & masks[xoffset]) != 0) {
                        int i = (ybase + yoffset & height - 1) * width + ((xbase + xoffset + bpr * 8) & width - 1);
                        unset = unset | (fb[i] == SETCOLOR);
                        fb[i] = fb[i] == SETCOLOR ? SETCOLOR - FADERATE : SETCOLOR;
                    }
                }
            }
        }
        return unset;
    }

    public void clearScreen() {
        Arrays.fill(framebuffer, 0);
        Arrays.fill(hframebuffer, 0);
        b.eraseColor(0);
        hb.eraseColor(0);
    }
}
