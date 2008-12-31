package localhost


import gov.nasa.worldwind.util.StatusBar
import gov.nasa.worldwind.avlist._
import gov.nasa.worldwind._
import gov.nasa.worldwind.geom._
import gov.nasa.worldwind.render._
import gov.nasa.worldwind.globes._
import gov.nasa.worldwind.layers._	
import gov.nasa.worldwind.awt._
import gov.nasa.worldwind.examples.LineBuilder
import gov.nasa.worldwind.view.ScheduledOrbitViewStateIterator
import gov.nasa.worldwind.view.BasicOrbitView

import javax.swing._
import java.awt._
import java.util.ArrayList
import javax.media.opengl.GLContext
import java.util.Random
import java.util.{ArrayList => JArrayList}
import java.lang.Iterable

import scala.actors._ 
import scala.actors.Actor._
import scala.collection.jcl.Conversions._
import scala.collection.jcl._

class AnimatedAnnotatedLine (val startPos: Position, val endPos: Position, val globeAnno: Option[GlobeAnnotation], val color: Color) extends Polyline
{
  customConfigurations
//  var progress:Float = 0
  
  def updateLine(progress: Float) = {
    //val lat: Angle = startPos.getLatitude.add((endPos.getLatitude.subtract(startPos.getLatitude)).multiply(progress))
    //val lon: Angle = startPos.getLongitude.add((endPos.getLongitude.subtract(startPos.getLongitude)).multiply(progress))
    //val elev: Double = startPos.getElevation + ((endPos.getElevation - startPos.getElevation) * progress)
    
    //val curPos: Position = new Position(new LatLon(lat, lon), elev)
    val curPos = Position.interpolate(progress, startPos, endPos)
    val posArray = new ArrayList[Position]
    posArray.add(startPos)
    posArray.add(curPos)
    setPositions(posArray)
 
    globeAnno match { 
      case Some(s) => s.setPosition(curPos)
      case None => // handle None case

 	}
  }
  
  def customConfigurations= {
    setLineWidth(3)
    setAntiAliasHint(Polyline.ANTIALIAS_NICEST)
    setHighlightColor(new Color(0f, 0f, 0f, 0.5f))
    setHighlighted(true)
    setFollowTerrain(true)
    setPathType(Polyline.LINEAR)
    setColor(color)
  }
}