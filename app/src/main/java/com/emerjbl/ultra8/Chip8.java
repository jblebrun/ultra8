package com.emerjbl.ultra8;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import android.media.AudioManager;
import android.media.ToneGenerator;

import android.os.SystemClock;
import android.util.Log;

public class Chip8 {
    final int[] mem = new int[4096];

    final Chip8Graphics gfx;
    final Chip8Input input;
    final Random random = new Random();
    final Chip8Timer timer = new Chip8Timer();
    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);


    Thread runThread;

    int[] font = new int[]{
            0xF0, 0x90, 0x90, 0x90, 0xF0,
            0x20, 0x60, 0x20, 0x20, 0x70,
            0xF0, 0x10, 0xF0, 0x80, 0xF0,
            0xF0, 0x10, 0x70, 0x10, 0xF0,
            0xA0, 0xA0, 0xF0, 0x20, 0x20,
            0xF0, 0x80, 0xF0, 0x10, 0xF0,
            0xF0, 0x80, 0xF0, 0x90, 0xF0,
            0xF0, 0x10, 0x10, 0x10, 0x10,
            0x60, 0x90, 0x60, 0x90, 0x60,
            0xF0, 0x90, 0xF0, 0x10, 0x10,
            0x60, 0x90, 0xF0, 0x90, 0x90,
            0xE0, 0x90, 0xE0, 0x90, 0xE0,
            0xF0, 0x80, 0x80, 0x80, 0xF0,
            0xE0, 0x90, 0x90, 0x90, 0xE0,
            0xF0, 0x80, 0xF0, 0x80, 0xF0,
            0xF0, 0x80, 0xE0, 0x80, 0x80
    };


    int opCount;
    long startTime;
    long endTime;
    boolean running = false;
    boolean calling = false;
    boolean waitingForKey = false;
    final int execStart = 0x200;
    final int fontStart = 0x100;
    int opsPerSecond = 500;

    public Chip8(Chip8Graphics gfx, Chip8Input input) {
        this.gfx = gfx;
        this.input = input;
        System.arraycopy(font, 0, mem, fontStart, font.length);
    }

    public void stop() throws InterruptedException {
        Log.i("ultra8", "stopping Chip8");
        if (runThread != null) {
            Log.i("ultra8", "threre's a thread to stop");
            running = false;
            runThread.interrupt();
            Log.i("ultra8", "waiting for run thread to complete");
            runThread.join();
            runThread = null;
        }
        Log.i("ultra8", "reset complete");
    }

    public void reset() {
        try {
            Log.i("ultra8", "resetting");
            stop();
        } catch (InterruptedException e) {
            Log.i("ultra8", "interrupted while resetting");
            e.printStackTrace();
        }
        startThread();
    }


    public void loadProgram(int[] data) {
        System.arraycopy(data, 0, mem, execStart, data.length);
    }

    public void loadProgram(InputStream file) {
        int at = execStart;
        int next;
        try {
            while ((next = file.read()) != -1) {
                mem[at++] = next;
            }
        } catch (IOException ex) {
            //???
        }
    }

    public synchronized final int runOps() throws InterruptedException {
        int X, Y, i, b1, b2, word = 0, maj_op, sub_op, nnn, tmp;
        final int[] V = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final int[] HP = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        final int[] stack = new int[64];
        final int[] timeCounts = new int[100];
        int opTime = 0;
        Arrays.fill(timeCounts, 0, 99, 0);
        int SP = 0;
        int I = 0;
        int PC = 0x200;
        //String dasm = "";
        long opEndMillis = 0;
        long opStartMillis = 0;
        gfx.clearScreen();
        gfx.hires = false;
        gfx.start();
        while (running) {
            opEndMillis = SystemClock.uptimeMillis();
            //Log.i("ultra8", "Ran instruction "+Integer.toHexString(word)+" in "+opEndMillis+"-"+opStartMillis+" = "+(opEndMillis-opStartMillis)+"ms");
            opTime = (int) (opEndMillis - opStartMillis);
            timeCounts[opTime > 99 ? 99 : opTime]++;
            opStartMillis = SystemClock.uptimeMillis();
            if (input.hurry) {
                Thread.sleep(0, 100);
            } else {
                Thread.sleep(1, 500000);
            }
            if (input.reset) {
                running = false;
                continue;
            }
            if (input.pause) {
                Log.i("ultra8", "Chip8 is waiting due to pause...");
                gfx.stop();
                synchronized (input) {
                    input.wait();
                }
                Log.i("ultra8", "Chip8 is restarting after pause...");
                gfx.start();
            }
            calling = false;
            //Log.i("ultra8-dasm", PC+": "+dasm);
            b1 = mem[PC++];
            b2 = mem[PC++];
            word = (b1 << 8) | b2;
            maj_op = b1 & 0xF0;
            nnn = word & 0xFFF;
            sub_op = b2 & 0x0F;
            tmp = 0;
            X = b1 & 0xF;
            Y = (b2 >> 4);
            opCount++;

            //dasm = "" + Integer.toHexString(word);
            /* Opcode clases defined in upper nibble */
            switch (maj_op) {

                case 0x00: /* NATIVE OP */
                    switch (b2) {
                        case 0xE0: /* Clear screen */
                            //dasm = "CLR";
                            gfx.clearScreen();
                            break;
                        case 0xEE: /* Return */
                            //dasm = "RET (SP="+SP+")";
                            if (SP < 0) {
                                running = false;
                                Log.i("ultra8", "FATAL: RET when stack is empty");
                                break;
                            }
                            PC = stack[--SP];
                            break;
                        case 0xFB:
                            gfx.rscroll();
                            break;

                        case 0xFC:
                            gfx.lscroll();
                            break;

                        case 0xFD:
                            running = false;
                            Log.i("ultra8", "Normal: EXIT instruction");
                            break;

                        case 0xFE:
                            //dasm = "LORES";
                            gfx.hires = false;
                            break;
                        case 0xFF:
                            //dasm = "HIRES";
                            gfx.hires = true;
                            break;
                        default:
                            if (Y == 0xC) {
                                gfx.scrolldown(sub_op);
                            } else {
                                Log.i("ultra8",
                                        "FATAL: trying to execute 0 byte, program missing halt");
                                running = false;
                            }
                    }
                    break;

                /* CALL NNN */
                case 0x20:
                    calling = true;
                    stack[SP++] = PC;
                    /* interntional fallthru to jp */
                    /* JP NNN */

                case 0x10:

                    //dasm = (calling ? "CALL " : "JP ") + nnn;
                    if (PC == nnn + 2) {
                        Log.i("Ultra8",
                                "normal: would spin in endless loop, stopping emulation");
                        running = false;
                    } else {
                        PC = nnn;
                    }
                    break;

                /* SE X NN */
                case 0x30:
                    //dasm = "SKE V[" + X + "] " + b2;
                    if (V[X] == b2)
                        PC += 2;
                    break;

                /* SNE X NN */
                case 0x40:
                    //dasm = "SKNE V[" + X + "] " + b2;
                    if (V[X] != b2) {
                        PC += 2;
                    }
                    break;

                /* SE X Y 0 */
                case 0x50:
                    if (sub_op != 0) {
                        Log.i("ultra8", "FATAL: Illegal opcode " + Integer.toHexString(word));
                        running = false;
                    }
                    //dasm = "SKEQ V[" + X + "], V[" + Y +"]";
                    if (V[X] == V[Y]) {
                        PC += 2;
                    }
                    break;

                /* LD X NN */
                case 0x60:
                    //dasm = "LD V[" + X + "] " + b2;
                    V[X] = b2;
                    break;

                /* ADD X NN */
                case 0x70:
                    //dasm = "ADD V["+X+"], "+b2+" = ";
                    V[X] = V[X] + b2;
                    V[15] = ((V[X] & 0x100) != 0) ? 1 : 0;
                    V[X] &= 0xFF;
                    //dasm += V[X];
                    break;

                /* Various ops 8XYO */
                case 0x80:
                    switch (sub_op) {
                        /* LD X Y */
                        case 0x00:
                            //dasm = "MOV "+X+","+Y;
                            V[X] = V[Y];
                            break;

                        /* OR X Y */
                        case 0x01:
                            //dasm = "OR "+X+","+Y;
                            V[X] |= V[Y];
                            break;

                        /* AND X Y */
                        case 0x02:
                            V[X] &= V[Y];
                            break;

                        /* XOR X Y */
                        case 0x03:
                            V[X] ^= V[Y];
                            break;

                        /* ADD X Y V[15] = carry */
                        case 0x04:
                            //dasm = "ADD V["+X+"]("+V[X]+"), V["+Y+"]("+V[Y]+") = ";
                            V[X] += V[Y];
                            V[X] &= 0xFF;
                            V[15] = ((V[X] & 0x100) != 0) ? 1 : 0;
                            //dasm += V[X]+"("+V[15]+")";
                            break;

                        /* SUB X Y V[15] = !borrow */
                        case 0x05:
                            //dasm = "SUB V["+X+"]("+V[X]+"), V["+Y+"]("+V[Y]+") = ";
                            V[X] = (V[X] - V[Y]);
                            V[15] = ((V[X] & 0x100) == 0 ? 1 : 0);
                            V[X] &= 0xFF;
                            //dasm += V[X]+"("+V[15]+")";
                            break;

                        /* SR X 1 V[15] = lsb before shift */
                        case 0x06:
                            V[15] = (V[X] & 0x01);
                            V[X] >>>= 1;
                            break;

                        /* SUB Y X V[15] = borrow */
                        case 0x07:
                            //dasm = "SUBN V["+X+"]("+V[X]+"), V["+Y+"]("+V[Y]+") = ";
                            V[X] = (V[Y] - V[X]);
                            V[15] = ((V[X] & 0x100) == 0 ? 0 : 1);
                            V[X] &= 0xFF;
                            //dasm += V[X]+"("+V[15]+")";

                            break;

                        /* SL X 1 V[15] = msb before shift */
                        case 0x0E:
                            V[15] = ((V[X] & 0x80) == 0x80 ? 1 : 0);
                            V[X] <<= 1;
                            V[X] &= 0xFF;
                            break;
                        default:
                            Log.i("Ultra8", "FATAL: Illegal opcdoe " + word);
                            running = false;
                    }
                    break;

                /* SNE X Y 0 */
                case 0x90:
                    if (sub_op != 0) {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe " + word);
                        running = false;
                    }
                    if (V[X] != V[Y])
                        PC += 2;
                    break;

                /* LD I NNN */
                case 0xA0:
                    I = nnn;
                    break;

                /* JP NNN + V[0] */
                case 0xB0:
                    PC = V[0] + nnn;
                    break;

                /* RND NN */
                case 0xC0:
                    V[X] = random.nextInt(b2 + 1);
                    //dasm = "LD RND, " + b2 + "V[" + X + "] = " + V[X];
                    break;

                /* DRAW X Y N - draw sprite at X,Y with height N */
                case 0xD0:
                    //dasm = "DRW " + V[X] + "," + V[Y] + "," + sub_op;
                    V[15] = gfx.putSprite(V[X], V[Y], mem, I, sub_op) ? 1 : 0;
                    break;

                /* input commands */
                case 0xE0:
                    switch (b2) {
                        case 0x9E:
                            //Log.i("ultra8","checking keypress: "+V[X]);
                            if (input.keyPressed(V[X])) {
                                PC += 2;
                            }
                            break;
                        case 0xA1:
                            //Log.i("ultra8","checking key not press: "+V[X]);
                            if (!input.keyPressed(V[X])) {
                                PC += 2;
                            }
                            break;
                        default:
                            Log.i("Ultra8", "FATAL: Illegal opcdoe " + word);
                            running = false;
                    }
                    break;
                /* Sound, timer, blocking: FXOO */
                case 0xF0:
                    switch (b2) {
                        case 0x07:
                            V[X] = timer.getValue();
                            //dasm = "gdelay V["+X+"] = "+V[X];
                            break;
                        case 0x0A:
                            int pressed = -1;
                            if (input != null) {
                                synchronized (input) {
                                    pressed = input.checkForPress();
                                    if (pressed == -1) {
                                        input.wait();
                                    }
                                    pressed = input.checkForPress();
                                    Log.i("ultra8", "Waited and got key " + pressed);
                                    V[X] = pressed;
                                }
                            }
                            break;
                        case 0x15:
                            //dasm = "sdelay V["+X+"]("+V[X]+")";
                            timer.setValue(V[X]);
                            break;
                        case 0x18:
                            //Log.i("ultra8","BEEP");
                            //if (sound != null) {
                            //	sound.setValue(V[X]);
                            //}
                            //tg.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, (V[X]+3) * 17);
                            break;
                        case 0x1E:
                            I += V[X];
                            break;
                        // Set I to location of font sprite in X
                        case 0x29:
                            I = (fontStart + V[X] * 5);
                            //dasm = "LDI LF[" + X + "]" + "(" + V[X] + ") = " + I;

                            break;
                        // Store BCD of X in M[I..I+2]
                        case 0x33:
                            tmp = V[X];
                            for (i = 0; tmp > 99; i++) {
                                tmp -= 100;
                            }
                            mem[I] = i;
                            for (i = 0; tmp > 9; i++) {
                                tmp -= 10;
                            }
                            mem[I + 1] = i;
                            for (i = 0; tmp > 0; i++) {
                                tmp--;
                            }
                            mem[I + 2] = i;
                            break;
                        // Store X registers starting at I
                        case 0x55:
                            //dasm = "LD [" + I + "],V[" + (b1 & 0x0F) + "]";
                            System.arraycopy(V, 0, mem, I, X + 1);
                            break;

                        // Load X registers starting at I
                        case 0x65:
                            //dasm = "LD HP[" + X + "], [" + I + "]";
                            System.arraycopy(mem, I, V, 0, X + 1);
                            break;
                        // Store X HP registers starting at I
                        case 0x75:
                            //dasm = "LD [" + I + "],V[" + (b1 & 0x0F) + "]";
                            System.arraycopy(HP, 0, mem, I, X + 1);
                            break;

                        // Load X registers starting at I
                        case 0x85:
                            //dasm = "LD HP[" + X + "], [" + I + "]";
                            System.arraycopy(mem, I, HP, 0, X + 1);
                            break;


                        default:
                            Log.i("Ultra8", "FATAL: Illegal opcdoe " + word);
                            running = false;

                    }
                    break;

                default:
                    Log.i("Ultra8", "FATAL: Illegal opcdoe " + word);
                    running = false;
            }
        }
        Log.i("ultra8", timeCounts.toString());
        return PC;
    }

    private class CPUThread extends Thread {
        public void run() {
            int PC = 0;
            try {
                running = true;
                startTime = SystemClock.uptimeMillis();
                PC = runOps();
                //Wait for last pixels to fade.
                Thread.sleep(2000);
                gfx.stop();
                runThread = null;
            } catch (InterruptedException ex) {
                Log.i("ultra8", "machine was interrupted. That's fine.");
                gfx.stop();
            }
            endTime = SystemClock.uptimeMillis();
            Log.i("ultra8", "Finished at PC " + PC);
            Log.i("ultra8", "Executed " + opCount + " ops in " + (endTime - startTime) + "ms");
            Log.i("ultra8", 1000L * opCount / (endTime - startTime) + "ops/sec");

            int SETCOLOR = 0xFF00FF00;
            int FADERATE = 0x08000000;
            int temp = SETCOLOR - FADERATE;
            long lmask = 0xFFFFFFFFL;
            Log.i("utlra8", "btw, temp is " + Integer.toHexString(temp));
            Log.i("utlra8", "btw, temp > FADERATE is " + (temp > FADERATE));
            Log.i("utlra8", "btw, cast temp > FADERATE is " + ((temp & lmask) > (FADERATE & lmask)));
            Log.i("utlra8", "btw, temp < SETCOLOR is " + (temp > SETCOLOR));
            Log.i("utlra8", "btw, cast temp < SETCOLOR is " + ((temp & lmask) > (SETCOLOR & lmask)));

        }
    }

    public void startThread() {
        Log.i("ultra8", "starting Chip8");
        if (runThread == null) {
            Log.i("ultra8", "creating new machine thread");
            runThread = new CPUThread();
            runThread.start();
            Log.i("ultra8", "started cpu");
        }
    }


}
