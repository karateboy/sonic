package com.mist.tools.os.verticle;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;

import org.slf4j.LoggerFactory;

import com.mist.tools.os.model.Samples;
import com.mist.tools.os.model.RealTimeValuesFactory;
import com.mist.tools.os.model.RealTimeValuesFileRecorder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class MistTCPWaveStreamingVerticle extends OceanSonicTCPWaveStreamingVerticle{
	public static final String TAG_RESET_SESSION = "reset session";
	public static final String TAG_SET_BACKGROUND_LEVEL = "set backgroundLevel";
	
	private Samples scaledSamples = new Samples();
	
	private RealTimeValuesFactory realTimeValuesFactory;
	
	private RealTimeValuesFileRecorder realTimeValuesFile = null;
	
	private boolean isResetSession = false;
	
	private double backgroundLevel = -1;
	
	public MistTCPWaveStreamingVerticle(RealTimeValuesFactory factory, String url, int port, String recordPathStr,
			ZoneOffset zoneOffset, int logLength, double modify){
		super(url, port, recordPathStr, zoneOffset, logLength, modify);
		this.realTimeValuesFactory = factory;
		this.logger = LoggerFactory.getLogger(MistTCPWaveStreamingVerticle.class);
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		vertx.eventBus().consumer(TAG_RESET_SESSION, msg -> setResetSession());
		vertx.eventBus().consumer(TAG_SET_BACKGROUND_LEVEL, msg -> setBackgroundLevel((String)msg.body()));
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
	}
	
	private void setResetSession(){
		this.isResetSession = true;
	}
	
	private void setBackgroundLevel(String backgroundLevel){
		this.backgroundLevel = backgroundLevel.matches("^\\d+(\\.\\d+)?$") ? Double.valueOf(backgroundLevel) : -1;
	}
	
	@Override
	protected void parseSetupHook(){
		long epochSeconds = logList.getDataDateTime().toEpochSecond(logList.offset);
		scaledSamples.init(epochSeconds, logFmt.sampleRate, logFmt.bytesPerSample, logList.sensitivity);
	}
	
	@Override
	protected void parseSamplesHook(int sampleIndex, int numberOfSamples, 
			IntUnaryOperator endianFunc, DoubleUnaryOperator unitFunc){
		double scaling = (double)(logList.maxAmp - logList.minAmp)/(logList.maxCount - logList.minCount);
		
		for(int i=0;i<numberOfSamples;i++){
			int count = endianFunc.applyAsInt(i);
			double sample = count * scaling;
			sample = unitFunc.applyAsDouble(sample);
			scaledSamples.setSample(sampleIndex+i, sample);
		}
		
		int updateCount = sampleIndex + numberOfSamples;
		updateRealTimeValues(updateCount);
	}
	
	private void updateRealTimeValues(int updateCount){
		if(updateCount == scaledSamples.sampleRate()){
			if(isResetSession){
				resetSession();
			}
			realTimeValuesFactory.setBackgroundLevel(this.backgroundLevel);
			JsonObject realTimeValues = realTimeValuesFactory.update(this.scaledSamples);
			if(this.logging){
				this.realTimeValuesFile.record(realTimeValues);
			}
		}
	}
	
	private void resetSession(){
		realTimeValuesFactory.resetSession();
		isResetSession = false;
	}
	
	@Override
	protected void initLoggingHook(){
		initRealTimeFile();
	}
	
	private void initRealTimeFile(){
		String fileName = String.format("%s.csv", logList.outputFileName);
		Buffer buffer = Buffer.buffer();
		byte[] bytes = fileName.getBytes(StandardCharsets.UTF_8);
		buffer.appendInt(bytes.length);
		buffer.appendBytes(bytes);
		buffer.appendString(zoneOffset.getId());
		this.realTimeValuesFile = new RealTimeValuesFileRecorder(recordPath, buffer);
	}
	
	@Override
	protected void outputLoggingHook(){
		this.realTimeValuesFile.output();
	}
	
	public boolean isLogging(){
		return isLogging;
	}
	
	public boolean isStreaming(){
		return isStreaming;
	}
	
	public boolean isConnected(){
		return isConnected;
	}
}
