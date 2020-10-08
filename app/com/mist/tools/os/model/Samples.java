package com.mist.tools.os.model;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Samples {
	private long epochSeconds = 0;
	
	private int sampleRate = 0;
	
	private int bytesPerSample = 0;
	
	private double sensitivity = 0;
	
	private double[] values = null;

	public Samples(){
		
	}
	
	public Samples(long epochSeconds, int sampleRate, int bytesPerSample, double sensitivity){
		init(epochSeconds, sampleRate, bytesPerSample, sensitivity);
	}
	
	public void init(long epochSeconds, int sampleRate, int bytesPerSample, double sensitivity){
		this.epochSeconds = epochSeconds;
		this.sampleRate = sampleRate;
		this.bytesPerSample = bytesPerSample;
		this.sensitivity = sensitivity;
		values = new double[sampleRate];
		IntStream.range(0, sampleRate).forEach(i-> values[i] = 0);
	}
	
	public void setSample(int index, double value){
		this.values[index] = value;
	}
	
	public double[] values(){
		return values;
	}
	
	public double[] valuesDeepCopy(){
		double[] copy = new double[values.length];
		IntStream.range(0, values.length).forEach(i -> copy[i] = values[i]);
		return copy;
	}
	
	public int sampleRate(){
		return sampleRate;
	}
	
	public int bytesPerSample(){
		return bytesPerSample;
	}
	
	public double sensitivity(){
		return sensitivity;
	}
	
	public long epochSeconds(){
		return epochSeconds;
	}
	
	public void epochSeconds(long seconds){
		epochSeconds = seconds;
	}
	
	public void epochSecondsForward(long seconds){
		epochSeconds += seconds;
	}
	
	public String encodeJsonString(){
		return Arrays.asList(values).stream()
				.map(v -> String.format("%.011f", v))
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
