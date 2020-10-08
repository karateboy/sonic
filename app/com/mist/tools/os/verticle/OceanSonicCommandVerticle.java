package com.mist.tools.os.verticle;

import com.mist.tools.os.utils.OceanSonicCommandUtils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public class OceanSonicCommandVerticle extends AbstractVerticle{

	protected NetClient client;
	
	protected NetSocket socket;
	
	protected String url;
	
	protected int port;
	
	public OceanSonicCommandVerticle(String url, int port){
		super();
		this.url = url;
		this.port = port;
	}
	
	protected void connectSocket(){
		NetClientOptions options = new NetClientOptions().setTcpKeepAlive(true);
		client = vertx.createNetClient(options);
		client.connect(port, url, res -> {
			if(res.succeeded()){
				System.out.println("Connected!");
				socket = res.result();
				socket.endHandler(e -> {
					System.out.println("Socket is end!");
				});
			}else{
				System.out.println("Failed to connect! : " + res.cause().getMessage());
			}
		});
	}
	
	protected void closeClient(){
		socket.close();
		client.close();
	}
	
	protected void sendDeviceCommand(){
		Buffer command = OceanSonicCommandUtils.getQueryDeviceInfoCommand();
		
		socket.write(command, socketRes -> {
			if(socketRes.succeeded()){
				System.out.println("Send command success!");
			}else{
				System.out.println("Send command failed");
			}
		});
		
		socket.handler(buf -> {
			System.out.println("I received some bytes: " + buf.length());
//			handleStreaming(buf);
		});
	}
	
	protected void sendSetTimeCommand(){
		Buffer command = OceanSonicCommandUtils.getSetTimeCommand();
		
		socket.write(command, socketRes -> {
			if(socketRes.succeeded()){
				System.out.println("Send command success!");
			}else{
				System.out.println("Send command failed");
			}
		});
		
		socket.handler(buf -> {
			System.out.println("I received some bytes: " + buf.length());
//			handleStreaming(buf);
		});
	}
	
	private void sendStopStreamingCommand(){
		Buffer command = OceanSonicCommandUtils.getStopStreamingCommandForCommandSocket();
		
		socket.write(command, socketRes -> {
			if(socketRes.succeeded()){
				System.out.println("Send command success!");
			}else{
				System.out.println("Send command failed");
			}
		});
		
		socket.handler(buf -> {
			System.out.println("I received some bytes: " + buf.length());
//			handleStreaming(buf);
		});
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		connectSocket();
		vertx.eventBus().consumer("device", msg -> sendDeviceCommand());
		vertx.eventBus().consumer("set time", msg -> sendSetTimeCommand());
		vertx.eventBus().consumer("stop streaming", msg -> sendStopStreamingCommand());
	}
	
	@Override
	public void stop() throws Exception {
		closeClient();
		super.stop();
	}
}
