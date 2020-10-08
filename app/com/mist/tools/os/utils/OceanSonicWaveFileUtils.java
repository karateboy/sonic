package com.mist.tools.os.utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.mist.tools.os.model.OceanSonicWaveFmt;
import com.mist.tools.os.model.OceanSonicWaveList;

import io.vertx.core.buffer.Buffer;

public class OceanSonicWaveFileUtils {
	private static final String EMPTY_BYTE_STRING = " ";
	
	private static OceanSonicWaveFileUtils utils = new OceanSonicWaveFileUtils();
	
	public static OceanSonicWaveFileUtils getUtils(){
		return utils;
	}

	private final DateTimeFormatter ICRD_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
	
	private final String ICMT_CONTENT_PATTERN = "%.06f V pk, %.01f dBV re 1uPa, %d = Max Count, unit = %s, %.02f = modify dB";
	
	private OceanSonicWaveFileUtils(){
		
	}
	
	public Buffer wrapToWaveChunk(OceanSonicWaveList logList,
			OceanSonicWaveFmt logFmt, ZoneOffset offset, Buffer samples){
		Buffer id = wrapStringToBuffer("RIFF", 4);
		Buffer content = Buffer.buffer();
		Buffer format = wrapStringToBuffer("WAVE", 4);
		Buffer list = wrapToListChunk(logList, offset);
		Buffer fmt = wrapToFmtChunk(logFmt);
		Buffer data = wrapToDataChunk(samples);
		content.appendBuffer(format);
		content.appendBuffer(list);
		content.appendBuffer(fmt);
		content.appendBuffer(data);
		return wrapToChunk(id, content);
	}
	
	public Buffer wrapToListChunk(OceanSonicWaveList logList, ZoneOffset offset){
		Buffer id = wrapStringToBuffer("LIST", 4);
		Buffer content = wrapStringToBuffer("INFO", 4);
		content.appendBuffer(wrapToListContent(logList, offset));
		return wrapToChunk(id, content);
	}
	
	private Buffer wrapToListContent(OceanSonicWaveList logList, ZoneOffset offset){
		Buffer buffer = Buffer.buffer();
		Buffer iart = wrapToIARTChunk(logList.deviceType, logList.serial);
		buffer.appendBuffer(iart);
		Buffer icrd = wrapToICRDChunk(logList.epochSecond, offset);
		buffer.appendBuffer(icrd);
		Buffer inam = wrapToINAMChunk(logList.outputFileName);
		buffer.appendBuffer(inam);
		Buffer icmt = wrapToICMTChunk(logList);
		buffer.appendBuffer(icmt);
		return buffer;
	}
	
	private Buffer wrapToICRDChunk(long epochSecond, ZoneOffset offset){
		Buffer id = wrapStringToBuffer("ICRD", 4);
		LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, offset);
		Buffer content = wrapStringToBuffer(String.format("%s%s", dateTime.format(ICRD_TIME_FORMAT), offset.toString()), 0);
		return wrapToChunk(id, content);
	}
	
	private Buffer wrapToINAMChunk(String outputFileName){
		Buffer id = wrapStringToBuffer("INAM", 4);
		Buffer content = wrapStringToBuffer(outputFileName, 0);
		return wrapToChunk(id, content);
	}
	
	private Buffer wrapToIARTChunk(String deviceType, int serial){
		Buffer id = wrapStringToBuffer("IART", 4);
		Buffer content = wrapStringToBuffer(String.format("%s #%d", deviceType, serial), 0);
		return wrapToChunk(id, content);
	}
	
	private Buffer wrapToICMTChunk(OceanSonicWaveList logList){
		Buffer id = wrapStringToBuffer("ICMT", 4);
		String unitStr = logList.unit == 1 ? "0.000001 V" : "2 dB re V";
		double maxAmp = (double)logList.maxAmp * (logList.unit == 1 ? 1e-6 : 0.5);
		String contentStr = String.format(ICMT_CONTENT_PATTERN, 
				maxAmp, logList.sensitivity, logList.maxCount, unitStr, logList.modify);
		Buffer content = wrapStringToBuffer(contentStr, 0);
		return wrapToChunk(id, content);
	}

	public Map<String, Object> unwrapOutputInfoFromListChunk(Buffer buffer){
		Buffer content = buffer.getBuffer(12, buffer.length());
		Map<String, Object> map = new HashMap<>();
		unwrapOutputInfoFromListContent(content, map);
		return map;
	}
	
	private void unwrapOutputInfoFromListContent(Buffer content, Map<String, Object> map){
//		unwrapIARTChunk();
//		wrapToICRDChunk();
//		wrapToINAMChunk();
//		wrapToICMTChunk();
	}
	
	public Buffer wrapToFmtChunk(OceanSonicWaveFmt logFmt){
		Buffer id = wrapStringToBuffer("fmt", 4);
		Buffer content = wrapToFmtContent(logFmt);
		return wrapToChunk(id, content);
	}

	private Buffer wrapToFmtContent(OceanSonicWaveFmt logFmt){
		Buffer buffer = Buffer.buffer();
		short compressionCode = 1;
		buffer.appendShortLE(compressionCode);
		short numberOfChannels = logFmt.numberOfChannels;
		buffer.appendShortLE(numberOfChannels);
		int sampleRate = logFmt.sampleRate;
		buffer.appendIntLE(sampleRate);
		short bytesPerSample = logFmt.bytesPerSample;
		int bytesPerSecond = bytesPerSample*sampleRate*numberOfChannels;
		buffer.appendIntLE(bytesPerSecond);
		short blockAlignment = (short) (numberOfChannels * bytesPerSample);
		buffer.appendShortLE(blockAlignment);
		short bitsPerSample = (short) (8 * bytesPerSample);
		buffer.appendShortLE(bitsPerSample);
		return buffer;
	}
	
	public Map<String, Object> unwrapOutputInfoFromFmtChunk(Buffer buffer){
		Buffer content = buffer.getBuffer(8, buffer.length());
		Map<String, Object> map = new HashMap<>();
		unwrapOutputInfoFromFmtContent(content, map);
		return map;
	}
	
	private void unwrapOutputInfoFromFmtContent(Buffer content, Map<String, Object> map){
		short channels = content.getShortLE(2);
		int sampleRate = content.getIntLE(4);
		short bitPerSample = content.getShortLE(14);
		map.put("channels", channels);
		map.put("sampleRate", sampleRate);
		map.put("bitsPerSample", bitPerSample);
	}
	
	public Buffer wrapStringToBuffer(String str, int length){
		Buffer buffer = Buffer.buffer();
		byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
		if(length != 0){
			if(strBytes.length < length){
				for(int i=strBytes.length;i<length;i++){
					str += EMPTY_BYTE_STRING;
				}
			}
		}else{
			int res = strBytes.length % 4;
			if(res != 0){
				for(int i=res;i<4;i++ ){
					str += EMPTY_BYTE_STRING;
				}
			}
		}
		buffer.appendString(str);
		return buffer;
	}
	
	public Buffer wrapToDataChunk(Buffer samples){
		Buffer id = wrapStringToBuffer("data", 4);
		return wrapToChunk(id, samples);		
	}
	
	public Buffer wrapToChunk(Buffer id, Buffer content){
		Buffer buffer = Buffer.buffer();
		buffer.appendBuffer(id);
		buffer.appendUnsignedIntLE(content.length());
		buffer.appendBuffer(content);
		return buffer;
	}
}
