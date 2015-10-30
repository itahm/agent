package com.itahm;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import com.itahm.json.JSONFile;

// TODO: Auto-generated Javadoc
/**
 * The Class Database.
 */
public class Data {
	
	private static JSONFile accountFile;
	private static JSONObject accountObj;
	
	private static JSONFile profileFile;
	private static JSONObject profileObj;
	
	private static JSONFile deviceFile;
	private static JSONObject deviceObj;
	
	private static JSONFile lineFile;
	private static JSONObject lineObj;
	
	private static JSONFile indexFile;
	private static JSONObject indexObj;
	
	private static JSONFile snmpFile;
	private static JSONObject snmpObj;
	
	private static JSONFile addressFile;
	private static JSONObject addressObj;
	
	private static boolean initialized = false;
	
	public static enum Table {
		ACCOUNT, PROFILE, DEVICE, LINE, INDEX, SNMP, ADDRESS
	}
	/**
	 * Instantiates a new database.
	 * @throws ITAhMException 
	 */
	
	public static boolean initialize(File root) throws IOException, ITAhMException{
		if (initialized) {
			return false;
		}
		
		accountFile = new JSONFile();
		
		accountFile.load(new File(root, "account"));
		accountObj = accountFile.getJSONObject();
		
		if (accountObj == null) {
			accountFile.close();
			
			new ITAhMException("can not read account json data.");
		}
		
		if (accountObj.length() == 0) {
			accountObj.put("root", new JSONObject().put("username", "root").put("password", "root"));
			accountFile.save();
		}
		
		profileFile = new JSONFile();
		
		profileFile.load(new File(root, "profile"));
		profileObj = profileFile.getJSONObject();
		
		if (profileObj == null) {
			profileFile.close();
			
			new ITAhMException("can not read profile json data.");
		}
		
		if (profileObj.length() == 0) {
			profileObj.put("public", new JSONObject()
				.put("name", "public")
				.put("version", "v2c")
				.put("community", "public")
				.put("udp", 161)
			);
			
			profileFile.save();
		}
		
		deviceFile = new JSONFile();
		
		deviceFile.load(new File(root, "device"));
		deviceObj = deviceFile.getJSONObject();
		
		if (deviceObj == null) {
			deviceFile.close();
			
			new ITAhMException("can not read device json data.");
		}
		
		if (deviceObj.length() == 0) {
			deviceObj.put("0", new JSONObject()
				.put("id", "0")
				.put("address", "127.0.0.1")
				.put("x", 0).put("y", 0)
				.put("name", "localhost")
				.put("snmp", "public")
				.put("type", "server")
				);
			
			deviceFile.save();
		}
		
		lineFile = new JSONFile();
		
		lineFile.load(new File(root, "line"));
		lineObj = lineFile.getJSONObject();
		
		if (lineObj == null) {
			lineFile.close();
			
			new ITAhMException("can not read line json data.");
		}
		
		indexFile = new JSONFile();
		
		indexFile.load(new File(root, "index"));
		indexObj = indexFile.getJSONObject();
		
		if (indexObj == null) {
			indexFile.close();
			
			new ITAhMException("can not read index json data.");
		}
		
		if (indexObj.length() == 0) {
			indexObj.put("index", 1);
			indexFile.save();
		}
		
		File snmpRoot = new File(root, "snmp");
		snmpRoot.mkdir();
		
		snmpFile = new JSONFile();
		
		snmpFile.load(new File(snmpRoot, "snmp"));
		snmpObj = snmpFile.getJSONObject();
		
		if (snmpObj == null) {
			snmpFile.close();
			
			new ITAhMException("can not read index json data.");
		}
		
		addressFile = new JSONFile();
		
		addressFile.load(new File(snmpRoot, "address"));
		addressObj = addressFile.getJSONObject();
		
		if (addressObj == null) {
			addressFile.close();
			
			new ITAhMException("can not read address json data.");
		}
		
		
		return initialized = true;
	}
	
	public static JSONObject getJSONObject(Table name) {
		switch (name) {
		case ACCOUNT:
			return accountObj;
			
		case PROFILE:
			return profileObj;
		
		case DEVICE:
			return deviceObj;

		case LINE:
			return lineObj;
			
		case INDEX:
			return indexObj;
			
		case SNMP:
			return snmpObj;
		
		case ADDRESS:
			return addressObj;
			
		default:
				return null;
		}
	}
	
	public static void save(Table name) throws IOException {
		switch (name) {
		case ACCOUNT:
			accountFile.save();
			
		case PROFILE:
			profileFile.save();
			
		case DEVICE:
			deviceFile.save();
		
		case LINE:
			lineFile.save();
		
		case INDEX:
			indexFile.save();
		
		case SNMP:
			snmpFile.save();
			
		case ADDRESS:
			addressFile.save();
		}
	}
	
	public static int newID() {
		int numID = -1;
		
		synchronized(indexObj) {
			numID = indexObj.getInt("index");
			indexObj.put("index", numID +1);
			
			try {
				indexFile.save();
			} catch (IOException e) {
				/** file이 몇차례 저장되지 않는 것은 큰 문제가 되지 않지만
				 *  file이 저장되지 않은 상태로 service가 종료되면 문제가 된다.
				 */
				
				e.printStackTrace();
			}
		}
		
		return numID;
	}
	
	public static void close() {
		try {
			accountFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			profileFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			deviceFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			lineFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			indexFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			snmpFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			addressFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
