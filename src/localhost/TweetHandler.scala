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
  var lastParentId = Tweet.findAll(NullRef(Tweet.parentId)).sort(_.tweetId.is < _.tweetId.is).last.tweetId.is 
  var index = 25
 
  def sendTweet: Unit = {
   // if (index == 31) index += 1		//weird bug, it was crashing on this tweet
    
    var newTweets = Tweet.findAll(NullRef(Tweet.parentId), By_>(Tweet.tweetId, lastParentId)).sort(_.tweetId.is < _.tweetId.is)
   // println("lastParentId = " + lastParentId + " and newTweets.length = " + newTweets.length )
    if (newTweets.length > 0) {
      lastParentId = newTweets.first.tweetId
      println("sendTweet (new) " + lastParentId)
      var newTweet = newTweets.first
      newTweet.setDepth(newTweet.recursivelyPopulateChildList)
      globeActor ! Pair("incoming new tweet", newTweet)
    } else if (index < numRootTweets) {
      println("sendTweet (old) " + index)
      var oldTweet = Tweet.findAll(StartAt(index), MaxRows(1), NullRef(Tweet.parentId), By_>(Tweet.numRetweets, 2)).first
      oldTweet.setDepth(oldTweet.recursivelyPopulateChildList)
      globeActor ! ("incoming old tweet", oldTweet)
      index += 1
    } else {
      index = 0
      sendTweet
    }
  }
}

object DBVendor extends ConnectionManager {
 def newConnection(name: ConnectionIdentifier): Can[Connection] = {
   try {
     Class.forName("com.mysql.jdbc.Driver")
     val dm = DriverManager.getConnection("jdbc:mysql://mysql.lehrblogger.com/retweettree?user=twiterra_app&password=jelf7ya9head8w")
     Full(dm)
   } catch {
     case e : Exception => e.printStackTrace; Empty
   }
 }
 def releaseConnection(conn: Connection) {conn.close}
}