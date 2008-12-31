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
import javax.media.opengl.GLContext
import java.util.Random
import java.util.{ArrayList => JArrayList}
import java.lang.{Iterable => JIterable}
import java.util.{Iterator => JIterator}
import java.util.{List => JList}
import java.util.{Collection => JCollection}

import scala.actors._ 
import scala.actors.Actor._
import scala.collection.jcl.Conversions._
import scala.collection.jcl._

import org.jdesktop.animation.timing.{Animator, TimingTargetAdapter}
import org.jdesktop.animation.timing.interpolation.PropertySetter


class TwiTerraAppPanel (val canvasSize: Dimension, val includeStatusBar: Boolean) extends JPanel
{
  var wwd: WorldWindowGLCanvas = new WorldWindowGLCanvas()												// random stuff I do not full understand for the DrawingContext, for the lines and Annotations
  var initLayerCount = 0;
    
  var statusBar: StatusBar = new StatusBar();
  if (includeStatusBar) {
  //    var statusBar = new StatusBar();
    add(statusBar, BorderLayout.PAGE_END);
    statusBar.setEventSource(wwd);
  }

  wwd.setPreferredSize(canvasSize);

  // Create the default model as described in the current worldwind properties.
  var m: Model = (WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME)).asInstanceOf[Model];
  wwd.setModel(m);

  // Setup a select listener for the worldmap click-and-go feature
  //wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

  add(this.wwd, BorderLayout.CENTER)							    // Create World Window GL Canvas
                                                                                            // Create the default model as described in the current worldwind properties.
  var context = wwd.getSceneController.getDrawContext
  context.setModel(m)
  context.setSurfaceGeometry(new SectorGeometryList) 									// spent a long time pouring through docs to find stuff for the DrawingContext that worked
  context.setGLContext(wwd.getContext)
    
      var layers: scala.List[Layer] = wwd.getModel.getLayers.toList
      layers = layers.filter { l =>
	      (l.isInstanceOf[gov.nasa.worldwind.layers.StarsLayer] ||
           l.isInstanceOf[gov.nasa.worldwind.layers.SkyGradientLayer] ||
           l.isInstanceOf[gov.nasa.worldwind.layers.FogLayer] ||
           l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGOneImage] ||
           l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGWMSLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.Earth.NAIPCalifornia] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.Earth.USGSUrbanAreaOrtho] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.Earth.EarthNASAPlaceNameLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.WorldMapLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.ScalebarLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.CompassLayer] ||
           false //for commenting 
	      )
	      //if (l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGOneImage] || l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGWMSLayer] || l.isInstanceOf[gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer] || l.isInstanceOf[WorldMapLayer]  || l.isInstanceOf[ScalebarLayer]|| l.isInstanceOf[CompassLayer]) 
	  }
      initLayerCount = layers.length
      wwd.getModel.setLayers(new LayerList(layers.toArray))
        
    val globeActor = actor {
        loop {
          react {
            case "animation complete" => react {
              case ("incoming new tweet", newTweet: Tweet) => displayTweetTree(newTweet, true)
              case ("incoming old tweet", newTweet: Tweet) => displayTweetTree(newTweet, false)
            }
          }
        }
    }
    val tweetHandler = new TweetHandler(globeActor)
    val twitterActor = actor { 
      loop {
        react { 
          case "request next tweet" => tweetHandler.sendTweet
        } 
      } 
    }
   globeActor ! "animation complete"
   twitterActor ! "request next tweet"
      
    val maxNumTrees = 6
    val animDuration = 2500
    val readDuration = 4000
    var minDepth = 2
    var minAvgDist = 2000
    var minDist = 300
    
    val transitionActor = actor {
      loop {
        react {
          case t: TweetPackage => {
            Thread.sleep(readDuration)
            displayTweet(t)
          }
          case "animation complete" => globeActor ! "animation complete" //wait until all the animations here are done, then it will wake up and go
        }
      }
    }
   
    
    
    def displayTweetTree(newTweet: Tweet, isNewTweet: Boolean): Unit = {
      twitterActor ! "request next tweet"
      twitterActor ! "request next tweet"
      
      println(newTweet.author + "::  minDepth=" + newTweet.depth.toInt + " minAvgDist=" + newTweet.avgDist.toInt + " minDist=" + newTweet.minDist.toInt)
      if (isNewTweet || ((newTweet.numRetweets > minDepth) && (newTweet.depth >= minDepth) && (newTweet.avgDist >= minAvgDist) && (newTweet.minDist >= minDist))) {
        val initEyePos: Position = new Position(wwd.getView.getCurrentEyePosition.getLatitude, wwd.getView.getCurrentEyePosition.getLongitude, 0)
        val newTweetPos: Position = Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0)
        wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(initEyePos, newTweetPos, animDuration, true))
        Thread.sleep(animDuration)
        
        
        var color = Color.LIGHT_GRAY
        if (isNewTweet) {
          var randColor = new Random()	
          color = new Color((randColor.nextFloat * 80).toInt + 175, (randColor.nextFloat * 80).toInt + 175, (randColor.nextFloat * 80).toInt + 175)
        }
          
        var rLayer: RenderableLayer = new RenderableLayer()
        wwd.getModel.getLayers.add(wwd.getModel.getLayers.size, rLayer)
      
        updateTreeLayers
        var tweetAnno = new TweetAnnotation(newTweet.toString, Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0), color, true)
        var aLayer: AnnotationLayer = new AnnotationLayer()
        aLayer.addAnnotation(tweetAnno)
        wwd.getModel.getLayers.add(wwd.getModel.getLayers.size, aLayer)
        Thread.sleep(readDuration)
        
        transitionActor ! new TweetPackage(newTweet, true, rLayer, aLayer, color)
      } else {
        globeActor ! "animation complete"
      }
    }
    
    def displayTweet(t: TweetPackage): Unit = {
      val newPos: Position = Position.fromDegrees(t.tweet.locLat, t.tweet.locLon, 0)
      var maxIndex = t.tweet.indexOfChildWithMaxAvgDist;
      var index = 0
      
      t.tweet.children.foreach(childTweet => {
        val childPos: Position = Position.fromDegrees(childTweet.locLat, childTweet.locLon, 0)
      
        val tweetAnno = new TweetAnnotation(childTweet.toString, newPos, t.color, (t.followThis && (maxIndex == index)))
        t.aLayer.addAnnotation(tweetAnno)
        var line = new AnimatedAnnotatedLine(newPos, childPos, tweetAnno, t.color)
        t.rLayer.addRenderable(line)
        
        val target = new LineEventHandler(line, new TweetPackage(childTweet, (t.followThis && (maxIndex == index)), t.rLayer, t.aLayer, t.color))
        val anim: Animator = new Animator(animDuration, target)
        anim.start()
        
     	if (maxIndex == index) {
     	  wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(newPos, childPos, animDuration, true))
        }
      
        index += 1
      })
      
      if (t.followThis && (t.tweet.children.length == 0)) {
        transitionActor ! "animation complete"
      }
    }
    
    def updateTreeLayers = {
      var initLayers: scala.List[Layer] = wwd.getModel.getLayers.toList
      var finalLayers: scala.List[Layer] = initLayers.dropRight(initLayers.length - initLayerCount)
      
      var alpha = (255 / maxNumTrees)
      initLayers = initLayers.drop(initLayerCount)
      initLayers = initLayers.drop(initLayers.length - (maxNumTrees.toInt * 2))
      initLayers.foreach( l => {
        if (l.isInstanceOf[RenderableLayer]) {
          setRLayerOpacity(l.asInstanceOf[RenderableLayer], alpha)
	    } else if (l.isInstanceOf[AnnotationLayer]) {
          setALayerOpacity(l.asInstanceOf[AnnotationLayer], alpha)
	    }
        finalLayers = finalLayers ++ scala.List(l)
      })
      
      wwd.getModel.setLayers(new LayerList(finalLayers.toArray))
    }
    
  def setRLayerOpacity(l: RenderableLayer, alpha: Int) {
	  var renderablesIterator: JIterator[Renderable] = l.getRenderables.iterator
   
	  while (renderablesIterator.hasNext) {
	    var r = renderablesIterator.next
	    if (r.isInstanceOf[AnimatedAnnotatedLine]) {
	      r.asInstanceOf[AnimatedAnnotatedLine].updateLineOpacity(alpha)
	    }
	 }
  }
  def setALayerOpacity(l: AnnotationLayer, alpha: Int) {
	  var renderablesIterator: JIterator[Annotation] = l.getAnnotations.iterator
   
	  while (renderablesIterator.hasNext) {
	    var a = renderablesIterator.next
        if (a.isInstanceOf[TweetAnnotation]) {
	      a.asInstanceOf[TweetAnnotation].updateAnnotationOpacity(alpha)
	    }
	 }
  }
    

  def getWwd: WorldWindowGLCanvas = {
     return wwd;
  }

  def getStatusBar: StatusBar = {
    return statusBar;
  }
    
    class LineEventHandler(line: AnimatedAnnotatedLine, t: TweetPackage) extends TimingTargetAdapter
    {
      override def begin = {
      }
      
      override def timingEvent(fraction: Float) = {
        line.updateLine(fraction)
      }
  
      override def end = {
        if (t.followThis) {
          transitionActor ! t
        } else {
          displayTweet(t)
        }
      } 
    }	    
}

case class TweetPackage(val tweet: Tweet, val followThis: Boolean, val rLayer: RenderableLayer, val aLayer: AnnotationLayer, val color: Color)




