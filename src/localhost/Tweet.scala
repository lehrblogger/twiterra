package localhost
 
import net.liftweb.mapper._
import net.liftweb.mapper.MappedDouble 
import net.liftweb.util.{Can, Full, Empty}
import java.sql.Timestamp
import java.util.Locale

object Tweet extends Tweet with KeyedMetaMapper[Long, Tweet] { override def dbTableName = "tweets" }

class Tweet extends KeyedMapper[Long, Tweet] {
  def getSingleton = Tweet
  def primaryKeyField = tweetId
  
  object tweetId extends MappedLongIndex(this) { override def dbColumnName = "tweet_id" }
  object author extends MappedString(this, 15)
  object original extends MappedString(this, 140)
  object locLat extends MappedDouble(this) { override def dbColumnName = "loc_lat" }
  object locLon extends MappedDouble(this) { override def dbColumnName = "loc_lon" }
  object time extends MappedString(this, 20) { override def dbColumnName = "time" }
  object numRetweets extends MappedLong(this) { override def dbColumnName = "num_retweets" }
  object parentId extends MappedLongForeignKey(this, Tweet) { override def dbColumnName = "parent_id" }
  object parentDist extends MappedDouble(this) { override def dbColumnName = "parent_dist" }
  
  var children: List[Tweet] = Nil

  def getChildren = Tweet.findAll(By(Tweet.parentId, tweetId))
  def descendants: List[Tweet] = children ++ children.flatMap(_.descendants)
  var depth = -1

  def recursivelyPopulateChildList: Int = {	 //returns depth of tree from this tweet
    children = getChildren
    
    var childDepths: List[Int] = Nil
    children.foreach(child => childDepths = childDepths ++ List(child.recursivelyPopulateChildList))
    
    if (children.length == 0) {
      return 1
    } else {
      return (childDepths.sort(_>_).first + 1)
    }
  }
 
  def indexOfMostInterestingChild:Int = {
    if (children.length <= 0) return -1	//bad style. this simplifies code later

    val list1 = for ((t, index) <- children.zipWithIndex) yield (index, t.depth, t.avgDist)
    val list2 = list1.sort(_._2 > _._2)
    val list3 = list2.filter(each => each._2 >= list2.first._2) 
    
    list3.sort(_._3 > _._3).first._1
  }
  
  /*
  
  def indicesOfChildrenOrderedByAvgDist = {
	var orderedIndicies: List[Pair[Int, Double]] = Nil
    
    var index = 0
    children.foreach(t => {
      orderedIndicies = orderedIndicies ++ List(Pair(index,t.avgDist))
      index += 1
    })
    
    orderedIndicies.sort(_._2 > _._2)
  }
  def indicesOfChildrenWithMaxAvgDist = {
    var max: Double = 0
    var index = 0
    var maxIndex = 0
    children.foreach(t => {
      if (t.avgDist > max) {
        max = t.avgDist
        maxIndex = index
      }
      index += 1
    })
    List(maxIndex)
  }
  */
  def minDist = {
    var min = Math.MAX_DOUBLE
    descendants.foreach(t => {
      if (t.parentDist < min)
        min = t.parentDist
    })
    min
  }
  
  def avgDist = {
    descendants.foldLeft(0.0)(_ + _.parentDist.is) / (descendants.length max 1)	
  }
  
  def setDepth(d: Int) = {
    depth = d
  }
  
  override def toString = {
    author + ": " + new String(original.getBytes(), "MacRoman")
  }
}