package com.example.traveltdea;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;


public class DeviceInfo {

    // Obtener el ID de Android (Android ID)
    public static String getDeviceId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }
    public static String getDeviceModel() {
        // Obtener el modelo del dispositivo
        String model = Build.MODEL;
        return model;
    }
    public static String getOperatingSystem() {
        // Obtener el sistema operativo
        String osVersion = "Android " + Build.VERSION.RELEASE;
        return osVersion;
    }
}


