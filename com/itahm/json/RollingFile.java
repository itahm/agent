package com.itahm.json;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;

import com.itahm.json.JSONObject;

import com.itahm.util.DataCleaner;

/**
 * The Class RollingFile.
 */
public class RollingFile implements Closeable {
	
	/** The lastHour. */
	//private int lastHour;
	//private int lastDay;
	private long _lastHour = -1;
	private long _lastDay = -1;
	
	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	private final JSONSummary summary;
	
	private JSONFile summaryFile;
	private JSONObject summaryData;
	private String summaryHour;
	
	private File dayDirectory;
	private JSONFile hourFile;
	private JSONObject hourData;
	
	private long max;
	private long min;
	private BigInteger hourSum = BigInteger.valueOf(0);
	private int hourCnt = 0;
	private BigInteger minuteSum = BigInteger.valueOf(0);
	private long minuteSumCnt = 0;
	
	/**
	 * Instantiates a new rolling file.
	 *
	 * @param root the root (itahm\snmp\ip\resource)
	 * @param index the index of host, interfaces, etc.
	 * @param type gauge or counter
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public RollingFile(File rscRoot, String index) throws IOException {
		root = new File(rscRoot, index);
		root.mkdir();
		
		summary = new JSONSummary(root);
	}
	
	private Calendar getCalendar() {
		Calendar c = Calendar.getInstance();
		
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		
		return c;
	}
	
	private Calendar getCalendar(long mills) {
		Calendar c = Calendar.getInstance();
	
		c.setTimeInMillis(mills);
		
		return c;
	}
	
	/**
	 * Roll.
	 *
	 * @param value the value
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	/*
	public void roll(long value) throws IOException {
		Calendar date = getCalendar();
		
		if (date.get(Calendar.HOUR_OF_DAY) != this.lastHour) {
			//summarize();
			
			if (date.get(Calendar.DAY_OF_YEAR) != this.lastDay) {
				initDay(date);
			}
			
			initHour(date);
		}
		
		roll(Long.toString(date.getTimeInMillis()), value);
	}
	*/
	public void _roll(long value) throws IOException {
		Calendar date = getCalendar();
		String minString;
		long hourMills;
		long dayMills;

		minString = Long.toString(date.getTimeInMillis());
		
		date.set(Calendar.MINUTE, 0);
		hourMills = date.getTimeInMillis();
		
		if (this._lastHour != hourMills) {
			date.set(Calendar.HOUR_OF_DAY, 0);
			dayMills = date.getTimeInMillis();
			
			if (this._lastDay != dayMills) {
				_initDay(dayMills);
			}
			
			_initHour(hourMills);
		}
		
		roll(minString, value);
	}
	
	private void roll(String minuteString, long value) throws IOException {
		if (this.hourData.has(minuteString)) {
			this.minuteSum = this.minuteSum.add(BigInteger.valueOf(value));
		}
		else {
			this.minuteSum = BigInteger.valueOf(value);
			this.minuteSumCnt = 0;
		}
		
		this.minuteSumCnt++;
		
		this.hourData.put(minuteString, this.minuteSum.divide(BigInteger.valueOf(this.minuteSumCnt)));
		
		if (this.hourCnt == 0) {
			this.hourSum = BigInteger.valueOf(value);
			this.max = value;
			this.min = value;
		}
		else {
			this.hourSum = this.hourSum.add(BigInteger.valueOf(value));
			this.max = Math.max(this.max, value);
			this.min = Math.min(this.min, value);
		}
		
		this.hourCnt++;
		
		summarize();
		
		// TODO 아래 반복되는 save가 성능에 영향을 주는가 확인 필요함.
		this.hourFile.save();
	}
	/*
	private void initDay(Calendar date) throws IOException {
		String dateString;
		
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.HOUR_OF_DAY, 0);
		
		dateString = Long.toString(date.getTimeInMillis());
		
		this.lastDay = date.get(Calendar.DAY_OF_YEAR);
		
		// day directory 생성
		this.dayDirectory = new File(this.root, dateString);
		this.dayDirectory.mkdir();
		
		if (this.summaryFile != null) {
			this.summaryFile.close();
		}
		
		// summary file 생성
		this.summaryFile = new JSONFile(new File(this.dayDirectory, "summary"));
		this.summaryData = this.summaryFile.getJSONObject();
		
		// 지난 파일 삭제
		date.add(Calendar.MONTH, -3);
		
		DataCleaner.deleteDirectory(new File(this.root, Long.toString(date.getTimeInMillis())));
	}
	*/
	private void _initDay(long dayMills) throws IOException {
		this._lastDay = dayMills;
		
		// day directory 생성
		this.dayDirectory = new File(this.root, Long.toString(dayMills));
		this.dayDirectory.mkdir();
		
		if (this.summaryFile != null) {
			this.summaryFile.close();
		}
		
		// summary file 생성
		this.summaryFile = new JSONFile(new File(this.dayDirectory, "summary"));
		this.summaryData = this.summaryFile.getJSONObject();
		
		clear(dayMills);
	}
	
	/**
	 * 
	 * @param date
	 * @throws IOException
	 */
	/*
	private void initHour(Calendar date) throws IOException {
		String hourString;
		
		if (this.hourFile != null) {
			this.hourFile.close();
		}
		
		date.set(Calendar.MINUTE, 0);
		
		hourString = Long.toString(date.getTimeInMillis());
		
		// 마지막 시간 변경
		this.lastHour = date.get(Calendar.HOUR_OF_DAY);
		
		// hourly file 생성
		this.hourFile = new JSONFile(new File(this.dayDirectory, hourString));
		this.hourData = this.hourFile.getJSONObject();
		this.summaryHour = hourString;
		this.hourCnt = 0;
	}
	*/
	private void _initHour(long hourMills) throws IOException {
		String hourString = Long.toString(hourMills);
		
		this._lastHour = hourMills;
		
		if (this.hourFile != null) {
			this.hourFile.close();
		}
		
		// hourly file 생성
		this.hourFile = new JSONFile(new File(this.dayDirectory, hourString));
		this.hourData = this.hourFile.getJSONObject();
		this.summaryHour = hourString;
		this.hourCnt = 0;
	}
	
	private void clear(long dayMills) {
		Calendar date = getCalendar(dayMills);
		
		// 지난 파일 삭제
		date.add(Calendar.MONTH, -3);
				
		DataCleaner.deleteDirectory(new File(this.root, Long.toString(date.getTimeInMillis())));
	}
	
	private void summarize() throws IOException {
		JSONObject summary;
		
		if (this.summaryData.has(this.summaryHour)) {
			summary = this.summaryData.getJSONObject(this.summaryHour);
		}
		else {
			summary = new JSONObject();
			
			this.summaryData.put(this.summaryHour, summary);
		}
		
		long avg = this.hourSum.divide(BigInteger.valueOf(this.hourCnt)).longValue();
		
		summary
			.put("avg", avg)
			.put("max", Math.max(avg, this.max))
			.put("min", Math.min(avg, this.min));
		
		this.summaryFile.save();
	}
	
	public JSONObject getData(long start, long end, boolean summary) {
		return (summary? this.summary: new JSONData(this.root)).getJSON(start, end);
	}
	
	@Override
	public void close() throws IOException {
		if (this.hourFile != null) {
			this.hourFile.close();
		}
		
		if (this.summaryFile != null) {
			this.summaryFile.close();
		}
	}
}

