package com.mist.tools.os.utils;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import io.vertx.core.json.JsonArray;

public class LnUtils {
	public static final double[] STD_SCORES = {1.65, 1.28, 0, -1.28, -1.65, -2.33};
	
	public static double[] getLn(Collection<Double> data){
		double[] ln = new double[5];
		Supplier<DoubleStream> sup = () -> data.stream().mapToDouble(v -> v.doubleValue());
		double mean = sup.get().average().orElse(0);
		double std = sup.get().map(v -> v - mean).map(v -> v*v).average().orElse(0);
		std = std <= 0 ? 0 : Math.sqrt(std);
		for(int i=0;i<5;i++){
			ln[i] = mean + std * STD_SCORES[i];
		}
		return ln;
	}
	
	public static double[] getLn(double mean, double stdSquare){
		double[] ln = new double[5];
		double std = stdSquare <= 0 ? 0: Math.sqrt(stdSquare);
		for(int i=0;i<5;i++){
			ln[i] = mean + std * STD_SCORES[i];
		}
		return ln;
	}
	
	public static JsonArray getLnJsonArray(Collection<Double> data){
		JsonArray ln = new JsonArray();
		Supplier<DoubleStream> sup = () -> data.stream().mapToDouble(v -> v.doubleValue());
		double mean = sup.get().average().orElse(0);
		double std = sup.get().map(v -> v - mean).map(v -> v*v).average().orElse(0);
		std = std <= 0 ? 0 : Math.sqrt(std);
		for(int i=0;i<5;i++){
			ln.add(mean + std * STD_SCORES[i]);
		}
		return ln;
	}
	
	public static JsonArray getLnJsonArray(double mean, double stdSquare){
		double std = stdSquare <= 0 ? 0: Math.sqrt(stdSquare);
		JsonArray ln = new JsonArray();
		for(int i=0;i<5;i++){
			ln.add(mean + std * STD_SCORES[i]);
		}
		return ln;
	}
}
