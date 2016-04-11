package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;

import org.json.JSONObject;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.event.Event;
import com.itahm.snmp.Node;
import com.itahm.snmp.NodeList;
import com.itahm.snmp.RequestPDU;
import com.itahm.table.Table;

public class SnmpManager extends TimerTask implements Closeable  {

	private final File snmpRoot;
	private final Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
	
	private final Table deviceTable = ITAhM.getTable("device");
	private final Table profileTable = ITAhM.getTable("profile");
	
	private final static PDU pdu = new RequestPDU();
	public static final NodeList nodeList = new NodeList();
	
	private final static String STRING_SNMP_STATUS = "snmp";
	private final static String STRING_SHUTDOWN = "shutdown";
	private final static String STRING_PROFILE = "profile";
	private final static String STRING_COMMUNITY = "community";
	private final static String STRING_UDP = "udp";
	
	public SnmpManager() throws IOException {
		snmpRoot = new File(ITAhM.getRoot(), "snmp");
		snmpRoot.mkdir();
		
		snmp.listen();
		
		System.out.println("snmp manager running...");
	}
	
	public synchronized void sendRequest(Node node, JSONObject profile) {
		node.setRequest(profile.getString(STRING_COMMUNITY), profile.getInt(STRING_UDP));
		
		// 중요! 이렇게 하지 않으면 항상 같은 ID로 request 한다.
		pdu.setRequestID(null);
		
		sendNextRequest(node, pdu);
	}
	
	public void sendNextRequest(Node node, PDU nextPDU) {
		try {
			this.snmp.send(nextPDU, node, this, node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onSuccess(Node node) throws IOException {
		ArrayList<String> testProfileList = node.test();
		JSONObject device = node.getDevice();
		
		// 정상 응답
		if (testProfileList == null) {
			// shutdown 상태였으면
			if (device.getBoolean(STRING_SHUTDOWN)) {
				device.put(STRING_SHUTDOWN, false);
				
				System.out.println(node.getAddress()+"\t"+ "up");
				
				Event.dispatch(new JSONObject()
					.put("id", device.getString("id"))
					.put("shutdown", false));
			}
		}
		// test 중이었다면
		else {
			String profileName = testProfileList.get(testProfileList.size() -1);
			
			node.test(null);
			
			device.put(STRING_SNMP_STATUS, true);
			device.put(STRING_SHUTDOWN, false);
			device.put(STRING_PROFILE, profileName);
				
			deviceTable.save();
		}
				
		node.requestCompleted();
	}
	
	public void onFailure(Node node) {
		ArrayList<String> testProfileList = node.test();
		JSONObject device = node.getDevice();
		
		// 정상 응답
		if (testProfileList == null) {
			if (!device.getBoolean(STRING_SHUTDOWN)) {
				device.put(STRING_SHUTDOWN, true);
				
				System.out.println(node.getAddress()+"\t"+ "down");
				
				Event.dispatch(new JSONObject()
					.put("id", device.getString("id"))
					.put("shutdown", true));
			}
		}
		// test 중이었다면
		else {
			int size = testProfileList.size();
		
			testProfileList.remove(--size);
			
			// 테스트 계속
			if (size > 0) {
				String profileName = testProfileList.get(size -1);
				
				sendRequest(node, profileTable.getJSONObject(profileName));
			}
			// 테스트 실패
			else {
				node.test(null);
				
				device.put(STRING_SNMP_STATUS, false);
				device.put(STRING_SHUTDOWN, true);
				
				deviceTable.save();
				
				// 알릴 필요는 없다.
			}
		}
	}
	
	/**
	 * ovverride
	 */
	@Override
	public void close() throws IOException {
		cancel();
		
		this.snmp.close();
	}
	
	@Override
	public void run() {
		JSONObject deviceData = this.deviceTable.getJSONObject();
		String [] idArray = JSONObject.getNames(deviceData);
		
		NodeList.clear();
		
		if (idArray != null) {
			int idArrayLength = idArray.length;
			JSONObject device;
			Node node;
			JSONObject profileData = this.profileTable.getJSONObject();
			String [] profileArray = JSONObject.getNames(profileData);
			ArrayList<String> testProfileList;
			
			for (int i=0; i<idArrayLength; i++) {
				device = deviceData.getJSONObject(idArray[i]);
				
				node = NodeList.join(device);
				
				if (node == null) {
					continue;
				}
				
				// 테스트
				if (!device.has(STRING_SNMP_STATUS)) {
					// 테스트 시작
					if ( node.test() == null) {
						
						testProfileList = new ArrayList<String>(Arrays.asList(profileArray));
						
						node.test(testProfileList);
						
						sendRequest(node, this.profileTable.getJSONObject(testProfileList.get(testProfileList.size() -1)));
					}
					// else 테스트 중
				}
				// 정상 요청.
				else if (device.getBoolean(STRING_SNMP_STATUS)) {
					sendRequest(node, profileData.getJSONObject(device.getString("profile")));
				}
			}
		}
		
		NodeList.reset();
	}
}