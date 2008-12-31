package localhost

import gov.nasa.worldwind._
import gov.nasa.worldwind.util.StatusBar
import gov.nasa.worldwind.event._
import gov.nasa.worldwind.examples.ClickAndGoSelectListener
import gov.nasa.worldwind.examples.LayerPanel
import gov.nasa.worldwind.examples.StatisticsPanel
import gov.nasa.worldwind.avlist.AVKey
import gov.nasa.worldwind.awt.WorldWindowGLCanvas
import gov.nasa.worldwind.layers._
import gov.nasa.worldwind.layers.placename.PlaceNameLayer

import javax.swing._
import java.awt._

class AppPanel (canvasSize: Dimension, includeStatusBar: Boolean) extends JPanel
{
  var statusBar: StatusBar = new StatusBar();

  //parent(new BorderLayout);

  var wwd: WorldWindowGLCanvas = new WorldWindowGLCanvas();
  wwd.setPreferredSize(canvasSize);

  // Create the default model as described in the current worldwind properties.
  var m: Model = (WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME)).asInstanceOf[Model];
  wwd.setModel(m);

  // Setup a select listener for the worldmap click-and-go feature
  //wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

  add(this.wwd, BorderLayout.CENTER);

  if (includeStatusBar) {
//    var statusBar = new StatusBar();
    add(statusBar, BorderLayout.PAGE_END);
    statusBar.setEventSource(wwd);
  }

  def getWwd: WorldWindowGLCanvas = {
     return wwd;
  }

  def getStatusBar: StatusBar = {
    return statusBar;
  }
}