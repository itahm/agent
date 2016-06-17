package com.itahm.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class DailyFile {

	private final File root;
	private int day;
	private RandomAccessFile file;
	private FileChannel channel;
	
	public DailyFile() throws IOException {
		this(new File("."));
	}
	
	public DailyFile(File root) throws IOException {
		this.root = root;
	}
	
	public boolean roll() throws IOException {
		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		boolean roll = false;
		
		if (this.day != day) {
			close();
			
			trim(calendar);
			
			this.file = new RandomAccessFile(new File(this.root, Long.toString(calendar.getTimeInMillis())), "rw");
			this.channel = this.file.getChannel();
			
			if (this.day != 0) {
				roll = true;
			}
			
			this.day = day;
		}
		
		return roll;
	}
	
	private byte [] read(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel channel = raf.getChannel();
		int size = (int)channel.size();
		ByteBuffer buffer = ByteBuffer.allocate(size);
		byte[] bytes = new byte [size];
		
		channel.read(buffer);
		
		channel.close();
		raf.close();
		
		buffer.flip();
		
		buffer.get(bytes);
		
		return bytes;
	}
	
	public byte [] read(long mills) throws IOException {
		File file = new File(this.root, Long.toString(mills));
		
		if (file.isFile()) {
			return read(file);
		}
		
		return null;
	}
	
	public void write(byte [] data) throws IOException {
		this.file.setLength(0);
		this.channel.write(ByteBuffer.wrap(data));
	}
	
	public void close() throws IOException {
		if (this.file != null) {
			if (this.channel != null) {
				this.channel.close();
			}
			
			this.file.close();
		}
	}
	
	public static Calendar trim(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		return c;
	}
	
	public static void main(String[] args) throws IOException {
		
	}

}
