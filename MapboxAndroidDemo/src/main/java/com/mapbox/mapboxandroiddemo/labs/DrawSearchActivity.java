package com.mapbox.mapboxandroiddemo.labs;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;

public class DrawSearchActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener {

  private MapView mapView;
  private MapboxMap mapboxMap;
  private GeoJsonSource sourceForSelectedPolygonArea;
  private GeoJsonSource sourceForCircleTouchPoints;
  private FillLayer selectedAreaFillLayerPolygon;
  private CircleLayer circleLayerOfTouchPoints;

  private FeatureCollection circleFeatureCollection;
  private FeatureCollection polygonAreaFeatureCollection;
  private Feature[] circleFeatureList;
  private Feature[] polygonFeatureList;
  private static final String CIRCLE_LAYER_GEOJSON_SOURCE_ID = "selected-points-for-circle-geojson-id";
  private static final String CIRCLE_LAYER_ID = "selected-points-for-circle-source-id";
  private static final String SELECTED_SOURCE_LAYER_ID = "selected-area-source-id";
  private static final String SELECTED_SOURCE_GEOJSON_ID = "selected-area-source-id";

  private int anchorPointNum;
  private boolean inDrawingMode;
  private boolean inPolygonTapMode;
  private String TAG = "DrawSearchActivity";
  private List<LatLng> polygon;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_draw_search);

    circleFeatureList = new Feature[0];
    polygonFeatureList = new Feature[0];
    circleFeatureCollection = FeatureCollection.fromFeatures(circleFeatureList);
    polygonAreaFeatureCollection = FeatureCollection.fromFeatures(polygonFeatureList);

    polygon = new ArrayList<>();
    anchorPointNum = 0;

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    DrawSearchActivity.this.mapboxMap = mapboxMap;
    setUpCircleSourceAndLayer();
    setUpPolygonSourceAndLayer();
    mapboxMap.addOnMapClickListener(this);
  }

  private void setUpPolygonSourceAndLayer() {
    sourceForSelectedPolygonArea = new GeoJsonSource(SELECTED_SOURCE_GEOJSON_ID);
    mapboxMap.addSource(sourceForSelectedPolygonArea);
    selectedAreaFillLayerPolygon = new FillLayer(SELECTED_SOURCE_LAYER_ID, SELECTED_SOURCE_GEOJSON_ID);
    mapboxMap.addLayer(selectedAreaFillLayerPolygon);
  }

  private void setUpCircleSourceAndLayer() {
    sourceForCircleTouchPoints = new GeoJsonSource(CIRCLE_LAYER_GEOJSON_SOURCE_ID);
    mapboxMap.addSource(sourceForCircleTouchPoints);
    circleLayerOfTouchPoints = new CircleLayer(CIRCLE_LAYER_ID, CIRCLE_LAYER_GEOJSON_SOURCE_ID);
    circleLayerOfTouchPoints.setProperties(
      circleRadius(9f),
      circleColor(Color.BLUE)
    );

    mapboxMap.addLayer(circleLayerOfTouchPoints);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {

    addClickPointAsCircleLayerAnchor(point);
  }


  private void addClickPointAsCircleLayerAnchor(LatLng point) {
    if (anchorPointNum <= 3) {
      if (sourceForCircleTouchPoints != null && circleFeatureCollection != null) {
        if (circleFeatureCollection.features().size() == 0) {
          Log.d(TAG, "addClickPointAsCircleLayerAnchor: polygonAreaFeatureCollection.features()().size() == 0");
          circleFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
            Point.fromLngLat(point.getLongitude(), point.getLatitude()))});

        } else {
          Log.d(TAG, "addClickPointAsCircleLayerAnchor: polygonAreaFeatureCollection.features()().size() == " +
            +polygonAreaFeatureCollection.features().size());

          polygonAreaFeatureCollection.features().add(Feature.fromGeometry(
            Point.fromLngLat(point.getLongitude(), point.getLatitude())));
        }
        sourceForCircleTouchPoints.setGeoJson(circleFeatureCollection);
        anchorPointNum++;
      }
    } else {
      mapboxMap.clear();
      circleFeatureCollection.features().clear();
      sourceForCircleTouchPoints.setGeoJson(circleFeatureCollection);
      anchorPointNum = 0;
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_finger_draw, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_tap_on_map:
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
        inPolygonTapMode = true;
        inDrawingMode = false;
        return true;
      case R.id.menu_draw_on_map:
        mapboxMap.getUiSettings().setAllGesturesEnabled(false);
        inDrawingMode = true;
        inPolygonTapMode = false;
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}