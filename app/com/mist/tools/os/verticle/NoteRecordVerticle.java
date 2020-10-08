package com.mist.tools.os.verticle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.vertx.core.AbstractVerticle;

public class NoteRecordVerticle extends AbstractVerticle{
	public static final String TAG_RECORD_NOTE = "record note";
	
	private Path recordPath;
	
	private ExecutorService service = Executors.newSingleThreadExecutor();
	
	public NoteRecordVerticle(String recordPathStr){
		this.recordPath = Paths.get(recordPathStr);
	}
	
	public NoteRecordVerticle(Path recordPath){
		this.recordPath = recordPath;
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		vertx.eventBus().consumer(TAG_RECORD_NOTE, msg -> handleMessage((String) msg.body()));
	}

	@Override
	public void stop() throws Exception {
		service.shutdown();
		super.stop();
	}
	
	private void handleMessage(String msg){
		Runnable job = () -> {
			try {
				Path notePath = setup(recordPath);
				initFile(recordPath, notePath);
				recordMessage(notePath, msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		};
		service.submit(job);
	}
	
	private Path setup(Path recordPath){
		String dateStr = LocalDate.now().toString();
		Path notePath = Paths.get(recordPath.toAbsolutePath().toString(), String.format("%s_note.txt", dateStr));
		return notePath;
	}
	
	private void initFile(Path recordPath, Path notePath) throws IOException{
		if(!Files.exists(recordPath, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(recordPath);
		if(!Files.exists(notePath, LinkOption.NOFOLLOW_LINKS)) Files.createFile(notePath);
	}
	
	private void recordMessage(Path notePath, String msg) throws IOException{
		BufferedWriter writer = Files.newBufferedWriter(notePath, StandardOpenOption.APPEND);
		writer.write(msg);
		writer.newLine();
		writer.flush();
		writer.close();
	}
}
