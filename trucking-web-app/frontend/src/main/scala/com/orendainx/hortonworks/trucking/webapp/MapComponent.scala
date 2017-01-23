package com.orendainx.hortonworks.trucking.webapp

import angulate2.std.{Component, OnInit}
import com.felstar.scalajs.leaflet._
import com.orendainx.hortonworks.trucking.common.models.TruckDataTypes

import scala.collection.mutable

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
@Component(
  selector = "map-component",
  templateUrl = "/assets/templates/map.component.html"
)
class MapComponent(webSocketService: WebSocketService) extends OnInit {

  // Map options
  private val MaxMarkers = 30
  private val CenterView = (40.0, -90.0)
  private val DefaultZoom = 6
  private val MinZoom = 5
  private val MaxZoom = 7
  private val MapBoxAccessToken = "pk.eyJ1Ijoib3JlbmRhaW4iLCJhIjoiY2l4cWwwYTJrMDkwZTMwbHZ1MG8wYmkxZiJ9.J0uY8A4pTzlcfhc0oUyebg"

  // Marker options
  private val MarkerFillOpacity = 0.5
  private val MarkerRadius = 10000
  private val MarkerBorderWeight = 2
  private val MarkerTypeColors = Map[String, String](
    TruckDataTypes.Normal -> "#0f0",
    TruckDataTypes.Speeding -> "#f00",
    TruckDataTypes.LaneDeparture -> "#ff0",
    TruckDataTypes.UnsafeFollowDistance -> "#0ff",
    TruckDataTypes.UnsafeTailDistance -> "#00f"
  )

  // Collections
  private val events = mutable.Buffer.empty[PrettyTruckAndTrafficData]
  private val markers = mutable.Buffer.empty[Layer] // TODO: best collection for trimming/appending?
  private var lmap: LMap = _

  override def ngOnInit(): Unit = {
    lmap = create("trucking-map")
    webSocketService.registerCallback(addEvent _)
  }

  def addEvent(event: PrettyTruckAndTrafficData): Unit = {
    events += event
    addMarker(event)
  }

  def create(el: String): LMap = {
    val map = L.map(el, LMapOptions.scrollWheelZoom(false)).setView(CenterView, DefaultZoom)

    val tileLayer =
    //L.tileLayer(s"https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=$MapBoxAccessToken",
      L.tileLayer(s"https://api.mapbox.com/styles/v1/orendain/cixwfdcue00112splqmktu0e9/tiles/256/{z}/{x}/{y}?access_token=$MapBoxAccessToken",
        TileLayerOptions.id("mapbox.streets").maxZoom(MaxZoom).minZoom(MinZoom)
          .attribution("""<a href="https://github.com/orendain/trucking-iot">Source Code</a>""".stripMargin)
      ).addTo(map)

    map
  }

  /** Create marker and add it to the map.
    * This method will automatically remove marker layers from the map in a FIFO fashion
    * when MaxMarkers markers are already displayed.
    *
    * @param event The PrettyTruckAndTrafficData event to add to the map
    * @return The changed [[LMap]] with the new marker, for chaining purposes.
    */
  def addMarker(event: PrettyTruckAndTrafficData): LMap = {
    val color = MarkerTypeColors(event.eventType)
    val circle = L.circle((event.latitude.toDouble, event.longitude.toDouble),
      CircleOptions.color(color).weight(MarkerBorderWeight).fillColor(color).fillOpacity(MarkerFillOpacity).radius(MarkerRadius)
    ).bindPopup(markerMarkup(event)).addTo(lmap)

    markers += circle
    if (markers.size > MaxMarkers) {
      lmap.removeLayer(markers(0))
      markers.trimStart(1)
    }

    lmap
  }

  private def markerMarkup(event: PrettyTruckAndTrafficData) = {
    s"""<b>Driver:</b> ${event.driverName}<br>
       |<b>Route:</b> ${event.routeName}<br>
       |<b>Event:</b> ${event.eventType}""".stripMargin
  }

}