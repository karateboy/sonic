package com.mist.tools.os.model;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.vertx.core.json.JsonArray;

public abstract class WavPlaybackWorker {
	protected RealTimeValuesFactory factory = new RealTimeValuesFactory();

	protected ExecutorService service = Executors.newSingleThreadExecutor();
	
	public Future<JsonArray> playback(String filePathStr){
		Path filePath = Paths.get(filePathStr);
		return playback(filePath);
	}
	
	public Future<JsonArray> playback(Path filePath){
		Callable<JsonArray> job = () -> play(filePath);
		return service.submit(job);
	}

	protected abstract JsonArray play(Path filePath) throws IOException;
}
