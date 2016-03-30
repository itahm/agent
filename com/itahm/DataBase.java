package com.itahm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import org.json.JSONObject;

import com.itahm.table.*;

// TODO: Auto-generated Javadoc
/**
 * The Class Database.
 */
public class DataBase {
	
	private final Map<String, Table> tableMap = new HashMap<String, Table>();
	
	public DataBase() throws IOException {
		tableMap.put("account", new Account());
		tableMap.put("profile", new Profile());
		tableMap.put("device", new Device());
		tableMap.put("index", new Index());
		tableMap.put("icon", new Icon());
	}
	
	public Table getTable(String tableName) {
		return this.tableMap.get(tableName);
	}
	/*
	public JSONObject getJSONObject(String tableName) {
		Table table = this.tableMap.get(tableName);
		
		return table != null? table.getJSONObject(): null;
	}
	*/
	public void close() throws IOException {
		for (Table table : this.tableMap.values()) {
			table.close();
		}
	}
}