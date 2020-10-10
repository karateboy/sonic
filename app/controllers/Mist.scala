package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._
import ModelHelper._
import com.typesafe.config._
import java.nio.file._
import com.mist.tools.os.verticle._

object Mist extends Controller {

  case class MistSetting(serverPort: Int, url: String, wavePort: Int,
                         epochPort: Int, timeZone: Int, recordLen: Int, recordPath: String, modify: Double)

  val settingPath = Paths.get(current.path.getAbsolutePath + "/conf/setting.conf")
  def getSetting = Action {
    implicit val writes = Json.writes[MistSetting]
    val config = ConfigFactory.parseFile(settingPath.toFile())
    val serverPort = config.getInt("serverPort")
    val url = config.getString("url")
    val wavePort = config.getInt("wavePort")
    val epochPort = config.getInt("epochPort")
    val timeZone = config.getInt("timeZone")
    val recordLen = config.getInt("recordLen")
    val recordPath = config.getString("recordPath")
    val modify = config.getDouble("modify")
    Ok(Json.toJson(
      MistSetting(serverPort, url, wavePort, epochPort,
        timeZone, recordLen, recordPath, modify)))
  }

  def postSetting = Action(BodyParsers.parse.json) {
    implicit request =>
      implicit val reader = Json.reads[MistSetting]
      val settingParam = request.body.validate[MistSetting]

      settingParam.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString()))
        },
        param => {
          val config = ConfigFactory.parseFile(settingPath.toFile())
          val newConfig = config.withValue("serverPort", ConfigValueFactory.fromAnyRef(param.serverPort))
          .withValue("url", ConfigValueFactory.fromAnyRef(param.url))
          .withValue("wavePort", ConfigValueFactory.fromAnyRef(param.wavePort))
          .withValue("epochPort", ConfigValueFactory.fromAnyRef(param.epochPort))
          .withValue("timeZone", ConfigValueFactory.fromAnyRef(param.timeZone))
          .withValue("recordLen", ConfigValueFactory.fromAnyRef(param.recordLen))
          .withValue("recordPath", ConfigValueFactory.fromAnyRef(param.recordPath))
          .withValue("modify", ConfigValueFactory.fromAnyRef(param.modify))
          
          val updateContent = newConfig.resolve().root().render()
          Files.write(settingPath, updateContent.getBytes());
          Ok(Json.obj("ok" -> true))
        })
  }

  def updateConfig = Action {
    val path = Paths.get(current.path.getAbsolutePath + "/conf/setting.conf")
    val oldConfig = ConfigFactory.parseFile(path.toFile())
    val newConfig = oldConfig.withValue("ServerPort", ConfigValueFactory.fromAnyRef(Int.box(100))).withValue("Wave", ConfigValueFactory.fromAnyRef("123.123.123.123"))
    val updateContent = newConfig.resolve().root().render()

    Files.write(path, updateContent.getBytes());
    Ok("OK")
  }

  def getRealtimeValues = Action {
    val json = VertxHub.realTimeValuesFactory.getRealTimeValues();
    json.put("test", 1)
    val str = json.encode()
    Ok(str)
  }

  case class ToggleParam(on: Boolean)

  def toggleHandler(onStr: String, offStr: String) = Action(BodyParsers.parse.json) {
    implicit request =>
      implicit val reader = Json.reads[ToggleParam]
      val loggingParam = request.body.validate[ToggleParam]

      loggingParam.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString()))
        },
        param => {
          if (param.on)
            VertxHub.vertx.eventBus().publish(onStr, "")
          else
            VertxHub.vertx.eventBus().publish(offStr, "")
          Ok(Json.obj("ok" -> true))
        })
  }

  def postLogging = toggleHandler(OceanSonicTCPWaveStreamingVerticle.TAG_START_LOGGING, OceanSonicTCPWaveStreamingVerticle.TAG_STOP_LOGGING)
  def postStreamming = toggleHandler(OceanSonicTCPWaveStreamingVerticle.TAG_START_STREAMING, OceanSonicTCPWaveStreamingVerticle.TAG_STOP_STREAMING)

  case class Status(connected: Boolean, logging: Boolean, streaming: Boolean)
  def getStatus = Action {
    implicit val write = Json.writes[Status]
    val stat = Status(VertxHub.streamingVerticle.isConnected(), VertxHub.streamingVerticle.isLogging(), VertxHub.streamingVerticle.isStreaming())
    Ok(Json.toJson(stat))
  }
    
}