package com.mapbox.mapboxandroiddemo.labs;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class InfoWindowSymbolLayerActivity extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener {

  private MapView mapView;
  private MapboxMap mapboxMap;
  private boolean markerSelected = false;
  private FeatureCollection mapLocationFeatureCollection;
  private HashMap<String, View> viewMap;
  private GeoJsonSource mapLocationsGeoJsonSource;
  private String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
  private String SELECTED_MARKER_GEOJSON_SOURCE_ID = "SELECTED_MARKER_GEOJSON_SOURCE_ID";
  private String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
  private String MAP_LOCATION_LAYER_ID = "MAP_LOCATION_LAYER_ID";
  private String SELECTED_MARKER_LAYER_ID = "SELECTED_MARKER_LAYER_ID";
  private String FEATURE_TITLE_PROPERTY_KEY = "FEATURE_TITLE_PROPERTY_KEY";
  private String FEATURE_DESCRIPTION_PROPERTY_KEY = "FEATURE_DESCRIPTION_PROPERTY_KEY";
  private String TAG = "InfoWindowSymbolLayerActivity";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_info_window_symbol_layer);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {

    this.mapboxMap = mapboxMap;

    // Create list of Feature objects
    List<Feature> mapLocationCoordinates = new ArrayList<>();

    // Create a single Feature location in Caracas, Venezuela
    Feature singleFeature = Feature.fromGeometry(Point.fromCoordinates(Position.fromCoordinates(
      -66.910519, 10.503250)));

    // Add a String property to the Feature to be used in the title of the popup bubble window
    singleFeature.addStringProperty(FEATURE_TITLE_PROPERTY_KEY, "Hello World!");
    singleFeature.addStringProperty(FEATURE_DESCRIPTION_PROPERTY_KEY, "Welcome to my marker");

    // Add the Feature to the List<> of Feature objects
    mapLocationCoordinates.add(singleFeature);

    // Add the list as a parameter to create a FeatureCollection
    mapLocationFeatureCollection = FeatureCollection.fromFeatures(mapLocationCoordinates);

    // Create a GeoJSON source with a unique ID and a FeatureCollection
    mapLocationsGeoJsonSource = new GeoJsonSource(GEOJSON_SOURCE_ID, mapLocationFeatureCollection);

    // Add the GeoJSON source to the map
    mapboxMap.addSource(mapLocationsGeoJsonSource);

    // Create a bitmap that will serve as the visual marker icon image
    Bitmap redMarkerIcon = BitmapFactory.decodeResource(
      InfoWindowSymbolLayerActivity.this.getResources(), R.drawable.red_marker);

    // Add the marker icon image to the map
    mapboxMap.addImage(MARKER_IMAGE_ID, redMarkerIcon);

    // Create a SymbolLayer with a unique id and a source. In this case, it's the GeoJSON source
    // that was created above. The red marker icon is added to the layer using run-time styling.
    SymbolLayer mapLocationSymbolLayer = new SymbolLayer(MAP_LOCATION_LAYER_ID, GEOJSON_SOURCE_ID)
      .withProperties(iconImage(MARKER_IMAGE_ID));
    mapboxMap.addLayer(mapLocationSymbolLayer);

    // Create an empty FeatureCollection that will eventually have the selected (i.e. tapped on) icon
    FeatureCollection emptyFeatureCollectionForSelectedIcon = FeatureCollection.fromFeatures(new Feature[] {});

    // Create a GeoJSONSource with a unique ID and the empty FeatureCollection created above
    GeoJsonSource selectedMarkerSource = new GeoJsonSource(
      SELECTED_MARKER_GEOJSON_SOURCE_ID, emptyFeatureCollectionForSelectedIcon);

    // Add the GeoJSONSource to the map
    mapboxMap.addSource(selectedMarkerSource);

    // Create a second SymbolLayer that will show any selected marker icons.
    SymbolLayer selectedMarkerSymbolLayer = new SymbolLayer(SELECTED_MARKER_LAYER_ID, SELECTED_MARKER_GEOJSON_SOURCE_ID)
      .withProperties(iconImage(MARKER_IMAGE_ID));

    // Add the layer to the map
    mapboxMap.addLayer(selectedMarkerSymbolLayer);

    // Initialize the map click listener
    mapboxMap.addOnMapClickListener(this);

    // Start the async task that creates the actual popup bubble window. This window will appear once a
    // SymbolLayer icon is tapped on.
    new GenerateViewIconTask(this, false, mapboxMap,
      mapLocationsGeoJsonSource).execute(mapLocationFeatureCollection);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {

    Log.d(TAG, "onMapClick: Clicked on map");

    final SymbolLayer markerSymbolLayer = (SymbolLayer) mapboxMap.getLayer(SELECTED_MARKER_LAYER_ID);

    final PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
    Log.d(TAG, "onMapClick: screenPoint = " + screenPoint);

    List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, MAP_LOCATION_LAYER_ID);
    List<Feature> selectedFeature = mapboxMap.queryRenderedFeatures(screenPoint, SELECTED_MARKER_LAYER_ID);

    if (selectedFeature.size() > 0 && markerSelected) {
      Log.d(TAG, "onMapClick: selectedFeature.size() > 0 && markerSelected");
      return;
    }

    if (features.isEmpty()) {
      Log.d(TAG, "onMapClick: features.isEmpty()");
      if (markerSelected) {
        Log.d(TAG, "onMapClick: markerSelected");
        runDeselectMarkerIconAnimation(markerSymbolLayer);
      }
      return;
    }

    FeatureCollection featureCollection = FeatureCollection.fromFeatures(
      new Feature[] {Feature.fromGeometry(features.get(0).getGeometry())});
    GeoJsonSource source = mapboxMap.getSourceAs(SELECTED_MARKER_GEOJSON_SOURCE_ID);
    if (source != null) {
      source.setGeoJson(featureCollection);
    }

    if (markerSelected) {
      runDeselectMarkerIconAnimation(markerSymbolLayer);
    }
    if (features.size() > 0) {
      runSelectMarkerIconAnimation(markerSymbolLayer);
    }

    if (!features.isEmpty()) {
      Log.d(TAG, "onMapClick: !features.isEmpty()");
      // we received a click event on the callout layer
      Feature feature = features.get(0);
      Log.d(TAG, "onMapClick: feature = " + feature.toString());
      PointF symbolScreenPoint = mapboxMap.getProjection().toScreenLocation(convertToLatLng(feature));
      Log.d(TAG, "onMapClick: symbolScreenPoint = " + symbolScreenPoint);

      handleClickCallout(feature, screenPoint, symbolScreenPoint);
    } else {
      // we didn't find a click event on callout layer, try clicking maki layer
      Timber.d("onMapClick: didn't find a click event on callout layer");
    }
  }

  private LatLng convertToLatLng(Feature feature) {
    Point symbolPoint = (Point) feature.getGeometry();
    Position position = symbolPoint.getCoordinates();
    return new LatLng(position.getLatitude(), position.getLongitude());
  }

  private void runSelectMarkerIconAnimation(final SymbolLayer symbolLayer) {
    ValueAnimator markerAnimator = new ValueAnimator();
    markerAnimator.setObjectValues(1f, 2f);
    markerAnimator.setDuration(300);
    markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        symbolLayer.setProperties(
          PropertyFactory.iconSize((float) animator.getAnimatedValue())
        );
      }
    });
    markerAnimator.start();
    markerSelected = true;
  }

  private void runDeselectMarkerIconAnimation(final SymbolLayer symbolLayer) {
    ValueAnimator markerAnimator = new ValueAnimator();
    markerAnimator.setObjectValues(2f, 1f);
    markerAnimator.setDuration(300);
    markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        symbolLayer.setProperties(
          PropertyFactory.iconSize((float) animator.getAnimatedValue())
        );
      }
    });
    markerAnimator.start();
    markerSelected = false;
  }

  /**
   * This method handles click events for callout symbols.
   * <p>
   * It creates a hit rectangle based on the the textView, offsets that rectangle to the location
   * of the symbol on screen and hit tests that with the screen point.
   * </p>
   *
   * @param feature           the feature that was clicked
   * @param screenPoint       the point on screen clicked
   * @param symbolScreenPoint the point of the symbol on screen
   */
  private void handleClickCallout(Feature feature, PointF screenPoint, PointF symbolScreenPoint) {
    Log.d(TAG, "handleClickCallout: ");
    if (viewMap != null) {
      View view = viewMap.get(feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY));
      View textContainer = view.findViewById(R.id.text_container);

      // create hit box for textView
      Rect hitRectText = new Rect();
      textContainer.getHitRect(hitRectText);

      // move hit box to location of symbol
      hitRectText.offset((int) symbolScreenPoint.x, (int) symbolScreenPoint.y);

      // offset vertically to match anchor behaviour
      hitRectText.offset(0, -view.getMeasuredHeight());

      // hit test if clicked point is in textview hit box
      if (!hitRectText.contains((int) screenPoint.x, (int) screenPoint.y)) {
        List<Feature> featureList = mapLocationFeatureCollection.getFeatures();
        for (int i = 0; i < featureList.size(); i++) {
          if (featureList.get(i).getStringProperty(
            FEATURE_TITLE_PROPERTY_KEY).equals(feature.getStringProperty(FEATURE_TITLE_PROPERTY_KEY))) {
          }
        }
      }
    } else {
      Log.d(TAG, "handleClickCallout: viewMap == null");
    }
  }

  private void refreshSource() {
    if (mapLocationsGeoJsonSource != null && mapLocationFeatureCollection != null) {
      mapLocationsGeoJsonSource.setGeoJson(mapLocationFeatureCollection);
    }
  }

  /**
   * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
   * <p>
   * Call be optionally be called to update the underlying data source after execution.
   * </p>
   * <p>
   * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
   * </p>
   */
  private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {

    private HashMap<String, View> viewMap = new HashMap<>();
    private final WeakReference<Activity> activityRef;
    private final boolean refreshSource;
    private final MapboxMap mapboxMapForViewGeneration;
    private String TAG = "RegularMapFragment";
    private GeoJsonSource source;
    private FeatureCollection asyncFeatureCollection;


    GenerateViewIconTask(Activity activity, boolean refreshSource,
                         MapboxMap map, GeoJsonSource source) {
      this.activityRef = new WeakReference<>(activity);
      this.refreshSource = refreshSource;
      this.mapboxMapForViewGeneration = map;
      this.source = source;
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {

      if (activityRef.get() != null) {
        HashMap<String, Bitmap> imagesMap = new HashMap<>();
        LayoutInflater inflater = LayoutInflater.from(activityRef.get());
        asyncFeatureCollection = params[0];

        for (Feature feature : asyncFeatureCollection.getFeatures()) {
          View view = inflater.inflate(R.layout.symbol_layer_info_window_layout_callout, null);

          String titleForBubbleWindow = feature.getStringProperty("FEATURE_TITLE_PROPERTY_KEY");
          TextView titleNumTextView = view.findViewById(R.id.symbol_layer_info_window_layout_callout_title);
          titleNumTextView.setText(titleForBubbleWindow);

          String descriptionForBubbleWindow = feature.getStringProperty("FEATURE_DESCRIPTION_PROPERTY_KEY");
          TextView descriptionNumTextView = view.findViewById(R.id.symbol_layer_info_window_layout_callout_description);
          descriptionNumTextView.setText(descriptionForBubbleWindow);

          Bitmap bitmap = SymbolGenerator.generate(view);
          imagesMap.put(titleForBubbleWindow, bitmap);
          viewMap.put(titleForBubbleWindow, view);
        }
        return imagesMap;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
      super.onPostExecute(bitmapHashMap);
      if (bitmapHashMap != null) {
        Log.d(TAG, "onPostExecute: bitmapHashMap != null");
        setImageGenResults(viewMap, bitmapHashMap);
        if (refreshSource) {
          refreshSource();
        }
      } else {
        Log.d(TAG, "onPostExecute: bitmapHashMap == null");
      }
    }

    /**
     * Invoked when the bitmaps have been generated from a view.
     */
    public void setImageGenResults(HashMap<String, View> viewMap, HashMap<String, Bitmap> imageMap) {
      if (mapboxMapForViewGeneration != null) {
        // calling addImages is faster as separate addImage calls for each bitmap.
        mapboxMapForViewGeneration.addImages(imageMap);
        Log.d(TAG, "setImageGenResults: images added");
      }
      // need to store reference to views to be able to use them as hit boxes for click events.
      this.viewMap = viewMap;
    }

    private void refreshSource() {
      if (source != null && asyncFeatureCollection != null) {
        source.setGeoJson(asyncFeatureCollection);
      }
    }
  }

  /**
   * Utility class to generate Bitmaps for Symbol.
   * <p>
   * Bitmaps can be added to the map with {@link com.mapbox.mapboxsdk.maps.MapboxMap#addImage(String, Bitmap)}
   * </p>
   */
  private static class SymbolGenerator {

    /**
     * Generate a Bitmap from an Android SDK View.
     *
     * @param view the View to be drawn to a Bitmap
     * @return the generated bitmap
     */
    static Bitmap generate(@NonNull View view) {
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      view.measure(measureSpec, measureSpec);

      int measuredWidth = view.getMeasuredWidth();
      int measuredHeight = view.getMeasuredHeight();

      view.layout(0, 0, measuredWidth, measuredHeight);
      Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.TRANSPARENT);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      return bitmap;
    }
  }

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
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}