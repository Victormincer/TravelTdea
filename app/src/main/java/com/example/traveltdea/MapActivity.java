package com.example.traveltdea;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        // Solicitar permisos de ubicación
        requestLocationPermissions();

        // Inicializar el mapa
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // Configurar el controlador del mapa
        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Configurar la capa de ubicación
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        locationOverlay.setOptionsMenuEnabled(true);
        mapView.getOverlays().add(locationOverlay);

        // Agregar brújula
        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // Inicializar botones
        btnCalculateRoute = findViewById(R.id.btnCalculateRoute);
        btnAddDestination = findViewById(R.id.btnAddDestination);

        // Inicializar Polyline para la ruta
        routePolyline = new Polyline();
        routePolyline.setColor(0xFF0000FF); // Color azul
        routePolyline.setWidth(10f);
        mapView.getOverlays().add(routePolyline);

        // Configurar listeners de botones
        btnAddDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRandomDestination();
            }
        });

        btnCalculateRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateRoute();
            }
        });

        // Inicializar LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        }
    }

    private void addRandomDestination() {
        if (currentLocation == null) {
            Toast.makeText(this, "Esperando ubicación actual...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear un punto de destino aleatorio cerca de la ubicación actual
        Random random = new Random();
        double latOffset = (random.nextDouble() - 0.5) * 0.05; // Aproximadamente 5km
        double lonOffset = (random.nextDouble() - 0.5) * 0.05;

        destinationPoint = new GeoPoint(
                currentLocation.getLatitude() + latOffset,
                currentLocation.getLongitude() + lonOffset);

        // Limpiar marcador anterior si existe
        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
        }

        // Agregar nuevo marcador
        destinationMarker = new Marker(mapView);
        destinationMarker.setPosition(destinationPoint);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setTitle("Destino");
        mapView.getOverlays().add(destinationMarker);

        // Actualizar el mapa
        mapView.invalidate();

        Toast.makeText(this, "Destino añadido", Toast.LENGTH_SHORT).show();
    }

    private void calculateRoute() {
        if (currentLocation == null || destinationPoint == null) {
            Toast.makeText(this, "Necesitas una ubicación actual y un destino", Toast.LENGTH_SHORT).show();
            return;
        }

        // En una app real, aquí se haría una llamada a una API de enrutamiento
        // Para este ejemplo, solo simularemos una ruta con algunos puntos intermedios

        List<GeoPoint> routePoints = new ArrayList<>();
        routePoints.add(currentLocation);

        // Crear algunos puntos intermedios para simular una ruta
        int steps = 10;
        for (int i = 1; i < steps; i++) {
            double lat = currentLocation.getLatitude() + ((destinationPoint.getLatitude() - currentLocation.getLatitude()) * i / steps);
            double lon = currentLocation.getLongitude() + ((destinationPoint.getLongitude() - currentLocation.getLongitude()) * i / steps);

            // Agregar algo de variación para que no sea una línea recta
            if (i > 1 && i < steps - 1) {
                Random random = new Random();
                lat += (random.nextDouble() - 0.5) * 0.01;
                lon += (random.nextDouble() - 0.5) * 0.01;
            }

            routePoints.add(new GeoPoint(lat, lon));
        }

        routePoints.add(destinationPoint);

        // Actualizar polyline
        routePolyline.setPoints(routePoints);

        // Actualizar el mapa
        mapView.invalidate();

        Toast.makeText(this, "Ruta calculada", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Si es la primera ubicación, centrar el mapa
        if (mapView.getMapCenter().getLatitude() == 0 && mapView.getMapCenter().getLongitude() == 0) {
            mapController.setCenter(currentLocation);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
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
            locationManager.removeUpdates(this);
        }
    }
}


