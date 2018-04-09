//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class RUBTClient {
	Torrent torrent; 
	TrackerManager tm;
	PeerManager pm;
	
	ByteBuffer fileBuffer;
	
	public RUBTClient(String torrentFile, String outputFile){
		this.torrent = Torrent.getInstanceFromFile(torrentFile);
		if (torrent == null) {
			System.err.println("error parsing torrent file");
			return;
		}
		this.torrent.generatePeerId();
		this.tm = new TrackerManager(this.torrent, this);
		this.pm = new PeerManager(this.torrent, this);

		List<Peer> peerList = this.tm.getPeersList();
		if (peerList == null) {
			System.err.println("error getting PeerList from Tracker");
			return;
		}
		for (Peer p : peerList) {
			System.out.println(p);
			if (p.getIpString().equals("128.6.5.130") && p.getPeerIdString().substring(0, 6).equals("RUBT01")) {
				System.out.println("found peer");
				if (this.downloadFromSinglePeer(p, outputFile))return;
			}
		}
	}
	
	public boolean downloadFromSinglePeer(Peer p, String outputFile) {
		if (this.pm == null)return false;
		if (!this.pm.connect(p)) {
			System.out.println("couldn't connect to peer: " + p.getPeerIdString());
			return false;
		}
		if (this.tm.sendStartedEvent())System.out.println("started event sent to tracker");
		
		this.fileBuffer = ByteBuffer.allocate(this.torrent.file_length);
		ByteBuffer temp;
		for (int pieceIndx = 0; pieceIndx < this.torrent.piece_hashes.length; pieceIndx++) {
			temp = this.pm.getPiece(pieceIndx);
			if (temp == null){
				System.out.println("couldn't download piece:" + pieceIndx);
				return false;
			}
			this.fileBuffer.put(temp);
		}
		
		this.pm.disconnect();
		System.out.println("disconnected from peer");
		if (this.tm.sendCompletedEvent())System.out.println("completed event sent to tracker");
		
		FileOutputStream file = null;
		try {
			file = new FileOutputStream(new File(outputFile));
			file.write(this.fileBuffer.array());
			System.out.println("writing to file:" + outputFile);
		} catch (IOException e) {
			System.err.println("IOException when writing pieces to file");
			//e.printStackTrace();
		} finally {
			try {
				if (file != null)file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("invalid number of arguments");
			return;
		}
		new RUBTClient(args[0], args[1]);
		//new RUBTClient("src/utilities/project1.torrent", "img2.jpg");
	}

}
