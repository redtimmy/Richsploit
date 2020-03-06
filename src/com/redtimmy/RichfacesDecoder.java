package com.redtimmy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;



public class RichfacesDecoder {

	public static byte[] decrypt(byte[] src) {
  		byte[] zipsrc = URL64Codec.decodeBase64(src);
  		Inflater decompressor = new Inflater();
  		byte[] uncompressed = new byte[zipsrc.length * 5];
  		decompressor.setInput(zipsrc);
  		
  		int totalOut = 0;
		try {
			totalOut = decompressor.inflate(uncompressed);
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  		byte[] out = new byte[totalOut];
  		System.arraycopy(uncompressed, 0, out, 0, totalOut);
  		decompressor.end();
  		return out;
  	}  

	protected static byte[] encrypt(byte[] src) {
		try {
			Deflater compressor = new Deflater(1);
			byte[] compressed = new byte[src.length + 100];

			compressor.setInput(src);
			compressor.finish();

			int totalOut = compressor.deflate(compressed);
			byte[] zipsrc = new byte[totalOut];

			System.arraycopy(compressed, 0, zipsrc, 0, totalOut);
			compressor.end();

			return URL64Codec.encode(zipsrc);
		} catch (Exception e) {
			System.out.println("Error encode resource data");
		}
		return null;
	}
	
	
	public static byte[] decode(String value) {
		byte[] objectArray = null;
		try {
			objectArray = decrypt(value.getBytes("ISO-8859-1"));
    	} catch(Exception e){
    		System.out.println("Cant parse string. "+e);
    	}
		return objectArray;
	}
	
	public static String encode(byte[] bin) {
    	try {
    	    return new String(encrypt(bin), "ISO-8859-1");
    	} catch(Exception e) {
    		System.out.println(e);
    	}
    	return null;
	}
	
	public static String encode(String filename) {
		byte[] fileContent = null;
		try {
			fileContent = Files.readAllBytes(new File(filename).toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return encode(fileContent);
	}
	
	
}
