package com.mist.tools.os.model;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import io.vertx.core.buffer.Buffer;

public class OceanSonicWavFile extends RandomAccessWavFile{

	private final OceanSonicWavFileProperties props = new OceanSonicWavFileProperties();
	
	public OceanSonicWavFile(String filePathStr){
		this.filePath = Paths.get(filePathStr);
		readFileInfo();
	}
	
	public OceanSonicWavFile(Path filePath){
		this.filePath = filePath;
		readFileInfo();
	}
	
	private String readId(RandomAccessFile file) throws IOException{
		return readFourBytes(file).getString(0, 4, "utf-8").trim();
	}
	
	private long readContentLength(RandomAccessFile file) throws IOException{
		return readFourBytes(file).getUnsignedIntLE(0);
	}
	
	@Override
	protected void readFileInfo(){
		try{
			RandomAccessFile file = new RandomAccessFile(filePath.toAbsolutePath().toString(), "r");
			readWavChunkHeader(file);
			readListChunk(file);
			readFmtChunk(file);
			readSamplesChunkInfo(file);
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void readWavChunkHeader(RandomAccessFile file) throws IOException{
		skip(file, 4);
		props.contentBytes(readContentLength(file));
		skip(file, 4);
	}
	
	private void readListChunk(RandomAccessFile file) throws IOException{
		String id = readId(file);
		if(id.equals("LIST")){
			long length = readContentLength(file);
			skip(file, 4);
			long shift = 4;
			while(length > shift){
				shift += readListSubChunk(file);
			}
		}else{
			this.position -= 4;
			file.seek(position);
		}
	}
	
	private long readListSubChunk(RandomAccessFile file) throws IOException{
		String id = readId(file);
		long length = readContentLength(file);
		switch(id){
		case "ICMT":
			parseICMT(readBytes(file, new byte[(int) length]));
			break;
		case "ICRD":
			parseICRD(readBytes(file, new byte[(int) length]));
			break;
		default :
			skip(file, (int) length);
		}
		return length + 8;
	}
	
	private void parseICMT(Buffer buffer){
		String str = buffer.getString(0, buffer.length(), "utf-8").trim();
		str = str.replace("\\x00", "");
		String[] contents = str.split(",");
		for(int i=0;i<contents.length;i++){
			parseICMTContent(contents[i]);
		}
	}

	private void parseICRD(Buffer buffer){
		String str = buffer.getString(0, buffer.length(), "utf-8").trim();
		props.dateTime(LocalDateTime.parse(str.substring(0, 19)));
		props.zoneOffset(ZoneOffset.of(str.substring(19, str.length())));
	}
	
	private void parseICMTContent(String content){
		if(content.matches(".*V pk.*")){
			props.peakVoltage(Double.valueOf(content.replace("V pk", "").trim()));
		}
		if(content.matches(".*Vpk=.*")){
			props.peakVoltage(Double.valueOf(content.split("=")[1].trim()));
		}
		if(content.matches(".*dBV re 1uPa.*")){
			props.sensitivity(Double.valueOf(content.replace("dBV re 1uPa", "").trim()));
		}
		if(content.matches(".*V re 1uPa=.*")){
			props.sensitivity(Double.valueOf(content.split("=")[1].trim()));
		}
		if(content.matches(".*unit.*")){
			props.unit(Double.valueOf(content.split("=")[1].replace("dB re V", "").replace("V", "").trim()));
		}
	}
	
	private void readFmtChunk(RandomAccessFile file) throws IOException{
		skip(file, 4);
		long length = readContentLength(file);
		skip(file, 2);
		props.channels(readTwoBytes(file).getUnsignedShortLE(0));
		props.sampleRate(readFourBytes(file).getUnsignedIntLE(0));
		props.bytesPerSecond(readFourBytes(file).getUnsignedIntLE(0));
		props.bytesPerSample(readTwoBytes(file).getUnsignedShortLE(0));
		props.bitsPerDataPoint(readTwoBytes(file).getUnsignedShortLE(0));
		long diff = length - 2 - 2 - 4 - 4 - 2 - 2;
		if(diff > 0){
			skip(file, (int) diff);
		}
	}
	
	private void readSamplesChunkInfo(RandomAccessFile file) throws IOException{
		skip(file, 4);
		props.totalDataBytes(readContentLength(file));
		props.wavLength((int) ((props.totalDataBytes() - props.totalDataBytes() % props.bytesPerSecond()) / props.bytesPerSecond()));
	}
	
	public OceanSonicWavFileProperties props(){
		return props;
	}
	
	public long samplesPosition(){
		return position;
	}
	
	public Path filePath(){
		return filePath;
	}
	
	@Override
	public String toString(){
		return props.toString();
	}

	@Override
	public double sensitivity() {
		return props.sensitivity();
	}

	@Override
	public double peakVoltage() {
		return props.peakVoltage();
	}

	@Override
	public int bytesPerSample() {
		return props.bytesPerSample();
	}

	@Override
	public long sampleRate() {
		return props.sampleRate();
	}

	@Override
	public int wavLength() {
		return props.wavLength();
	}

	@Override
	public long datasPosition() {
		return position;
	}

	@Override
	public LocalDateTime dateTime() {
		return props.dateTime();
	}
}
