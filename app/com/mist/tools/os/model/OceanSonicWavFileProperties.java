package com.mist.tools.os.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class OceanSonicWavFileProperties {
	private LocalDateTime dateTime;
	private ZoneOffset offset;
	private long contentBytes = 0;
	private double peakVoltage = 0;	
	private double sensitivity = 0;
	private double unit = 0;
	private int channels = 0;
	private long sampleRate = 0;
	private long bytesPerSecond = 0;
	private int bytesPerSample = 0;
	private int bitsPerDataPoint = 0;
	private long totalDataBytes = 0;
	private int wavLength = 0;
	
	protected void dateTime(LocalDateTime dateTime){
		this.dateTime = dateTime;
	}
	public LocalDateTime dateTime(){
		return dateTime;
	}
	protected void zoneOffset(ZoneOffset offset){
		this.offset = offset;
	}
	public ZoneOffset zoneOffset(){
		return offset;
	}
	public long contentBytes() {
		return contentBytes;
	}
	protected void contentBytes(long contentBytes) {
		this.contentBytes = contentBytes;
	}
	public double peakVoltage() {
		return peakVoltage;
	}
	protected void peakVoltage(double peakVoltage) {
		this.peakVoltage = peakVoltage;
	}
	public double sensitivity() {
		return sensitivity;
	}
	protected void sensitivity(double sensitivity) {
		this.sensitivity = sensitivity;
	}
	public double unit() {
		return unit;
	}
	protected void unit(double unit) {
		this.unit = unit;
	}
	public int channels() {
		return channels;
	}
	protected void channels(int channels) {
		this.channels = channels;
	}
	public long sampleRate() {
		return sampleRate;
	}
	protected void sampleRate(long sampleRate) {
		this.sampleRate = sampleRate;
	}
	public long bytesPerSecond() {
		return bytesPerSecond;
	}
	protected void bytesPerSecond(long bytesPerSecond) {
		this.bytesPerSecond = bytesPerSecond;
	}
	public int bytesPerSample() {
		return bytesPerSample;
	}
	protected void bytesPerSample(int bytesPerSample) {
		this.bytesPerSample = bytesPerSample;
	}
	public int bitsPerDataPoint() {
		return bitsPerDataPoint;
	}
	protected void bitsPerDataPoint(int bitsPerDataPoint) {
		this.bitsPerDataPoint = bitsPerDataPoint;
	}
	public long totalDataBytes() {
		return totalDataBytes;
	}
	protected void totalDataBytes(long totalDataBytes) {
		this.totalDataBytes = totalDataBytes;
	}
	public int wavLength() {
		return wavLength;
	}
	protected void wavLength(int wavLength) {
		this.wavLength = wavLength;
	}
	@Override
	public String toString() {
		return "IcListenWavProps [contentBytes=" + contentBytes + ", peakVoltage=" + peakVoltage + ", sensitivity="
				+ sensitivity + ", unit=" + unit + ", channels=" + channels + ", sampleRate=" + sampleRate
				+ ", bytesPerSecond=" + bytesPerSecond + ", bytesPerSample=" + bytesPerSample + ", bitsPerDataPoint="
				+ bitsPerDataPoint + ", totalDataBytes=" + totalDataBytes + ", wavLength=" + wavLength + "]";
	}
}
