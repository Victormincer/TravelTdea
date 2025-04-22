package com.example.traveltdea;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private IMapController mapController;
    private LocationManager locationManager;
    private Button btnCalculateRoute;
    private Button btnAddDestination;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private GeoPoint currentLocation;
    private GeoPoint destinationPoint;
    private boolean isFirstLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        requestLocationPermissions();

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

        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        btnCalculateRoute = findViewById(R.id.btnCalculateRoute);
        btnAddDestination = findViewById(R.id.btnAddDestination);

        routePolyline = new Polyline();
        routePolyline.setColor(0xFF0000FF);
        routePolyline.setWidth(10f);
        mapView.getOverlays().add(routePolyline);

        btnAddDestination.setOnClickListener(v -> addRandomDestination());
        btnCalculateRoute.setOnClickListener(v -> calculateRoute());

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Se requieren permisos de ubicación para el funcionamiento de la app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (locationManager != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        }
    }

    private void addRandomDestination() {
        if (currentLocation == null) {
            Toast.makeText(this, "Esperando ubicación actual...", Toast.LENGTH_SHORT).show();
            return;
        }

        Random random = new Random();
        double latOffset = (random.nextDouble() - 0.5) * 0.05;
        double lonOffset = (random.nextDouble() - 0.5) * 0.05;

        destinationPoint = new GeoPoint(
                currentLocation.getLatitude() + latOffset,
                currentLocation.getLongitude() + lonOffset);

        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
        }

        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(destinationPoint);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setTitle("Destino");
        mapView.getOverlays().add(destinationMarker);
        mapView.invalidate();

        Toast.makeText(this, "Destino añadido", Toast.LENGTH_SHORT).show();
    }

    private void calculateRoute() {
        if (currentLocation == null || destinationPoint == null) {
            Toast.makeText(this, "Necesitas una ubicación actual y un destino", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String apiKey = "c4b2c2c4-c2fe-4d0c-bf1f-140e90757e67"; // Reemplaza con tu clave
        String baseUrl = "https://graphhopper.com/api/1/route";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
        urlBuilder.addQueryParameter("point", currentLocation.getLatitude() + "," + currentLocation.getLongitude());
        urlBuilder.addQueryParameter("point", destinationPoint.getLatitude() + "," + destinationPoint.getLongitude());
        urlBuilder.addQueryParameter("vehicle", "car");
        urlBuilder.addQueryParameter("locale", "es");
        urlBuilder.addQueryParameter("instructions", "true");
        urlBuilder.addQueryParameter("points_encoded", "false");
        urlBuilder.addQueryParameter("key", apiKey);

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error al calcular la ruta: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray paths = jsonResponse.getJSONArray("paths");

                        if (paths.length() > 0) {
                            JSONObject path = paths.getJSONObject(0);
                            double distance = path.getDouble("distance");
                            double time = path.getDouble("time");

                            JSONObject pointsJson = path.getJSONObject("points");
                            JSONArray coordinates = pointsJson.getJSONArray("coordinates");

                            List<GeoPoint> decodedPath = decodePolyline(coordinates);

                            runOnUiThread(() -> {
                                drawRouteOnMap(decodedPath);
                                String info = String.format("Distancia: %.1f km, Tiempo: %.1f min", distance / 1000, time / 60000);
                                Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("RouteParsing", "Error al analizar la ruta", e);
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error al analizar los datos de la ruta", Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error en la respuesta: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }

            private List<GeoPoint> decodePolyline(JSONArray coordinates) {
                List<GeoPoint> poly = new ArrayList<>();
                try {
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);
                        double lon = point.getDouble(0);
                        double lat = point.getDouble(1);
                        poly.add(new GeoPoint(lat, lon));
                    }
                } catch (JSONException e) {
                    Log.e("DecodePolyline", "Error decodificando puntos", e);
                }
                return poly;
            }

            private void drawRouteOnMap(List<GeoPoint> decodedPath) {
                routePolyline.setPoints(decodedPath);
                mapView.invalidate();
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (isFirstLocation) {
            mapController.setCenter(currentLocation);
            isFirstLocation = false;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

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
            locationManager.removeUpdates(this);
        }
    }
}


