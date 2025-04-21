package com.example.traveltdea;

public class DEviceInfoModel {
    private String deviceId;
    private String model;
    private String osVersion;

    public DEviceInfoModel() { }

    public DEviceInfoModel(String deviceId, String model, String osVersion) {
        this.deviceId = deviceId;
        this.model = model;
        this.osVersion = osVersion;
    }

    public String getDeviceId() { return deviceId; }
    public String getModel() { return model; }
    public String getOsVersion() { return osVersion; }
}
