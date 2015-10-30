package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RollingMap {
	
	public static enum Resource {
		HRPROCESSORLOAD("hrProcessorLoad"),
		IFINOCTETS("ifInOctets"),
		IFOUTOCTETS("ifOutOctets"),
		HRSTORAGEUSED("hrStorageUsed"),
		DELAY("delay");
		
		private String string;
		
		private Resource(String arg) {
			string = arg;
		}
		
		public String toString() {
			return this.string;
		}
	}
	
	private final Map<Resource, HashMap<String, RollingFile>> map;
	
	private final File root;
	
	public RollingMap(File nodeRoot) {
		root = nodeRoot;
		map = new HashMap<Resource, HashMap<String, RollingFile>>();
		
		for (Resource resource : Resource.values()) {
			map.put(resource, new HashMap<String, RollingFile>());
			
			new File(root, resource.toString()).mkdir();
		}
	}

	public void put(Resource resource, String index, long value) throws IOException {
		Map<String, RollingFile> map = this.map.get(resource);
		RollingFile rollingFile = map.get(index);
		
		if (rollingFile == null) {
			map.put(index, rollingFile = new RollingFile(new File(this.root, resource.toString()), index));
		}
		
		rollingFile.roll(value);
	}
	
	/**
	 * 
	 * @param resource
	 * @param index
	 * @return rollingFile, 요청한 resource와 index에 mapping되는 rollingFile이 존재하지 않는 경우 null
	 */
	public RollingFile getFile(Resource resource, String index) {
		return this.map.get(resource).get(index);
	}
}
