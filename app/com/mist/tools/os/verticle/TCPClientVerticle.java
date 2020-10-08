package com.mist.tools.os.verticle;

import org.slf4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public abstract class TCPClientVerticle extends AbstractVerticle {
	public static final String TAG_CONNECT_SOCKET = "connect socket";
	
	protected Logger logger;
	
	protected NetClientOptions options = new NetClientOptions()
			.setTcpKeepAlive(true)
			.setConnectTimeout(2000)
			.setReconnectAttempts(3)
			.setReconnectInterval(1000);
	
	protected NetClient client;
	
	protected NetSocket socket;
	
	protected String url;
	
	protected int port;
	
	protected boolean isConnected = false;
	
	@Override
	public void start() throws Exception{
		super.start();
		vertx.eventBus().consumer(TAG_CONNECT_SOCKET, msg -> connectSocket());
		connectSocket();
	}
	
	@Override
	public void stop() throws Exception {
		closeSocket();
		super.stop();
	}
	
	protected void connectSocket(){
		if (isConnected) return;
		
		client = vertx.createNetClient(options);
		client.connect(port, url, res -> {
			if(res.succeeded()){
				logger.info("Socket connected!");
				isConnected = true;
				socket = res.result();
				socket.handler(buffer -> handleMessage(buffer));
				socket.exceptionHandler(e -> {
					isConnected = false;
					logger.error("Socket connect error : {}", e.getMessage());
					
				});
				socket.endHandler(e -> {
					isConnected = false;
					logger.info("Socket is end!");
				});
				socket.closeHandler(e -> {
					isConnected = false;
					logger.info("Socket is close!");
					client.close();
				});
				
			}else{
				logger.error("Failed to connect! : {}", res.cause().getMessage());
				isConnected = false;
			}
		});
	}
	
	protected void closeSocket(){
		socket.close();
	}
	
	protected abstract void handleMessage(Buffer buffer);
}
