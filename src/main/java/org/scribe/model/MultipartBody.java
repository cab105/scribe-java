package org.scribe.model;

public class MultipartBody {
	private byte[] contents;
	private String header;
	private String name;
	private String filename;
	private boolean encoded;
	
	public MultipartBody() {
		contents = null;
		header = null;
		name = null;
		encoded = false;
	}
	
	public MultipartBody(String header, boolean encoded) {
		this.header = header;
		this.encoded = encoded;
	}
	
	public MultipartBody(String header, String n, String filename, boolean encoded, byte[] contents) {
		this.header = header;
		this.contents = contents;
		this.encoded = encoded;
		this.filename = filename;
		this.name = n;
	}
	
	public void setContents(byte[] contents, String n, String f) {
		this.contents = contents;
		this.name = n;
		this.filename = f;
	}
	
	public void setHeader(String header, boolean encoded) {
		this.header = header;
		this.encoded = encoded;
	}

	public String getHeader() {
		return header;
	}

	public boolean isEncoded() {
		return encoded;
	}

	public byte[] getContents() {
		return contents;
	}

	public String getName() {
		return name;
	}

	public String getFilename() {
		return filename;
	}
}