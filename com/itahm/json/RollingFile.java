package com.itahm.json;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.json.JSONObject;

import com.itahm.util.DataCleaner;

/**
 * The Class RollingFile.
 */
public class RollingFile implements Closeable {
	
	/** The lastHour. */
	private int lastHour;
	private String lastHourString;
	
	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	
	private JSONFile summary;
	private JSONObject summaryData;
	
	private File dir;
	private JSONFile file;
	private JSONObject data;
	
	private long max;
	private long min;
	private double avg;
	private int count;
	private long sum;
	private long sumCnt;
	/**
	 * Instantiates a new rolling file.
	 *
	 * @param root the root (itahm\snmp\ip\resource)
	 * @param index the index of host, interfaces, etc.
	 * @param type gauge or counter
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public RollingFile(File rscRoot, String index) throws IOException {
		Calendar calendar = Calendar.getInstance();
		int hour;
		String hourString;
		
		root = new File(rscRoot, index);
		root.mkdir();
		
		hour = calendar.get(Calendar.HOUR_OF_DAY);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		hourString = Long.toString(calendar.getTimeInMillis());
		
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		initDay(calendar);
		initHour(hourString, hour);
		
		avg = 0;
		count = 0;
		sum = 0;
		sumCnt = 0;
	}
	
	/**
	 * Roll.
	 *
	 * @param value the value
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	
	public boolean roll(long value) throws IOException {
		Calendar calendar = Calendar.getInstance();
		long now;
		int hour;
		boolean roll = false;
		String hourString;
		
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		
		now = calendar.getTimeInMillis();
		hour = calendar.get(Calendar.HOUR_OF_DAY);
		hourString = Long.toString(now);

		if (hour != this.lastHour) {
			summarize();
			
			if (hour == 0) {
				roll = true;
				
				initDay(calendar);
			}
									
			initHour(hourString, hour);
		}
		
		roll(hourString, value);
		
		return roll;
	}
	
	private void roll(String hourString, long value) throws IOException {
		if (this.data.has(hourString)) {
			this.sum += value;
		}
		else {
			this.sum = value;
			this.sumCnt = 0;
		}
		
		this.sumCnt++;
		
		this.data.put(hourString, this.sum / this.sumCnt);
		
		if (this.count == 0) {
			this.avg = value;
			this.max = value;
			this.min = value;
		}
		else {
			this.avg = this.avg / (this.count +1) * this.count + value / (this.count +1.0);
			this.max = Math.max(this.max, value);
			this.min = Math.min(this.min, value);
		}
		
		this.count++;
		
		// TODO 아래 반복되는 save가 성능에 영향을 주는가 확인 필요함.
		this.file.save();
	}
	
	private void initDay(Calendar date) throws IOException {
		String dateString = Long.toString(date.getTimeInMillis());
		
		// day directory 생성
		this.dir = new File(this.root, dateString);
		this.dir.mkdir();
		
		if (this.summary != null) {
			this.summary.close();
		}
		
		// summary file 생성
		this.summary = new JSONFile(new File(this.dir, "summary"));
		this.summaryData = this.summary.getJSONObject();
		
		// 지난 파일 삭제
		date.add(Calendar.MONTH, -3);
		
		dateString = Long.toString(date.getTimeInMillis());

		DataCleaner.deleteDirectory(new File(this.root, dateString));
	}
	
	/**
	 * 
	 * @param hourString 
	 * @param hour of day (0 ~ 23)
	 * @throws IOException
	 */
	private void initHour(String hourString, int hour) throws IOException {
		if (this.file != null) {
			this.file.close();
		}
		
		// hourly file 생성
		this.file = new JSONFile(new File(this.dir, hourString));
		this.data = this.file.getJSONObject();
		
		// 마지막 시간 변경
		this.lastHourString = hourString;
		this.lastHour = hour;
	}
	
	private void summarize() throws IOException {
		if (this.count > 0) {
			this.summaryData.put(this.lastHourString,
				new JSONObject()
				.put("avg", (long)this.avg)
				.put("max", this.max)
				.put("min", this.min)
			);
			
			this.count = 0;
			
			this.summary.save();
		}
	}
	
	public JSONObject getData(long start, long end, boolean summary) {
		return (summary? new JSONSummary(this.root): new JSONData(this.root)).getJSON(start, end);
	}
	
	@Override
	public void close() throws IOException {
		if (this.file != null) {
			this.file.close();
		}
		
		if (this.summary != null) {
			this.summary.close();
		}
	}
}

