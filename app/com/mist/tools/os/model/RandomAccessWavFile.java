package com.mist.tools.os.model;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.LocalDateTime;

import io.vertx.core.buffer.Buffer;

public abstract class RandomAccessWavFile {
	protected Path filePath;
	
	protected long position = 0;
	
	protected byte[] twoBytes = new byte[2];
	
	protected byte[] fourBytes = new byte[4];
	
	protected Buffer readTwoBytes(RandomAccessFile file) throws IOException{
		return readBytes(file, twoBytes);
	}
	
	protected Buffer readFourBytes(RandomAccessFile file) throws IOException{
		return readBytes(file, fourBytes);
	}
	
	protected Buffer readBytes(RandomAccessFile file, byte[] bytes) throws IOException{
		file.read(bytes);
		this.position += bytes.length;
		return Buffer.buffer(bytes);
	}
	
	protected void skip(RandomAccessFile file, int length) throws IOException{
		file.skipBytes(length);
		this.position += length;
	}
	
	protected abstract void readFileInfo();
	
	public Path filePath(){
		return filePath;
	}
	
	public abstract double sensitivity();
	
	public abstract double peakVoltage();
	
	public abstract int bytesPerSample();
	
	public abstract long sampleRate();
	
	public abstract int wavLength();
	
	public long datasPosition(){
		return this.position;
	}
	
	public abstract LocalDateTime dateTime();
}
