package com.mist.tools.os.model;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OceanSonicWavPlaybackWorker extends WavPlaybackWorker{
	@Override
	protected JsonArray play(Path filePath) throws IOException{
		RandomAccessWavFile wav = new OceanSonicWavFile(filePath);
		return getRealTimeValuesFromWav(wav);
	}
	
	private JsonArray getRealTimeValuesFromWav(RandomAccessWavFile wav) throws IOException{
		double sensitivity = wav.sensitivity();
		double peakVoltage = wav.peakVoltage();
		int bytesPerSample = wav.bytesPerSample();
		
		int sampleRate = (int) wav.sampleRate();
		int wavLength = wav.wavLength();
		LocalDateTime dateTime = wav.dateTime();
		long epochSeconds = dateTime.toEpochSecond(ZoneOffset.ofHours(0));
		long samplesPosition = wav.datasPosition();
		
		Function<Buffer, Double> scalingFunc = generateScalingSampleFunction(peakVoltage, bytesPerSample);
		Samples samples = new Samples(epochSeconds, sampleRate, bytesPerSample, sensitivity);
				
		RandomAccessFile file = new RandomAccessFile(wav.filePath().toAbsolutePath().toString(), "r");
		file.seek(samplesPosition);
		JsonArray realTimeArray = getAllRealTimeValues(file, wavLength, scalingFunc, samples);
		
		file.close();
		
		return realTimeArray;
	}
	
	private Function<Buffer, Double> generateScalingSampleFunction(double peakVoltage, int bytesPerSample){
		Function<Buffer, Integer> readCountFunc = generateReadCountFunction(bytesPerSample);
		Function<Integer, Double> scalingFunc = generateScalingCountFunction(peakVoltage, bytesPerSample);
		return readCountFunc.andThen(scalingFunc);
	}
	
	private Function<Buffer, Integer> generateReadCountFunction(int bytesPerSample){
		switch(bytesPerSample){
		case 1:
			return b -> (int) b.getByte(0) ;
		case 2:
			return b -> (int) b.getShortLE(0);
		case 3:
			return b -> b.getMediumLE(0);
		case 4:
			return b -> b.getIntLE(0);
		default :
			return null;
		}
	}
	
	private Function<Integer, Double> generateScalingCountFunction(double peakVoltage, int bytesPerSample){
		int bitsPerSample = bytesPerSample * 8;
		int scalePowerLevel = bitsPerSample - 1;
		return count -> (double) count * peakVoltage / (Math.pow(2, scalePowerLevel) - 1);
	}
	
	private JsonArray getAllRealTimeValues(RandomAccessFile file, int wavLength, Function<Buffer, Double> scalingFunc, Samples samples) throws IOException{
		JsonArray realTimeArray = new JsonArray();
		for(int i=0;i<wavLength;i++){
			parseOneSecondSamples(file, scalingFunc, samples);
			JsonObject realTimeValues = factory.update(samples);
			realTimeArray.add(realTimeValues);
			samples.epochSecondsForward(1);
		}
		return realTimeArray;
	}
	
	private void parseOneSecondSamples(RandomAccessFile file, Function<Buffer, Double> scalingFunc, Samples samples) throws IOException{
		byte[] tmp = new byte[samples.bytesPerSample()];
		int sampleRate = samples.sampleRate();
		for(int i=0;i<sampleRate;i++){
			file.read(tmp);
			Buffer buffer = Buffer.buffer(tmp);
			double sample = scalingFunc.apply(buffer);
			samples.setSample(i, sample);
		}
	}
}
