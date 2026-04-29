package com.rs.game.player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.rs.utils.Utils;

public class TimeManager {

	private static Calendar cal = Calendar.getInstance();
	
	final static int SUNDAY = 1;
	final static int MONDAY = 2;
	final static int TUESDAY = 3;
	final static int WEDNESDAY = 4;
	final static int THURSDAY = 5;
	final static int FRIDAY = 6;
	final static int SATURDAY = 7;
	
	public static String getTime() { 
		SimpleDateFormat format = new SimpleDateFormat("h:mm a zzz"); 
		return format.format(new Date());
	}
	
	public static boolean isEaster() {
		cal.setTimeInMillis(Utils.currentTimeMillis());
	    return cal.get(Calendar.MONTH) == Calendar.APRIL && getCurrentDay() == 20;
	}
	
	public static int getCurrentMonth() {
		cal.setTimeInMillis(Utils.currentTimeMillis());
		return cal.get(Calendar.MONTH);
	}
	
	public static int getCurrentDay() {
		cal.setTimeInMillis(Utils.currentTimeMillis());
		return cal.get(Calendar.DAY_OF_MONTH);
	}
	
	public static int dayOfWeek() {
		cal.setTimeInMillis(Utils.currentTimeMillis());
		return cal.get(Calendar.DAY_OF_WEEK);
	}
	
	public static boolean isMidWeek() {
		   return dayOfWeek() == WEDNESDAY;
	}
	
	public static boolean isWeekend() {
	   return dayOfWeek() == SUNDAY ? true:
	          dayOfWeek() == FRIDAY ? true:
	          dayOfWeek() == SATURDAY ? true: false;
	}
	
}
