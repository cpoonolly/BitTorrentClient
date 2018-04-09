//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class PeerManager {
	private Peer peer;
	private Torrent torrent;
	private RUBTClient controller;
	
	private DataOutputStream out;
	private DataInputStream in;
	
	public PeerManager(Torrent torrent, RUBTClient controller){
		this.torrent = torrent;
		this.controller = controller;
	}
	
	public boolean connect(Peer peer){
		try {
			this.peer = peer;
			
			//set socket connection
			System.out.println(peer.getSocketAddress());
			peer.connect();

			this.out = new DataOutputStream(peer.getOutputStream());
			this.in = new DataInputStream(peer.getInputStream());
			
			//send handshake
			out.write(PeerMessage.sendHandshake(peer, torrent.info_hash.array(), torrent.peerId.array()));

			//read and confirm handshake from peer
			byte[] inbuff = new byte[68];
			in.read(inbuff);
			if (PeerMessage.confirmHandShake(inbuff, torrent.info_hash.array(), peer.getPeerId().array())) {
				System.out.println("good handshake");
			} else {
				System.out.println("bad handshake");
				throw new IOException();
			}

			//send interested
			out.write(PeerMessage.sendInterested());
			return true;
		} catch (IOException e) {
			System.err.println("error connecting to Peer, peerId=" + peer.getPeerIdString());
			//e.printStackTrace();
			try {
				if (this.out != null)this.out.close();
				if (this.in != null)this.in.close();
				if (peer != null) peer.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			return false;
		}
	}
	
	public boolean isConnected() {
		if (this.peer != null) {
			return this.peer.isClosed();
		}
		return false;
	}
	
	public boolean sendKeepAlive() {
		try {
			if (this.out != null) {
				out.write(PeerMessage.sendKeepAlive());
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public ByteBuffer getPiece(int index) {
		try {
			ByteBuffer fileoutbuffer = null;
			
			//read and parse input coming from peer
			int messagelength = 0;
			int messageid = 0;
			ByteBuffer message;
			boolean canrequestpiece = true;
			do {
				out.write(PeerMessage.sendKeepAlive());
				if (canrequestpiece)
					if (index == torrent.piece_hashes.length - 1)
						out.write(PeerMessage.sendRequest(index,0, torrent.file_length % torrent.piece_length));
					else
						out.write(PeerMessage.sendRequest(index,0, torrent.piece_length));
				canrequestpiece = false;
				
				if ((messagelength = in.readInt()) != 0) {
					messageid = in.readByte();
					message = ByteBuffer.allocate(messagelength - 1);

					switch (messageid) {
						case 0: System.out.println("choke"); in.readFully(message.array()); break;
						case 1: System.out.println("unchoke"); in.readFully(message.array()); break;
						case 2: System.out.println("interested"); in.readFully(message.array()); break;
						case 3: System.out.println("notinterested"); in.readFully(message.array()); break;
						case 4: System.out.println("have"); in.readFully(message.array()); break;
						case 5: System.out.println("bitfield"); in.readFully(message.array()); break;
						case 6: System.out.println("request"); in.readFully(message.array()); break;
						case 7:
							ByteBuffer payload = ByteBuffer.allocate(messagelength - 9);
							int piece_index = in.readInt();
							int piece_begin = in.readInt();
							in.readFully(payload.array());
							if (torrent.verifySHA1Hash(index, payload.array())) {
								out.write(PeerMessage.sendHave(piece_index));
								fileoutbuffer = ByteBuffer.wrap(payload.array());
								//fileoutbuffer.put(payload.array());
							}
							else
								System.out.println("piece unverified");
							canrequestpiece = true;
							break;
					}

				}			
			} while (!canrequestpiece);
			System.out.println(torrent.file_name);
			return fileoutbuffer;
		} catch (IOException e) {
			System.err.println("error communicating with Peer");
			//e.printStackTrace();
			return null;
		}
	}
	
	public void disconnect() {
		try {
			if (this.out != null)this.out.close();
			if (this.in != null)this.in.close();
			if (this.peer != null)peer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
