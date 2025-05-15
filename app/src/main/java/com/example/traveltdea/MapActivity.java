package com.example.traveltdea;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private TextView txtDistance, txtTime, txtDistanceIcon, txtTimeIcon;
    private View cardInfo;
    private ImageView toggleCardBtn;
    private EditText searchInput;
    private boolean isCardVisible = true;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long ROUTE_UPDATE_INTERVAL_MS = 5000;
    private static final float MIN_ROUTE_DISTANCE_UPDATE_METERS = 10f;
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private LocationManager locationManager;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private GeoPoint currentLocation;
    private GeoPoint destinationPoint;
    private GeoPoint lastRouteLocation = null;
    private boolean isFirstLocation = true;
    private long lastRouteTimestamp = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_map);

        txtDistance = findViewById(R.id.txtDistance);
        txtTime = findViewById(R.id.txtTime);
        txtDistanceIcon = findViewById(R.id.txtDistanceIcon);
        txtTimeIcon = findViewById(R.id.txtTimeIcon);
        cardInfo = findViewById(R.id.cardInfo);
        toggleCardBtn = findViewById(R.id.toggleCardBtn);
        searchInput = findViewById(R.id.searchInput);

        toggleCardBtn.setOnClickListener(v -> toggleCardInfo());

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

        searchInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchPlace(query);
                } else {
                    Toast.makeText(this, "Ingrese un lugar para buscar", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestLocationPermissions();
    }

    //Metodo para realizar  busqueda
    private void searchPlace(String placeName) {
        OkHttpClient client = new OkHttpClient();

        HttpUrl url = HttpUrl.parse("https://nominatim.openstreetmap.org/search").newBuilder()
                .addQueryParameter("q", placeName)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", "1")
                .addQueryParameter("addressdetails", "1")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) TravelTdeaApp/1.0")  // <- más confiable
                .build();

        Log.d("SearchURL", url.toString());  // <- útil para depuración

        client.newCall(request).enqueue(new Callback() {
            @Override
            //metodo para manejar errores
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }

            @Override
            //metodo para manejar respuesta
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> Toast.makeText(MapActivity.this, "Lugar no encontrado", Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JSONArray results = new JSONArray(responseBody);
                    if (results.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(MapActivity.this, "No se encontró el lugar", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    JSONObject place = results.getJSONObject(0);
                    double lat = place.getDouble("lat");
                    double lon = place.getDouble("lon");

                    runOnUiThread(() -> {
                        GeoPoint point = new GeoPoint(lat, lon);
                        mapController.setCenter(point);
                        mapController.setZoom(17.0);
                        setDestinationPoint(point);

                        // Si ya existe un marcador, elimínalo del mapa
                        if (destinationMarker != null) {
                            mapView.getOverlays().remove(destinationMarker);
                        }

                        // Crear nuevo marcador
                        destinationMarker = new Marker(mapView);
                        destinationMarker.setPosition(point);
                        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        destinationMarker.setTitle("Destino: " + placeName);
                        mapView.getOverlays().add(destinationMarker);
                        mapView.invalidate();  // Redibujar mapa

                        Toast.makeText(MapActivity.this, "Destino establecido", Toast.LENGTH_SHORT).show();
                    });

                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MapActivity.this, "Error al procesar la respuesta", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }
        });
    }
    //metodo para animación de menu
    private void toggleCardInfo() {
        int startHeight = isCardVisible ? cardInfo.getHeight() : 0;
        cardInfo.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int endHeight = isCardVisible ? 0 : cardInfo.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            cardInfo.getLayoutParams().height = value;
            cardInfo.requestLayout();
        });

        if (!isCardVisible) {
            cardInfo.setVisibility(View.VISIBLE);
        } else {
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    cardInfo.setVisibility(View.GONE);
                }
            });
        }

        animator.start();
        isCardVisible = !isCardVisible;
        toggleCardBtn.setImageResource(isCardVisible ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
    }
    //solicitar permisos de ubicación
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
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        try {
            if (locationManager != null &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
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
            float distanceMoved = 0;

            if (lastRouteLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        lastRouteLocation.getLatitude(), lastRouteLocation.getLongitude(),
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        results
                );
                distanceMoved = results[0];
            }

            if ((now - lastRouteTimestamp > ROUTE_UPDATE_INTERVAL_MS)
                    || (distanceMoved >= MIN_ROUTE_DISTANCE_UPDATE_METERS)) {
                lastRouteTimestamp = now;
                lastRouteLocation = currentLocation;


                if (distanceMoved >= 50) {
                    calculateRoute();
                } else {
                    updateRoutePolyline(currentLocation);
                    updateDistanceAndTimeText(loc);
                }
            }
        }
    }
    //metodo para actualizar la distancia y tiempo
    private void updateDistanceAndTimeText(Location location) {
        if (routePolyline == null || currentLocation == null) return;

        List<GeoPoint> points = routePolyline.getActualPoints();
        if (points == null || points.isEmpty()) return;

        // Buscar el punto más cercano a la ubicación actual
        int closestIndex = 0;
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            float[] result = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    points.get(i).getLatitude(), points.get(i).getLongitude(),
                    result
            );
            if (result[0] < minDistance) {
                minDistance = result[0];
                closestIndex = i;
            }
        }

        // Calcular distancia restante desde el punto más cercano
        double totalDistance = 0;
        for (int i = closestIndex; i < points.size() - 1; i++) {
            GeoPoint p1 = points.get(i);
            GeoPoint p2 = points.get(i + 1);
            float[] result = new float[1];
            Location.distanceBetween(
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude(),
                    result
            );
            totalDistance += result[0];
        }

        // Mostrar distancia
        String distanciaKm = String.format("%.2f km", totalDistance / 1000.0);
        txtDistanceIcon.setText(distanciaKm);

        // Calcular tiempo estimado usando velocidad
        float speed = location.hasSpeed() ? location.getSpeed() : 13.89f; // 50 km/h ≈ 13.89 m/s
        double estimatedTimeMinutes = totalDistance / speed / 60.0;

        String tiempoMin = String.format("%.0f min", estimatedTimeMinutes);
        txtTimeIcon.setText(tiempoMin);
    }

 //metodo para actualizar la ruta
    private void updateRoutePolyline(GeoPoint userLocation) {
        if (routePolyline == null || routePolyline.getActualPoints().isEmpty()) return;

        List<GeoPoint> originalPoints = new ArrayList<>(routePolyline.getActualPoints());
        List<GeoPoint> updatedPoints = new ArrayList<>();

        // Mantén solo los puntos que están más lejos que un umbral (por ejemplo, 20m) desde el usuario
        boolean startAdding = false;
        float THRESHOLD_METERS = 20f;

        for (GeoPoint point : originalPoints) {
            float[] results = new float[1];
            Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    point.getLatitude(), point.getLongitude(),
                    results
            );

            if (results[0] > THRESHOLD_METERS || startAdding) {
                updatedPoints.add(point);
                startAdding = true;
            }
        }


        if (updatedPoints.size() > 1) {
            routePolyline.setPoints(updatedPoints);
            mapView.invalidate();
        }
    }
 //metodo para establecer el destino
    private void setDestinationPoint(GeoPoint p) {
        destinationPoint = p;

        runOnUiThread(() -> {
            if (destinationMarker != null) {
                mapView.getOverlays().remove(destinationMarker);
            }

            destinationMarker = new Marker(mapView);
            destinationMarker.setPosition(destinationPoint);
            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            destinationMarker.setTitle("Buscando nombre..."); // Temporal
            mapView.getOverlays().add(destinationMarker);
            mapView.invalidate();
        });

        // Construir URL para Nominatim
        String url = String.format(Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
                destinationPoint.getLatitude(), destinationPoint.getLongitude());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "traveltdea-app") // IMPORTANTE para evitar errores de bloqueo
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    destinationMarker.setTitle("Destino");
                    mapView.invalidate();
                });
            }
            //metodo para obtener el nombre del destino
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        String displayName = jsonObject.optString("display_name", "Destino");

                        runOnUiThread(() -> {
                            destinationMarker.setTitle(displayName);
                            mapView.invalidate();
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            destinationMarker.setTitle("Destino");
                            mapView.invalidate();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        destinationMarker.setTitle("Destino");
                        mapView.invalidate();
                    });
                }
            }
        });

        if (currentLocation != null) {
            lastRouteTimestamp = System.currentTimeMillis();
            calculateRoute();
        } else {
            runOnUiThread(() -> {
                txtDistance.setText("Distancia");
                txtTime.setText("Tiempo estimado");
                txtDistanceIcon.setText("0 km");
                txtTimeIcon.setText("0 min");
                cardInfo.setVisibility(View.VISIBLE);
                Toast.makeText(MapActivity.this, "Esperando conexión GPS", Toast.LENGTH_SHORT).show();
            });
        }
    }

 //metodo para calcular la ruta
    private void calculateRoute() {
        if (currentLocation == null || destinationPoint == null) return;

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
        client.newCall(request).enqueue(new Callback(){
            //metodo para manejar errores
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    txtDistance.setText("Distancia");
                    txtTime.setText("Tiempo estimado");
                    txtDistanceIcon.setText("0 km");
                    txtTimeIcon.setText("0 min");
                    cardInfo.setVisibility(View.VISIBLE);
                    Toast.makeText(MapActivity.this, "Esperando conexión GPS", Toast.LENGTH_SHORT).show();

                });
            }
            //metodo para manejar respuesta
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        txtDistance.setText("Distancia");
                        txtTime.setText("Tiempo estimado");
                        txtDistanceIcon.setText("0 km");
                        txtTimeIcon.setText("0 min");
                        cardInfo.setVisibility(View.VISIBLE);
                        Toast.makeText(MapActivity.this, "Esperando conexión GPS", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    JSONObject body = new JSONObject(response.body().string());
                    JSONArray paths = body.getJSONArray("paths");
                    if (paths.length() == 0) throw new JSONException("Lista vacía");

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
                        txtDistanceIcon.setText(distanciaKm);
                        txtTimeIcon.setText(tiempoMin);
                        cardInfo.setVisibility(View.VISIBLE);

                    });

                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        txtDistance.setText("Distancia");
                        txtTime.setText("Tiempo estimado");
                        txtDistanceIcon.setText("0 km");
                        txtTimeIcon.setText("0 min");
                        cardInfo.setVisibility(View.VISIBLE);
                        Toast.makeText(MapActivity.this, "Esperando GPS", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
  //metodo para manejar ciclo de vida
    @Override protected void onResume() {
        super.onResume();
        mapView.onResume();
        startLocationUpdates();
    }
  //metodo para manejar ciclo de vida
    @Override protected void onPause() {
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
    //metodo para manejar ciclo de vida
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
}
