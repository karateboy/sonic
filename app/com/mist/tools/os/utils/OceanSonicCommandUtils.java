package com.mist.tools.os.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import io.vertx.core.buffer.Buffer;

public class OceanSonicCommandUtils {
	
	public static Buffer getStartStreamingCommand(int duration){
		Buffer buffer = Buffer.buffer(8);
		byte type = 0x33;
		byte sync = 0x2A;
		buffer.appendByte(type);
		buffer.appendByte(sync);
		int payloadLength = 2;
		buffer.appendUnsignedShortLE(payloadLength);
		buffer.appendUnsignedShortLE(duration);
		int rd = 0;
		buffer.appendUnsignedShortLE(rd);
		return buffer;
	}
	
	public static Buffer getStopStreamingCommandForStreamingSocket(){
		Buffer buffer = Buffer.buffer(6);
		byte type = 0x34;
		byte sync = 0x2A;
		buffer.appendByte(type);
		buffer.appendByte(sync);
		int payloadLength = 0;
		buffer.appendUnsignedShortLE(payloadLength);
		return buffer;
	}
	
	public static Buffer getStopStreamingCommandForCommandSocket(){
		Buffer buffer = Buffer.buffer(6);
		byte type = 0x51;
		byte sync = 0x2A;
		buffer.appendByte(sync);
		buffer.appendByte(type);		
		int payloadLength = 0;
		buffer.appendUnsignedShortLE(payloadLength);
		int crc = CRCUtils.calculateTxCrc(buffer);
		buffer.appendUnsignedShortLE(crc);
		return buffer;
	}
	
	public static Buffer getQueryDeviceInfoCommand(){
		Buffer buffer = Buffer.buffer(6);
		byte sync = 0x2A;
		byte type = 0x45;
		buffer.appendByte(sync);
		buffer.appendByte(type);
		int payloadLength = 0;
		buffer.appendUnsignedShortLE(payloadLength);
		int crc = CRCUtils.calculateTxCrc(buffer);
		buffer.appendUnsignedShortLE(crc);
		return buffer;
	}
	
	public static Buffer getSetTimeCommand(){
		Buffer buffer = Buffer.buffer(10);
		byte sync = 0x2A;
		byte type = 0x41;
		buffer.appendByte(sync);
		buffer.appendByte(type);
		buffer.appendUnsignedShortLE(4);
		LocalDateTime currentDateTime = LocalDateTime.now();
		long epochSecond = currentDateTime.toEpochSecond(ZoneOffset.ofHours(8));
		buffer.appendUnsignedIntLE(epochSecond);
		int crc = CRCUtils.calculateTxCrc(buffer);
		buffer.appendUnsignedShortLE(crc);
		return buffer;
	}
}
