package com.mist.tools.os.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class OceanSonicWaveList {
	private LocalDateTime dataDateTime = LocalDateTime.now();
	
	public long logStartEpochSecond = 0;
	
	//ICRD
	public long epochSecond = 0;
	public ZoneOffset offset = ZoneOffset.UTC;
	
	//INAM
	public String outputFileName;
	
	//IART
	public String deviceType;
	public int serial;
	
	//ICMT
	public double sensitivity = 0;
	public short unit = 1;
	public int maxCount;
	public int minCount;
	public int maxAmp;
	public int minAmp;
	public double modify = 0;
	
	public void setDataDateTime(LocalDateTime dateTime){
		this.dataDateTime = dateTime;
	}
	
	public LocalDateTime getDataDateTime(){
		return dataDateTime;
	}
}
