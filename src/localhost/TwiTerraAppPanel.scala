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
    
  //parent(new BorderLayout);

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
              case newTweet: Tweet => displayTweetTree(newTweet)
          }
       }
    }
    val tweetHandler = new TweetHandler(globeActor)
    val twitterActor = actor { 
      loop {
        react { 
          case "force next tweet" => tweetHandler.sendTweet
          case "starting one animation" => tweetHandler.incrementAnimationCount
          case "finished one animation" => tweetHandler.decrementAnimationCount
        } 
      } 
    }
    
   twitterActor ! "force next tweet"
     
      
    val duration = 3000
    var minDepth = 2
    var avgDist = 1000
    def displayTweetTree(newTweet: Tweet): Unit = {
      if (newTweet.numRetweets < minDepth) {
        twitterActor ! "force next tweet"
        
      } else {
        val depth = newTweet.recursivelyPopulateChildList
       
        println(newTweet.author + " depth=" + depth + " avgDist=" + newTweet.avgDist)
        if ((depth > minDepth) && (newTweet.avgDist > avgDist)) {
          val initEyePos: Position = new Position(wwd.getView.getCurrentEyePosition.getLatitude, wwd.getView.getCurrentEyePosition.getLongitude, 0)
          val newTweetPos: Position = Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0)
          wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(initEyePos, newTweetPos, duration, true))
      
          var randColor = new Random()	
          val color = new Color((randColor.nextFloat * 100).toInt + 155, (randColor.nextFloat * 100).toInt + 155, (randColor.nextFloat * 100).toInt + 155)
          var layer: RenderableLayer = new RenderableLayer()
          wwd.getModel.getLayers.add(wwd.getModel.getLayers.size, layer)

          var annoAttr = new AnnotationAttributes
          annoAttr.setBorderColor(color)
          annoAttr.setTextColor(color)
          var globeAnno = new GlobeAnnotation(newTweet.author + ": " + newTweet.original, Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0))
          globeAnno.setAttributes(annoAttr)
          layer.addRenderable(globeAnno)
       
          updateTreeLayers
          displayTweet(newTweet, true, globeAnno, layer, color)
        
        } else {
          twitterActor ! "force next tweet"
        }
      }
    }
    
    def displayTweet(newTweet: Tweet, followThis: Boolean, globeAnno: GlobeAnnotation, layer: RenderableLayer, color: Color): Unit = {
      val newPos: Position = Position.fromDegrees(newTweet.locLat, newTweet.locLon, 0)
    
      var maxIndex = newTweet.indexOfChildWithMaxAvgDist;
      var index = 0
      newTweet.children.foreach(childTweet => {
        val childPos: Position = Position.fromDegrees(childTweet.locLat, childTweet.locLon, 0)
      
        var line: AnimatedAnnotatedLine = new AnimatedAnnotatedLine(newPos, childPos, None, color)
        if (maxIndex == index) {
          line = new AnimatedAnnotatedLine(newPos, childPos, Some(globeAnno), color)
        }
        layer.addRenderable(line)
        
        val target = new LineEventHandler(line, childTweet, (followThis && (maxIndex == index)), globeAnno, layer, color)
        val anim: Animator = new Animator(duration, target)
        twitterActor ! "starting one animation" 	// must be here and not begin, otherwise it doesnt work
        anim.start()
        
     	if (maxIndex == index) {
     	  wwd.getView.applyStateIterator(ScheduledOrbitViewStateIterator.createCenterIterator(newPos, childPos, duration, true))
        }
      
        index += 1
      })
    }
    
    def updateTreeLayers = {
      val maxNumTrees = 5;
      var initLayers: scala.List[Layer] = wwd.getModel.getLayers.toList
      var finalLayers: scala.List[Layer] = initLayers.dropRight(initLayers.length - initLayerCount)
      var renderLayers: scala.List[Layer] = Nil
      
      var alpha = 255
      initLayers = initLayers.drop(initLayerCount)
      initLayers = initLayers.drop(initLayers.length - maxNumTrees.toInt)
      initLayers.reverse.foreach( l => {
        /*
        if (l.isInstanceOf[RenderableLayer]) {
	      println("here")
          val renderables: scala.List[Renderable] = l.asInstanceOf[RenderableLayer].getRenderables.asInstanceOf[scala.Iterable[Renderable]].toList
	      println("here")
        //  renderables.foreach(r => println(r))
        }
                                        */
	       /* {
	          if (r.isInstanceOf[AnimatedAnnotatedLine]) {
	         /*    val aal = r.asInstanceOf[AnimatedAnnotatedLine]
	           val lc = aal.getColor
	            aal.setColor(new Color(lc.getRed, lc.getGreen, lc.getBlue, alpha - (255 - lc.getAlpha)))
	            val hc = aal.getHighlightColor
	            aal.setHighlightColor(new Color(hc.getRed, hc.getGreen, hc.getBlue, alpha - (255 - hc.getAlpha)))*/
	          } else if (r.isInstanceOf[GlobeAnnotation]) {
	        /*      val ga = r.asInstanceOf[GlobeAnnotation]
	            val gaa = ga.getAttributes
	          
	            val gaabc = gaa.getBorderColor
	            gaa.setBorderColor(new Color(gaabc.getRed, gaabc.getGreen, gaabc.getBlue, alpha - (255 - gaabc.getAlpha)))
	            
	            val gaatc = gaa.getTextColor
	            gaa.setTextColor(new Color(gaatc.getRed, gaatc.getGreen, gaatc.getBlue, alpha - (255 - gaatc.getAlpha)))
	
	            ga.setAttributes(gaa) */
	          }
	          
	        })*/

        
        renderLayers = renderLayers ++ scala.List(l)
        println(l.getOpacity + " " + l)
        alpha -= (255 / maxNumTrees)
      })
      
      finalLayers = finalLayers ++ renderLayers.reverse
      wwd.getModel.setLayers(new LayerList(finalLayers.toArray))
    }
    
    
    
  def getWwd: WorldWindowGLCanvas = {
     return wwd;
  }

  def getStatusBar: StatusBar = {
    return statusBar;
  }
    
    class LineEventHandler(line: AnimatedAnnotatedLine, childTweet: Tweet, followThis: Boolean, globeAnno: GlobeAnnotation, layer: RenderableLayer, color: Color) extends TimingTargetAdapter
    {
      override def begin = {
      }
      
      override def timingEvent(fraction: Float) = {
        line.updateLine(fraction)
      }
  
      override def end = {
        displayTweet(childTweet, followThis, globeAnno, layer, color)
        twitterActor ! "finished one animation"
      } 
    }	    
}






