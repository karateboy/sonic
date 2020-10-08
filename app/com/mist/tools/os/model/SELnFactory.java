package com.mist.tools.os.model;

import com.mist.tools.os.utils.LnUtils;

import io.vertx.core.json.JsonArray;

public class SELnFactory {
	private double sumSELAllTime = 0;
	private double stdSquareSumSELAllTime = 0;
	private long countsSELAllTime = 0;
	
	public JsonArray update(double sel1s){
		this.countsSELAllTime += 1;
		this.sumSELAllTime += sel1s;
		double mean = sumSELAllTime / countsSELAllTime;
		this.stdSquareSumSELAllTime += Math.pow(sel1s - mean, 2);
		double stdSquare = 	this.stdSquareSumSELAllTime / this.countsSELAllTime;
		return LnUtils.getLnJsonArray(mean, stdSquare);
	}
	
	public void resetSession(){
		this.sumSELAllTime = 0;
		this.stdSquareSumSELAllTime = 0;
		this.countsSELAllTime = 0;
	}
}
