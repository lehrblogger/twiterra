package localhost

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
