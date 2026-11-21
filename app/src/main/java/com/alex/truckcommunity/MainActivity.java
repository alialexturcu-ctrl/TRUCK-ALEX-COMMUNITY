
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
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
    private TextView distanceText;
    private TextView durationText;

    private EditText startInput;
    private EditText destinationInput;

    private Button instructionsButton;

    private TextToSpeech tts;
    private boolean voiceReady = false;

    private Marker startMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private String routeInstructions = "";

    private GeoPoint currentPosition;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private static final int LOCATION_PERMISSION = 100;

    private final int BACKGROUND =
            Color.rgb(12, 19, 27);

    private final int PANEL =
            Color.rgb(25, 37, 49);

    private final int GREEN =
            Color.rgb(0, 160, 105);

    private final int BLUE =
            Color.rgb(0, 135, 220);

    private final int WHITE =
            Color.WHITE;

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

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable background(
            int color,
            int radius) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private TextView makeText(
            String value,
            int size,
            int color,
            boolean bold) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        return text;
    }

    private Button makeButton(
            String value,
            int color) {

        Button button = new Button(this);

        button.setText(value);
        button.setTextColor(WHITE);
        button.setTextSize(12);
        button.setTypeface(null, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(background(color, 14));
        button.setPadding(dp(8), dp(8), dp(8), dp(8));

        return button;
    }

    private EditText makeInput(String hint) {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setTextColor(WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setHint(hint);
        input.setTextSize(14);
        input.setPadding(dp(14), dp(8), dp(14), dp(8));
        input.setBackground(background(
                Color.rgb(38, 52, 66), 12
        ));

        return input;
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(10), dp(6), dp(10), dp(6));

        // HEADER

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        header.setBackground(background(PANEL, 16));

        LinearLayout logoArea = new LinearLayout(this);
        logoArea.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText(
                "TRUCK ALEX",
                21,
                WHITE,
                true
        );

        TextView subtitle = makeText(
                "COMMUNITY NAVIGATION",
                10,
                Color.rgb(0, 210, 145),
                true
        );

        logoArea.addView(title);
        logoArea.addView(subtitle);

        header.addView(
                logoArea,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView online = makeText(
                "● GPS",
                12,
                Color.rgb(0, 220, 140),
                true
        );

        header.addView(online);
        root.addView(header);

        addSpace(root, 6);

        // SEARCH PANEL

        LinearLayout searchPanel = new LinearLayout(this);
        searchPanel.setOrientation(LinearLayout.VERTICAL);
        searchPanel.setPadding(dp(10), dp(8), dp(10), dp(8));
        searchPanel.setBackground(background(PANEL, 16));

        TextView searchTitle = makeText(
                "PLANIFICĂ TRASEUL",
                13,
                WHITE,
                true
        );

        searchPanel.addView(searchTitle);

        addSpace(searchPanel, 5);

        startInput = makeInput(
                "Plecare (gol = poziția GPS)"
        );

        searchPanel.addView(startInput);

        addSpace(searchPanel, 5);

        destinationInput = makeInput(
                "Destinație: ex. Roma, Italia"
        );

        searchPanel.addView(destinationInput);

        addSpace(searchPanel, 5);

        Button routeButton = makeButton(
                "CALCULEAZĂ TRASEUL",
                GREEN
        );

        routeButton.setOnClickListener(v -> calculateRoute());

        searchPanel.addView(
                routeButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(44)
                )
        );

        root.addView(searchPanel);

        addSpace(root, 6);

        // MAP

        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setUseDataConnection(true);
        map.setBuiltInZoomControls(false);
        map.setMinZoomLevel(3.0);
        map.setMaxZoomLevel(19.0);

        map.getController().setZoom(5.0);
        map.getController().setCenter(
                new GeoPoint(45.0, 10.0)
        );

        LinearLayout.LayoutParams mapParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1.0f
                );

        mapParams.setMargins(0, 0, 0, dp(6));

        root.addView(map, mapParams);

        // ROUTE INFORMATION

        LinearLayout information = new LinearLayout(this);
        information.setOrientation(LinearLayout.VERTICAL);
        information.setPadding(dp(12), dp(8), dp(12), dp(8));
        information.setBackground(background(PANEL, 16));

        status = makeText(
                "Aștept introducerea destinației...",
                12,
                Color.LTGRAY,
                false
        );

        information.addView(status);

        addSpace(information, 5);

        LinearLayout statistics = new LinearLayout(this);
        statistics.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout distanceBox = new LinearLayout(this);
        distanceBox.setOrientation(LinearLayout.VERTICAL);

        distanceText = makeText(
                "-- km",
                21,
                Color.rgb(0, 190, 255),
                true
        );

        distanceBox.addView(distanceText);
        distanceBox.addView(
                makeText("DISTANȚĂ", 10, Color.LTGRAY, true)
        );

        LinearLayout durationBox = new LinearLayout(this);
        durationBox.setOrientation(LinearLayout.VERTICAL);

        durationText = makeText(
                "-- h -- min",
                21,
                Color.rgb(0, 220, 140),
                true
        );

        durationBox.addView(durationText);
        durationBox.addView(
                makeText("DURATĂ ESTIMATĂ", 10, Color.LTGRAY, true)
        );

        statistics.addView(
                distanceBox,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        statistics.addView(
                durationBox,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        information.addView(statistics);
        root.addView(information);

        addSpace(root, 6);

        // BOTTOM BUTTONS

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button gpsButton = makeButton("GPS", BLUE);

        gpsButton.setOnClickListener(v -> {
            checkLocationPermission();
            goToMyLocation();
        });

        Button zoomIn = makeButton("+", PANEL);
        zoomIn.setOnClickListener(v ->
                map.getController().zoomIn()
        );

        Button zoomOut = makeButton("−", PANEL);
        zoomOut.setOnClickListener(v ->
                map.getController().zoomOut()
        );

        instructionsButton = makeButton(
                "ASCULTĂ MANEVRELE",
                GREEN
        );

        instructionsButton.setEnabled(false);

        instructionsButton.setOnClickListener(v ->
                readRouteInstructions()
        );

        addBottomButton(buttons, gpsButton, 1.1f);
        addBottomButton(buttons, zoomIn, 0.6f);
        addBottomButton(buttons, zoomOut, 0.6f);
        addBottomButton(buttons, instructionsButton, 1.7f);

        root.addView(buttons);

        addSpace(root, 4);

        TextView attribution = makeText(
                "Hartă © OpenStreetMap contributors",
                9,
                Color.LTGRAY,
                false
        );

        attribution.setGravity(Gravity.CENTER);
        root.addView(attribution);

        setContentView(root);
    }

    private void addBottomButton(
            LinearLayout parent,
            Button button,
            float weight) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        weight
                );

        params.setMargins(dp(2), 0, dp(2), 0);

        parent.addView(button, params);
    }

    private void addSpace(
            LinearLayout parent,
            int height) {

        View space = new View(this);

        parent.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        dp(height)
                )
        );
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

    private String translateManeuver(JSONObject maneuver) {

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

        status.setText("Se calculează traseul...");
        distanceText.setText("-- km");
        durationText.setText("-- h -- min");

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

                GeoPoint destinationPoint =
                        searchCity(destination);

                JSONObject route =
                        getRoute(origin, destinationPoint);

                String instructions =
                        buildRouteInstructions(route);

                handler.post(() -> {

                    showRoute(
                            origin,
                            destinationPoint,
                            route
                    );

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

        JSONObject response =
                new JSONObject(httpGet(address));

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

                JSONArray coordinate =
                        coordinates.getJSONArray(i);

                double longitude = coordinate.getDouble(0);
                double latitude = coordinate.getDouble(1);

                points.add(new GeoPoint(latitude, longitude));
            }

            if (routeLine != null) {
                map.getOverlays().remove(routeLine);
            }

            routeLine = new Polyline();
            routeLine.setPoints(points);
            routeLine.setColor(BLUE);
            routeLine.setWidth(10f);

            map.getOverlays().add(routeLine);

            if (destinationMarker != null) {
                map.getOverlays().remove(destinationMarker);
            }

            destinationMarker = new Marker(map);
            destinationMarker.setPosition(destination);
            destinationMarker.setTitle("Destinație");

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

            int minutes =
                    (int) ((durationHours - hours) * 60);

            distanceText.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f km",
                            distanceKm
                    )
            );

            durationText.setText(
                    hours + " h " + minutes + " min"
            );

            status.setText(
                    "TRASEU CALCULAT • Previzualizare disponibilă"
            );

            map.zoomToBoundingBox(
                    routeLine.getBounds(),
                    true,
                    60
            );

            map.invalidate();

            speak("Traseul a fost calculat. Distanța este de aproximativ "
                    + String.format(
                            Locale.getDefault(),
                            "%.0f kilometri.",
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
