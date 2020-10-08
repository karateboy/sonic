package com.mist.tools.os.model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mist.tools.os.utils.OceanSonicWaveFileUtils;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;

public class OceanSonicWavFileRecorder {
	private static final OpenOptions OUTPUT_OPEN_OPTIONS = new OpenOptions().setCreate(false).setTruncateExisting(false).setWrite(true);
	
	private Logger logger = LoggerFactory.getLogger(OceanSonicWavFileRecorder.class);
	
	private Buffer pseudoHeader = null;
	
	private Path folderPath = null;
	
	private String fileName = null;
	
	private Path path = null;
	
	private long dataSize = 0;
	
	private int dataSizePosition = 0;
	
	private ExecutorService service = Executors.newSingleThreadExecutor();
	
	private Vertx vertx;
	
	public OceanSonicWavFileRecorder(Vertx vertx, Path folderPath,
			OceanSonicWaveList list, OceanSonicWaveFmt fmt, ZoneOffset zoneOffset){
		this.vertx = vertx;
		this.folderPath = folderPath;
		init(list, fmt, zoneOffset);
	}

	private void init(OceanSonicWaveList list, OceanSonicWaveFmt fmt, ZoneOffset zoneOffset){
		initPseudoWaveHeader(list, fmt, zoneOffset);
		initFilePath(list);
		initFile();
	}
	
	private void initPseudoWaveHeader(OceanSonicWaveList list, OceanSonicWaveFmt fmt, ZoneOffset zoneOffset){
		OceanSonicWaveFileUtils wavFileUtils = OceanSonicWaveFileUtils.getUtils();

		Buffer listChunk = wavFileUtils.wrapToListChunk(list, zoneOffset);
		Buffer fmtChunk = wavFileUtils.wrapToFmtChunk(fmt);
		
		Buffer midContent = Buffer.buffer();
		midContent
			.appendBuffer(wavFileUtils.wrapStringToBuffer("WAVE", 4))
			.appendBuffer(listChunk)
			.appendBuffer(fmtChunk)
			.appendBuffer(wavFileUtils.wrapStringToBuffer("data", 4))
			.appendUnsignedIntLE(0);
		
		this.pseudoHeader = Buffer.buffer();
		this.pseudoHeader
			.appendBuffer(wavFileUtils.wrapStringToBuffer("RIFF", 4))
			.appendUnsignedIntLE(0)
			.appendBuffer(midContent);
	}
	
	private void initFilePath(OceanSonicWaveList list){
		this.fileName = String.format("%s.wav", list.outputFileName);
		this.path = Paths.get(folderPath.toAbsolutePath().toString(), this.fileName);
	}
	
	private void initFile(){
		Runnable job = () -> writeHeader();
		service.submit(job);
	}
	
	private void writeHeader(){
		try{
			if(!Files.exists(folderPath, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(folderPath);
			if(Files.exists(path, LinkOption.NOFOLLOW_LINKS)) Files.delete(path);
			Files.createFile(path);
			BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.APPEND));
			this.dataSizePosition = this.pseudoHeader.length() - 4;
			bos.write(this.pseudoHeader.getBytes());
			bos.flush();
			bos.close();
		}catch(IOException e){
			e.printStackTrace();
			logger.error("Error when create file [{}]. Error : {}", path, e.getMessage());
		}
	}
	
	public Path getFolderPath() {
		return folderPath;
	}

	public String getFileName() {
		return fileName;
	}

	public Path getPath() {
		return path;
	}
	
	public void record(Buffer buffer){
		Runnable job = () -> writeBuffer(buffer);
		service.submit(job);
	}
	
	private void writeBuffer(Buffer buffer){
		try {
			BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.APPEND));
			bos.write(buffer.getBytes());
			bos.flush();
			bos.close();
			dataSize += buffer.length();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Write data to file [{}] failed. Error : {}", path, e.getMessage());
		}
	}
	
	public void output(){
		Runnable job = () -> writeDataSize();
		service.submit(job);
		service.shutdown();
	}
	
	private void writeDataSize(){
		long totalSize = dataSize + dataSizePosition - 4;
		logger.debug("data size : {}, data size position: {}, total size : {}, actual file length : {}", dataSize, dataSizePosition, totalSize, path.toFile().length());	
		
		vertx.fileSystem().open(path.toAbsolutePath().toString(), OUTPUT_OPEN_OPTIONS, result -> {
			if(result.succeeded()){
				AsyncFile out = result.result();
				out.write(Buffer.buffer(4).appendUnsignedIntLE(totalSize), 4, r -> {
					logger.debug("write total size to file [{}] successed.", path);
				});
				out.write(Buffer.buffer(4).appendUnsignedIntLE(dataSize), dataSizePosition, r -> {
					logger.debug("write data size to file [{}] successed.", path);
				});
				out.flush();
				out.close(r -> {
					if(r.succeeded()){
						logger.info("File [{}] output successed.", path);
					}
				});
			}else{
				logger.info("Can not write info to file [{}], file still output, but may have some problem when read it.", path);
			}
		});
	}
}
