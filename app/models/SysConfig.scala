package models

import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import com.github.nscala_time.time.Imports._
import org.mongodb.scala.model._
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._

object SysConfig extends Enumeration {
  val ColName = "sysConfig"
  val collection = MongoDB.database.getCollection(ColName)

  val valueKey = "value"
  val ALARM_LAST_READ = Value
  val DATA_PERIOD = Value
  val OPERATION_MODE = Value
  val GCNAME_LIST = Value
  val STOP_WARN = Value

  val LocalMode = 0
  val RemoteMode = 1

  lazy val defaultConfig = Map(
    ALARM_LAST_READ -> Document(valueKey -> DateTime.parse("2019-10-1").toDate()),
    DATA_PERIOD -> Document(valueKey -> 30),
    OPERATION_MODE -> Document(valueKey -> 0),
    STOP_WARN -> Document(valueKey -> false)
  )

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }

    val idSet = values map {
      _.toString()
    }
    //Clean up unused
    val f1 = collection.deleteMany(Filters.not(Filters.in("_id", idSet.toList: _*))).toFuture()
    f1.onFailure(errorHandler)
    val updateModels =
      for ((k, defaultDoc) <- defaultConfig) yield {
        UpdateOneModel(
          Filters.eq("_id", k.toString()),
          Updates.setOnInsert(valueKey, defaultDoc(valueKey)), UpdateOptions().upsert(true))
      }

    val f2 = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()

    import scala.concurrent._
    val f = Future.sequence(List(f1, f2))
    waitReadyResult(f)
  }

  def upsert(_id: SysConfig.Value, doc: Document) = {
    val f = collection.replaceOne(Filters.equal("_id", _id.toString()), doc, ReplaceOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def get(_id: SysConfig.Value) = {
    val f = collection.find(Filters.eq("_id", _id.toString())).first().toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield {
      val doc =
        if (ret == null)
          defaultConfig(_id)
        else
          ret
      doc("value")
    }
  }

  def get(_id: SysConfig.Value, defaultDoc:Document) = {
    val f = collection.find(Filters.eq("_id", _id.toString())).first().toFuture()
    f.onFailure(errorHandler)
    for (ret <- f) yield {
      val doc =
        if (ret == null)
          defaultDoc
        else
          ret
      doc(valueKey)
    }
  }

  def set(_id: SysConfig.Value, v: BsonValue) = upsert(_id, Document(valueKey -> v))

  def getAlarmLastRead() = {
    import java.util.Date
    import java.time.Instant
    val f = get(ALARM_LAST_READ)
    f.failed.foreach(errorHandler)
    for (ret <- f) yield Date.from(Instant.ofEpochMilli(ret.asDateTime().getValue))
  }

  def setAlarmLastRead() = {
    import java.util.Date
    import java.time.Instant
    val f = upsert(ALARM_LAST_READ, Document(valueKey -> Date.from(Instant.now())))
    f.failed.foreach(errorHandler)
    f
  }

  def getDataPeriod() = {
    val f = get(DATA_PERIOD)
    f.failed.foreach(errorHandler)
    for (ret <- f)
      yield ret.asInt32().getValue
  }

  def setDataPeriod(min: Int) = {
    val f = upsert(DATA_PERIOD, Document(valueKey -> min))
    f.failed.foreach(errorHandler)
    f
  }


  def getOperationMode() = {
    val f = get(OPERATION_MODE)
    f.failed.foreach(errorHandler)
    for (ret <- f)
      yield ret.asInt32().getValue
  }

  def setOperationMode(mode: Int) = {
    val f = upsert(OPERATION_MODE, Document(valueKey -> mode))
    f.failed.foreach(errorHandler)
    f
  }

  def getGcNameList() = {
    val f = get(GCNAME_LIST, Document(valueKey -> Monitor.indParkList))
    f.failed.foreach(errorHandler)
    for (ret <- f)
      yield ret.asArray().getValues.asScala.map(v => v.asString().getValue).toSeq
  }

  def setGcNameList(nameList: Seq[String]) = {
    val f = upsert(GCNAME_LIST, Document(valueKey -> nameList))
    f.failed.foreach(errorHandler)
    f
  }

  def getStopWarn() = {
    val f = get(STOP_WARN)
    f.failed.foreach(errorHandler)
    for (ret <- f)
      yield ret.asBoolean().getValue()
  }

  def setStopWarn(v: Boolean) = {
    val f = upsert(STOP_WARN, Document(valueKey -> v))
    f.failed.foreach(errorHandler)
    f
  }
}