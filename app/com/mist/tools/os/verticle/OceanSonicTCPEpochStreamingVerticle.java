package com.mist.tools.os.verticle;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.slf4j.LoggerFactory;

import com.mist.tools.os.model.RealTimeValuesFactory;

import io.vertx.core.buffer.Buffer;

public class OceanSonicTCPEpochStreamingVerticle extends TCPClientVerticle {
	
	private RealTimeValuesFactory realTimeValuesFactory = null;
	
	private int[] epochQueue = new int[30];
	
	public OceanSonicTCPEpochStreamingVerticle(String url, int port, RealTimeValuesFactory factory){
		this.url = url;
		this.port = port;
		this.realTimeValuesFactory = factory;
		this.logger = LoggerFactory.getLogger(OceanSonicTCPEpochStreamingVerticle.class);
		IntStream.range(0, 30).forEach(i -> epochQueue[i] = 0);
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		vertx.setPeriodic(1000, id -> updateQueue());
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
	}
	
	@Override
	protected void handleMessage(Buffer buffer){
		String msg = buffer.toString(StandardCharsets.US_ASCII);
		logger.debug("Epoch message : {}", msg);
		String[] clips = msg.split(",");
		if(clips.length > 4){
			synchronized(this.epochQueue){
				this.epochQueue[29] +=1;
			}
		}
	}
	
	private void updateQueue(){
		IntStream.range(0, 29).forEach(i -> epochQueue[i] = epochQueue[i+1]);
		this.epochQueue[29] = 0;
		int sumCounts = IntStream.range(0, 29).map(i -> epochQueue[i]).sum();
		logger.debug("epoch counts : {}", sumCounts);
//		realTimeValuesFactory.update30sEpochCounts(sumCounts);
	}
}
