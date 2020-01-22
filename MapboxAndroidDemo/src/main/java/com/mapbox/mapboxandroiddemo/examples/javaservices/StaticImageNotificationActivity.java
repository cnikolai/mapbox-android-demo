package com.mapbox.mapboxandroiddemo.examples.javaservices;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import com.mapbox.api.staticmap.v1.StaticMapCriteria;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.MainActivity;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import static android.app.PendingIntent.getActivity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Test activity showing how to use a the {@link MapSnapshotter}
 * in a way that utilizes provided bitmaps in native notifications.
 */
public class StaticImageNotificationActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final String MARKER_SOURCE_ID = "marker-source-id";
  private static final String MARKER_LAYER_ID = "marker-layer-id";
  private static final String MARKER_ICON_ID = "marker-icon-id";
  private static final String WIFI_SPOT_SOURCE_ID = "wifi-source-id";
  private static final String WIFI_SPOT_ICON_ID = "wifi-icon-id";
  private static final String WIFI_SPOT_LAYER_ID = "wifi-layer-id";
  private static final String ROUTE_SOURCE_ID = "route-source-id";
  private static final String ROUTE_LINE_LAYER_ID = "route-layer-id";
  private static final String NOTIFICATION_CHANNEL_ID = "channel_id";
  private static final double STATIC_IMAGE_ZOOM = 10.0;
  private static final int IMAGE_WIDTH = 400;
  private static final int IMAGE_HEIGHT = 400;
  private static final int ANIMATION_DURATION_MILLIS = 3000;
  private int count = 0;
  private Handler handler;
  private Runnable runnable;
  private GeoJsonSource markerSource;
  private ValueAnimator markerIconAnimator;
  private LatLng markerIconCurrentLocation;
  private List<Point> routeCoordinateList;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private NotificationManager notificationManager;
  private Target picassoTarget;

  /**
   * Listener interface for when the ValueAnimator provides an updated value
   */
  private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
    new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        LatLng animatedPosition = (LatLng) valueAnimator.getAnimatedValue();
        if (markerSource != null) {
          markerSource.setGeoJson(Point.fromLngLat(
            animatedPosition.getLongitude(), animatedPosition.getLatitude()));
        }
//        getStaticImageFromApi(new LatLng(animatedPosition.getLatitude(), animatedPosition.getLongitude()));
      }
    };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_static_image_api_notification);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);

    // Set a callback for when MapboxMap is ready to be used
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(@NonNull final MapboxMap mapboxMap) {
    StaticImageNotificationActivity.this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(Style.DARK, new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        for (Layer singleLayer : style.getLayers()) {
          Log.d("SNA", singleLayer.getId());
        }
        initData(style);
      }
    });
  }

  private void initData(@NonNull Style fullyLoadedStyle) {
    initSources(fullyLoadedStyle);
    initMarkerSymbolLayer(fullyLoadedStyle);
    initWifiSpotSymbolLayer(fullyLoadedStyle);
    initRoutePathLineLayer(fullyLoadedStyle);
    initRunnable();
  }

  /**
   * Creates bitmap from given parameters, and creates a notification with that bitmap
   *
   * @param staticImageCenterLatLng coordinates for center of static image
   */
  private void getStaticImageFromApi(LatLng staticImageCenterLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {

        picassoTarget = new Target() {
          @Override
          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            createNotification(bitmap);
          }

          @Override
          public void onBitmapFailed(Drawable errorDrawable) {
            Toast.makeText(StaticImageNotificationActivity.this, "", Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onPrepareLoad(Drawable placeHolderDrawable) {
          }
        };

        Picasso.with(StaticImageNotificationActivity.this).load(
          buildStaticImageUrl(staticImageCenterLatLng,
            StaticMapCriteria.DARK_STYLE, IMAGE_WIDTH, IMAGE_HEIGHT)
            .url().toString()).into(picassoTarget);
      }
    });
  }

  private MapboxStaticMap buildStaticImageUrl(LatLng staticImageCenterLatLng, String styleUrl, int width, int height) {
    return MapboxStaticMap.builder()
      .accessToken(getString(R.string.access_token))
      .styleId(styleUrl)
      .cameraPoint(Point.fromLngLat(staticImageCenterLatLng.getLongitude(), staticImageCenterLatLng.getLatitude()))
      .cameraZoom(STATIC_IMAGE_ZOOM)
      .width(width)
      .height(height)
      .retina(true)
      .build();
  }

  /**
   * Creates a notification with given bitmap as a large icon
   *
   * @param bitmap to set as large icon
   */
  private void createNotification(Bitmap bitmap) {
    final int notifyId = 1002;
    if (notificationManager == null) {
      notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notificationManager != null) {
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
        if (notificationChannel == null) {
          notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
            "channel_name", NotificationManager.IMPORTANCE_HIGH);
          notificationChannel.setDescription("channel_description");
          notificationManager.createNotificationChannel(notificationChannel);
        }
      }
    }
    Intent intent = new Intent(this, MainActivity.class)
      .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle("content")
      .setSmallIcon(R.drawable.ic_circle)
      .setContentTitle(getString(R.string.static_image_api_notification_title))
      .setContentText(String.format(getString(R.string.static_image_api_notification_description), "5", 5))
      .setContentIntent(getActivity(this, 0, intent, 0))
      .setLargeIcon(bitmap);
    Notification notification = builder.build();
    notificationManager.notify(notifyId, notification);
  }

  /**
   * Add various sources to the map.
   */
  private void initSources(@NonNull Style loadedMapStyle) {
    loadedMapStyle.addSource(new VectorSource(WIFI_SPOT_SOURCE_ID, "mapbox://appsatmapboxcom.bml2ioc4"));
    markerSource = new GeoJsonSource(MARKER_SOURCE_ID);
    loadedMapStyle.addSource(markerSource);
    FeatureCollection lineStringRouteFeatureCollection = FeatureCollection.fromJson(
      "{\n" +
        "  \"type\": \"FeatureCollection\",\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"type\": \"Feature\",\n" +
        "      \"properties\": {},\n" +
        "      \"geometry\": {\n" +
        "        \"type\": \"LineString\",\n" +
        "        \"coordinates\": [\n" +
        "          [\n" +
        "            -73.991668,\n" +
        "            40.754723\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.987355,\n" +
        "            40.752906\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.987522,\n" +
        "            40.752214\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.987814,\n" +
        "            40.750689\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.987264,\n" +
        "            40.750468\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.988702,\n" +
        "            40.748509\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.988185,\n" +
        "            40.748262\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989223,\n" +
        "            40.742655\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989231,\n" +
        "            40.742596\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989226,\n" +
        "            40.74255\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989211,\n" +
        "            40.742517\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989158,\n" +
        "            40.742491\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.98906,\n" +
        "            40.742454\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.988924,\n" +
        "            40.742428\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.989597,\n" +
        "            40.741548\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.98643,\n" +
        "            40.740214\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.987777,\n" +
        "            40.738342\n" +
        "          ],\n" +
        "          [\n" +
        "            -73.986126,\n" +
        "            40.737643\n" +
        "          ]\n" +
        "        ]\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"
    );
    LineString lineString = (LineString) lineStringRouteFeatureCollection.features().get(0).geometry();
    routeCoordinateList = lineString.coordinates();
    loadedMapStyle.addSource(new GeoJsonSource(ROUTE_SOURCE_ID, lineStringRouteFeatureCollection));
  }

  /**
   * Add the marker icon SymbolLayer.
   */
  private void initMarkerSymbolLayer(@NonNull Style loadedMapStyle) {
    loadedMapStyle.addImage(MARKER_ICON_ID, BitmapUtils.getBitmapFromDrawable(
      getResources().getDrawable(R.drawable.pink_dot)));

    SymbolLayer markerSymbolLayer = new SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
      iconImage(MARKER_ICON_ID),
      iconSize(1f),
      iconIgnorePlacement(true),
      iconAllowOverlap(true)
    );
//    markerSymbolLayer.setSourceLayer("NYC_Wi-Fi_Hotspot_Locations-8qwm7n");
    loadedMapStyle.addLayer(markerSymbolLayer);
  }

  /**
   * Add the marker icon SymbolLayer.
   */
  private void initWifiSpotSymbolLayer(@NonNull Style loadedMapStyle) {
    loadedMapStyle.addImage(WIFI_SPOT_ICON_ID, BitmapUtils.getBitmapFromDrawable(
      getResources().getDrawable(R.drawable.ic_circle)));

    SymbolLayer wifiSymbolLayer = new SymbolLayer(WIFI_SPOT_LAYER_ID, WIFI_SPOT_SOURCE_ID).withProperties(
      iconImage(WIFI_SPOT_ICON_ID),
      iconSize(.2f),
      iconIgnorePlacement(true),
      iconAllowOverlap(true)
    );

    wifiSymbolLayer.setSourceLayer("NYC_Wi-Fi_Hotspot_Locations-8qwm7n");
    loadedMapStyle.addLayerBelow(wifiSymbolLayer, MARKER_LAYER_ID);
  }

  /**
   * Add the LineLayer for the marker icon's travel route.
   */
  private void initRoutePathLineLayer(@NonNull Style loadedMapStyle) {
    LineLayer routeLineLayer = new LineLayer(ROUTE_LINE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
      lineColor(Color.parseColor("#a6f13c")),
      lineOpacity(.7f),
      lineWidth(4f)
    );
    if (loadedMapStyle.getLayer("road-label") != null) {
      loadedMapStyle.addLayerBelow(routeLineLayer, "road-label");
    } else {
      loadedMapStyle.addLayerBelow(routeLineLayer, WIFI_SPOT_LAYER_ID);
    }
  }

  /**
   * Set up the repeat logic for moving the icon along the route.
   */
  private void initRunnable() {
    // Animating the marker requires the use of both the ValueAnimator and a handler.
    // The ValueAnimator is used to move the marker between the GeoJSON points, this is
    // done linearly. The handler is used to move the marker along the GeoJSON points.
    handler = new Handler();
    runnable = new Runnable() {
      @Override
      public void run() {
        // Check if we are at the end of the points list, if so we want to stop using
        // the handler.
        if ((routeCoordinateList.size() - 1 > count)) {

          Point nextLocation = routeCoordinateList.get(count + 1);

          if (markerIconAnimator != null && markerIconAnimator.isStarted()) {
            markerIconCurrentLocation = (LatLng) markerIconAnimator.getAnimatedValue();
            markerIconAnimator.cancel();
          }
          markerIconAnimator = ObjectAnimator
            .ofObject(latLngEvaluator, count == 0 || markerIconCurrentLocation == null
                ? new LatLng(40.754723, -73.991668)
                : markerIconCurrentLocation,
              new LatLng(nextLocation.latitude(), nextLocation.longitude()))
            .setDuration(ANIMATION_DURATION_MILLIS);
          markerIconAnimator.setInterpolator(new LinearInterpolator());
          markerIconAnimator.addUpdateListener(animatorUpdateListener);
          markerIconAnimator.start();

          // Keeping the current point count we are on.
          count++;

          // Once we finish we need to repeat the entire process by executing the
          // handler again once the ValueAnimator is finished.
          handler.postDelayed(this, 3000);
        }
      }
    };
    handler.post(runnable);
  }


  /**
   * Method is used to interpolate the SymbolLayer icon animation.
   */
  private static final TypeEvaluator<LatLng> latLngEvaluator = new TypeEvaluator<LatLng>() {

    private final LatLng latLng = new LatLng();

    @Override
    public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
      latLng.setLatitude(startValue.getLatitude()
        + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
      latLng.setLongitude(startValue.getLongitude()
        + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
      return latLng;
    }
  };

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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    Picasso.with(this).cancelRequest(picassoTarget);
    mapView.onDestroy();
    super.onDestroy();
  }
}
