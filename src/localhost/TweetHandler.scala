package localhost

import scala.actors.Actor
import net.liftweb.util.{Log, Can, Full, Empty}
import net.liftweb.mapper._
import java.sql.{Connection, DriverManager, SQLException}

import net.liftweb.mapper.Schemifier


class TweetHandler (
	val globeActor: Actor	
  ) {
 // var waiting = false
  var animationCount = 0
  
  println("Initializing database connection")
  DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
  println("Connection defined")
  Schemifier.schemify(false, Log.infoF _, Tweet)
  println("Table schemified")
  
  var numRootTweets = Tweet.findAll(NullRef(Tweet.parentId), By_>(Tweet.numRetweets, 2)).length   
  //var numRootTweets = Tweet.findAll(NullRef(Tweet.parentId), By(Tweet.numRetweets, 1)).length 
  var index = 0;
  
//  var allRootTweets: List[Tweet] = Tweet.findAll(StartAt(0), MaxRows(1), NullRef(Tweet.parentId))
//  allRootTweets.foreach(t => println(t.toString))	
 
  def sendTweet = {
    if (index == 31) index += 1
    //waiting = false
    if (index < numRootTweets) {
      println("sendTweet " + index)
      globeActor ! Tweet.findAll(StartAt(index), MaxRows(1), NullRef(Tweet.parentId), By_>(Tweet.numRetweets, 2)).first
      //globeActor ! Tweet.findAll(StartAt(index), MaxRows(1), NullRef(Tweet.parentId), By(Tweet.numRetweets, 1)).first
      index += 1
    }
  }
  
  def incrementAnimationCount = {
    animationCount += 1
    //println("  starting one animation  " + animationCount)
  }
  
  def decrementAnimationCount = {
    animationCount -= 1
    //println("   finished one animation " + animationCount)
    if (animationCount == 0) sendTweet//IfReady
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