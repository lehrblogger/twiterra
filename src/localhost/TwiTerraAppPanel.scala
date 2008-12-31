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
   
      //var tweetLayer: Layer = new RenderableLayer;
      var layers: scala.List[Layer] = wwd.getModel.getLayers.toList
      layers = layers.filter { l =>
	      (l.isInstanceOf[gov.nasa.worldwind.layers.StarsLayer] ||
           //l.isInstanceOf[gov.nasa.worldwind.layers.SkyGradientLayer] ||
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
      layers.foreach { l =>
       //     l.setEnabled(false)
	      //if (l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGOneImage] || l.isInstanceOf[gov.nasa.worldwind.layers.Earth.BMNGWMSLayer] || l.isInstanceOf[gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer] || l.isInstanceOf[WorldMapLayer]  || l.isInstanceOf[ScalebarLayer]|| l.isInstanceOf[CompassLayer]) 
	  }    
      initLayerCount = layers.length
      wwd.getModel.setLayers(new LayerList(layers.toArray))
        
    val globeActor = actor {
        loop {
          react {
            case "animation complete" => react {
              case newTweet: Tweet => displayTweetTree(newTweet)
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
     
    var animationCount = 0
    
    val maxNumTrees = 20
    val duration = 3000
    var minDepth = 2
    var minAvgDist = 1000
    var minDist = 300
    
    def displayTweetTree(newTweet: Tweet): Unit = {
      twitterActor ! "request next tweet"
      
      println(newTweet.author + "::  minDepth=" + newTweet.depth + " minAvgDist=" + newTweet.avgDist)
      if ((newTweet.numRetweets > minDepth) && (newTweet.depth >= minDepth) && (newTweet.avgDist >= minAvgDist) && (newTweet.minDist >= minDist)) {
        val initEyePos: Position = new Position(wwd.getView.getCurrentEyePosition.getLatitude, wwd.getView.getCurrentEyePosition.getLongitude, 0)
        val newTweetPos: Position = Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0)
        Thread.sleep(duration)
        wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(initEyePos, newTweetPos, duration, true))
        Thread.sleep(duration)
        
        var randColor = new Random()	
        val color = new Color((randColor.nextFloat * 100).toInt + 155, (randColor.nextFloat * 100).toInt + 155, (randColor.nextFloat * 100).toInt + 155)
        var layer: RenderableLayer = new RenderableLayer()
        wwd.getModel.getLayers.add(/*wwd.getModel.getLayers.size,*/ layer)


        var tweetAnno = new TweetAnnotation(newTweet.author + ": " + newTweet.original, Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0), color)
        layer.addRenderable(tweetAnno)
       
        updateTreeLayers
        displayTweet(newTweet, true, tweetAnno, layer, color)
      } else {
        globeActor ! "animation complete"
      }
    }
    
    def displayTweet(newTweet: Tweet, followThis: Boolean, tweetAnno: TweetAnnotation, layer: RenderableLayer, color: Color): Unit = {
      val newPos: Position = Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0)
    
      var maxIndex = newTweet.indexOfChildWithMaxAvgDist;
      var index = 0
      newTweet.children.foreach(childTweet => {
        val childPos: Position = Position.fromDegrees(childTweet.locLat, childTweet.locLon, 0)
      
        var line: AnimatedAnnotatedLine = new AnimatedAnnotatedLine(newPos, childPos, None, color)
        if (maxIndex == index) {
          line = new AnimatedAnnotatedLine(newPos, childPos, Some(tweetAnno), color)
        }
        layer.addRenderable(line)
        
        val target = new LineEventHandler(line, childTweet, (followThis && (maxIndex == index)), tweetAnno, layer, color)
        val anim: Animator = new Animator(duration, target)
        animationCount += 1
        anim.start()
        
     	if (maxIndex == index) {
     	  wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(newPos, childPos, duration, true))
        }
      
        index += 1
      })
    }
    
    def updateTreeLayers = {
      var initLayers: scala.List[Layer] = wwd.getModel.getLayers.toList
      var finalLayers: scala.List[Layer] = initLayers.dropRight(initLayers.length - initLayerCount)
      var renderLayers: scala.List[Layer] = Nil
      
      var alpha = (255 / maxNumTrees)
      initLayers = initLayers.drop(initLayerCount)
      initLayers = initLayers.drop(initLayers.length - maxNumTrees.toInt)
      initLayers.reverse.foreach( l => {
        if (l.isInstanceOf[RenderableLayer]) {
          setLayerOpacity(l.asInstanceOf[RenderableLayer], alpha)
	    }
        renderLayers = renderLayers ++ scala.List(l)
        //alpha -= (255 / maxNumTrees)
      })
      
      finalLayers = finalLayers ++ renderLayers.reverse
      wwd.getModel.setLayers(new LayerList(finalLayers.toArray))
    }
    
  def setLayerOpacity(l: RenderableLayer, alpha: Int) {
    println("here")
	  var renderables: JIterable[Renderable] = l.asInstanceOf[RenderableLayer].getRenderables
	  var renderablesIterator: JIterator[Renderable] = renderables.iterator
	  
//	  def calcAlpha(a: Int): Int = 255 min (0 max (alpha - (255 - a)))
	  
  
	  while (renderablesIterator.hasNext) {
	    var r = renderablesIterator.next
	    if (r.isInstanceOf[AnimatedAnnotatedLine]) {
	      r.asInstanceOf[AnimatedAnnotatedLine].updateLineOpacity(alpha)
	    } else if (r.isInstanceOf[TweetAnnotation]) {
	      r.asInstanceOf[TweetAnnotation].updateAnnotationOpacity(alpha)
	    }
	 }
  }
    
  
  def getWwd: WorldWindowGLCanvas = {
     return wwd;
  }

  def getStatusBar: StatusBar = {
    return statusBar;
  }
    
    class LineEventHandler(line: AnimatedAnnotatedLine, childTweet: Tweet, followThis: Boolean, globeAnno: TweetAnnotation, layer: RenderableLayer, color: Color) extends TimingTargetAdapter
    {
      override def begin = {
      }
      
      override def timingEvent(fraction: Float) = {
        line.updateLine(fraction)
      }
  
      override def end = {
        displayTweet(childTweet, followThis, globeAnno, layer, color)
        animationCount -= 1
        if (animationCount == 0) globeActor ! "animation complete"
      } 
    }	    
}

class TweetAnnotation (val text: String, var position: Position, val color: Color) extends GlobeAnnotation
{
  var annoAttr = new AnnotationAttributes
  //annoAttr.getFont.setSize(annoAttr.getFont.getSize * 1.25)
  annoAttr.setBorderColor(color)
  annoAttr.setTextColor(color)
  annoAttr.setBackgroundColor(new Color(Color.BLACK.getRed, Color.BLACK.getGreen, Color.BLACK.getBlue, 190))
  setAttributes(annoAttr)
  
  
  def updateAnnotationOpacity(alpha: Int) = {
    def calcAlpha(a: Int): Int = 0 max (a - alpha)
  
    val attributes = getAttributes
      
    val bc = attributes.getBorderColor
    attributes.setBorderColor(new Color(bc.getRed, bc.getGreen, bc.getBlue, calcAlpha(bc.getAlpha)))
   
    val bgc = attributes.getBackgroundColor
    attributes.setBackgroundColor(new Color(bgc.getRed, bgc.getGreen, bgc.getBlue, calcAlpha(bgc.getAlpha)))
        
    val tc = attributes.getTextColor
    attributes.setTextColor(new Color(tc.getRed, tc.getGreen, tc.getBlue, calcAlpha(tc.getAlpha)))

    setAttributes(attributes)
  }  
}

class AnimatedAnnotatedLine (val startPos: Position, val endPos: Position, val tweetAnno: Option[TweetAnnotation], val color: Color) extends Polyline
{
  customConfigurations
  
  def updateLine(progress: Float) = {
    val curPos = Position.interpolate(progress, startPos, endPos)
    val posArray = new ArrayList[Position]
    posArray.add(startPos)
    posArray.add(curPos)
    setPositions(posArray)
 
    tweetAnno match { 
      case Some(s) => s.setPosition(curPos)
      case None => // handle None case

 	}
  }
  
  def customConfigurations= {
    setLineWidth(3)
    setHighlightColor(new Color(0f, 0f, 0f, 0.5f))
    setHighlighted(true)
    setFollowTerrain(true)
    setPathType(Polyline.LINEAR)
    setColor(color)
    setAntiAliasHint(Polyline.ANTIALIAS_FASTEST)
  }
  
  def updateLineOpacity(alpha: Int) = {
    def calcAlpha(a: Int): Int = 0 max (a - alpha)
  
	val lc = getColor
	setColor(new Color(lc.getRed, lc.getGreen, lc.getBlue, calcAlpha(lc.getAlpha)))
	val hc = getHighlightColor
	setHighlightColor(new Color(hc.getRed, hc.getGreen, hc.getBlue, calcAlpha(hc.getAlpha)))   
  }
}


