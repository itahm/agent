package com.itahm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itahm.json.JSONObject;

public class TopTable <E extends Enum<E>> implements Comparator<String> {
	
	private final Class<E> e;
	private final Map<E, HashMap<String, Long>> map;
	private Map<String, Long> sortTop;
	
	public TopTable(Class<E> e) {
		this.e = e;
		
		map = new HashMap<>();
		
		for (E key : e.getEnumConstants()) {
			map.put(key, new HashMap<String, Long>());
		}
	}
	
	public synchronized void submit(String ip, E resource, long value) {
		this.map.get(resource).put(ip, value);
	}
	
	public synchronized JSONObject getTop(final int count) {
		JSONObject top = new JSONObject();
		
		for (E key : e.getEnumConstants()) {
			top.put(key.toString(), getTop(this.map.get(key), count));
		}
		
		return top;
	}
	
	private Map<String, Long> getTop(HashMap<String, Long> sortTop, int count) {
		Map<String, Long > top = new HashMap<String, Long>();
		List<String> list = new ArrayList<String>();
		String ip;
		
		this.sortTop = sortTop;
		
        list.addAll(sortTop.keySet());
         
        Collections.sort(list, this);
        
        count = Math.min(list.size(), count);
        for (int i=0; i< count; i++) {
        	ip = list.get(i);
        	
        	top.put(ip, this.sortTop.get(ip));
        }
        
        return top;
	}

	public void remove(String ip) {
		for (E key : e.getEnumConstants()) {
			this.map.get(key).remove(ip);
		}
	}
	
	@Override
	public int compare(String ip1, String ip2) {
		Long value1 = this.sortTop.get(ip1);
        Long value2 = this.sortTop.get(ip2);
         
        return value2.compareTo(value1);
	}
/*
	public static void main(String [] args) {
		TopTable<Key> t = new TopTable<>(Key.class);
		
		t.submit("127", Key.MEMORY, 100);
		t.submit("192", Key.MEMORY, 200);
		t.submit("172", Key.MEMORY, 300);
		t.submit("10", Key.MEMORY, 10);
		
		System.out.println(t.getTop(3));
	}

	
	private enum Key {
		RESPONSETIME("responseTime"),
		FAILURERATE("failureRate"),
		PROCESSOR("processor"),
		MEMORY("memory"),
		MEMORYRATE("memoryRate"),
		STORAGE("storage"),
		STORAGERATE("storageRate"),
		THROUGHPUT("throughput"),
		THROUGHPUTRATE("throughputRate"),
		THROUGHPUTERR("throughputErr");
		
		private String string;
		
		private Key(String string) {
			this.string = string;
		}
		
		public String toString() {
			return this.string;
		}
	};
*/	
}