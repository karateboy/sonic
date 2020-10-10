package models

import io.vertx.core._
import com.mist.tools.os.verticle.MistTCPWaveStreamingVerticle
import com.mist.tools.os.model.RealTimeValuesFactory
import java.time._
object VertxHub {
  val vertx = Vertx.vertx()
  val realTimeValuesFactory = new RealTimeValuesFactory();
  var streamingVerticle: MistTCPWaveStreamingVerticle = _

  def init = {
    val zoneOffset = ZoneOffset.ofHours(8);
    val recordLength = 2;
    val modify = 0d;
    val url = "";
    val recordPath = "";

    val deploymentOptions = new DeploymentOptions().setWorker(true);
    streamingVerticle = new MistTCPWaveStreamingVerticle(realTimeValuesFactory, "", 51678,
      recordPath, zoneOffset, recordLength, modify);
    vertx.deployVerticle(streamingVerticle, deploymentOptions);
    //		vertx.deployVerticle(new OceanSonicTCPEpochStreamingVerticle(url, Integer.parseInt(properties.getProperty("port.stream.epoch", "51680")), realTimeValuesFactory));
  }
}