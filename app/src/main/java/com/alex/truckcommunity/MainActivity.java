
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
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
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.Polyline;
import org.maplibre.android.annotations.PolylineOptions;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private TextView status;

    private MapView mapView;
    private MapLibreMap map;

    private Marker truckMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private LocationManager locationManager;

    private EditText addressInput;
    private EditText coordinateInput;

    private double latitude = 0;
    private double longitude = 0;

    private double destinationLat = 0;
    private double destinationLon = 0;

    private boolean destinationSelected = false;
    private boolean mapOpened = false;

    private static final int GPS_PERMISSION = 100;

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

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density + 0.5f);
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

    // MENIU PRINCIPAL

    private void showHome() {

        mapOpened = false;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(12), dp(5), dp(12), dp(5));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(5), dp(5), dp(5), dp(5));
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GPS ALEX");
        title.setTextColor(Color.rgb(20, 45, 70));
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("CAMION • GPS • NAVIGATIE");
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
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
                "Ghidarea vocala va fi adaugata intr-o etapa urmatoare."));

        addButton("DESPRE APLICATIE", () ->
            showMessage("GPS ALEX",
                "GPS ALEX TRUCK NAVI\nVersiunea 1.0"));

        TextView searchTitle = new TextView(this);
        searchTitle.setText("CAUTARE LOCATII");
        searchTitle.setTextColor(Color.rgb(20, 45, 70));
        searchTitle.setTextSize(18);
        searchTitle.setTypeface(null, Typeface.BOLD);
        searchTitle.setGravity(Gravity.CENTER);
        layout.addView(searchTitle);

        addButton("PARCARI TIR", () -> searchPlaces("parking"));
        addButton("MOTORINA", () -> searchPlaces("fuel"));
        addButton("SERVICE CAMIOANE", () -> searchPlaces("service"));

        status = new TextView(this);
        status.setText("GPS ALEX pregatit pentru navigare");
        status.setTextColor(Color.DKGRAY);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        scroll.addView(layout);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 7));

        LinearLayout mapContainer = new LinearLayout(this);
        mapContainer.setOrientation(LinearLayout.VERTICAL);

        TextView mapTitle = new TextView(this);
        mapTitle.setText("HARTA GPS - POZITIA CAMIONULUI");
        mapTitle.setTextColor(Color.rgb(20, 45, 70));
        mapTitle.setTextSize(16);
        mapTitle.setTypeface(null, Typeface.BOLD);
        mapTitle.setGravity(Gravity.CENTER);

        mapContainer.addView(mapTitle,
            new LinearLayout.LayoutParams(-1, dp(35)));

        mapView = new MapView(this);

        mapContainer.addView(mapView,
            new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(mapContainer,
            new LinearLayout.LayoutParams(-1, 0, 3));

        setContentView(root);

        initializeMap();
    }

    // BUTOANE

    private void addButton(String name, Runnable action) {

        TextView button = new TextView(this);

        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.BLACK);
        background.setCornerRadius(dp(12));
        button.setBackground(background);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, dp(48));

        params.setMargins(0, dp(3), 0, dp(3));

        layout.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private TextView createActionButton(String name) {

        TextView button = new TextView(this);
        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.BLACK);
        background.setCornerRadius(dp(10));

        button.setBackground(background);

        return button;
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("INCHIDE", null)
            .show();
    }

    // INITIALIZARE HARTA

    private void initializeMap() {

        if (mapView == null) return;

        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();

        mapView.getMapAsync(mapLibreMap -> {

            map = mapLibreMap;

            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty",
                style -> {

                    double lat = latitude != 0 ? latitude : 45.4642;
                    double lon = longitude != 0 ? longitude : 9.1900;

                    map.setCameraPosition(
                        new CameraPosition.Builder()
                            .target(new LatLng(lat, lon))
                            .zoom(12)
                            .build());

                    addTruckMarker(lat, lon);

                    map.addOnMapClickListener(point -> {
                        setDestination(
                            point.getLatitude(),
                            point.getLongitude());
                        return true;
                    });
                });
        });
    }

    private void addTruckMarker(double lat, double lon) {

        if (map == null) return;

        if (truckMarker != null) {
            map.removeMarker(truckMarker);
        }

        truckMarker = map.addMarker(
            new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title("CAMIONUL MEU"));
    }

    private void updateTruckPosition() {

        if (map == null || latitude == 0 || longitude == 0) return;

        addTruckMarker(latitude, longitude);
    }

    // DESTINATIE

    private void setDestination(double lat, double lon) {

        destinationLat = lat;
        destinationLon = lon;
        destinationSelected = true;

        if (map != null) {

            if (destinationMarker != null) {
                map.removeMarker(destinationMarker);
            }

            destinationMarker = map.addMarker(
                new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .title("DESTINATIE"));

            map.setCameraPosition(
                new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(14)
                    .build());
        }

        Toast.makeText(this,
            "Destinatia a fost selectata.",
            Toast.LENGTH_SHORT).show();
    }

    private void showDestinationConfirmation() {

        if (!destinationSelected) return;

        String message =
            "Latitudine: " + destinationLat + "\n" +
            "Longitudine: " + destinationLon +
            "\n\nConfirma destinatia?";

        new AlertDialog.Builder(this)
            .setTitle("CONFIRMA DESTINATIA")
            .setMessage(message)
            .setPositiveButton("CONFIRMA",
                (dialog, which) -> showTruckProfileConfirmation())
            .setNegativeButton("ANULEAZA", null)
            .show();
    }

    private void showTruckProfileConfirmation() {

        String profile =
            "PROFIL CAMION ACTIV\n\n" +
            "Inaltime: 4,00 m\n" +
            "Latime: 2,55 m\n" +
            "Lungime: 16,50 m\n" +
            "Greutate: 40 tone\n\n" +
            "Datele hartilor si restrictiile pot fi incomplete. " +
            "Verifica traseul si indicatoarele rutiere.";

        new AlertDialog.Builder(this)
            .setTitle("CONFIRMA PROFILUL CAMIONULUI")
            .setMessage(profile)
            .setPositiveButton("CALCULEAZA TRASEUL",
                (dialog, which) -> requestTruckRoute())
            .setNegativeButton("INAPOI", null)
            .show();
    }

    // CALCUL TRASEU VALHALLA

    private void requestTruckRoute() {

        if (map == null || !destinationSelected) {
            Toast.makeText(this,
                "Harta sau destinatia nu este disponibila.",
                Toast.LENGTH_LONG).show();
            return;
        }

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this,
                "Pozitia GPS nu este disponibila.",
                Toast.LENGTH_LONG).show();
            return;
        }

        final double startLat = latitude;
        final double startLon = longitude;
        final double endLat = destinationLat;
        final double endLon = destinationLon;

        Toast.makeText(this,
            "Se calculeaza traseul...",
            Toast.LENGTH_LONG).show();

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                    "https://valhalla1.openstreetmap.de/route");

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(40000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                    "Content-Type", "application/json");

                JSONObject truck = new JSONObject();
                truck.put("height", 4.0);
                truck.put("width", 2.55);
                truck.put("length", 16.5);
                truck.put("weight", 40.0);

                JSONObject costingOptions = new JSONObject();
                costingOptions.put("truck", truck);

                JSONArray locations = new JSONArray();

                JSONObject start = new JSONObject();
                start.put("lat", startLat);
                start.put("lon", startLon);
                locations.put(start);

                JSONObject end = new JSONObject();
                end.put("lat", endLat);
                end.put("lon", endLon);
                locations.put(end);

                JSONObject request = new JSONObject();
                request.put("locations", locations);
                request.put("costing", "truck");
                request.put("costing_options", costingOptions);
                request.put("units", "kilometers");
                request.put("language", "ro-RO");
                request.put("format", "json");
                request.put("shape_format", "polyline6");
                request.put("directions_type", "instructions");

                OutputStream output = connection.getOutputStream();
                output.write(request.toString().getBytes("UTF-8"));
                output.flush();
                output.close();

                int responseCode = connection.getResponseCode();

                java.io.InputStream input =
                    responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, "UTF-8"));

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode != 200) {
                    throw new Exception(
                        "Server HTTP " + responseCode);
                }

                JSONObject result =
                    new JSONObject(response.toString());

                JSONObject trip = result.getJSONObject("trip");

                JSONArray legs = trip.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);

                ArrayList<LatLng> points =
                    decodePolyline6(leg.getString("shape"));

                if (points.size() < 2) {
                    throw new Exception("Traseu invalid.");
                }

                JSONObject summary = trip.getJSONObject("summary");

                double distance = summary.optDouble("length", 0);
                double seconds = summary.optDouble("time", 0);

                JSONArray maneuvers =
                    leg.optJSONArray("maneuvers");

                StringBuilder instructions = new StringBuilder();

                if (maneuvers != null) {

                    for (int i = 0;
                         i < maneuvers.length() && i < 12;
                         i++) {

                        JSONObject maneuver =
                            maneuvers.getJSONObject(i);

                        String instruction =
                            maneuver.optString("instruction", "");

                        if (!instruction.isEmpty()) {
                            instructions.append(i + 1)
                                .append(". ")
                                .append(instruction)
                                .append("\n\n");
                        }
                    }
                }

                final double finalDistance = distance;
                final double finalSeconds = seconds;
                final String finalInstructions =
                    instructions.toString();

                runOnUiThread(() -> {

                    if (map == null) return;

                    if (routeLine != null) {
                        map.removePolyline(routeLine);
                    }

                    routeLine = map.addPolyline(
                        new PolylineOptions()
                            .addAll(points)
                            .color(Color.BLUE)
                            .width(6));

                    map.setCameraPosition(
                        new CameraPosition.Builder()
                            .target(new LatLng(startLat, startLon))
                            .zoom(10)
                            .build());

                    String message =
                        "DISTANTA: " +
                        String.format(Locale.US, "%.1f", finalDistance) +
                        " km\n\n" +
                        "DURATA ESTIMATA: " +
                        (long) (finalSeconds / 60) +
                        " minute\n\n" +
                        "INDICATII RUTIERE:\n\n" +
                        (finalInstructions.isEmpty()
                            ? "Nu sunt disponibile indicatii."
                            : finalInstructions) +
                        "\nATENTIE: Verifica separat toate restrictiile " +
                        "de inaltime, latime, lungime si greutate. " +
                        "Traseul nu este o garantie de siguranta.";

                    showMessage("TRASEU CALCULAT", message);
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(this,
                        "Eroare la calcularea traseului: " +
                        e.getMessage(),
                        Toast.LENGTH_LONG).show());

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private ArrayList<LatLng> decodePolyline6(String encoded) {

        ArrayList<LatLng> points = new ArrayList<>();

        int index = 0;
        int lat = 0;
        int lon = 0;

        while (index < encoded.length()) {

            int result = 0;
            int shift = 0;
            int b;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < encoded.length());

            int deltaLat = (result & 1) != 0
                ? ~(result >> 1)
                : (result >> 1);

            lat += deltaLat;

            result = 0;
            shift = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < encoded.length());

            int deltaLon = (result & 1) != 0
                ? ~(result >> 1)
                : (result >> 1);

            lon += deltaLon;

            points.add(new LatLng(
                lat / 1000000.0,
                lon / 1000000.0));
        }

        return points;
    }

    // CAUTARE LOCATII

    private void searchPlaces(String category) {

        if (latitude == 0 || longitude == 0) {
            getGPS();
            Toast.makeText(this,
                "Asteptam pozitia GPS.",
                Toast.LENGTH_LONG).show();
            return;
        }

        if (status != null) {
            status.setText("Se cauta locatii pe 25 km...");
        }

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
                    throw new Exception("Serverele nu raspund.", lastError);
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

                        JSONObject center =
                            item.getJSONObject("center");

                        lat = center.getDouble("lat");
                        lon = center.getDouble("lon");

                    } else {
                        continue;
                    }

                    JSONObject tags = item.optJSONObject("tags");

                    String name = "Locatie fara nume";

                    if (tags != null) {
                        name = tags.optString(
                            "name",
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

                    if (status != null) {
                        status.setText("Cautarea nu este disponibila.");
                    }

                    Toast.makeText(this,
                        "Eroare la cautarea locatiilor.",
                        Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private String postOverpass(String server, String query)
        throws Exception {

        HttpURLConnection connection =
            (HttpURLConnection) new URL(server).openConnection();

        try {

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

            String data = "data=" +
                URLEncoder.encode(query, "UTF-8");

            OutputStream output = connection.getOutputStream();
            output.write(data.getBytes("UTF-8"));
            output.flush();
            output.close();

            int code = connection.getResponseCode();

            if (code != 200) {
                throw new Exception("HTTP " + code);
            }

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();

            return result.toString();

        } finally {
            connection.disconnect();
        }
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
                String.format(Locale.US, "%.1f km",
                    p.distance / 1000f);
        }

        status.setText("Au fost gasite " + places.size() + " locatii.");

        String title = category.equals("parking")
            ? "PARCARI"
            : category.equals("fuel")
                ? "MOTORINA"
                : "SERVICE CAMIOANE";

        new AlertDialog.Builder(this)
            .setTitle(title + " - 25 KM")
            .setItems(names, (dialog, which) -> {
                Place selected = places.get(which);
                openGoogleMaps(selected.lat, selected.lon);
            })
            .setNegativeButton("INCHIDE", null)
            .show();
    }

    // HARTA PE TOT ECRANUL

    private void showMap() {

        destroyCurrentMap();

        mapOpened = true;

        destinationSelected = false;
        destinationLat = 0;
        destinationLon = 0;

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(8), dp(5), dp(8), dp(5));
        screen.setBackgroundColor(Color.WHITE);

        TextView back = createActionButton("INAPOI LA GPS ALEX");

        screen.addView(back,
            new LinearLayout.LayoutParams(-1, dp(45)));

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextSize(16);
        addressInput.setHint("Cauta adresa sau localitatea...");

        screen.addView(addressInput,
            new LinearLayout.LayoutParams(-1, dp(50)));

        TextView addressButton = createActionButton("CAUTA ADRESA");

        screen.addView(addressButton,
            new LinearLayout.LayoutParams(-1, dp(42)));

        coordinateInput = new EditText(this);
        coordinateInput.setSingleLine(true);
        coordinateInput.setTextSize(16);
        coordinateInput.setHint("Coordonate: 45.4642, 9.1900");

        screen.addView(coordinateInput,
            new LinearLayout.LayoutParams(-1, dp(50)));

        TextView coordinateButton =
            createActionButton("CAUTA COORDONATE");

        screen.addView(coordinateButton,
            new LinearLayout.LayoutParams(-1, dp(42)));

        TextView navigateButton =
            createActionButton("CONFIRMA SI CALCULEAZA TRASEUL");

        screen.addView(navigateButton,
            new LinearLayout.LayoutParams(-1, dp(42)));

        TextView mapHint = new TextView(this);
        mapHint.setText("Atinge harta pentru a selecta destinatia.");
        mapHint.setTextColor(Color.DKGRAY);
        mapHint.setGravity(Gravity.CENTER);

        screen.addView(mapHint,
            new LinearLayout.LayoutParams(-1, dp(28)));

        mapView = new MapView(this);

        screen.addView(mapView,
            new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(screen);

        back.setOnClickListener(v -> returnHome());

        addressButton.setOnClickListener(v -> searchAddress());

        coordinateButton.setOnClickListener(v -> searchCoordinates());

        navigateButton.setOnClickListener(v -> {

            if (destinationSelected) {
                showDestinationConfirmation();
            } else {
                Toast.makeText(this,
                    "Selecteaza mai intai o destinatie.",
                    Toast.LENGTH_LONG).show();
            }
        });

        initializeMap();
    }

    // CAUTARE ADRESA

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

            HttpURLConnection connection = null;

            try {

                String urlText =
                    "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" +
                    URLEncoder.encode(address, "UTF-8");

                connection = (HttpURLConnection)
                    new URL(urlText).openConnection();

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
                        "Eroare la cautarea adresei.",
                        Toast.LENGTH_LONG).show());

            } finally {
                if (connection != null) connection.disconnect();
            }

        }).start();
    }

    // COORDONATE

    private void searchCoordinates() {

        String value = coordinateInput.getText().toString().trim();

        try {

            String[] parts = value.split("[,;\\s]+");

            if (parts.length != 2) throw new Exception();

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            if (lat < -90 || lat > 90 ||
                lon < -180 || lon > 180) {
                throw new Exception();
            }

            setDestination(lat, lon);

        } catch (Exception e) {

            Toast.makeText(this,
                "Introdu: 45.4642, 9.1900",
                Toast.LENGTH_LONG).show();
        }
    }

    // GOOGLE MAPS PENTRU ORIENTARE

    private void openGoogleMaps(double lat, double lon) {

        String url =
            "https://www.google.com/maps/search/?api=1&query=" +
            lat + "," + lon;

        try {

            android.content.Intent intent =
                new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url));

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(this,
                "Google Maps nu este disponibil.",
                Toast.LENGTH_LONG).show();
        }
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
                updateTruckPosition();
            }

            locationManager.removeUpdates(locationListener);

            locationManager.requestLocationUpdates(
                provider,
                5000,
                10,
                locationListener);

        } catch (Exception e) {

            if (status != null) {
                status.setText("GPS indisponibil.");
            }
        }
    }

    private final LocationListener locationListener =
        new LocationListener() {

            @Override
            public void onLocationChanged(Location loc) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
                updateTruckPosition();
            }

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}

            @Override
            public void onStatusChanged(
                String provider, int status, Bundle extras) {}
        };

    // CICLU HARTA

    private void destroyCurrentMap() {

        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }

        map = null;
        truckMarker = null;
        destinationMarker = null;
        routeLine = null;
    }

    private void returnHome() {

        destroyCurrentMap();
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

        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
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

            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                getGPS();
            } else {
                Toast.makeText(this,
                    "Permisiunea GPS nu a fost acordata.",
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
