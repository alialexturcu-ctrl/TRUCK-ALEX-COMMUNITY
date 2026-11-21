
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private MapView map;
    private LocationManager locationManager;
    private TextToSpeech tts;
    private TextView status;
    private Marker truckMarker;

    private boolean voiceReady = false;

    private static final int LOCATION_PERMISSION = 100;

    private final GeoPoint defaultPosition =
            new GeoPoint(45.0, 10.0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                androidx.preference.PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext())
        );

        Configuration.getInstance().setUserAgentValue(getPackageName());

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        createInterface();
        initializeVoice();
        checkLocationPermission();
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(16, 24, 32));

        TextView title = new TextView(this);
        title.setText("TRUCK ALEX COMMUNITY");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 15, 8, 15);
        title.setBackgroundColor(Color.rgb(23, 43, 58));

        root.addView(title);

        status = new TextView(this);
        status.setText("Se pregătește harta...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(8, 8, 8, 8);

        root.addView(status);

        // HARTA
        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setUseDataConnection(true);
        map.setBuiltInZoomControls(false);
        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(19.0);

        map.getController().setZoom(5.0);
        map.getController().setCenter(defaultPosition);

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1.0f
                );

        root.addView(map, mapParams);

        // RANDUL CU BUTOANELE GPS SI ZOOM
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button positionButton = new Button(this);
        positionButton.setText("POZIȚIA MEA");
        positionButton.setTextSize(12);
        positionButton.setOnClickListener(v -> {
            checkLocationPermission();
            goToMyLocation();
        });

        Button zoomInButton = new Button(this);
        zoomInButton.setText("+");
        zoomInButton.setTextSize(20);
        zoomInButton.setOnClickListener(v -> {
            if (map != null) {
                map.getController().zoomIn();
            }
        });

        Button zoomOutButton = new Button(this);
        zoomOutButton.setText("−");
        zoomOutButton.setTextSize(20);
        zoomOutButton.setOnClickListener(v -> {
            if (map != null) {
                map.getController().zoomOut();
            }
        });

        buttons.addView(positionButton,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 2));

        buttons.addView(zoomInButton,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        buttons.addView(zoomOutButton,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        root.addView(buttons);

        // BUTON VOCE
        Button voiceButton = new Button(this);
        voiceButton.setText("TESTEAZĂ VOCEA");
        voiceButton.setOnClickListener(v ->
                speak("Bine ai venit în Truck Alex Community.")
        );

        root.addView(voiceButton);

        setContentView(root);
    }

    private void initializeVoice() {

        tts = new TextToSpeech(this, result -> {

            if (result == TextToSpeech.SUCCESS) {

                int languageResult =
                        tts.setLanguage(new Locale("ro", "RO"));

                voiceReady =
                        languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;

                if (voiceReady) {
                    status.setText("Harta este pregătită. Vocea română este activă.");
                } else {
                    status.setText("Vocea română nu este disponibilă.");
                }
            }
        });
    }

    private void speak(String message) {

        if (tts != null && voiceReady) {
            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "truck_voice"
            );
        } else {
            Toast.makeText(
                    this,
                    "Vocea nu este încă pregătită.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void checkLocationPermission() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

        } else {
            startGPS();
        }
    }

    private void startGPS() {

        if (locationManager == null) {
            status.setText("Serviciul GPS nu este disponibil.");
            return;
        }

        try {

            if (!locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                status.setText("Activează GPS-ul telefonului.");
                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    this
            );

            status.setText("GPS activ. Se caută poziția...");

            Location lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }

        } catch (SecurityException e) {
            status.setText("Permisiunea GPS nu este acordată.");
        } catch (Exception e) {
            status.setText("Nu se poate porni GPS-ul.");
        }
    }

    private void goToMyLocation() {

        if (truckMarker != null && map != null) {
            map.getController().animateTo(
                    truckMarker.getPosition()
            );
            map.getController().setZoom(16.0);
        } else {
            status.setText("Așteaptă detectarea poziției GPS.");
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        GeoPoint position = new GeoPoint(latitude, longitude);

        if (truckMarker == null) {

            truckMarker = new Marker(map);
            truckMarker.setTitle("Poziția GPS");
            truckMarker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            map.getOverlays().add(truckMarker);
        }

        truckMarker.setPosition(position);

        map.getController().setZoom(16.0);
        map.getController().animateTo(position);
        map.invalidate();

        float speed = location.getSpeed() * 3.6f;

        status.setText(
                String.format(
                        Locale.getDefault(),
                        "GPS ACTIV | Viteză: %.1f km/h",
                        speed
                )
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startGPS();

            } else {

                status.setText("Permisiunea GPS nu a fost acordată.");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (map != null) {
            map.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (map != null) {
            map.onPause();
        }

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (map != null) {
            map.onDetach();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (status != null) {
            status.setText("Activează GPS-ul telefonului.");
        }
    }

    @Override
    public void onStatusChanged(
            String provider,
            int statusValue,
            Bundle extras) {
    }
}
