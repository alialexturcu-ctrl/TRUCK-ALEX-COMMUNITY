
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends Activity {

    private LinearLayout home;
    private MapView mapView;
    private LocationManager locationManager;
    private Marker truckMarker;

    private static final int GPS_PERMISSION = 100;

    private final LocationListener locationListener = location -> {
        if (mapView != null) {
            GeoPoint point = new GeoPoint(
                    location.getLatitude(),
                    location.getLongitude()
            );

            if (truckMarker == null) {
                truckMarker = new Marker(mapView);
                truckMarker.setTitle("Camionul meu");
                truckMarker.setAnchor(
                        Marker.ANCHOR_CENTER,
                        Marker.ANCHOR_BOTTOM
                );
                mapView.getOverlays().add(truckMarker);
            }

            truckMarker.setPosition(point);
            mapView.getController().animateTo(point);
            mapView.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        showHome();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(18, 18, 18, 18);
        home.setBackgroundColor(Color.rgb(20, 28, 40));

        scroll.addView(home);

        addPanel("HARTA GPS", "Deschide harta GPS.");
        addPanel("PROFIL TIR",
                "Inaltime: 4.00 m\nLatime: 2.55 m\nLungime: 16.50 m\nGreutate: 40 tone");
        addPanel("CAMION RIGID",
                "Inaltime: 3.50 m\nLatime: 2.55 m\nLungime: 8.00 m\nGreutate: 18 tone\n2 axe");
        addPanel("SETARI CAMION",
                "Setarile camionului vor fi adaugate.");
        addPanel("VOCE ROMANA",
                "Vocea romana va fi adaugata.");
        addPanel("DESPRE APLICATIE",
                "TRUCK ALEX COMMUNITY\nVersiunea 1.0");

        setContentView(scroll);
    }

    private void addPanel(String title, String message) {
        TextView panel = new TextView(this);
        panel.setText(title);
        panel.setTextColor(Color.WHITE);
        panel.setTextSize(21);
        panel.setGravity(Gravity.CENTER);
        panel.setBackgroundColor(Color.rgb(25, 105, 190));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 95);
        params.setMargins(0, 0, 0, 14);
        home.addView(panel, params);

        panel.setOnClickListener(v -> {
            if (title.equals("HARTA GPS")) {
                showMap();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("INCHIDE", null)
                        .show();
            }
        });
    }

    private void showMap() {
        Configuration.getInstance().setUserAgentValue(getPackageName());

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        TextView back = new TextView(this);
        back.setText("← INAPOI");
        back.setTextSize(20);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.rgb(25, 105, 190));

        screen.addView(back,
                new LinearLayout.LayoutParams(-1, 60));

        mapView = new MapView(this);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(5.5);
        mapView.getController().setCenter(new GeoPoint(42.5, 12.5));

        screen.addView(mapView,
                new LinearLayout.LayoutParams(-1, 0, 1));

        back.setOnClickListener(v -> {
            stopGps();
            mapView = null;
            truckMarker = null;
            showHome();
        });

        setContentView(screen);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    GPS_PERMISSION
            );
        }
    }

    private void startGps() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    2,
                    locationListener
            );

            Location last = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
            );

            if (last != null) {
                locationListener.onLocationChanged(last);
            }
        } catch (SecurityException e) {
            new AlertDialog.Builder(this)
                    .setMessage("Permisiunea GPS nu este activata.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void stopGps() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == GPS_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGps();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage("Fara permisiunea GPS, pozitia nu poate fi afisata.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        stopGps();
        if (mapView != null) mapView.onPause();
        super.onPause();
    }
}
