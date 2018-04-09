//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Peer extends Socket {

	private int port;
	private ByteBuffer peerId;
	private ByteBuffer ip;
	private String peerId_urlEncoded;
	private String peerId_string;
	private String ip_string;
	
	private InetSocketAddress socketAddress;

	public Peer(ByteBuffer ip, int port, ByteBuffer peerId) throws UnknownHostException {
		this.ip = ip;
		this.port = port;
		this.peerId = peerId;
		
		this.peerId_urlEncoded = Torrent.urlEncodeBytes(peerId);
		this.peerId_string = new String(peerId.array());
		this.ip_string = new String(ip.array());
		
		this.setSocketAddress(new InetSocketAddress(InetAddress.getByName(this.ip_string), port));
	}
	
	public String getIpString() {
		return this.ip_string;
	}
	
	public ByteBuffer getIp() {
		return ip;
	}

	public void setIp(ByteBuffer ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public String getPeerIdString() {
		return this.peerId_string;
	}

	public ByteBuffer getPeerId() {
		return peerId;
	}

	public void setPeerId(ByteBuffer peerId) {
		this.peerId = peerId;
	}
	
	public Socket getConnection() {
		return this;
	}
	
	public boolean connect() throws IOException {
		if (this.socketAddress == null) return false;
		this.connect(this.socketAddress);
		return true;
	}
	
	public String toString() {
		return "peerId=" + this.peerId_string + "\tip=" + this.ip_string +
				" port=" + this.port + "\tpeerId_urlEncoded=" + this.peerId_urlEncoded;
	}

	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	public void setSocketAddress(InetSocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}
}
