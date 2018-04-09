//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.nio.ByteBuffer;

public class PeerMessage {
	
	public static byte[] sendHandshake(Peer peer, byte[] info_hash, byte[] peerId) {
		ByteBuffer message = ByteBuffer.allocate(68);
		message.put((byte)19);
		byte[] pstr = new String("BitTorrent protocol").getBytes();
		message.put(pstr);
		message.put(new byte[8]);
		message.put(info_hash);
		message.put(peerId);
		return message.array();
	}
	
	public static boolean confirmHandShake(byte[] packet, byte[] infohash, byte[] peerId) {
		if (packet[0] != 19)
			return false;
		
		byte[] pstr = new String("BitTorrent protocol").getBytes();
		for (int i = 1; i < 20; i++)
			if (pstr[i-1] != packet[i])
				return false;
		
		for (int i = 28; i < 48; i++) {
			if (infohash[i-28] != packet[i])
				return false;
		}
		
		for (int i = 48; i < 68; i++) {
			if (peerId[i-48] != packet[i])
				return false;
		}
		
		return true;
	}
	
	public static byte[] sendKeepAlive(){
		ByteBuffer message = ByteBuffer.allocate(4);
		message.putInt(0);
		
		return message.array();		
	}
	
	public static byte[] sendInterested(){
		ByteBuffer message = ByteBuffer.allocate(5);
		message.putInt(1);
		message.put((byte)2);
		
		return message.array();
	}
	
	public static byte[] sendRequest(int index, int begin, int length){
		ByteBuffer message = ByteBuffer.allocate(17);
		message.putInt(13);
		message.put((byte)6);
		message.putInt(index);
		message.putInt(begin);
		message.putInt(length);
		
		System.out.println("request " + index + ", " + begin + ", " + length);
		return message.array();
	}
	
	public static byte[] sendHave(int piece_index){
		ByteBuffer message = ByteBuffer.allocate(17);
		message.putInt(5);
		message.put((byte)4);
		message.putInt(piece_index);
		
		System.out.println("have " + piece_index);
		return message.array();		
	}
	
	public static void sendUninterested(Peer peer){
		
	}
}
