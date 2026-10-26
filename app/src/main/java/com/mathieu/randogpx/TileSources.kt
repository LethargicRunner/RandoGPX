package com.mathieu.randogpx

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

object TileSources {

    val OPENSTREETMAP = TileSourceFactory.MAPNIK
    val OPENTOPOMAP = TileSourceFactory.OPEN_TOPO_MAP

    /** Plan IGN v2 — gratuit, sans clé, via la Géoplateforme (data.geopf.fr). */
    val IGN_PLAN: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "IGN Plan", 0, 18, 256, ".png",
        arrayOf("https://data.geopf.fr"), "© IGN"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl +
                "/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                "&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2" +
                "&STYLE=normal&TILEMATRIXSET=PM&FORMAT=image/png" +
                "&TILEMATRIX=" + z + "&TILEROW=" + y + "&TILECOL=" + x
        }
    }

    fun nameOf(source: org.osmdroid.tileprovider.tilesource.ITileSource): String =
        when (source.name()) {
            "IGN Plan" -> "IGN Plan (topo France)"
            "OpenTopoMap" -> "OpenTopoMap (courbes de niveau)"
            else -> "OpenStreetMap (standard)"
        }
}
