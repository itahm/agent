package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.itahm.snmp.Constants;
import com.itahm.snmp.Node;
import com.itahm.http.Request;
import com.itahm.http.Response;

import com.itahm.event.Event;

public class SnmpManager extends TimerTask implements ResponseListener, Closeable  {

	private final org.snmp4j.Snmp snmp;
	private final EventListener itahm;
	private final Timer timer;
	private final File root;
	private JSONObject snmpData;
	
	public static enum FILE {
		SNMP, ADDRESS
	}
	
	private final Map<String, Node> nodeList;
	private final static Set<String> realTimeNodeList = new HashSet<String>();
	
	private final static PDU pdu = new PDU();
	{
		pdu.setType(PDU.GETNEXT);
		pdu.add(new VariableBinding(Constants.sysDescr));
		pdu.add(new VariableBinding(Constants.sysObjectID));
		pdu.add(new VariableBinding(Constants.sysName));
		pdu.add(new VariableBinding(Constants.sysServices));
		pdu.add(new VariableBinding(Constants.ifIndex));
		pdu.add(new VariableBinding(Constants.ifDescr));
		pdu.add(new VariableBinding(Constants.ifType));
		pdu.add(new VariableBinding(Constants.ifSpeed));
		pdu.add(new VariableBinding(Constants.ifPhysAddress));
		pdu.add(new VariableBinding(Constants.ifAdminStatus));
		pdu.add(new VariableBinding(Constants.ifOperStatus));
		pdu.add(new VariableBinding(Constants.ifName));
		pdu.add(new VariableBinding(Constants.ifInOctets));
		pdu.add(new VariableBinding(Constants.ifOutOctets));
		pdu.add(new VariableBinding(Constants.ifHCInOctets));
		pdu.add(new VariableBinding(Constants.ifHCOutOctets));
		pdu.add(new VariableBinding(Constants.ifAlias));
		pdu.add(new VariableBinding(Constants.ipNetToMediaType));
		pdu.add(new VariableBinding(Constants.ipNetToMediaPhysAddress));
		pdu.add(new VariableBinding(Constants.hrSystemUptime));
		pdu.add(new VariableBinding(Constants.hrProcessorLoad));
		pdu.add(new VariableBinding(Constants.hrStorageIndex));
		pdu.add(new VariableBinding(Constants.hrStorageType));
		pdu.add(new VariableBinding(Constants.hrStorageDescr));
		pdu.add(new VariableBinding(Constants.hrStorageAllocationUnits));
		pdu.add(new VariableBinding(Constants.hrStorageSize));
		pdu.add(new VariableBinding(Constants.hrStorageUsed));
		
	}
	
	private int lastRequestTime = -1;
	
	public SnmpManager(EventListener eventListener) throws IOException, ITAhMException {
		this(new File("."), eventListener);
	}
	
	public SnmpManager(File path, EventListener eventListener) throws IOException, ITAhMException {
		itahm = eventListener;
		snmp = new org.snmp4j.Snmp(new DefaultUdpTransportMapping());
		timer = new Timer(true);
		root = new File(path, "snmp");
		nodeList = Node.getMap();
		root.mkdir();
		
		snmpData = Data.getJSONObject(Data.Table.SNMP);
		
		snmp.listen();
		
		timer.scheduleAtFixedRate(this, 1000, 1000);
		
		System.out.println("snmp manager is running");
	}
	
	public Node addNode(String ip, int udp, String community) {
		JSONObject nodeData;
		
		if (this.snmpData.has(ip)) {
			nodeData = this.snmpData.getJSONObject(ip);
		}
		else {
			nodeData = new JSONObject()
			.put("ifEntry", new JSONObject())
			.put("hrProcessorEntry", new JSONObject())
			.put("hrStorageEntry", new JSONObject());
			
			this.snmpData.put(ip, nodeData);
			
			try {
				Data.save(Data.Table.SNMP);
			} catch (IOException ioe) {
				// TODO 당장 문제가 되지는 않지만 일반적으로 발생하면 안되는 예외
				ioe.printStackTrace();
			}
		}
		
		try {
			return new Node(this.itahm, this.root, nodeData, ip, udp, community);
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	public void removeNode(String ip) {
		synchronized(this.nodeList) {
			this.nodeList.remove(ip);
		}
	}
	
	public static void addRealTimeNode(String ip) {
		synchronized(realTimeNodeList) {
			realTimeNodeList.add(ip);
		}
	}
	
	public File getRoot() {
		return this.root;
	}
	
	@Override
	public void close() throws IOException {
		this.timer.cancel();
		
		snmp.close();
	}
	
	@Override
	public void onResponse(ResponseEvent event) {
		PDU request = event.getRequest();
		PDU response = event.getResponse();
		Node node = ((Node)event.getUserObject());
		
		((org.snmp4j.Snmp)event.getSource()).cancel(request, this);
		
		try {
			if (response == null) {			
				// TODO response timed out
				
				node.success(false);
				return;
			}
		}
		catch (IOException ioe) {
			// TODO rolling file에 쓰기 실패하는 경우
		}
		
		int status = response.getErrorStatus();
		
		if (status == PDU.noError) {
			try {
				PDU nextRequest = node.parse(request, response);
				
				if (nextRequest == null) {
					// end of get-next request
					node.success(true);
				}
				else {
					snmp.send(nextRequest, node, node, this);
				}
			} catch (IOException e) {
				// TODO fatal error
				e.printStackTrace();
			} catch (JSONException jsone) {
				jsone.printStackTrace();
			}
		}
		else {
			// TODO 
			System.out.println(String.format("error index[%d] status : %s", response.getErrorIndex(), response.getErrorStatusText()));
		}
	}
	
	/**
	 * TimerTask 의 abstract method
	 * 1초마다 수행되는 것으로, 실시간 요청이 있는 node에게만 snmp request를 전송하며
	 * 만일 분이 바뀌면 (1분마다) 모든 node에게 snmp request를 전송한다. 
	 */
	public void run() {
		
		Calendar calendar = Calendar.getInstance();
		int minutes = calendar.get(Calendar.MINUTE);
		long requestTime = calendar.getTimeInMillis();
		
		try {
			if (this.lastRequestTime != minutes) {
				Data.save(Data.Table.SNMP);
				Data.save(Data.Table.ADDRESS);
				loop(requestTime);
				
				this.lastRequestTime = minutes;
			}
			else {
				realTimeLoop(requestTime);
			}
		} catch (IOException ioe) {
			// TODO fatal error
			
			ioe.printStackTrace();
		}
	}
	
	private void loop (long requestTime) throws IOException{
		Iterator<String> it;
		Node node;
		
		synchronized(this.nodeList) {
			it = this.nodeList.keySet().iterator();
			
			while (it.hasNext()) {
				node = this.nodeList.get(it.next());
				node.setRequestTime(requestTime);
				
				this.snmp.send(pdu, node, node, this);
			}
		}
	}

	/**
	 * 요청이 완료되면 queue를 비운다.
	 * @param requestTime
	 * @throws IOException
	 */
	private void realTimeLoop (long requestTime) throws IOException{
		Iterator<String> it;
		Node node;
		
		synchronized(realTimeNodeList) {
			it = realTimeNodeList.iterator();	
	
			while (it.hasNext()) {
				node = this.nodeList.get(it.next());
				
				if (node != null) {
					node.setRequestTime(requestTime);
					this.snmp.send(pdu, node, node, this);
				}
				
				it.remove();
			}
		}
	}
	
	public static void main(String [] args) throws ITAhMException {
		SnmpManager manager;
		try {
			manager = new SnmpManager(new EventListener () {

				@Override
				public void onConnect(SocketChannel channel) {
					
				}

				@Override
				public void onClose(SocketChannel channel) {
					
				}

				@Override
				public void onRequest(Request request, Response response) {
					
				}

				@Override
				public void onError(Exception e) {
					
				}

				@Override
				public void onEvent(Event event) {
					
				}
				
			});
			
			boolean more = true;
			while (more) {
				switch(System.in.read()) { 
				case 'd':
					
					break;
				case -1:
					more = false;
					
					break;
				default:
						
				}
			}
			
			manager.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
}