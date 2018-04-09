//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utilities.Bencoder2;
import utilities.BencodingException;

public class TrackerManager {
	private static final int CLIENT_PORT_INIT = 6881;
	private static final int CLIENT_PORT_MAX = 6889;
	
	private static final String EVENT_STATUS_STARTED = "started";
	private static final String EVENT_STATUS_STOPPED = "stopped";
	private static final String EVENT_STATUS_COMPLETED = "completed";
	
	private static final ByteBuffer KEY_FAIL = ByteBuffer.wrap(new byte[] {'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'});
	private static final ByteBuffer KEY_COMPLETE = ByteBuffer.wrap(new byte[] {'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'});
	private static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {'i', 'n', 't', 'e', 'r', 'v', 'a', 'l'});
	
	private static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {'p', 'e', 'e', 'r', 's'});
	private static final ByteBuffer KEY_PEER_PEERID = ByteBuffer.wrap(new byte[] {'p', 'e', 'e', 'r', ' ', 'i', 'd'});
	private static final ByteBuffer KEY_PEER_IP = ByteBuffer.wrap(new byte[] {'i', 'p'});
	private static final ByteBuffer KEY_PEER_PORT = ByteBuffer.wrap(new byte[] {'p', 'o', 'r', 't'});
		
	
	RUBTClient controller;
	Torrent torrent;
	
	int clientPort;
	List<Peer> peerList;
	private int interval;
	private URL trkURL;
	private URLConnection trkURLConn;
	private BufferedInputStream trkInptStrm;
	
	public TrackerManager(Torrent torrent, RUBTClient controller) {
		this.torrent = torrent;
		this.controller = controller;
		this.clientPort = CLIENT_PORT_INIT;
	}
	
	@SuppressWarnings("unchecked")
	public List<Peer> getPeersList() {
		try {
			this.trkURL = new URL(this.torrent.announce_url.toString() +
					"?info_hash=" + this.torrent.getInfoHash_URLEncoded() +
					"&peer_id=" + this.torrent.getPeerId_URLEncoded() +
					"&port=" + this.clientPort + 
					"&uploaded=" + this.torrent.uploaded + 
					"&downloaded=" + this.torrent.downloaded +
					"&left=" + this.torrent.left);
			
			//OPEN CONNECTION & READ SIZE OF RESPONSE FILE
			int responseSize;
			trkInptStrm = new BufferedInputStream(trkURL.openStream());
			trkInptStrm.mark(Integer.MAX_VALUE);
			for (responseSize = 0; trkInptStrm.read() != -1; responseSize++) {}
			
			//TRY TO RESET POSITION INPUT STREAM POSITION AFTER FILE SIZE IS FOUND
			try {
				trkInptStrm.reset();
			} catch (IOException e) {
				//MARK WAS LOST - RESPONSE IS TOO BIG ( > INTERGER.MAX_VALUE)
				System.out.println("Tracker Response too large");
				throw new IOException();
			}
			
			//STORE RESPONSE IN BUFFER & PARSE MAIN DICTIONARY
			ByteBuffer response = ByteBuffer.allocate(responseSize);
			trkInptStrm.read(response.array());
			Map<ByteBuffer, Object> dict = (Map<ByteBuffer,Object>)Bencoder2.decode(response.array());
			
			//PARSE LIST OF PEERS IN RESPONSE & PARSE VALUE FOR INTERVAL
			if (dict.containsKey(KEY_FAIL) || !dict.containsKey(KEY_PEERS))return null;
			List peerBuffers = (List)dict.get(KEY_PEERS);
			this.interval = ((Integer)dict.get(KEY_INTERVAL)).intValue();
			
			//PARSE EACH PEER AND CREATE A LIST<PEER> OBJECT TO RETURN
			Peer peer = null;
			Map<ByteBuffer, Object> peerBuffer;
			this.peerList = new ArrayList<Peer>();
			for (Object o : peerBuffers) {
				peerBuffer = (Map<ByteBuffer, Object>)o;
				
				//TRY TO CREATE PEER IF CONSTRUCTOR FAILS CONTINUE TO NEXT PEER
				try {
					peer = new Peer(
							(ByteBuffer)peerBuffer.get(KEY_PEER_IP),
							((Integer)peerBuffer.get(KEY_PEER_PORT)).intValue(),
							(ByteBuffer)peerBuffer.get(KEY_PEER_PEERID));
				} catch (UnknownHostException e) {
					if (peer != null)System.err.println("couldn't create Peer for peerId:" + peer.getPeerIdString());
					//e.printStackTrace();
					continue;
				}
				this.peerList.add(peer);
			}
		} catch (MalformedURLException e) {
			System.err.println("invalid tracker URL");
			//e.printStackTrace();
			return null;
		} catch (IOException e) {
			System.err.println("error occurred while communicating with tracker");
			//e.printStackTrace();
			return null;
		} catch (BencodingException e) {
			System.err.println("error parsing tracker response");
			//e.printStackTrace();
			return null;
		} finally {
			try {
				if (trkInptStrm != null)trkInptStrm.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this.peerList;
	}
	
	public boolean sendStartedEvent(){
		//let tracker know we're downloading from a peer
		return this.sendEvent(EVENT_STATUS_STARTED);
	}
	
	public boolean sendCompletedEvent(){
		//let tracker know we've finished downloading from a peer
		return this.sendEvent(EVENT_STATUS_COMPLETED);
	}
	
	public boolean sendStoppedEvent(){
		//let tracker know download with a peer has stopped
		return this.sendEvent(EVENT_STATUS_STOPPED);
	}
	
	private boolean sendEvent(String status) {
		try {
			this.trkURL = new URL(this.torrent.announce_url.toString() +
					"?info_hash=" + this.torrent.getInfoHash_URLEncoded() +
					"&peer_id=" + this.torrent.getPeerId_URLEncoded() +
					"&port=" + this.clientPort + 
					"&uploaded=" + this.torrent.uploaded + 
					"&downloaded=" + this.torrent.downloaded +
					"&left=" + this.torrent.left + 
					"&event=" + status);
			this.trkURLConn = this.trkURL.openConnection();
			this.trkURLConn.connect();
			return true;
		} catch (MalformedURLException e) {
			System.err.println("invalid tracker URL");
			//e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.err.println("error occurred while communicating with tracker");
			//e.printStackTrace();
			return false;
		}
	}
}