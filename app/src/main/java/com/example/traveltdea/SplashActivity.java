package com.example.traveltdea;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_splash);
            ImageView loadingSpinner = findViewById(R.id.loadingSpinner);
            Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
            if (loadingSpinner != null && rotate != null) {
                loadingSpinner.startAnimation(rotate);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}