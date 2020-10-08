package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class User(_id: String, password: String, name: String, phone: String, groupId: String = Group.adminID, 
    alarm:Option[Boolean] = Some(true)){
}

object User {
  import scala.concurrent._
  import scala.concurrent.duration._
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[User]), DEFAULT_CODEC_REGISTRY)

  val COLNAME = "users"
  val collection = MongoDB.database.getCollection[User](COLNAME).withCodecRegistry(codecRegistry)
  implicit val userRead = Json.reads[User]
  implicit val userWrite = Json.writes[User]
  
  def init(colNames: Seq[String]) {
    if (!colNames.contains(COLNAME)) {
      val f = MongoDB.database.createCollection(COLNAME).toFuture()
      f.onFailure(errorHandler)
    }
    val f = collection.countDocuments().toFuture()
    f.onSuccess({
      case count =>
        if (count == 0) {
          val defaultUser = User("user", "abc123", "名洋科技", "0955577328")
          Logger.info("Create default user:" + defaultUser.toString())
          newUser(defaultUser)
        }
    })
    f.onFailure(errorHandler)
  }

  def newUser(user: User) = {
    collection.insertOne(user).toFuture()
  }

  import org.mongodb.scala.model.Filters._
  def deleteUser(email: String) = {
    collection.deleteOne(equal("_id", email)).toFuture()
  }

  def updateUser(user: User) = {
    val f = collection.replaceOne(equal("_id", user._id), user).toFuture()
    f
  }

  def getUserByIdFuture(_id: String) = {
    val f = collection.find(equal("_id", _id)).limit(1).toFuture()
    f.onFailure { errorHandler }
    for (ret <- f)
      yield if (ret.length == 0)
      None
    else
      Some(ret.head)
  }

  def getAllUsersFuture() = {
    val f = collection.find().toFuture()
    f.onFailure { errorHandler }
    for (ret <- f) yield ret
  }
}
