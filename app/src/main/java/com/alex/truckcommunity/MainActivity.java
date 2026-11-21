
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private TextView status;
    private MapView mapView;
    private MapLibreMap map;

    private LocationManager locationManager;

    private double latitude = 0;
    private double longitude = 0;

    private static final int GPS_PERMISSION = 100;
    private static final String OVERPASS =
            "https://overpass.kumi.systems/api/interpreter";

    private final int BLUE = Color.rgb(30, 75, 115);

    private boolean mapOpened = false;

    private static class Place {
        String name;
        double lat;
        double lon;
        float distance;

        Place(String name, double lat, double lon, float distance) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        showHome();
        getGPS();
    }

    private void showHome() {

        ScrollView scroll = new ScrollView(this);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 25, 20, 30);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GPS ALEX");
        title.setTextColor(Color.rgb(20, 45, 70));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(5, 15, 5, 15);

        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("CAMION • GPS • NAVIGATIE");
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(5, 0, 5, 20);

        layout.addView(subtitle);

        addButton("HARTA GPS", () -> showMap());

        addButton("CAMION", () ->
                showMessage("CAMION",
                        "INALTIME: 4,00 m\n" +
                        "LATIME: 2,55 m\n" +
                        "LUNGIME: 16,50 m\n" +
                        "GREUTATE: 40 tone"));

        addButton("SETARI CAMION", () ->
                showMessage("SETARI CAMION",
                        "Profil activ: CAMION\n\n" +
                        "Inaltime: 4,00 m\n" +
                        "Latime: 2,55 m\n" +
                        "Lungime: 16,50 m\n" +
                        "Greutate: 40 tone"));

        addButton("VOCE ROMANA", () ->
                showMessage("VOCE ROMANA",
                        "Configurarea vocii romane va fi adaugata ulterior."));

        addButton("DESPRE APLICATIE", () ->
                showMessage("GPS ALEX",
                        "GPS ALEX TRUCK NAVI\nVersiunea 1.0"));

        TextView searchTitle = new TextView(this);
        searchTitle.setText("CAUTARE LOCATII");
        searchTitle.setTextColor(Color.rgb(20, 45, 70));
        searchTitle.setTextSize(20);
        searchTitle.setTypeface(null, Typeface.BOLD);
        searchTitle.setGravity(Gravity.CENTER);
        searchTitle.setPadding(5, 25, 5, 10);

        layout.addView(searchTitle);

        addButton("PARCARI TIR", () ->
                searchPlaces("parking"));

        addButton("MOTORINA", () ->
                searchPlaces("fuel"));

        addButton("SERVICE CAMIOANE", () ->
                searchPlaces("service"));

        status = new TextView(this);
        status.setText("GPS ALEX pregatit pentru navigare");
        status.setTextColor(Color.DKGRAY);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(5, 15, 5, 15);

        layout.addView(status);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addButton(String name, Runnable action) {

        Button button = new Button(this);
        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);

        GradientDrawable background = new GradientDrawable();
        background.setColor(BLUE);
        background.setCornerRadius(20);
        background.setStroke(2, Color.rgb(70, 150, 210));

        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 70);

        params.setMargins(0, 7, 0, 7);

        layout.addView(button, params);

        button.setOnClickListener(v -> action.run());
    }

    private void showMessage(String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("INCHIDE", null)
                .show();
    }

    private void searchPlaces(String category) {

        if (latitude == 0 || longitude == 0) {
            status.setText("Asteptam pozitia GPS...");
            getGPS();
            Toast.makeText(this,
                    "Activeaza localizarea GPS.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        status.setText("Se cauta locatii pe 25 km...");

        final double searchLat = latitude;
        final double searchLon = longitude;

        new Thread(() -> {

            try {

                String filter;

                if (category.equals("parking")) {
                    filter = "[\"amenity\"=\"parking\"]";
                } else if (category.equals("fuel")) {
                    filter = "[\"amenity\"=\"fuel\"]";
                } else {
                    filter = "[\"shop\"=\"truck\"]" +
                            ";way(around:25000," +
                            searchLat + "," + searchLon +
                            ")[\"craft\"=\"truck_repair\"]";
                }

                String query =
                        "[out:json][timeout:35];(";

                if (category.equals("service")) {

                    query +=
                            "node(around:25000," + searchLat + "," +
                            searchLon + ")[\"shop\"=\"truck\"];";

                    query +=
                            "way(around:25000," + searchLat + "," +
                            searchLon + ")[\"shop\"=\"truck\"];";

                    query +=
                            "node(around:25000," + searchLat + "," +
                            searchLon + ")[\"craft\"=\"truck_repair\"];";

                    query +=
                            "way(around:25000," + searchLat + "," +
                            searchLon + ")[\"craft\"=\"truck_repair\"];";

                } else {

                    query +=
                            "node(around:25000," + searchLat + "," +
                            searchLon + ")" + filter + ";";

                    query +=
                            "way(around:25000," + searchLat + "," +
                            searchLon + ")" + filter + ";";

                    query +=
                            "relation(around:25000," + searchLat + "," +
                            searchLon + ")" + filter + ";";
                }

                query += ");out center tags;";

                URL url = new URL(OVERPASS);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded");

                connection.setRequestProperty(
                        "User-Agent",
                        "GPS-ALEX-TRUCK-NAVI/1.0");

                String data = "data=" +
                        URLEncoder.encode(query, "UTF-8");

                OutputStream output = connection.getOutputStream();
                output.write(data.getBytes("UTF-8"));
                output.flush();
                output.close();

                int responseCode = connection.getResponseCode();

                if (responseCode != 200) {
                    throw new Exception("Server HTTP " + responseCode);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONObject json = new JSONObject(result.toString());
                JSONArray elements = json.getJSONArray("elements");

                ArrayList<Place> places = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {

                    JSONObject item = elements.getJSONObject(i);

                    double lat;
                    double lon;

                    if (item.has("lat") && item.has("lon")) {
                        lat = item.getDouble("lat");
                        lon = item.getDouble("lon");
                    } else if (item.has("center")) {
                        JSONObject center = item.getJSONObject("center");
                        lat = center.getDouble("lat");
                        lon = center.getDouble("lon");
                    } else {
                        continue;
                    }

                    JSONObject tags = item.optJSONObject("tags");

                    String name = "Locatie fara nume";

                    if (tags != null) {
                        name = tags.optString("name",
                                tags.optString("operator", name));
                    }

                    float[] distance = new float[1];

                    Location.distanceBetween(
                            searchLat, searchLon,
                            lat, lon, distance);

                    if (distance[0] <= 25000) {
                        places.add(new Place(
                                name, lat, lon, distance[0]));
                    }
                }

                Collections.sort(places,
                        Comparator.comparingDouble(p -> p.distance));

                runOnUiThread(() -> showPlaces(places, category));

            } catch (Exception e) {

                runOnUiThread(() -> {
                    status.setText("Eroare la cautare: " +
                            e.getMessage());
                    Toast.makeText(this,
                            "Serverul de cautare nu a raspuns.",
                            Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private void showPlaces(ArrayList<Place> places, String category) {

        if (places.isEmpty()) {
            status.setText("Nu au fost gasite locatii in 25 km.");
            return;
        }

        String[] names = new String[places.size()];

        for (int i = 0; i < places.size(); i++) {

            Place p = places.get(i);

            names[i] = p.name + "\n" +
                    String.format("%.1f km", p.distance / 1000f);
        }

        status.setText("Au fost gasite " +
                places.size() + " locatii.");

        String title;

        if (category.equals("parking")) {
            title = "PARCARI";
        } else if (category.equals("fuel")) {
            title = "MOTORINA";
        } else {
            title = "SERVICE CAMIOANE";
        }

        new AlertDialog.Builder(this)
                .setTitle(title + " - 25 KM")
                .setItems(names, (dialog, which) -> {
                    Place selected = places.get(which);
                    openGoogleMaps(selected);
                })
                .setNegativeButton("INCHIDE", null)
                .show();
    }

    private void openGoogleMaps(Place place) {

        String destination =
                place.name + "," + place.lat + "," + place.lon;

        String url =
                "https://www.google.com/maps/search/?api=1&query=" +
                Uri.encode(destination);

        Intent intent = new Intent(
                Intent.ACTION_VIEW, Uri.parse(url));

        startActivity(intent);
    }

    private void getGPS() {

        boolean fine = checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean coarse = checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, GPS_PERMISSION);

            return;
        }

        try {

            String provider = fine
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;

            Location location =
                    locationManager.getLastKnownLocation(provider);

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            locationManager.requestLocationUpdates(
                    provider,
                    5000,
                    10,
                    new LocationListener() {

                        @Override
                        public void onLocationChanged(Location loc) {
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                        }

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}

                        @Override
                        public void onStatusChanged(
                                String provider, int status, Bundle extras) {}
                    });

        } catch (Exception e) {
            if (status != null) {
                status.setText("GPS indisponibil.");
            }
        }
    }

    private void showMap() {

        mapOpened = true;

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        Button back = new Button(this);
        back.setText("INAPOI LA GPS ALEX");
        back.setTextColor(Color.WHITE);
        back.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(BLUE));

        screen.addView(back);

        mapView = new MapView(this);

        screen.addView(mapView,
                new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(screen);

        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();

        mapView.getMapAsync(mapLibreMap -> {

            map = mapLibreMap;

            map.setStyle(
                    "https://tiles.openfreemap.org/styles/liberty",
                    style -> {

                        if (latitude != 0 && longitude != 0) {

                            map.setCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(new LatLng(
                                                    latitude, longitude))
                                            .zoom(12)
                                            .build());
                        }
                    });
        });

        back.setOnClickListener(v -> returnHome());
    }

    private void returnHome() {

        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }

        mapOpened = false;
        showHome();
    }

    @Override
    public void onBackPressed() {

        if (mapOpened) {
            returnHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == GPS_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getGPS();
        }
    }
}
