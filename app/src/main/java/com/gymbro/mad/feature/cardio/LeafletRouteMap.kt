package com.gymbro.mad.feature.cardio

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gymbro.mad.data.model.RoutePoint
import org.json.JSONArray

@Composable
@SuppressLint("SetJavaScriptEnabled")
internal fun LeafletRouteMap(points: List<RoutePoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestPoints = rememberUpdatedState(points)
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.userAgentString = "GymBroAndroid/0.1 (Android fitness app)"
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.evaluateJavascript("window.setRoute && window.setRoute(${pointsJson(latestPoints.value)});", null)
                }
            }
            loadDataWithBaseURL("https://gymbro.local/", mapHtml, "text/html", "UTF-8", null)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { view ->
            view.evaluateJavascript("window.setRoute && window.setRoute(${pointsJson(points)});", null)
        },
    )

    DisposableEffect(webView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.stopLoading()
            webView.destroy()
        }
    }
}

private fun pointsJson(points: List<RoutePoint>): String = JSONArray().apply {
    points.forEach { point ->
        put(JSONArray().put(point.latitude).put(point.longitude))
    }
}.toString()

private val mapHtml = """
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
<style>html,body,#map{height:100%;width:100%;margin:0;background:#141417}.leaflet-container{background:#1c1c20}.leaflet-control-attribution{font-size:10px}</style>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
</head><body><div id="map"></div><script>
var map=L.map('map',{zoomControl:true}).setView([20,0],2);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'}).addTo(map);
var routeLayer=L.layerGroup().addTo(map);
var hasCenteredOnRoute=false;
window.setRoute=function(points){
  routeLayer.clearLayers();
  if(!points||points.length===0)return;
  if(points.length===1){L.circleMarker(points[0],{radius:7,color:'#22ff88',fillOpacity:1}).addTo(routeLayer);map.setView(points[0],16);return;}
  L.polyline(points,{color:'#22ff88',weight:5,opacity:.95}).addTo(routeLayer);
  L.circleMarker(points[0],{radius:6,color:'#ffffff',fillColor:'#22ff88',fillOpacity:1}).addTo(routeLayer);
  L.circleMarker(points[points.length-1],{radius:7,color:'#ffffff',fillColor:'#ff5a1f',fillOpacity:1}).addTo(routeLayer);
  if(!hasCenteredOnRoute){map.fitBounds(L.latLngBounds(points),{padding:[24,24],maxZoom:17});hasCenteredOnRoute=true;}
  else map.panTo(points[points.length-1],{animate:false});
};
</script></body></html>
""".trimIndent()
