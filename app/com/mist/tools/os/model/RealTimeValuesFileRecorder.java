package com.mist.tools.os.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import static com.mist.tools.os.model.RealTimeValuesFactory.*;

public class RealTimeValuesFileRecorder{
	
	public static final String[] COLUMN_HEADER = {
			"Epoch Seconds", "Lpeak", "SEL1s", "Loeak30s", "SEL30s", "Leq30s", 
			"Background Level", "Epoch Counts 30s", "LE30s",
			"# SELn 30s",
			"SEL5", "SEL10", "SEL50", "SEL90", "SEL95",
			"# 1/3 Octave Bands",
			"6.3", "8", "10", "12.5", "16", "20", "25", "31.5", "40", "50",
			"63", "80", "100", "125", "160", "200", "250", "315", "400", "500",
			"630", "800", "1k", "1.25k", "1.6k", "2k", "2.5k", "3.15k", "4k", "5k",
			"6.3k", "8k", "10k", "12.5k", "16k", "20k",
			"# 1/3 Octave Bands 30s",
			"6.3", "8", "10", "12.5", "16", "20", "25", "31.5", "40", "50",
			"63", "80", "100", "125", "160", "200", "250", "315", "400", "500",
			"630", "800", "1k", "1.25k", "1.6k", "2k", "2.5k", "3.15k", "4k", "5k",
			"6.3k", "8k", "10k", "12.5k", "16k", "20k",
			"# SELn All Time",
			"SEL5", "SEL10", "SEL50", "SEL90", "SEL95",
			"Total Epoch Counts"
	};
	
	private Logger logger = LoggerFactory.getLogger(RealTimeValuesFileRecorder.class);
	
	private ExecutorService service = Executors.newSingleThreadExecutor();
	
	private Path recordPath;
	
	private Path filePath;
	
	public RealTimeValuesFileRecorder(Path recordPath, Buffer header){
		this.recordPath = recordPath;
		init(header);
	}
	
	private void init(Buffer header){
		Runnable job = () -> initRealTimeFile(header);
		service.submit(job);
	}
	
	private void initRealTimeFile(Buffer buffer){
		int nameLength = buffer.getInt(0);
		int shift = 4 + nameLength;
		String fileName = buffer.getString(4, shift);
		String zoneOffset = buffer.getString(shift, buffer.length());
		try {
			initFolder();
			initFile(fileName);
			BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
			writer.write(String.format("Zone Offset : %s", zoneOffset));
			writer.newLine();
			writer.write("Epoch seconds is related to local. For UTC epoch seconds, must remove seconds from offset.");
			writer.newLine();
			writer.write(Arrays.asList(COLUMN_HEADER).stream().collect(Collectors.joining(",")));
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Init real time file [{}] failed. Error : {}", filePath.toAbsolutePath().toString(), e.getMessage());
		}		
	}
	
	private void initFolder() throws IOException{
		if(!Files.exists(recordPath, LinkOption.NOFOLLOW_LINKS)){
			Files.createDirectories(recordPath);
		}
	}
	
	private void initFile(String fileName) throws IOException{
		filePath = Paths.get(recordPath.toAbsolutePath().toString(), fileName);
		Files.deleteIfExists(filePath);
		Files.createFile(filePath);
	}
	
	public void output(){
		service.shutdown();
	}
	
	public void record(JsonObject bean){
		Runnable job = () -> writeRealTimeValues(bean);
		service.submit(job);
	}
	
	private void writeRealTimeValues(JsonObject bean){
		try {
			BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
			writer.write(String.format("%d, %.01f, %.01f, %.01f, %.01f, %.01f, %.01f, %d, %.01f, ", 
					bean.getLong(EPOCH_SECONDS), bean.getDouble(LPEAK_1s), bean.getDouble(SEL_1s), 
					bean.getDouble(LPEAK_30s), bean.getDouble(SEL_30s), bean.getDouble(LEQ_30s),
					bean.getDouble(BACKGROUND_LEVEL) < 0 ? 0:bean.getDouble(BACKGROUND_LEVEL), 
					bean.getInteger(EPOCH_COUNTS_30s), bean.getDouble(LE_30s)));
			String SELn = bean.getJsonArray(SELn_30s).stream().map(v -> String.format("%.01f", v)).collect(Collectors.joining(", "));
			writer.write(String.format("#, %s, ", SELn));
			String octaveBands = bean.getJsonArray(OCTAVE_BANDS_1s).stream().map(v -> String.format("%.01f", v)).collect(Collectors.joining(", "));
			writer.write(String.format("#, %s, ", octaveBands));
			String octaveBands30s = bean.getJsonArray(OCTAVE_BANDS_30s).stream().map(v -> String.format("%.01f", v)).collect(Collectors.joining(", "));
			writer.write(String.format("#, %s,", octaveBands30s));
			String SELnH = bean.getJsonArray(SELn_ALL_TIME).stream().map(v -> String.format("%.01f", v)).collect(Collectors.joining(", "));
			writer.write(String.format("#, %s, %d", SELnH, bean.getLong(TOTAL_EPOCH_COUNTS)));
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("write real time file [{}] failed. Error : {}", filePath.toAbsolutePath().toString(), e.getMessage());
		}
	}
}
