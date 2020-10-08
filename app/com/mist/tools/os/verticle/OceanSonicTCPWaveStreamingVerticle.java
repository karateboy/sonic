package com.mist.tools.os.verticle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import org.slf4j.LoggerFactory;

import com.mist.tools.os.model.OceanSonicWaveFmt;
import com.mist.tools.os.model.OceanSonicWaveList;
import com.mist.tools.os.model.OceanSonicWavFileRecorder;
import com.mist.tools.os.utils.OceanSonicCommandUtils;
import io.vertx.core.buffer.Buffer;

public class OceanSonicTCPWaveStreamingVerticle extends TCPClientVerticle {
	public static final String OUTPUT_FILE_PREFIX = "HY";
	
	public static final String TAG_START_STREAMING = "start streaming";
	public static final String TAG_STOP_STREAMING = "stop streaming";
	public static final String TAG_START_LOGGING = "start logging";
	public static final String TAG_STOP_LOGGING = "stop logging";
	
	protected DateTimeFormatter outputNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	
	protected boolean isStreaming = false;
	
	protected boolean isLogging = false;
	
	protected boolean logging = false;
	
	protected ZoneOffset zoneOffset = ZoneOffset.UTC;
	
	protected Path recordPath = null;
	
	protected int logLength = 60;
	
	protected OceanSonicWaveList logList = new OceanSonicWaveList();
	
	protected OceanSonicWaveFmt logFmt = new OceanSonicWaveFmt();
	
	private Buffer remainBuffer = Buffer.buffer(0);
	
	private OceanSonicWavFileRecorder wavFileRecorder = null;
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public OceanSonicTCPWaveStreamingVerticle(String url, int port, String recordPathStr,
			ZoneOffset zoneOffset, int logLength, double modify){
		this.url = url;
		this.port = port;
		this.recordPath = Paths.get(recordPathStr);
		this.zoneOffset = zoneOffset;
		this.logLength = logLength*60;
		logList.modify = modify;
		this.logger = LoggerFactory.getLogger(OceanSonicTCPWaveStreamingVerticle.class);
	}
	
	@Override
	public void start() throws Exception{
		super.start();
		vertx.eventBus().consumer(TAG_START_STREAMING, msg -> startStreaming());
		vertx.eventBus().consumer(TAG_STOP_STREAMING, msg -> stopStreaming());
		vertx.eventBus().consumer(TAG_START_LOGGING, msg -> startLogging());
		vertx.eventBus().consumer(TAG_STOP_LOGGING, msg -> stopLogging());
	}
	
	@Override
	public void stop() throws Exception {
		stopLogging();
		stopStreaming();
		outputLogging();
		executor.shutdown();
		super.stop();
	}
	
	private void stopStreaming(){
		Buffer stopStream = OceanSonicCommandUtils.getStopStreamingCommandForStreamingSocket();
		
		socket.write(stopStream, socketRes -> {
			if(socketRes.succeeded()){
				isStreaming = false;
				logger.info("Send stop streaming message success!");
			}else{
				logger.warn("Send stop streaming message failed");
			}
		});
		socket.pause();
		socket.drainHandler(done ->{
			socket.resume();
		});
		isLogging = false;
	}
	
	private void startStreaming(){
		Buffer startStream = OceanSonicCommandUtils.getStartStreamingCommand(0);
		socket.write(startStream, socketRes -> {
			if(socketRes.succeeded()){
				isStreaming = true;
				logger.info("Send start streaming message success!");
			}else{
				logger.warn("Send start streaming message failed");
			}
		});
		
		socket.handler(buf -> {
			logger.debug("I received some bytes: {}", buf.length());
			handleStreaming(buf);
		});
	}

	@Override
	protected void handleMessage(Buffer buffer){
		handleStreaming(buffer);
	}
	
	private void handleStreaming(Buffer buffer){
		logger.debug("buffer length : {}, remainBuffer length : {}", buffer.length(), remainBuffer.length());
		remainBuffer = remainBuffer.appendBuffer(buffer);
		int shift = parseDataStreamingMessage(remainBuffer);
		logger.debug("after parse -> remainBuffer length : {}, shift : {}", remainBuffer.length(), shift);
		remainBuffer = remainBuffer.getBuffer(shift, remainBuffer.length());
	}
	
	private int parseDataStreamingMessage(Buffer buffer){
		char type = (char)buffer.getByte(0);
		logger.debug("buffer type : {}", type);
		
		int shift = 0;
		switch(type){
		case '2':
			shift = parseEventHeader(buffer);
			break;
		case '1':
			boolean continueParse = true;
			while(continueParse){
				logger.debug("before parse data shift : {}, and buffer length : {}", shift, buffer.length());
				int prevShift = shift;
				shift = parseData(shift, buffer);
				logger.debug("after parse data shift : {}, and buffer length : {}", shift, buffer.length());
				continueParse = (prevShift != shift) && (buffer.length() > shift + 4);
			}
			break;
		case '0':
			shift = parseNotify(buffer);
		}
		return shift;
	}
	
	private int parseNotify(Buffer buffer){
		int code = buffer.getInt(4);
		logger.info("notify code : {}", code);
//		switch(code){
//		case 1:
//		case 2:
//		case 3:
//		case 4:
//		case 5:
//		}
		return 8;
	}
	
	private int parseEventHeader(Buffer buffer){
		int shift = 0;
		int payloadLength = buffer.getUnsignedShort(2);
//		System.out.println("Event header payload length : " + payloadLength);
		int size = payloadLength + 4;
		boolean continueParse = buffer.length() >= size;
//		System.out.println(continueParse);
		if(continueParse){
//			storageEventKey();
			shift += 4;
			shift = parseEventKeyChunk(shift, buffer);
//			shift += 20;
			shift = parseEventHeaderChunk(shift, buffer);
			handleLoggingStatus();
		}
		return shift;
	}
	
	private void handleLoggingStatus(){
		if(logging){
			long logEpochSecond = logList.epochSecond - logList.logStartEpochSecond;//LocalDateTime.now().toEpochSecond(zoneOffset) - logList.epochSecond;
			if(logEpochSecond >= logLength){
				outputLogging();
			}else if(!isLogging){
				outputLogging();
			}
		}
		
		logging = isLogging;
		
		if(logging && logList.logStartEpochSecond == 0){
			initLogging();
		}
	}
	
	private final void initLogging(){
		initLoggingFlag();
		initLoggingWorker();
		initLoggingHook();
	}
	
	private void initLoggingFlag(){
		logList.logStartEpochSecond = logList.epochSecond;
		LocalDateTime logDateTime = LocalDateTime.ofEpochSecond(logList.logStartEpochSecond, 0, zoneOffset);
		logList.outputFileName = String.format("%s_%s", OUTPUT_FILE_PREFIX, logDateTime.format(outputNameFormatter));
	}
	
	private void initLoggingWorker(){
		wavFileRecorder = new OceanSonicWavFileRecorder(vertx, recordPath, logList, logFmt, zoneOffset);
	}

	protected void initLoggingHook(){
		//TODO
	}
	
	private final void outputLogging(){
		outputLoggingFile();
		outputLoggingHook();
		resetLogggingFlag();
	}
	
	private void outputLoggingFile(){
		if (wavFileRecorder == null) return;
		wavFileRecorder.output();
	}
	
	protected void outputLoggingHook(){
		//TODO
	}
	
	private void resetLogggingFlag(){
		logList.logStartEpochSecond = 0;
		wavFileRecorder = null;
	}
	
	private int parseEventKeyChunk(int shift, Buffer buffer){
		logList.epochSecond = buffer.getInt(shift + 4);
		LocalDateTime dateTime = LocalDateTime.ofEpochSecond(logList.epochSecond, 0, zoneOffset);
		logList.setDataDateTime(dateTime);
		logger.debug("epoch second : " + logList.epochSecond);
//		System.out.println(LocalDateTime.ofEpochSecond(currentEpochSecond, 0, zoneOffset));
//		System.out.println(buffer.getUnsignedInt(shift + 8));
		return shift + 16;
	}
	
	private int parseEventHeaderChunk(int shift, Buffer buffer) {
		shift = parseDeviceInfoChunk(shift, buffer);
		shift = parseStatusChunk(shift, buffer);
		shift = parseSetupChunk(shift, buffer);
		shift = parseScalingChunk(shift, buffer);
		return shift;
	}
	
	private int parseSetupChunk(int shift, Buffer buffer){
//		char type = (char) buffer.getByte(shift);
//		System.out.println("WaveSetupChunk Type : " + type);
//		short version = buffer.getByte(shift + 1);
//		System.out.println("WaveSetupChunk version : " + version);
		int payloadLength = buffer.getUnsignedShort(shift + 2);
//		System.out.println("WaveSetupChunk payload size : " + payloadLength);
		int sampleRate = buffer.getInt(shift + 4);
		logFmt.sampleRate = sampleRate;
		
//		System.out.println("WaveSetupChunk sampleRate : " + setupInfos.sampleRate);
		int gain = buffer.getUnsignedShort(shift + 8);
		logFmt.gain = gain;

//		System.out.println("WaveSetupChunk gain : " + setupInfos.gain);
		short dataFormat = buffer.getUnsignedByte(shift + 10);
		logFmt.dataFormat = dataFormat;

//		System.out.println("WaveSetupChunk dataFormat : " + setupInfos.dataFormat);
//		short endian = (short) (setupInfos.dataFormat >> 7);
//		System.out.println("WaveSetupChunk endian : " + endian);
//		short bits = (short) ((setupInfos.dataFormat << 9) >> 9);
//		System.out.println("WaveSetupChunk bits : " + bits);
		parseSetupHook();
		return shift + payloadLength + 4;
	}
	
	protected void parseSetupHook(){
		//TODO
	}
	
	private int parseData(int shift, Buffer buffer){
		char type = (char)buffer.getByte(shift);
		if(type == '1'){
			int payloadLength = buffer.getUnsignedShort(shift + 2);
//			System.out.println("Data payload length :" + payloadLength);
			int size = payloadLength + 4;
			boolean continueParse = buffer.length() >= shift + size;
			if(continueParse){
//				shift += 4;
//				shift = parseEventKeyChunk(shift, buffer);
				shift += 20;
				shift = parseDataChunk(shift, buffer);
			}
		}
		return shift;
	}
	
	private int parseDataChunk(int shift, Buffer buffer){
//		char type = (char) buffer.getByte(shift);
//		System.out.println("Data chunk Type : " + type);
//		short version = buffer.getByte(shift + 1);
//		System.out.println("Data chunk version : " + version);
		int chunkSize = buffer.getUnsignedShort(shift + 2);
//		System.out.println("Data chunk size : " + chunkSize);
		int sampleIndex = buffer.getInt(shift + 4);
//		System.out.println("Sample # : " + sampleIndex);
//		short channels = buffer.getByte(shift + 8);
//		System.out.println("Channels : " + channels);
		short dataType = buffer.getUnsignedByte(shift + 9);
//		short endian = (short) (dataType >> 7);
//		short bits = (short) ((dataType << 9) >> 9);
//		System.out.println("dataType : " + dataType);
//		System.out.println("Endian : " + endian);
//		System.out.println("bits : " + bits);
		int numberOfSamples = buffer.getUnsignedShort(shift + 10);
//		System.out.println("Number of Samples in Chunk : " + numberOfSamples);
		if(logging){
			loggingSamples(buffer.getBuffer(shift+12, shift + 4 + chunkSize));
		}
		parseSampleChunk(shift + 12, buffer, sampleIndex, dataType, numberOfSamples);

		return shift + chunkSize + 4;
	}
	
	private void parseSampleChunk(int shift, Buffer buffer, int sampleIndex, short dataType, int numberOfSamples){
		short bytesPerSample = logFmt.bytesPerSample;
		DoubleUnaryOperator unitFunc = getUnitFunction();
		IntUnaryOperator endianFunc = null;
		
		switch (dataType){
		case 2:
			endianFunc = i -> (int)buffer.getShort(shift + i*bytesPerSample);
			break;
		case 3:
			endianFunc = i -> (int)buffer.getMedium(shift + i*bytesPerSample);
			break;
		case 4:
			endianFunc = i -> (int)buffer.getInt(shift + i*bytesPerSample);
			break;
		case 130:
			endianFunc = i -> (int)buffer.getShortLE(shift + i*bytesPerSample);
			break;
		case 131:
			endianFunc = i -> (int)buffer.getMediumLE(shift + i*bytesPerSample);
			break;
		case 132:
			endianFunc = i -> (int)buffer.getIntLE(shift + i*bytesPerSample);
			break;
			
		}
		
		parseSamplesHook(sampleIndex, numberOfSamples, endianFunc, unitFunc);
	}
	
	private DoubleUnaryOperator getUnitFunction(){
		switch(logList.unit){
		case 1:
			return v -> v * 1e-6; 
		case 4:
			return v -> v / 2;
		default:
			return v -> v;
		}
	}
	
	protected void parseSamplesHook(int sampleIndex, int numberOfSamples, 
			IntUnaryOperator endianFunc, DoubleUnaryOperator unitFunc){
		//TODO
	}
	
	private int parseDeviceInfoChunk(int shift, Buffer buffer){
//		char type = (char) buffer.getByte(shift);
//		System.out.println("DeviceInfoChunk Type : " + type);
//		short version = buffer.getByte(shift + 1);
//		System.out.println("DeviceInfoChunk version : " + version);
		int payloadLength = buffer.getUnsignedShort(shift + 2);
//		System.out.println("DeviceInfoChunk payload size : " + payloadLength);
		short deviceType = buffer.getByte(shift + 4);
		switch (deviceType){
		case 5:
			logList.deviceType = "icListen HF"; break;
		case 7:
			logList.deviceType = "icListen AF"; break;
		case 8:
			logList.deviceType = "Host Controller"; break;
		}
//		System.out.println("DeviceInfoChunk deviceType : " + deviceType);
//		String firmwareVersion = convertByteToNullTerminatedASCIIString(buffer.getBuffer(shift+5,  shift+8));
//		System.out.println("DeviceInfoChunk firmwareVersion : " + firmwareVersion);
		int serialNumber = buffer.getUnsignedShort(shift + 8);
		logList.serial = serialNumber;
//		System.out.println("DeviceInfoChunk serialNumber : " + serialNumber);
		int sensitivity = buffer.getShort(shift + 10);
		logList.sensitivity = sensitivity * 0.1 + logList.modify;
//		System.out.println("DeviceInfoChunk sensitivity : " + sensitivity);
//		short channelNumber = buffer.getUnsignedByte(shift + 12);
//		System.out.println("DeviceInfoChunk channelNumber : " + channelNumber);
		return shift + payloadLength + 4;
	}
	
	private int parseStatusChunk(int shift, Buffer buffer){
//		char type = (char) buffer.getByte(shift);
//		System.out.println("StatusChunk Type : " + type);
//		short version = buffer.getByte(shift + 1);
//		System.out.println("StatusChunk version : " + version);
		int payloadLength = buffer.getUnsignedShort(shift + 2);
//		System.out.println("StatusChunk payload size : " + payloadLength);
		return shift + payloadLength + 4;
	}
	
	private int parseScalingChunk(int shift, Buffer buffer){
//		char type = (char) buffer.getByte(shift);
//		System.out.println("ScalingChunk Type : " + type);
//		short version = buffer.getByte(shift + 1);
//		System.out.println("ScalingChunk version : " + version);
		int payloadLength = buffer.getUnsignedShort(shift + 2);
//		System.out.println("ScalingChunk payload size : " + payloadLength);
//		short channel = buffer.getUnsignedByte(shift + 4);
//		System.out.println("ScalingChunk channel : " + channel);
		short unit = buffer.getUnsignedByte(shift + 5);
		logList.unit = unit;
//		System.out.println("ScalingChunk unit : " + unit);
		short bytesPerSample = buffer.getUnsignedByte(shift + 7);
		logFmt.bytesPerSample = bytesPerSample;
//		System.out.println("ScalingChunk bytesPerSample : " + bytesPerSample);
		int maxCount = buffer.getInt(shift + 8);
		logList.maxCount = maxCount;
//		System.out.println("ScalingChunk maxCount : " + maxCount);
		int minCount = buffer.getInt(shift + 12);
		logList.minCount = minCount;
//		System.out.println("ScalingChunk minCount : " + minCount);
		int maxAmp = buffer.getInt(shift + 16);
		logList.maxAmp = maxAmp;
//		System.out.println("ScalingChunk maxAmp : " + maxAmp);
		int minAmp = buffer.getInt(shift + 20);
		logList.minAmp = minAmp;
//		System.out.println("ScalingChunk minAmp : " + minAmp);
		return shift + payloadLength + 4;
	}
	
	protected void startLogging(){
		//TODO
//		System.out.println("start logging");
		this.isLogging = true;
	}
	
	protected void stopLogging(){
		//TODO
//		System.out.println("stop logging");
		this.isLogging = false;
	}
	
	private void loggingSamples(Buffer samples){
		if (wavFileRecorder == null) return;
		wavFileRecorder.record(samples);
	}
	
	
}
