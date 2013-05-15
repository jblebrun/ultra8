package com.emerjbl.ultra8;
import android.os.SystemClock;

public class Chip8Timer  {
	private int tickCount;
	private long timeSetAt;
	
	public int getValue() {
		long now = SystemClock.uptimeMillis();
		long elapsedTicks = (now-timeSetAt)/17; //16.66ms for 60Hz
		tickCount = (int) (elapsedTicks > tickCount ? 0 : tickCount-elapsedTicks);
		return tickCount;
	}

	public void setValue(int ticks) {
		tickCount = ticks;
		timeSetAt = SystemClock.uptimeMillis();
		
	}

	
	
}
