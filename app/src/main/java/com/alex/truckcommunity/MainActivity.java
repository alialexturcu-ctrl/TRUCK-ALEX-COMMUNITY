
package com.alex.gpscamion;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView speedText;
    private TextView gpsText;
    private TextView statusText;

    private LocationManager locationManager;
    private TextToSpeech voice;
    private boolean voiceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = voice.setLanguage(new Locale("ro", "RO"));
                voiceReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGPS();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 100);
        }
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25, 30, 25, 30);
        layout.setBackgroundColor(Color.rgb(15, 23, 35));

        TextView title = new TextView(this);
        title.setText("GPS CAMION");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("TRUCK ALEX COMMUNITY");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);

        speedText = new TextView(this);
        speedText.setText("0 km/h");
        speedText.setTextSize(54);
        speedText.setTextColor(Color.GREEN);
        speedText.setGravity(Gravity.CENTER);

        gpsText = new TextView(this);
        gpsText.setText("Se așteaptă semnal GPS...");
        gpsText.setTextSize(16);
        gpsText.setTextColor(Color.WHITE);
        gpsText.setGravity(Gravity.CENTER);

        statusText = new TextView(this);
        statusText.setText("GPS OPRIT");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.YELLOW);
        statusText.setGravity(Gravity.CENTER);

        Button gpsButton = new Button(this);
        gpsButton.setText("PORNEȘTE GPS");
        gpsButton.setOnClickListener(v -> startGPS());

        Button voiceButton = new Button(this);
        voiceButton.setText("TESTEAZĂ VOCEA");
        voiceButton.setOnClickListener(v -> speak(
                "Bun venit în aplicația GPS Camion."));

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(speedText);
        layout.addView(gpsText);
        layout.addView(statusText);
        layout.addView(gpsButton);
        layout.addView(voiceButton);

        setContentView(layout);
    }

    private void startGPS() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
            return;
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    new LocationListener() {

                        @Override
                        public void onLocationChanged(Location location) {

                            float speed = location.hasSpeed()
                                    ? location.getSpeed() * 3.6f : 0;

                            speedText.setText(
                                    String.format(Locale.getDefault(),
                                            "%.0f km/h", speed));

                            gpsText.setText(
                                    "Latitudine: " + location.getLatitude()
                                    + "\nLongitudine: "
                                    + location.getLongitude());

                            statusText.setText("GPS ACTIV");
                        }
                    });

            statusText.setText("Se caută semnal GPS...");

        } catch (SecurityException e) {
            statusText.setText("Permisiune GPS indisponibilă");
        }
    }

    private void speak(String message) {
        if (voiceReady && voice != null) {
            voice.speak(message, TextToSpeech.QUEUE_FLUSH, null, "gps_voice");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 100
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGPS();
        } else {
            statusText.setText("Permisiunea GPS nu a fost acordată");
        }
    }

    @Override
    protected void onDestroy() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        if (voice != null) {
            voice.stop();
            voice.shutdown();
        }

        super.onDestroy();
    }

    private final LocationListener locationListener =
            new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                }
            };
}
