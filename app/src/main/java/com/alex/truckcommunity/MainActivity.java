
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
import android.widget.EditText;
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

    private EditText addressInput;
    private EditText coordinateInput;

    private double latitude = 0;
    private double longitude = 0;

    private double destinationLat = 0;
    private double destinationLon = 0;

    private static final int GPS_PERMISSION = 100;

    private final int BLUE = Color.rgb(30, 75, 115);

    private boolean mapOpened = false;

    private static final String[] OVERPASS_SERVERS = {
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
    };

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
        layout.setPadding(20, 20, 20, 25);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GPS ALEX");
        title.setTextColor(Color.rgb(20, 45, 70));
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(5, 12, 5, 12);

        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("CAMION • GPS • NAVIGATIE");
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(5, 0, 5, 15);

        layout.addView(subtitle);

        addButton("HARTA GPS", this::showMap);

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
        searchTitle.setPadding(5, 22, 5, 10);

        layout.addView(searchTitle);

        addButton("PARCARI TIR", () -> searchPlaces("parking"));
        addButton("MOTORINA", () -> searchPlaces("fuel"));
        addButton("SERVICE CAMIOANE", () -> searchPlaces("service"));

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
                        LinearLayout.LayoutParams.MATCH_PARENT, 65);

        params.setMargins(0, 6, 0, 6);

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

    // CAUTARE PARCARI, MOTORINA SI SERVICE

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

                String query = "[out:json][timeout:25];(";

                if (category.equals("parking")) {
                    query +=
                            "node(around:25000," + searchLat + "," + searchLon + ")[amenity=parking];" +
                            "way(around:25000," + searchLat + "," + searchLon + ")[amenity=parking];" +
                            "relation(around:25000," + searchLat + "," + searchLon + ")[amenity=parking];";
                } else if (category.equals("fuel")) {
                    query +=
                            "node(around:25000," + searchLat + "," + searchLon + ")[amenity=fuel];" +
                            "way(around:25000," + searchLat + "," + searchLon + ")[amenity=fuel];" +
                            "relation(around:25000," + searchLat + "," + searchLon + ")[amenity=fuel];";
                } else {
                    query +=
                            "node(around:25000," + searchLat + "," + searchLon + ")[shop=truck];" +
                            "way(around:25000," + searchLat + "," + searchLon + ")[shop=truck];" +
                            "node(around:25000," + searchLat + "," + searchLon + ")[craft=truck_repair];" +
                            "way(around:25000," + searchLat + "," + searchLon + ")[craft=truck_repair];";
                }

                query += ");out center tags;";

                String response = null;
                Exception lastError = null;

                for (String server : OVERPASS_SERVERS) {
                    try {
                        response = postOverpass(server, query);
                        if (response != null) break;
                    } catch (Exception e) {
                        lastError = e;
                    }
                }

                if (response == null) {
                    if (lastError != null) throw lastError;
                    throw new Exception("Serverele de cautare nu raspund.");
                }

                JSONObject json = new JSONObject(response);
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
                        places.add(new Place(name, lat, lon, distance[0]));
                    }
                }

                Collections.sort(places,
                        Comparator.comparingDouble(p -> p.distance));

                runOnUiThread(() -> showPlaces(places, category));

            } catch (Exception e) {

                runOnUiThread(() -> {
                    status.setText("Cautarea nu este disponibila momentan.");
                    Toast.makeText(this,
                            "Serverul nu a raspuns. Incearca din nou mai tarziu.",
                            Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private String postOverpass(String server, String query)
            throws Exception {

        URL url = new URL(server);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded");

        connection.setRequestProperty(
                "User-Agent",
                "GPS-ALEX-TRUCK-NAVI/1.0");

        String data = "data=" + URLEncoder.encode(query, "UTF-8");

        OutputStream output = connection.getOutputStream();
        output.write(data.getBytes("UTF-8"));
        output.flush();
        output.close();

        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            connection.disconnect();
            throw new Exception("HTTP " + responseCode);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        connection.disconnect();

        return result.toString();
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

        status.setText("Au fost gasite " + places.size() + " locatii.");

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
                    openGoogleMaps(selected.lat, selected.lon);
                })
                .setNegativeButton("INCHIDE", null)
                .show();
    }

    // HARTA GPS

    private void showMap() {

        mapOpened = true;

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.WHITE);

        Button back = new Button(this);
        back.setText("INAPOI LA GPS ALEX");
        back.setTextColor(Color.WHITE);
        back.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(BLUE));

        screen.addView(back);

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextSize(16);
        addressInput.setHint("Cauta adresa sau localitatea...");
        addressInput.setPadding(15, 8, 15, 8);

        screen.addView(addressInput,
                new LinearLayout.LayoutParams(-1, 55));

        Button addressButton = new Button(this);
        addressButton.setText("CAUTA ADRESA");
        addressButton.setTextColor(Color.WHITE);
        addressButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(BLUE));

        screen.addView(addressButton);

        coordinateInput = new EditText(this);
        coordinateInput.setSingleLine(true);
        coordinateInput.setTextSize(16);
        coordinateInput.setHint("Coordonate: 45.4642, 9.1900");
        coordinateInput.setPadding(15, 8, 15, 8);

        screen.addView(coordinateInput,
                new LinearLayout.LayoutParams(-1, 55));

        Button coordinateButton = new Button(this);
        coordinateButton.setText("CAUTA COORDONATE");
        coordinateButton.setTextColor(Color.WHITE);
        coordinateButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(BLUE));

        screen.addView(coordinateButton);

        Button navigateButton = new Button(this);
        navigateButton.setText("NAVIGHEAZA LA DESTINATIE");
        navigateButton.setTextColor(Color.WHITE);
        navigateButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(BLUE));

        screen.addView(navigateButton);

        mapView = new MapView(this);

        screen.addView(mapView,
                new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(screen);

        back.setOnClickListener(v -> returnHome());

        addressButton.setOnClickListener(v -> searchAddress());

        coordinateButton.setOnClickListener(v -> searchCoordinates());

        navigateButton.setOnClickListener(v -> navigateToDestination());

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
                                            .target(new LatLng(latitude, longitude))
                                            .zoom(12)
                                            .build());
                        } else {
                            map.setCameraPosition(
                                    new CameraPosition.Builder()
                                            .target(new LatLng(45.4642, 9.1900))
                                            .zoom(5)
                                            .build());
                        }
                    });
        });
    }

    // CAUTARE ADRESA CU OPENSTREETMAP

    private void searchAddress() {

        String address = addressInput.getText().toString().trim();

        if (address.isEmpty()) {
            Toast.makeText(this,
                    "Introdu o adresa.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this,
                "Se cauta adresa...",
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {

            try {

                String urlText =
                        "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" +
                        URLEncoder.encode(address, "UTF-8");

                URL url = new URL(urlText);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);

                connection.setRequestProperty(
                        "User-Agent",
                        "GPS-ALEX-TRUCK-NAVI/1.0");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONArray results = new JSONArray(result.toString());

                if (results.length() == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Adresa nu a fost gasita.",
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                JSONObject item = results.getJSONObject(0);

                double lat = Double.parseDouble(item.getString("lat"));
                double lon = Double.parseDouble(item.getString("lon"));

                runOnUiThread(() -> setDestination(lat, lon));

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Eroare la cautarea adresei. Incearca din nou.",
                                Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    // INTRODUCERE COORDONATE

    private void searchCoordinates() {

        String value = coordinateInput.getText().toString().trim();

        try {

            String[] parts = value.split("[,;\\s]+");

            if (parts.length != 2) {
                throw new Exception();
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new Exception();
            }

            setDestination(lat, lon);

        } catch (Exception e) {

            Toast.makeText(this,
                    "Introdu coordonatele astfel: 45.4642, 9.1900",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setDestination(double lat, double lon) {

        destinationLat = lat;
        destinationLon = lon;

        if (map != null) {
            map.setCameraPosition(
                    new CameraPosition.Builder()
                            .target(new LatLng(lat, lon))
                            .zoom(15)
                            .build());
        }

        Toast.makeText(this,
                "Destinatia a fost selectata.",
                Toast.LENGTH_LONG).show();
    }

    private void navigateToDestination() {

        if (destinationLat == 0 && destinationLon == 0) {
            Toast.makeText(this,
                    "Cauta mai intai o adresa sau coordonate.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        openGoogleMaps(destinationLat, destinationLon);
    }

    private void openGoogleMaps(double lat, double lon) {

        String url =
                "https://www.google.com/maps/dir/?api=1&destination=" +
                lat + "," + lon;

        Intent intent = new Intent(
                Intent.ACTION_VIEW, Uri.parse(url));

        startActivity(intent);
    }

    // GPS

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

    private void returnHome() {

        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }

        map = null;
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
