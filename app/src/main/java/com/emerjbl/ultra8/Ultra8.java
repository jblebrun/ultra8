package com.emerjbl.ultra8;

import java.lang.reflect.Field;

import android.app.Activity;

import android.content.Context;

import android.media.AudioManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class Ultra8 extends Activity implements View.OnTouchListener {

    int duration = Toast.LENGTH_SHORT;
    Toast toast;
    static Toast toastNotYet;
    Chip8 machine;
    Chip8Input input;
    Button[] buttonViews = new Button[16];
    int[] buttonVals = new int[]{1, 2, 3, 0xC, 4, 5, 6, 0xD, 7, 8, 9, 0xE, 0xA, 0, 0xB, 0xF};
    int[] pressed = new int[]{-1, -1, -1};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu sampleItems = menu.addSubMenu("Programs");
        sampleItems.setIcon(R.drawable.ic_menu_agenda);
        for (Field f : R.raw.class.getFields()) {
            try {
                Log.i("ultra8", "Field f: " + f.getName() + ": " + f.getInt(null));
                sampleItems.add(Menu.NONE, f.getInt(null), Menu.NONE, f.getName());
            } catch (IllegalArgumentException e) {
                // skip it.
            } catch (IllegalAccessException e) {
                // skip it
            }
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.run_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Log.i("ultra8", "item id: " + item.getItemId());
        int itemId = item.getItemId();
        if (itemId == R.id.menu_open) {
            toastNotYet.show();
            return true;
        } else if (itemId == R.id.menu_settings) {
            toastNotYet.show();
            return true;
        } else if (itemId == R.id.menu_edit) {
            toastNotYet.show();
            return true;
        } else if (itemId == R.id.menu_reset) {
            machine.reset();
            return true;
        } else {
            //If it's not one of the above menu items, then hopefully it's one of the 
            //dynamically generated program submenu items. Try to load that.
            try {
                machine.loadProgram(getResources().openRawResource(item.getItemId()));
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.i("ultra8", "returning superclass item selected callback");
                return super.onOptionsItemSelected(item);
            }

        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.i("ultra8", "on pause...");
        synchronized (input) {
            input.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("ultra8", "on resume...");
        synchronized (input) {
            input.resume();
        }
        Log.i("ultra8", "finished resume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("ultra8", "on stop...");
    }

    public boolean onTouch(View v, MotionEvent me) {
        int action = me.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = me.getAction() >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        int pointerId = me.getPointerId(pointerIndex);
        int pointerCount = me.getPointerCount();
        int x = 0, y = 0, button_x = 0, button_y = 0, button_index = -1;

        final int ta_width = v.getMeasuredWidth();
        final int ta_height = v.getMeasuredHeight();

        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < pointerCount; i++) {
                x = (int) Math.floor(me.getX(i));
                y = (int) Math.floor(me.getY(i));
                button_y = 4 * y / ta_height;
                button_x = 4 * x / ta_width;
                button_index = button_y * 4 + button_x;
                if (pressed[i] >= 0 && pressed[i] < 16) {
                    buttonViews[pressed[i]].setPressed(false);
                    input.resetKey(buttonVals[pressed[i]]);
                }
                if (button_index >= 0 && button_index < 16) {
                    input.setKey(buttonVals[button_index]);
                    buttonViews[button_index].setPressed(true);
                }

                pressed[i] = button_index;
            }
            //Log.i("ultra8-buttons", pointerCount+" pointers --- "+pointerIndex+":"+pointerId+"... action_move");
            //Log.i("ultra8-noisy", me.getX()+","+me.getY()+"; "+me.getX(1)+", "+me.getY(1));
            return true;
        }
        x = (int) Math.floor(me.getX(pointerIndex));
        y = (int) Math.floor(me.getY(pointerIndex));
        button_y = 4 * y / ta_height;
        button_x = 4 * x / ta_width;
        button_index = button_y * 4 + button_x;
        Log.i("ultra8-pointercount", pointerCount + ";");
        if (button_index >= 0 && button_index < 16 && (action == MotionEvent.ACTION_DOWN)) {
            Log.i("ultra8-buttons", pointerIndex + ":" + pointerId + "... action_down: " + button_index);
            pressed[pointerId] = button_index;
            input.setKey(buttonVals[button_index]);
            buttonViews[button_index].setPressed(true);
        }
        if (button_index >= 0 && button_index < 16 && (action == MotionEvent.ACTION_POINTER_DOWN)) {
            Log.i("ultra8-buttons", pointerIndex + ":" + pointerId + "... action_pointer_down: ");
            pressed[pointerId] = button_index;
            input.setKey(buttonVals[button_index]);
            buttonViews[button_index].setPressed(true);
        }
        if ((action == MotionEvent.ACTION_UP) && pressed[pointerId] != -1) {
            Log.i("ultra8-buttons", pointerIndex + ":" + pointerId + "... action_up: " + pressed[pointerId]);
            buttonViews[pressed[pointerId]].setPressed(false);
            input.resetKey(buttonVals[pressed[pointerId]]);
            pressed[pointerId] = -1;
        }
        if ((action == MotionEvent.ACTION_POINTER_UP) && pressed[pointerId] != -1) {
            Log.i("ultra8-buttons", pointerIndex + ":" + pointerId + "... action_pointer_up: " + pressed[pointerId]);
            buttonViews[pressed[pointerId]].setPressed(false);
            input.resetKey(buttonVals[pressed[pointerId]]);
            pressed[pointerId] = -1;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        toastNotYet = Toast.makeText(getApplicationContext(), R.string.not_yet_imp, Toast.LENGTH_SHORT);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final TableLayout buttonArea = findViewById(R.id.button_area);
        int buttoncount = 0;
        TableRow trow = null;
        Context context = getApplicationContext();
        for (int buttonVal : buttonVals) {
            if (buttoncount++ % 4 == 0) {
                trow = new TableRow(context);
                buttonArea.addView(trow);
            }
            if (trow != null) {
                MTButton b = new MTButton(context);
                buttonViews[buttoncount - 1] = b;
                trow.addView(b);
                b.setTag(buttonVal);
                b.setText(Integer.toHexString(buttonVal).toUpperCase());
            }
        }
        //trow = new TableRow(context);
        //buttonArea.addView(trow);
        //Button b = new Button(context);
        //b.setTag(0x10);
        //b.setText("Hurry!");
        //b.setOnTouchListener(this);
        //trow.addView(b);
        ImageView gfx_area = findViewById(R.id.graphics);
        View touchArea = findViewById(R.id.button_touch_area);
        touchArea.setOnTouchListener(this);
        touchArea = findViewById(R.id.button_area);
        touchArea.setOnTouchListener(this);


        Chip8Graphics gfx = new Chip8Graphics(gfx_area);
        this.input = new Chip8Input();
        if (gfx == null) {
            Log.i("ultra8", "couldn't get graphics view...");
            System.exit(0);
        }

        machine = new Chip8(gfx, input);

        //Font test 
        /*machine.loadProgram(new int[]{0x62,0x00,
            0x61,0x00,0x63,0x01,0xF3,0x29,0xD1,0x25,
            0x61,0x08,0x63,0x03,0xF3,0x29,0xD1,0x25,
            0x61,0x10,0x63,0x05,0xF3,0x29,0xD1,0x25,
            0x61,0x18,0x63,0x07,0xF3,0x29,0xD1,0x25,
            0x61,0x20,0x63,0x09,0xF3,0x29,0xD1,0x25,
            0x61,0x28,0x63,0x0B,0xF3,0x29,0xD1,0x25,
            0x61,0x30,0x63,0x0D,0xF3,0x29,0xD1,0x25,
            0x61,0x38,0x63,0x0F,0xF3,0x29,0xD1,0x25,
            0x62,0x10,
            0x61,0x00,0x63,0x02,0xF3,0x29,0xD1,0x25,
            0x61,0x08,0x63,0x04,0xF3,0x29,0xD1,0x25,
            0x61,0x10,0x63,0x06,0xF3,0x29,0xD1,0x25,
            0x61,0x18,0x63,0x08,0xF3,0x29,0xD1,0x25,
            0x61,0x20,0x63,0x0A,0xF3,0x29,0xD1,0x25,
            0x61,0x28,0x63,0x0C,0xF3,0x29,0xD1,0x25,
            0x61,0x30,0x63,0x0E,0xF3,0x29,0xD1,0x25,
            
            0x00,0x00
        });*/


        //machine.loadProgram(getResources().openRawResource(R.raw.superblinky));
        //machine.reset();
    }

    public void onClick(View v) {
        Log.i("ultra8-test", "CLICK " + v.getTag());
    }

}
