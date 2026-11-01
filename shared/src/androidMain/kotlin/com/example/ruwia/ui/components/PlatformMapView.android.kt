package com.example.ruwia.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Int,
    onMapMoved: (latitude: Double, longitude: Double) -> Unit,
) {
    val currentOnMapMoved by rememberUpdatedState(onMapMoved)
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var lastSentLat by remember { mutableStateOf(latitude) }
    var lastSentLng by remember { mutableStateOf(longitude) }

    LaunchedEffect(latitude, longitude) {
        val latDiff = kotlin.math.abs(latitude - lastSentLat)
        val lngDiff = kotlin.math.abs(longitude - lastSentLng)
        if (latDiff > 0.0001 || lngDiff > 0.0001) {
            lastSentLat = latitude
            lastSentLng = longitude
            webViewRef.value?.evaluateJavascript(
                "if (window.setMapCenter) { window.setMapCenter($latitude, $longitude, $zoom); }",
                null
            )
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    clearCache(true)
                    setBackgroundColor(Color.parseColor("#E5E3DF"))

                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onLocationChanged(lat: Double, lng: Double) {
                            post {
                                lastSentLat = lat
                                lastSentLng = lng
                                currentOnMapMoved(lat, lng)
                            }
                        }
                    }, "AndroidBridge")

                    val html = buildLeafletHtml(latitude, longitude, zoom)
                    // Use unpkg.com as baseUrl so WebView has a valid secure origin context
                    loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "UTF-8", null)
                    webViewRef.value = this
                }
            },
            update = { wv ->
                webViewRef.value = wv
            }
        )
    }
}

private fun buildLeafletHtml(startLat: Double, startLng: Double, startZoom: Int): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<style>
  html, body, #map {
    height: 100%;
    width: 100%;
    margin: 0;
    padding: 0;
    background-color: #f2efe9;
  }
  .leaflet-control-attribution {
    display: none !important;
  }
</style>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</head>
<body>
<div id="map"></div>
<script>
  function initMap() {
    if (typeof L === 'undefined') {
      setTimeout(initMap, 150);
      return;
    }
    var map = L.map('map', {
      zoomControl: true,
      attributionControl: false
    }).setView([$startLat, $startLng], $startZoom);

    // Primary: OpenStreetMap Humanitarian (HOT) - 100% free, high clarity, NO API key required
    var primaryTiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
      subdomains: ['a', 'b', 'c'],
      maxZoom: 19
    });

    // Fallback 1: OSM France standard mirror - 100% free, NO API key required
    var fallbackTiles = L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
      subdomains: ['a', 'b', 'c'],
      maxZoom: 20
    });

    // Fallback 2: ESRI World Street Map - 100% free, NO API key required
    var esriTiles = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
      maxZoom: 19
    });

    primaryTiles.on('tileerror', function() {
      if (!map.hasLayer(fallbackTiles)) {
        map.removeLayer(primaryTiles);
        fallbackTiles.addTo(map);
      }
    });

    fallbackTiles.on('tileerror', function() {
      if (!map.hasLayer(esriTiles)) {
        map.removeLayer(fallbackTiles);
        esriTiles.addTo(map);
      }
    });

    primaryTiles.addTo(map);

    function notifyUserMoved() {
      var center = map.getCenter();
      if (window.AndroidBridge && window.AndroidBridge.onLocationChanged) {
        window.AndroidBridge.onLocationChanged(center.lat, center.lng);
      }
    }

    // CRITICAL: Only report location changes when the user physically drags or taps the map.
    // Never report on initialization or programmatic setMapCenter so we do not overwrite search input!
    map.on('dragend', notifyUserMoved);

    map.on('click', function(e) {
      map.panTo(e.latlng);
      setTimeout(notifyUserMoved, 250);
    });

    window.setMapCenter = function(lat, lng, zoom) {
      map.setView([lat, lng], zoom || map.getZoom(), { animate: false });
    };
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initMap);
  } else {
    initMap();
  }
</script>
</body>
</html>
""".trimIndent()
}
