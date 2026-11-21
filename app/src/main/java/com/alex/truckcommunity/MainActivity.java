
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private MapView map;
    private LocationManager locationManager;
    private TextView status;
    private EditText startInput;
    private EditText destinationInput;
    private Button instructionsButton;

    private TextToSpeech tts;
    private boolean voiceReady = false;

    private Marker startMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private String routeInstructions = "";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private static final int LOCATION_PERMISSION = 100;

    private GeoPoint currentPosition;

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
        title.setPadding(8, 14, 8, 14);
        title.setBackgroundColor(Color.rgb(23, 43, 58));
        root.addView(title);

        startInput = new EditText(this);
        startInput.setSingleLine(true);
        startInput.setTextColor(Color.WHITE);
        startInput.setHintTextColor(Color.LTGRAY);
        startInput.setHint("Plecare (gol = poziția GPS)");
        startInput.setPadding(12, 8, 12, 8);
        root.addView(startInput);

        destinationInput = new EditText(this);
        destinationInput.setSingleLine(true);
        destinationInput.setTextColor(Color.WHITE);
        destinationInput.setHintTextColor(Color.LTGRAY);
        destinationInput.setHint("Destinație: ex. Roma, Italia");
        destinationInput.setPadding(12, 8, 12, 8);
        root.addView(destinationInput);

        Button routeButton = new Button(this);
        routeButton.setText("CALCULEAZĂ TRASEUL");
        routeButton.setOnClickListener(v -> calculateRoute());
        root.addView(routeButton);

        instructionsButton = new Button(this);
        instructionsButton.setText("ASCULTĂ INSTRUCȚIUNILE");
        instructionsButton.setEnabled(false);
        instructionsButton.setOnClickListener(v -> readRouteInstructions());
        root.addView(instructionsButton);

        status = new TextView(this);
        status.setText("Se pregătește harta...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(8, 8, 8, 8);
        root.addView(status);

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setUseDataConnection(true);
        map.setBuiltInZoomControls(false);
        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(19.0);

        map.getController().setZoom(5.0);
        map.getController().setCenter(new GeoPoint(45.0, 10.0));

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1.0f
                );

        root.addView(map, mapParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button gpsButton = new Button(this);
        gpsButton.setText("POZIȚIA MEA");
        gpsButton.setOnClickListener(v -> {
            checkLocationPermission();
            goToMyLocation();
        });

        Button zoomIn = new Button(this);
        zoomIn.setText("+");
        zoomIn.setOnClickListener(v -> map.getController().zoomIn());

        Button zoomOut = new Button(this);
        zoomOut.setText("−");
        zoomOut.setOnClickListener(v -> map.getController().zoomOut());

        buttons.addView(gpsButton,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 2));

        buttons.addView(zoomIn,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        buttons.addView(zoomOut,
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        root.addView(buttons);

        Button voiceButton = new Button(this);
        voiceButton.setText("TESTEAZĂ VOCEA");
        voiceButton.setOnClickListener(v ->
                speak("Bine ai venit în Truck Alex Community.")
        );
        root.addView(voiceButton);

        TextView attribution = new TextView(this);
        attribution.setText("Hartă © OpenStreetMap contributors");
        attribution.setTextColor(Color.LTGRAY);
        attribution.setTextSize(10);
        attribution.setGravity(Gravity.CENTER);
        root.addView(attribution);

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
        }
    }

    private void readRouteInstructions() {

        if (routeInstructions.isEmpty()) {
            Toast.makeText(
                    this,
                    "Calculează mai întâi un traseu.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!voiceReady) {
            Toast.makeText(
                    this,
                    "Vocea în limba română nu este pregătită.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String[] instructions = routeInstructions.split("\n");

        tts.stop();

        for (int i = 0; i < instructions.length; i++) {

            String instruction = instructions[i].trim();

            if (!instruction.isEmpty()) {
                tts.speak(
                        instruction,
                        i == 0
                                ? TextToSpeech.QUEUE_FLUSH
                                : TextToSpeech.QUEUE_ADD,
                        null,
                        "route_step_" + i
                );
            }
        }
    }

    private String translateManeuver(JSONObject maneuver)
            throws Exception {

        String type = maneuver.optString("type", "");
        String modifier = maneuver.optString("modifier", "");

        switch (type) {

            case "depart":
                return "Porniți de la punctul de plecare.";

            case "arrive":
                return "Ați ajuns la destinație.";

            case "turn":
            case "end of road":
            case "fork":
                return directionText(modifier);

            case "continue":
                return "Continuați înainte.";

            case "new name":
                return "Continuați pe drumul următor.";

            case "merge":
                return "Încadrați-vă pe drumul principal.";

            case "on ramp":
                return "Intrați pe bretea.";

            case "off ramp":
                return "Ieșiți de pe bretea.";

            case "roundabout":
            case "rotary":
                int exit = maneuver.optInt("exit", 0);

                if (exit > 0) {
                    return "Intrați în sensul giratoriu și ieșiți la a "
                            + exit + "-a ieșire.";
                }

                return "Intrați în sensul giratoriu.";

            case "roundabout turn":
                return "Urmați traseul prin sensul giratoriu.";

            case "uturn":
                return "Întoarceți.";

            default:
                return "Continuați pe traseu.";
        }
    }

    private String directionText(String modifier) {

        switch (modifier) {

            case "left":
                return "Virați la stânga.";

            case "right":
                return "Virați la dreapta.";

            case "slight left":
                return "Virați ușor la stânga.";

            case "slight right":
                return "Virați ușor la dreapta.";

            case "sharp left":
                return "Virați puternic la stânga.";

            case "sharp right":
                return "Virați puternic la dreapta.";

            case "uturn":
                return "Întoarceți.";

            default:
                return "Continuați înainte.";
        }
    }

    private String buildRouteInstructions(JSONObject route)
            throws Exception {

        StringBuilder result = new StringBuilder();

        JSONArray legs = route.getJSONArray("legs");

        for (int legIndex = 0;
             legIndex < legs.length();
             legIndex++) {

            JSONObject leg = legs.getJSONObject(legIndex);
            JSONArray steps = leg.getJSONArray("steps");

            for (int i = 0; i < steps.length(); i++) {

                JSONObject step = steps.getJSONObject(i);

                JSONObject maneuver =
                        step.getJSONObject("maneuver");

                double distance =
                        step.optDouble("distance", 0);

                String instruction =
                        translateManeuver(maneuver);

                if (distance >= 50) {

                    int meters = (int)
                            (Math.round(distance / 50.0) * 50);

                    result.append(
                            "După aproximativ "
                                    + meters
                                    + " metri, "
                                    + instruction
                    );

                } else {
                    result.append(instruction);
                }

                String roadName = step.optString("name", "");

                if (!roadName.isEmpty()) {
                    result.append(" Drumul se numește ")
                            .append(roadName)
                            .append(".");
                }

                result.append("\n");
            }
        }

        if (result.length() == 0) {
            result.append("Nu există instrucțiuni disponibile.");
        }

        return result.toString();
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
            getCurrentGPS();
        }
    }

    private void getCurrentGPS() {

        try {

            Location location =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (location == null) {
                location = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            if (location != null) {

                currentPosition = new GeoPoint(
                        location.getLatitude(),
                        location.getLongitude()
                );

                updateGPSMarker(currentPosition);
            }

        } catch (SecurityException e) {
            status.setText("Permisiunea GPS nu este disponibilă.");
        } catch (Exception e) {
            status.setText("Nu s-a putut obține poziția GPS.");
        }
    }

    private void updateGPSMarker(GeoPoint point) {

        if (startMarker == null) {

            startMarker = new Marker(map);
            startMarker.setTitle("Poziția mea");
            startMarker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            map.getOverlays().add(startMarker);
        }

        startMarker.setPosition(point);
        map.invalidate();
    }

    private void goToMyLocation() {

        getCurrentGPS();

        if (currentPosition != null) {
            map.getController().animateTo(currentPosition);
            map.getController().setZoom(16.0);
        } else {
            status.setText(
                    "Nu există încă o poziție GPS. Încearcă afară."
            );
        }
    }

    private void calculateRoute() {

        String start = startInput.getText().toString().trim();
        String destination =
                destinationInput.getText().toString().trim();

        if (destination.isEmpty()) {
            Toast.makeText(
                    this,
                    "Introdu destinația.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (start.isEmpty()) {
            getCurrentGPS();

            if (currentPosition == null) {
                status.setText(
                        "Nu avem poziție GPS. Introdu orașul de plecare."
                );
                return;
            }
        }

        instructionsButton.setEnabled(false);
        routeInstructions = "";

        if (tts != null) {
            tts.stop();
        }

        status.setText("Se caută orașele și se calculează traseul...");

        executor.execute(() -> {

            try {

                GeoPoint origin;

                if (start.isEmpty()) {
                    origin = currentPosition;
                } else {
                    origin = searchCity(start);
                }

                if (!start.isEmpty()) {
                    Thread.sleep(1100);
                }

                GeoPoint destinationPoint = searchCity(destination);

                JSONObject route = getRoute(origin, destinationPoint);

                String instructions =
                        buildRouteInstructions(route);

                handler.post(() -> {
                    showRoute(origin, destinationPoint, route);
                    routeInstructions = instructions;
                    instructionsButton.setEnabled(true);
                });

            } catch (Exception e) {

                handler.post(() -> status.setText(
                        "Eroare: " + e.getMessage()
                ));
            }
        });
    }

    private GeoPoint searchCity(String city) throws Exception {

        String encoded = URLEncoder.encode(city, "UTF-8");

        String address =
                "https://nominatim.openstreetmap.org/search"
                + "?format=jsonv2&limit=1&q=" + encoded;

        JSONArray results = new JSONArray(httpGet(address));

        if (results.length() == 0) {
            throw new Exception("Orașul nu a fost găsit: " + city);
        }

        JSONObject item = results.getJSONObject(0);

        double latitude = item.getDouble("lat");
        double longitude = item.getDouble("lon");

        return new GeoPoint(latitude, longitude);
    }

    private JSONObject getRoute(
            GeoPoint origin,
            GeoPoint destination) throws Exception {

        String coordinates =
                origin.getLongitude() + "," + origin.getLatitude()
                + ";" +
                destination.getLongitude() + ","
                + destination.getLatitude();

        String address =
                "https://router.project-osrm.org/route/v1/driving/"
                + coordinates
                + "?overview=full&geometries=geojson&steps=true";

        JSONObject response = new JSONObject(httpGet(address));

        if (!"Ok".equals(response.optString("code"))) {
            throw new Exception("Nu s-a găsit un traseu rutier.");
        }

        JSONArray routes = response.getJSONArray("routes");

        if (routes.length() == 0) {
            throw new Exception("Nu există traseu disponibil.");
        }

        return routes.getJSONObject(0);
    }

    private String httpGet(String address) throws Exception {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(address);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);

            connection.setRequestProperty(
                    "User-Agent",
                    "TRUCK-ALEX-COMMUNITY/1.0 (Android app)"
            );

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                throw new Exception(
                        "Server indisponibil. Cod: " + responseCode
                );
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            "UTF-8"
                    )
            );

            StringBuilder result = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();

            return result.toString();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showRoute(
            GeoPoint origin,
            GeoPoint destination,
            JSONObject route) {

        try {

            JSONArray coordinates =
                    route.getJSONObject("geometry")
                            .getJSONArray("coordinates");

            ArrayList<GeoPoint> points = new ArrayList<>();

            for (int i = 0; i < coordinates.length(); i++) {

                JSONArray coordinate = coordinates.getJSONArray(i);

                double longitude = coordinate.getDouble(0);
                double latitude = coordinate.getDouble(1);

                points.add(new GeoPoint(latitude, longitude));
            }

            if (routeLine != null) {
                map.getOverlays().remove(routeLine);
            }

            routeLine = new Polyline();
            routeLine.setPoints(points);
            routeLine.setColor(Color.rgb(0, 150, 255));
            routeLine.setWidth(10f);

            map.getOverlays().add(routeLine);

            Marker destinationPin = new Marker(map);
            destinationPin.setPosition(destination);
            destinationPin.setTitle("Destinație");

            if (destinationMarker != null) {
                map.getOverlays().remove(destinationMarker);
            }

            destinationMarker = destinationPin;
            map.getOverlays().add(destinationMarker);

            if (startMarker != null) {
                map.getOverlays().remove(startMarker);
            }

            startMarker = new Marker(map);
            startMarker.setPosition(origin);
            startMarker.setTitle("Plecare");

            map.getOverlays().add(startMarker);

            double distanceKm =
                    route.getDouble("distance") / 1000.0;

            double durationHours =
                    route.getDouble("duration") / 3600.0;

            int hours = (int) durationHours;
            int minutes = (int) ((durationHours - hours) * 60);

            String message = String.format(
                    Locale.getDefault(),
                    "TRASEU CALCULAT\nDistanță: %.1f km\nDurată estimată: %d h %d min",
                    distanceKm,
                    hours,
                    minutes
            );

            status.setText(message);

            map.zoomToBoundingBox(
                    routeLine.getBounds(),
                    true,
                    60
            );

            map.invalidate();

            speak("Traseul a fost calculat. "
                    + String.format(
                            Locale.getDefault(),
                            "Distanța este de aproximativ %.0f kilometri.",
                            distanceKm
                    ));

        } catch (Exception e) {

            status.setText("Eroare la afișarea traseului.");
        }
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

                getCurrentGPS();

            } else {
                status.setText("Permisiunea GPS nu a fost acordată.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map != null) {
            map.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executor.shutdownNow();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (map != null) {
            map.onDetach();
        }
    }
}
