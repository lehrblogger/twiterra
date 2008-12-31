$Id: README.txt 5284 2008-05-03 00:09:27Z patrickmurris $

To run the basic demo on Mac OS X or Linux while in the release directory:
    chmod +x run-demo.bash
    ./run-demo.bash gov.nasa.worldwind.examples.ApplicationTemplate
or on Windows
    java -Dsun.java2d.noddraw=true -classpath worldwind.jar;jogl.jar;gluegen-rt.jar gov.nasa.worldwind.examples.ApplicationTemplate

Your computer must have a modern graphics card with an up-to-date driver.  The
source of most getting-up-and-running problems is an out-of-date graphics driver.
To get an updated driver, visit your graphics card manufacturer's web site.  This
will most likely be either NVIDIA, ATI or Intel. The drivers are typically under
a link named "Downloads" or "Support". If your computer is a laptop, then updated
drivers are probably at the laptop manufacturer's web site rather than the graphics
card manufacturer's.

Change Summary for 0.4 to 0.5:

- Includes a WMS server.

- Major changes to the view code - the eye can now go very close to
  the ground, and underwater. New interface methods.
- New Restorable interface to save and restore objects state to/from an xml
  document. Implemented in UserFacingIcons, Annotations, Polyline, View...
- Flat Worlds with projection switching are now usable.
- Mars and Moon globes with elevations and full layersets from NASA servers.
- MGRS, UTM and TM coordinates classes and converters in geom.coords
- Tiled image layers will not wait for lower res tiles to load before
  showing the needed ones.
- New layers:
  - NAIPCalifornia.
  - BMNGWMSLayer gives access to any of the 12 BMNG 2004 layers.
  - OpenStreeMapLayer.
  - MGRSGraticuleLayer and UTMGraticuleLayer.
  - CrosshairLayer.
- All non Earth specific layers have been moved from layers.Earth to layers:
  CrosshairLayer, FogLayer, ScalebarLayer, SkyColorLayer, SkyGradientLayer,
  StarsLayer, TerrainProfileLayer and WorldMapLayer.
- StatusBar moved from examples to util.
- New GeographicText support - used for placenames.
- More accurate scalebar.
- Increased performance for Polyline.
- Icons can have a background image.
- WWJApplet example updated with new capabilities.
- Build script completly revised.
- SurfaceImage from an http source.
- Zoom with middle mouse button down and drag up/down.
- AlwaysOnTop property for icons and annotations.
- New Mipmap flag for TiledImageLayer
- Better TiledImageLayer image capture and composition.
- Enhanced NITFS/RPF support.
- Better gps tracks support
- New examples: AlarmIcons, BMNGTwelveMonth, FlatWorldEarthquakes, MGRSGraticule,
  RemoteSurfaceImage, ViewLookAround, Mars, Moon...
- Also includes an application for Search And Rescue support.

- Many other bug fixes and changes...

