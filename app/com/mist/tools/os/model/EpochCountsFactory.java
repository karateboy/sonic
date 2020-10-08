package com.mist.tools.os.model;

import java.util.stream.IntStream;

public class EpochCountsFactory {
	private double backgroundLevel = -1;
	private double triggerLevel = -1;
	private double endTriggerLevel = -1;
	
	private int totalEpochCounts = 0;
	
	private int inTriggerCounts = 0;
	
	private int[] epochQueue = new int[30];
	
	private boolean isTriggered = false;
	
	public EpochCountsFactory(){
		init();
	}
	
	private void init(){
		resetSession();
	}
	
	public void setBackgroundLevel(double backgroundLevel){
		this.backgroundLevel = backgroundLevel;
		this.triggerLevel = backgroundLevel + 10;
		this.endTriggerLevel = backgroundLevel + 7;
	}
	
	public double getBackgroundLevel(){
		return backgroundLevel;
	}
	
	public void resetSession(){
		resetFlag();
		resetStatistics();
		resetQueue();
	}
	
	private void resetFlag(){
		isTriggered = false;
		inTriggerCounts = 0;
	}
	
	private void resetStatistics(){		
		totalEpochCounts = 0;
	}
	
	private void resetQueue(){
		IntStream.range(0, 30).forEach(i -> epochQueue[i] = 0);
	}
	
	public int update(double sel1s){
		if(backgroundLevel < 0) return 0;
		
		pushQueue();
		updateTrigger(sel1s);
		
		return getEpochCounts30s();
	}
	
	private void pushQueue(){
		IntStream.range(0, 29).forEachOrdered(i -> epochQueue[i] = epochQueue[i+1]);
		epochQueue[29] = 0;
	}
	
	private void updateTrigger(double sel1s){
		if(isTriggered){
			if(isContinueTrigger(sel1s)){
				continueTriggered();
			}else{
				endTriggered();
			}
		}else{
			if(isTrigger(sel1s)){
				startTriggered();
			}
		}
	}
	
	private boolean isTrigger(double sel1s){
		return sel1s >= triggerLevel;
	}
	
	private void startTriggered(){
		isTriggered = true;
		epochQueue[29] = 1;
		totalEpochCounts += 1;
	}
	
	private boolean isContinueTrigger(double sel1s){
		return sel1s > endTriggerLevel;
	}
	
	private void continueTriggered(){
		inTriggerCounts += 1;
		inTriggerCounts %= 30;
		if(inTriggerCounts == 0){
			epochQueue[29] = 1;
		}
	}
	
	private void endTriggered(){
		inTriggerCounts = 0;
		isTriggered = false;
	}
	
	private int getEpochCounts30s(){
		return IntStream.range(0, 30).map(i -> epochQueue[i]).sum();
	}
	
	public int getTotalEpochCounts(){
		return totalEpochCounts;
	}
}
