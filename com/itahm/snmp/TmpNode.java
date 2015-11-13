package com.itahm.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

import com.itahm.Data.Table;
import com.itahm.ITAhM;
import com.itahm.SnmpManager;

public class TmpNode extends CommunityTarget implements Node {

	private static final long serialVersionUID = -203035949001905708L;
	
	private final SnmpManager snmp = ITAhM.getSnmp();
	private final String ip;
	private final ArrayList<JSONObject> profileList = new ArrayList<JSONObject>();
	
	
	/** 
	 * @param snmp
	 * @param ip
	 * @throws IOException
	 */
	public TmpNode(String ip) throws IOException {
		this.ip = ip;
		
		setVersion(SnmpConstants.version2c);
		setTimeout(TIMEOUT);
				
		JSONObject profileData = Table.PROFILE.getJSONObject();
		String [] names = JSONObject.getNames(profileData);
		
		if (names != null) {
			for (int i=0, length=names.length; i< length; i++) {
				this.profileList.add(profileData.getJSONObject(names[i]));
			}
			
			trySNMP();
		}
	}
	
	private void trySNMP() throws IOException {
		JSONObject profile = this.profileList.get(this.profileList.size() -1);
		
		try {
			setAddress(new UdpAddress(InetAddress.getByName(this.ip), profile.getInt("udp")));
			setCommunity(new OctetString(profile.getString("community")));
			
			this.snmp.request(this);
		}
		catch(JSONException jsone) {
		}
	}
	
	public void requestCompleted(boolean success) throws IOException {
			int index = this.profileList.size() -1;
			
			if (success) {
				this.snmp.getNode(this.ip);
				this.profileList.get(index).getString("name");
				
				// TODO 성공 
			}
			else {
				this.profileList.remove(index);
				
				if (index > 0) {
					trySNMP();
				}
				else {
					// TODO 실패
				}
			}
	}
	
}
