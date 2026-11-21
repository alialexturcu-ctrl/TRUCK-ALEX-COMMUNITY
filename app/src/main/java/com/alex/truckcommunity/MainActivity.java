
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;

import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout home;
    private MapView mapView;
    private MapLibreMap map;
    private LocationManager locationManager;

    private Marker truckMarker;
    private Marker destinationMarker;
    private Location lastLocation;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final int GPS_PERMISSION = 100;

    private final LocationListener locationListener = location -> {
        lastLocation = location;
        updateTruckPosition(location);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

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
                new LinearLayout.LayoutParams(-1, 95);

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
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        TextView back = new TextView(this);
        back.setText("← INAPOI");
        back.setTextSize(20);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.rgb(25, 105, 190));

        screen.addView(back,
                new LinearLayout.LayoutParams(-1, 55));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(6, 6, 6, 6);

        EditText searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setHint(
                "Oras sau coordonate: 45.4642, 9.1900"
        );
        searchInput.setBackgroundColor(Color.WHITE);
        searchInput.setPadding(12, 4, 12, 4);

        searchRow.addView(searchInput,
                new LinearLayout.LayoutParams(0, 55, 1));

        Button searchButton = new Button(this);
        searchButton.setText("CAUTA");

        searchRow.addView(searchButton,
                new LinearLayout.LayoutParams(-2, 55));

        screen.addView(searchRow);

        mapView = new MapView(this);
        mapView.onCreate(null);

        screen.addView(mapView,
                new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(screen);

        mapView.onStart();
        mapView.onResume();

        mapView.getMapAsync(mapLibreMap -> {
            map = mapLibreMap;

            map.setStyle(
                    new Style.Builder().fromUri(
                            "https://tiles.openfreemap.org/styles/liberty"
                    ),
                    style -> {
                        map.getUiSettings()
                                .setCompassEnabled(true);

                        map.setCameraPosition(
                                new CameraPosition.Builder()
                                        .target(new LatLng(42.5, 12.5))
                                        .zoom(5.5)
                                        .build()
                        );

                        if (lastLocation != null) {
                            updateTruckPosition(lastLocation);
                        }
                    }
            );
        });

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText()
                    .toString().trim();

            if (query.isEmpty()) {
                searchInput.setError("Scrie o destinatie");
                return;
            }

            searchButton.setEnabled(false);
            searchButton.setText("CAUT...");

            executor.execute(() -> {
                LatLng foundPoint = null;
                String error = null;

                try {
                    String[] parts = query.split("[,;\\s]+");

                    if (parts.length == 2) {
                        try {
                            double lat =
                                    Double.parseDouble(parts[0]);

                            double lon =
                                    Double.parseDouble(parts[1]);

                            if (lat >= -90 && lat <= 90
                                    && lon >= -180 && lon <= 180) {
                                foundPoint = new LatLng(lat, lon);
                            } else {
                                error = "Coordonate in afara limitelor.";
                            }

                        } catch (NumberFormatException e) {
                            // Cautam ca adresa.
                        }
                    }

                    if (foundPoint == null && error == null) {
                        Geocoder geocoder =
                                new Geocoder(this, Locale.getDefault());

                        List<Address> results =
                                geocoder.getFromLocationName(query, 1);

                        if (results != null && !results.isEmpty()) {
                            Address address = results.get(0);

                            foundPoint = new LatLng(
                                    address.getLatitude(),
                                    address.getLongitude()
                            );
                        } else {
                            error = "Destinatia nu a fost gasita.";
                        }
                    }

                } catch (IOException e) {
                    error = "Verifica internetul.";
                } catch (Exception e) {
                    error = "Nu am putut cauta destinatia.";
                }

                final LatLng resultPoint = foundPoint;
                final String resultError = error;

                runOnUiThread(() -> {
                    searchButton.setEnabled(true);
                    searchButton.setText("CAUTA");

                    if (map == null || mapView == null) {
                        return;
                    }

                    if (resultPoint == null) {
                        showMessage(resultError == null
                                ? "Destinatia nu a fost gasita."
                                : resultError);
                        return;
                    }

                    if (destinationMarker != null) {
                        map.removeMarker(destinationMarker);
                    }

                    destinationMarker = map.addMarker(
                            new MarkerOptions()
                                    .position(resultPoint)
                                    .title("Destinatia cautata")
                    );

                    map.setCameraPosition(
                            new CameraPosition.Builder()
                                    .target(resultPoint)
                                    .zoom(15.5)
                                    .tilt(55)
                                    .build()
                    );
                });
            });
        });

        back.setOnClickListener(v -> {
            stopGps();
            destroyMap();
            showHome();
        });

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
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

    private void updateTruckPosition(Location location) {
        if (map == null || mapView == null) {
            return;
        }

        LatLng point = new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );

        if (truckMarker == null) {
            truckMarker = map.addMarker(
                    new MarkerOptions()
                            .position(point)
                            .title("Camionul meu")
            );
        } else {
            truckMarker.setPosition(point);
            map.updateMarker(truckMarker);
        }

        map.setCameraPosition(
                new CameraPosition.Builder()
                        .target(point)
                        .zoom(15.5)
                        .tilt(55)
                        .build()
        );
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void startGps() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    2,
                    locationListener
            );

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (last != null) {
                locationListener.onLocationChanged(last);
            }

        } catch (SecurityException e) {
            showMessage("Permisiunea GPS nu este activata.");
        }
    }

    private void stopGps() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void destroyMap() {
        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
        }

        mapView = null;
        map = null;
        truckMarker = null;
        destinationMarker = null;
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
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {
                startGps();
            } else {
                showMessage(
                        "Fara permisiunea GPS, pozitia nu poate fi afisata."
                );
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
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

    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        stopGps();

        if (mapView != null) {
            mapView.onDestroy();
        }

        executor.shutdownNow();
        super.onDestroy();
    }
}
