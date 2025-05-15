package com.example.traveltdea;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad principal de la aplicación TravelTdea.
 * Muestra la versión de la app y el ID del dispositivo,
 * y permite iniciar la navegación en el mapa.
 */
public class MainActivity extends AppCompatActivity {

    // Declaración de los componentes de UI
    private Button btnStartNavigation;
    private TextView deviceInfoTextView;
    private TextView versionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa los elementos de la interfaz de usuario
        btnStartNavigation = findViewById(R.id.btnMap);
        deviceInfoTextView = findViewById(R.id.AndroidId);
        versionTextView = findViewById(R.id.versionapp);

        // Obtiene el ID del dispositivo y lo muestra en pantalla
        String deviceId = DeviceInfo.getDeviceId(this);
        deviceInfoTextView.setText(deviceId);

        // Obtiene la versión de la app y la muestra con formato adecuado
        String appVersion = getAppVersion();
        versionTextView.setText("Versión / " + appVersion);

        // Configura el botón para iniciar la actividad del mapa
        btnStartNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // Inicia la actividad del mapa
                //se puede incluir un api de login
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });
    }

    private String getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Desconocida";
        }
    }
}

