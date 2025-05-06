package com.example.traveltdea;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private TextView txtDistance;
    private TextView txtTime;
    private TextView txtDistanceIcon;
    private TextView txtTimeIcon;
    private View cardInfo;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long ROUTE_UPDATE_INTERVAL_MS = 5000;

    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private LocationManager locationManager;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private GeoPoint currentLocation;
    private GeoPoint destinationPoint;
    private boolean isFirstLocation = true;
    private long lastRouteTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        // MOVER INICIALIZACIONES AQUÍ, DESPUÉS DE setContentView()
        txtDistance = findViewById(R.id.txtDistance);
        txtTime = findViewById(R.id.txtTime);
        txtDistanceIcon = findViewById(R.id.txtDistanceIcon);
        txtTimeIcon = findViewById(R.id.txtTimeIcon);
        cardInfo = findViewById(R.id.cardInfo);
        mapView = findViewById(R.id.mapView);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        mapView.getOverlays().add(locationOverlay);

        CompassOverlay compass = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compass.enableCompass();
        mapView.getOverlays().add(compass);

        routePolyline = new Polyline();
        routePolyline.setWidth(8f);
        routePolyline.getOutlinePaint().setStrokeWidth(12f);
        routePolyline.getOutlinePaint().setAlpha(100);
        mapView.getOverlays().add(routePolyline);

        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                setDestinationPoint(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapView.getOverlays().add(new MapEventsOverlay(receiver));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        requestLocationPermissions();
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        try {
            if (locationManager != null
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location loc) {
        if (loc == null) return;
        currentLocation = new GeoPoint(loc.getLatitude(), loc.getLongitude());

        if (isFirstLocation) {
            mapController.setCenter(currentLocation);
            isFirstLocation = false;
        }

        if (destinationPoint != null) {
            long now = System.currentTimeMillis();
            if (now - lastRouteTimestamp > ROUTE_UPDATE_INTERVAL_MS) {
                lastRouteTimestamp = now;
                calculateRoute();
            }
        }
    }

    @Override public void onStatusChanged(String s, int i, Bundle b) {}
    @Override public void onProviderEnabled(String p) {}
    @Override public void onProviderDisabled(String p) {}

    private void setDestinationPoint(GeoPoint p) {
        destinationPoint = p;

        runOnUiThread(() -> {
            if (destinationMarker != null) {
                mapView.getOverlays().remove(destinationMarker);
            }

            destinationMarker = new Marker(mapView);
            destinationMarker.setPosition(destinationPoint);
            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            destinationMarker.setTitle("Destino");
            mapView.getOverlays().add(destinationMarker);
            mapView.invalidate();

            if (currentLocation != null) {
                lastRouteTimestamp = System.currentTimeMillis();
                calculateRoute();
            } else {
                txtDistance.setText("Esperando ubicación...");
                txtTime.setText("");
                txtDistanceIcon.setText("0 km");
                txtTimeIcon.setText("0 min");
                cardInfo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void calculateRoute() {
        if (currentLocation == null || destinationPoint == null) return;
        if (Double.isNaN(currentLocation.getLatitude()) || Double.isNaN(destinationPoint.getLatitude()))
            return;

        OkHttpClient client = new OkHttpClient();
        String apiKey = "c4b2c2c4-c2fe-4d0c-bf1f-140e90757e67";

        String url = HttpUrl.parse("https://graphhopper.com/api/1/route").newBuilder()
                .addQueryParameter("point", currentLocation.getLatitude() + "," + currentLocation.getLongitude())
                .addQueryParameter("point", destinationPoint.getLatitude() + "," + destinationPoint.getLongitude())
                .addQueryParameter("vehicle", "car")
                .addQueryParameter("points_encoded", "false")
                .addQueryParameter("key", apiKey)
                .build().toString();

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    txtDistance.setText("Sin ruta");
                    txtTime.setText("");
                    txtDistanceIcon.setText("0 km");
                    txtTimeIcon.setText("0 min");
                    cardInfo.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        txtDistance.setText("Sin ruta");
                        txtTime.setText("");
                        txtDistanceIcon.setText("0 km");
                        txtTimeIcon.setText("0 min");
                        cardInfo.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                try {
                    String json = response.body().string();
                    JSONObject body = new JSONObject(json);

                    if (!body.has("paths")) throw new JSONException("Falta 'paths'");

                    JSONArray paths = body.getJSONArray("paths");
                    if (paths.length() == 0) throw new JSONException("Lista de rutas vacía");

                    JSONObject path = paths.getJSONObject(0);
                    double distanceMeters = path.getDouble("distance");
                    long timeMillis = path.getLong("time");

                    JSONObject points = path.getJSONObject("points");
                    JSONArray coords = points.getJSONArray("coordinates");
                    List<GeoPoint> pts = new ArrayList<>();

                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray pt = coords.getJSONArray(i);
                        pts.add(new GeoPoint(pt.getDouble(1), pt.getDouble(0)));
                    }

                    String distanciaKm = String.format("%.2f km", distanceMeters / 1000.0);
                    String tiempoMin = String.format("%.0f min", timeMillis / 60000.0);

                    runOnUiThread(() -> {
                        routePolyline.setPoints(pts);
                        mapView.invalidate();
                        txtDistance.setText("");
                        txtTime.setText("");
                        txtDistanceIcon.setText(distanciaKm);
                        txtTimeIcon.setText(tiempoMin);
                        cardInfo.setVisibility(View.VISIBLE);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        txtDistance.setText("Sin ruta");
                        txtTime.setText("");
                        txtDistanceIcon.setText("0 km");
                        txtTimeIcon.setText("0 min");
                        cardInfo.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}



