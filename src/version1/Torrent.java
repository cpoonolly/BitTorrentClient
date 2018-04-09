//Eric Kanagusuku
//Ryan Poonolly

package version1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import utilities.BencodingException;
import utilities.TorrentInfo;

public class Torrent extends TorrentInfo {
	private static final char[] hexDictionary = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	ByteBuffer peerId;
	int uploaded;
	int downloaded;
	int left;
	
	public static Torrent getInstanceFromFile(String filepath) {
		Torrent torrent = null;
		FileInputStream fin = null;
		byte filebytes[];
		File file = new File(filepath);
		
		if (!file.isFile()) {
			System.err.print("invalid path");
			return null;
		} else if (file.length() > Integer.MAX_VALUE){
			System.err.print(".torrent file is too large");
			return null;
		}
		
		try {
			fin = new FileInputStream(file);
			filebytes = new byte[(int)file.length()];
			fin.read(filebytes);
			torrent = new Torrent(filebytes);
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			System.err.println("couldn't read file");
			torrent = null;
		} catch (IOException e) {
			//e.printStackTrace();
			System.err.println("error reading file");
			torrent = null;
		} catch (BencodingException e) {
			//e.printStackTrace();
			System.err.println("error parsing file");
			torrent = null;
		} finally {
			if (fin != null){
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return torrent;
	}
	
	public Torrent(byte[] file_bytes) throws BencodingException {
		super(file_bytes);
		this.peerId = ByteBuffer.allocate(20);
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = this.file_length;
	}
	
	public static String urlEncodeBytes(ByteBuffer buffer) {
		String result = "";
		byte temp;
		int bufferPos = buffer.position();
		
		buffer.rewind();
		while (buffer.hasRemaining()) {
			temp = buffer.get();
			result += "%" + hexDictionary[(temp & 0xF0) >> 4] + hexDictionary[(temp & 0x0F)];
		}
		buffer.position(bufferPos);
		return result;
	}
	
	public void generatePeerId() {
		Random randGen = new Random();
		this.peerId.clear();
		this.peerId.putInt(randGen.nextInt());
		this.peerId.putInt(randGen.nextInt());
		this.peerId.putInt(randGen.nextInt());
		this.peerId.putInt(randGen.nextInt());
		this.peerId.putInt(randGen.nextInt());
	}
	
	public String getInfoHash_URLEncoded() {
		return Torrent.urlEncodeBytes(this.info_hash);
	}
	
	public String getPeerId_URLEncoded() {
		return Torrent.urlEncodeBytes(this.peerId);
	}
	
	public static byte hexStringToByte(String hex) {
		byte result = 0;
		for (int i = 0; i < 2; i++)
		{
			switch (hex.charAt(i)) {
				case '0': result += 0*Math.pow(16,(1-i)); break;
				case '1': result += 1*Math.pow(16,(1-i)); break;
				case '2': result += 2*Math.pow(16,(1-i)); break;
				case '3': result += 3*Math.pow(16,(1-i)); break;
				case '4': result += 4*Math.pow(16,(1-i)); break;
				case '5': result += 5*Math.pow(16,(1-i)); break;
				case '6': result += 6*Math.pow(16,(1-i)); break;
				case '7': result += 7*Math.pow(16,(1-i)); break;
				case '8': result += 8*Math.pow(16,(1-i)); break;
				case '9': result += 9*Math.pow(16,(1-i)); break;
				case 'A': result += 10*Math.pow(16,(1-i)); break;
				case 'B': result += 11*Math.pow(16,(1-i)); break;
				case 'C': result += 12*Math.pow(16,(1-i)); break;
				case 'D': result += 13*Math.pow(16,(1-i)); break;
				case 'E': result += 14*Math.pow(16,(1-i)); break;
				case 'F': result += 15*Math.pow(16,(1-i)); break;
			}
		}
		return result;
	}
	
	public boolean verifySHA1Hash(int piece_index, byte[] piece) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			byte[] hash = md.digest(piece);
			
			for (int i = 0; i < 20;i++)
			{
				if (hash[i] != piece_hashes[piece_index].array()[i])
					return false;
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return true;
	}
}
