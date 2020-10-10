package com.mist.tools.os.model;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jtransforms.fft.DoubleFFT_1D;

import com.mist.tools.os.utils.LnUtils;
import com.mist.tools.os.utils.OctaveBandsUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RealTimeValuesFactory {
	public static final String LPEAK_1s = "Lpeak";
	public static final String SEL_1s = "SEL1s";
	public static final String LPEAK_30s = "Lpeak30s";
	public static final String SEL_30s = "SEL30s";
	public static final String LEQ_30s = "Leq30s";
	public static final String OCTAVE_BANDS_1s = "octaveBands";
	public static final String OCTAVE_BANDS_30s = "octaveBands30s";
	public static final String LE_30s = "LE30s";
	public static final String SELn_30s = "SELn";
	public static final String SELn_ALL_TIME = "SELnH";
	public static final String EPOCH_SECONDS = "epochSeconds";
	public static final String EPOCH_COUNTS_30s = "epochCounts30s";
	public static final String TOTAL_EPOCH_COUNTS = "totalEpochCounts";
	public static final String BACKGROUND_LEVEL = "backgroundLevel";
	
	private SELnFactory SELnFactory = new SELnFactory();
	private EpochCountsFactory epochCountsFactory = new EpochCountsFactory();
	
	private double[] spectrum = null;
	
	private Queue<Double> sel1sQueue = new ArrayDeque<>(30);
	
	private Queue<Double> lpeakQueue = new ArrayDeque<>(30);
	
	private Queue<double[]> octaveBandsQueue = new ArrayDeque<>(30);
	
//	private Queue<JsonObject> realTimeValuesQueue = new ArrayDeque<>(60);
	
	private JsonObject realTimeValues = new JsonObject();
	
	public String getJsonSpectrum(){
		if(this.spectrum == null) return "[]";
		return IntStream.range(0, this.spectrum.length)
				.mapToObj(i -> String.format("%.011f", this.spectrum[i]))
				.collect(Collectors.joining(", ", "[", "]"));
	}
	
	public Queue<Double> getSEL1sQueue() {
		return sel1sQueue;
	}
	
	public JsonObject update(Samples samples){
		JsonObject bean = new JsonObject();
		
		updateEpochSeconds(samples, bean);
		updateLPeak(samples, bean);
		
		generateSpectrum(samples);
		updateOneThirdOctaveBands(samples, bean);	
		
		updateSELRelatedValues(samples, bean);	
		this.realTimeValues = bean;
		return bean;
	}
	
	private void updateEpochSeconds(Samples samples, JsonObject bean){
		bean.put(EPOCH_SECONDS, samples.epochSeconds());
	}
	
	private void updateLPeak(Samples samples, JsonObject bean){
		double sensitivity = samples.sensitivity();
		double[] values = samples.values();
		double lpeak = IntStream.range(0, values.length)
				.mapToDouble(i -> Math.abs(values[i]))
				.map(v -> v <= 0 ? 0 : 20 * Math.log10(v) - sensitivity)
				.max().orElse(0);
		
		updateLpeakQueue(lpeak);
		bean.put(LPEAK_1s, lpeak);
		
		double lpeak30s = getLpeak30s();
		bean.put(LPEAK_30s, lpeak30s);
	}
	
	private void updateLpeakQueue(double lpeak) {
		if(lpeakQueue.size() == 30) {
			lpeakQueue.remove();
		}
		lpeakQueue.add(lpeak);
	}
	
	private double getLpeak30s() {
		return lpeakQueue.stream().max((a, b) -> Double.compare(a, b)).orElse(0d);
	}
	
	private void generateSpectrum(Samples samples){
		double[] values = samples.valuesDeepCopy();

		int sampleLength = values.length;
		int fftResultLength = sampleLength / 2 + 1;
		int lastIndex = fftResultLength - 1;
		this.spectrum = new double[fftResultLength];
		DoubleFFT_1D fft = new DoubleFFT_1D(sampleLength);
		fft.realForward(values);
		
		spectrum[0] = Math.sqrt(Math.pow(values[0], 2));
		for(int i=1;i<lastIndex;i++){
			spectrum[i] = Math.sqrt(2 * (Math.pow(values[2*i], 2) + Math.pow(values[2*i+1], 2)));
		}
		if(sampleLength % 2 == 0){
			spectrum[lastIndex] = Math.sqrt(Math.pow(values[1], 2));
		}else{
			spectrum[lastIndex] = Math.sqrt(Math.pow(values[2*lastIndex], 2) + Math.pow(values[1], 2));
		}
	}
	
	private void updateOneThirdOctaveBands(Samples samples, JsonObject bean){
		int sampleRate = samples.sampleRate();
		double sensitivity = samples.sensitivity();
		double[] bounds = OctaveBandsUtils.ONE_THIRD_OCTAVE_BANDS_BOUND;
		double[] octaveBands = getOctaveBandsBean();
		
		JsonArray ary = new JsonArray();
		for(int i=0;i<36;i++){
			octaveBands[i] = calculateOctaveBand(bounds[i], bounds[i+1], sampleRate, sensitivity);
			ary.add(octaveBands[i]);
		}
		bean.put(OCTAVE_BANDS_1s, ary);
		
		octaveBandsQueue.add(octaveBands);
		
		updateOneThirdOctaveBands30s(bean);
	}
	
	private double[] getOctaveBandsBean(){
		if(octaveBandsQueue.size() < 30){
			return new double[36];
		}else{
			return octaveBandsQueue.poll();
		}
	}
	
	private double calculateOctaveBand(double lowerBound, double upperBound, double sampleRate, double sensitivity){
		double[] spectrum = this.spectrum;
		int n = spectrum.length;
		double freqStep = 1;

		double res = IntStream.range(0, n)
				.filter(idx -> idx*freqStep >= lowerBound)
				.filter(idx -> idx*freqStep < upperBound)
				.mapToDouble(idx -> spectrum[idx]*spectrum[idx])
				.sum();
		
		res = res <= 0 ? 0 : res / Math.pow(sampleRate, 2);
		return res <= 0 ? 0 : 10 * Math.log10(res) - sensitivity;
	}
	
	private void updateOneThirdOctaveBands30s(JsonObject bean){
		JsonArray ary = new JsonArray();
		IntStream.range(0, 36).forEach(i -> {
			double band = octaveBandsQueue.stream().mapToDouble(bands -> bands[i]).map(v -> v <= 0 ? 0 : Math.pow(10, v/10)).sum();			
			band = band <= 0 ? 0 : 10 * Math.log10(band);
			ary.add(band);
		});
		bean.put(OCTAVE_BANDS_30s, ary);
	}
	
	private void updateSELRelatedValues(Samples samples, JsonObject bean){
		double sel1s = findSEL1sForFreqRange(20, 20000, samples);
		
		updateSEL1s(sel1s, bean);
		updateSELnAllTime(sel1s, bean);
		
		updateSELn30s(bean);
		updateSEL30s(bean);
		
		updateEpoch(sel1s, bean);
	}
	
	private double findSEL1sForFreqRange(int lowerFreq, int upperFreq, Samples samples){
		int sampleRate = samples.sampleRate();
		double sensitivity = samples.sensitivity();
		double[] spectrum = this.spectrum;
		int n = spectrum.length;
		n = n > upperFreq ? upperFreq : n;
		double sumPower = IntStream.range(lowerFreq, n)
				.mapToDouble(idx -> spectrum[idx]*spectrum[idx])
				.sum();
		
		sumPower = sumPower <= 0 ? 0 : sumPower / Math.pow(sampleRate, 2);
		return sumPower <= 0 ? 0 : 10 * Math.log10(sumPower) - sensitivity;
	}

	private void updateSEL1s(double sel1s, JsonObject bean){
		bean.put(SEL_1s, sel1s);
		updateSEL1sQueue(sel1s);
	}
	
	private void updateSEL1sQueue(double sel1s){
		if(this.sel1sQueue.size() == 30){
			this.sel1sQueue.remove();
		}
		this.sel1sQueue.add(sel1s);
	}
	
	private void updateSELnAllTime(double sel1s, JsonObject bean){
		JsonArray SELn = SELnFactory.update(sel1s);
		bean.put(SELn_ALL_TIME, SELn);
	}
	
	private void updateSELn30s(JsonObject bean){
		double[] ln = LnUtils.getLn(sel1sQueue);
		JsonArray ary = new JsonArray();
		IntStream.range(0, 5).forEach(i -> ary.add(ln[i]));
		bean.put(SELn_30s, ary);
	}
	
	private void updateSEL30s(JsonObject bean){
		double energy = sel1sQueue.stream().mapToDouble(v -> Math.pow(10, v/10)).sum();
		
		double SEL30s = (energy <= 0) ? 0 : 10 * Math.log10(energy);
		bean.put(SEL_30s, SEL30s);
		
		double leq30s = (energy <= 0) ? 0 : 10 * Math.log10(energy/sel1sQueue.size());
		bean.put(LEQ_30s, leq30s);
	}
	
	private void updateEpoch(double sel1s, JsonObject bean){
		updateEpochCounts(sel1s, bean);
		updateLE30s(bean);
	}
	
	private void updateEpochCounts(double sel1s, JsonObject bean){
		bean.put(EPOCH_COUNTS_30s, epochCountsFactory.update(sel1s));
		bean.put(TOTAL_EPOCH_COUNTS, epochCountsFactory.getTotalEpochCounts());
		bean.put(BACKGROUND_LEVEL, epochCountsFactory.getBackgroundLevel());
	}
	
	private void updateLE30s(JsonObject bean){
		double counts = bean.getInteger(EPOCH_COUNTS_30s);
		double SEL30s = bean.getDouble(SEL_30s);
		double LE30s = (counts <= 0 || SEL30s <= 0) ? 0 : 10 * Math.log10(Math.pow(10,  SEL30s/10)/counts);
		bean.put(LE_30s, LE30s);
	}
	
	private double[] hanningWindow(double[] recordedData) {

	    // iterate until the last line of the data buffer
	    for (int n = 1; n < recordedData.length; n++) {
	        // reduce unnecessarily performed frequency part of each and every frequency
	        recordedData[n] *= 0.5 * (1 - Math.cos((2 * Math.PI * n)
	                / (recordedData.length - 1)));
	    }
	    // return modified buffer to the FFT function
	    return recordedData;
	}

	public JsonObject getRealTimeValues() {
		return realTimeValues;
	}
	
	public void setBackgroundLevel(double backgroundLevel){
		epochCountsFactory.setBackgroundLevel(backgroundLevel);
	}
	
	public void resetSession(){
		resetQueue();
		resetStatisticsSession();
		resetEpochSession();
	}
	
	private void resetQueue() {
		sel1sQueue.clear();
		lpeakQueue.clear();
	}
	
	private void resetStatisticsSession(){
		SELnFactory.resetSession();
	}
	
	private void resetEpochSession(){
		epochCountsFactory.resetSession();
	}
}
