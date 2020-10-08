package com.mist.tools.os.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import io.vertx.core.json.JsonObject;

public class PropertiesUtils {
	private static final String CONFIG_FILE_NAME = "OceanSonicRealTime.conf";

	private static final Properties PROPS = new Properties();
	
	public static Properties getProperties(){
		if(PROPS.isEmpty()){
			initProperties();
		}
		return PROPS;
	}
	
	private static void initProperties(){
		Path path = Paths.get(CONFIG_FILE_NAME);
		try {
			BufferedReader reader = Files.newBufferedReader(path);
			reader.lines()
				.filter(line -> !line.startsWith("[!|#]"))
				.filter(line -> !line.isEmpty())
				.map(line -> {
					int idx = line.indexOf("!");
					idx = idx > 0 ? idx : line.length();
					return line.substring(0, idx).trim();
				})
				.map(line -> {
					int idx = line.indexOf("#");
					idx = idx > 0 ? idx : line.length();
					return line.substring(0, idx).trim();
				})
				.forEach(line -> {
					String[] entry = line.split(":");
					PROPS.put(entry[0].trim(), entry[1].trim());
				});
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static JsonObject getJsonProperties(){
		JsonObject json = new JsonObject();
		PROPS.stringPropertyNames().forEach(key -> json.put(key, PROPS.getProperty(key)));
		return json;
	}
	
	public static void setProperty(String key, String value){
		PROPS.setProperty(key, value);
	}
	
	public static void saveProperties(){
		Path path = Paths.get(CONFIG_FILE_NAME);
		try{
			BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
			Set<String> keySet = PROPS.stringPropertyNames();
			for(String key : keySet){
				bw.write(String.format("%s:%s\n", key, PROPS.getProperty(key)));
			}
			bw.flush();
			bw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void savePropertiesFromEntrys(List<Entry<String, String>> entrys) throws IOException{
		Path path = Paths.get(CONFIG_FILE_NAME);
		
		BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
		for(Entry<String, String> entry : entrys){
			bw.write(String.format("%s:%s\n", entry.getKey(), entry.getValue()));
		}
		bw.flush();
		bw.close();
		
	}
}
