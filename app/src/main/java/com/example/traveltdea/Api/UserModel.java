package com.example.traveltdea.Api;

import com.example.traveltdea.DeviceInfo;

public class UserModel {
    private String email;
    private String username;
    private String password;
    private String createdAt;
    private String lastLogin;
    private DeviceInfo deviceInfo;

    public UserModel() { } // Constructor vacío para Firebase

    public UserModel(String email, String username, String password, String createdAt, String lastLogin, DeviceInfo deviceInfo) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.deviceInfo = deviceInfo;
    }

    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCreatedAt() { return createdAt; }
    public String getLastLogin() { return lastLogin; }
    public DeviceInfo getDeviceInfo() { return deviceInfo; }
}
