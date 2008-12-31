package localhost

import scala.actors.Actor
import net.liftweb.util.{Log, Can, Full, Empty}
import net.liftweb.mapper._
import java.sql.{Connection, DriverManager, SQLException}

import net.liftweb.mapper.Schemifier


class TweetHandler (
	val globeActor: Actor	
  ) {
  
  println("Initializing database connection")
  DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
  println("Connection defined")
  Schemifier.schemify(false, Log.infoF _, Tweet)
  println("Table schemified")
  
  var numRootTweets = Tweet.findAll(NullRef(Tweet.parentId), By_>(Tweet.numRetweets, 2)).length   
  var index = 0;
 
  def sendTweet = {
    if (index == 31) index += 1		//weird bug, it was crashing on this tweet

    if (index < numRootTweets) {
      println("sendTweet " + index)
      var newTweet = Tweet.findAll(StartAt(index), MaxRows(1), NullRef(Tweet.parentId), By_>(Tweet.numRetweets, 2)).first
      newTweet.setDepth(newTweet.recursivelyPopulateChildList)
      globeActor ! newTweet
      index += 1
    }
  }
}

object DBVendor extends ConnectionManager {
 def newConnection(name: ConnectionIdentifier): Can[Connection] = {
   try {
     Class.forName("com.mysql.jdbc.Driver")
     val dm = DriverManager.getConnection("jdbc:mysql://mysql.lehrblogger.com/retweettree?user=phpuser&password=tev9shesh5gi3ha")
     Full(dm)
   } catch {
     case e : Exception => e.printStackTrace; Empty
   }
 }
 def releaseConnection(conn: Connection) {conn.close}
}