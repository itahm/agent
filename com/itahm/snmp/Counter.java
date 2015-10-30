package com.itahm.snmp;

import java.util.Calendar;

public class Counter {

	private long last;
	private long count;
	
	public Counter(long value) {
		last = Calendar.getInstance().getTimeInMillis();
		count = value;
	}

	public long count(long value) {
		long time = Calendar.getInstance().getTimeInMillis();
		long diff = value - this.count;
		
		if (diff == 0) {
			return 0;
		}
		
		long counter = diff *1000 /(time - this.last);
		
		this.last = time;
		this.count = value;
		
		return counter;
	}
	
}
