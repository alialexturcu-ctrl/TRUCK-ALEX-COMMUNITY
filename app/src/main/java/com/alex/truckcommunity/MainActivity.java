
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity
        implements LocationListener, MapEventsReceiver {

    private MapView map;
    private EditText startInput, destinationInput, coordinateInput;
    private TextView statusText, distanceText, durationText, maneuverText;

    private Button routeButton, voiceButton;

    private LocationManager locationManager;
    private Location currentLocation;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private Marker gpsMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private GeoPoint destinationPoint;

    private final List<String> instructions = new ArrayList<>();

    private static final int LOCATION_PERMISSION = 1001;

    private final int DARK = Color.rgb(20, 25, 31);
    private final int PANEL = Color.rgb(31, 39, 47);
    private final int GREEN = Color.rgb(0, 180, 100);
    private final int WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(
                getPackageName()
        );

        createMap();
        createInterface();
        initializeVoice();
        checkLocationPermission();
    }

    private void createMap() {

        map = new MapView(this);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);

        map.getController().setZoom(6.0);
        map.getController().setCenter(
                new GeoPoint(45.0, 10.0)
        );

        MapEventsOverlay eventsOverlay =
                new MapEventsOverlay(this);

        map.getOverlays().add(eventsOverlay);
    }

    private void createInterface() {

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(DARK);

        root.addView(
                map,
                new FrameLayout.LayoutParams(
                        -1, -1
                )
        );

        LinearLayout topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.VERTICAL);
        topPanel.setPadding(14, 12, 14, 12);
        topPanel.setBackground(roundBackground(PANEL, 18));

        TextView title = new TextView(this);
        title.setText("TRUCK ALEX COMMUNITY");
        title.setTextColor(GREEN);
        title.setTextSize(21);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        topPanel.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("COMMUNITY NAVIGATION");
        subtitle.setTextColor(WHITE);
        subtitle.setTextSize(11);
        subtitle.setGravity(Gravity.CENTER);
        topPanel.addView(subtitle);

        startInput = createInput("Plecare: adresă sau gol pentru GPS");
        destinationInput = createInput("Destinație: introdu adresa");

        coordinateInput = createInput(
                "Coordonate destinație: latitudine, longitudine"
        );

        topPanel.addView(startInput);
        topPanel.addView(destinationInput);
        topPanel.addView(coordinateInput);

        Button coordinateButton = createButton(
                "FOLOSEȘTE COORDONATELE"
        );

        coordinateButton.setOnClickListener(v ->
                useCoordinates()
        );

        topPanel.addView(coordinateButton);

        routeButton = createButton("CALCULEAZĂ TRASEUL");

        routeButton.setOnClickListener(v ->
                calculateRoute()
        );

        topPanel.addView(routeButton);

        FrameLayout.LayoutParams topParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -2,
                        Gravity.TOP
                );

        topParams.setMargins(10, 10, 10, 0);
        root.addView(topPanel, topParams);

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(14, 12, 14, 12);
        bottomPanel.setBackground(roundBackground(PANEL, 18));

        statusText = createText("GPS în așteptare...");
        distanceText = createText("Distanță: --");
        durationText = createText("Durată estimată: --");
        maneuverText = createText("Prima manevră: --");

        bottomPanel.addView(statusText);
        bottomPanel.addView(distanceText);
        bottomPanel.addView(durationText);
        bottomPanel.addView(maneuverText);

        voiceButton = createButton("ASCULTĂ MANEVRELE");

        voiceButton.setOnClickListener(v ->
                speakInstructions()
        );

        bottomPanel.addView(voiceButton);

        TextView attribution = createText(
                "Hartă © OpenStreetMap | Traseu OSRM"
        );

        attribution.setTextSize(10);
        attribution.setGravity(Gravity.CENTER);
        bottomPanel.addView(attribution);

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -2,
                        Gravity.BOTTOM
                );

        bottomParams.setMargins(10, 0, 10, 10);
        root.addView(bottomPanel, bottomParams);

        LinearLayout zoomPanel = new LinearLayout(this);
        zoomPanel.setOrientation(LinearLayout.VERTICAL);

        Button zoomIn = createButton("+");
        Button zoomOut = createButton("−");
        Button gpsButton = createButton("GPS");

        zoomIn.setOnClickListener(v ->
                map.getController().zoomIn()
        );

        zoomOut.setOnClickListener(v ->
                map.getController().zoomOut()
        );

        gpsButton.setOnClickListener(v ->
                showMyPosition()
        );

        zoomPanel.addView(zoomIn);
        zoomPanel.addView(zoomOut);
        zoomPanel.addView(gpsButton);

        FrameLayout.LayoutParams zoomParams =
                new FrameLayout.LayoutParams(
                        85,
                        -2,
                        Gravity.RIGHT | Gravity.CENTER_VERTICAL
                );

        zoomParams.setMargins(0, 0, 10, 0);
        root.addView(zoomPanel, zoomParams);

        setContentView(root);
    }

    private EditText createInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextColor(WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setPadding(12, 4, 12, 4);
        input.setBackground(roundBackground(
                Color.rgb(48, 57, 66), 10
        ));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, 48
                );

        params.setMargins(0, 5, 0, 5);
        input.setLayoutParams(params);

        return input;
    }

    private TextView createText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(14);
        view.setPadding(4, 5, 4, 5);

        return view;
    }

    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setBackground(roundBackground(GREEN, 12));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, 45
                );

        params.setMargins(0, 5, 0, 5);
        button.setLayoutParams(params);

        return button;
    }

    private GradientDrawable roundBackground(
            int color, int radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    private void initializeVoice() {

        tts = new TextToSpeech(
                this,
                result -> {

                    if (result == TextToSpeech.SUCCESS) {

                        int languageResult =
                                tts.setLanguage(
                                        new Locale("ro", "RO")
                                );

                        ttsReady =
                                languageResult !=
                                TextToSpeech.LANG_MISSING_DATA &&
                                languageResult !=
                                TextToSpeech.LANG_NOT_SUPPORTED;
                    }
                }
        );
    }

    private void checkLocationPermission() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION
            );

        } else {

            startGps();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == LOCATION_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startGps();

            } else {

                statusText.setText(
                        "Permisiunea GPS nu a fost acordată."
                );
            }
        }
    }

    private void startGps() {

        try {

            locationManager =
                    (LocationManager) getSystemService(
                            LOCATION_SERVICE
                    );

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }

            Location last =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (last == null) {

                last = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            if (last != null) {
                updateLocation(last);
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5,
                    this
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10,
                    this
            );

            statusText.setText("GPS activ.");

        } catch (Exception e) {

            statusText.setText(
                    "Nu s-a putut porni GPS-ul."
            );
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocation(location);
    }

    private void updateLocation(Location location) {

        currentLocation = location;

        GeoPoint point = new GeoPoint(
                location.getLatitude(),
                location.getLongitude()
        );

        if (gpsMarker == null) {

            gpsMarker = new Marker(map);
            gpsMarker.setTitle("Poziția mea");

            map.getOverlays().add(gpsMarker);
        }

        gpsMarker.setPosition(point);

        statusText.setText(
                "GPS: " +
                String.format(
                        Locale.US,
                        "%.5f, %.5f",
                        location.getLatitude(),
                        location.getLongitude()
                )
        );

        map.invalidate();
    }

    private void showMyPosition() {

        if (currentLocation == null) {

            Toast.makeText(
                    this,
                    "Așteaptă obținerea poziției GPS.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        GeoPoint point = new GeoPoint(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );

        map.getController().animateTo(point);
        map.getController().setZoom(15.0);
    }

    private void useCoordinates() {

        String value =
                coordinateInput.getText().toString().trim();

        try {

            String[] parts = value.split(",");

            if (parts.length != 2) {
                throw new Exception();
            }

            double latitude =
                    Double.parseDouble(parts[0].trim());

            double longitude =
                    Double.parseDouble(parts[1].trim());

            if (latitude < -90 || latitude > 90 ||
                    longitude < -180 || longitude > 180) {

                throw new Exception();
            }

            destinationPoint =
                    new GeoPoint(latitude, longitude);

            destinationInput.setText(
                    latitude + ", " + longitude
            );

            setDestinationMarker(destinationPoint);

            map.getController().animateTo(destinationPoint);
            map.getController().setZoom(13.0);

            statusText.setText(
                    "Destinație selectată prin coordonate."
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Format invalid. Exemplu: 45.123, 11.456",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint point) {

        destinationPoint = point;

        coordinateInput.setText(
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        point.getLatitude(),
                        point.getLongitude()
                )
        );

        destinationInput.setText(
                "Punct selectat pe hartă"
        );

        setDestinationMarker(point);

        statusText.setText(
                "Destinație selectată pe hartă."
        );

        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint point) {
        return false;
    }

    private void setDestinationMarker(GeoPoint point) {

        if (destinationMarker != null) {
            map.getOverlays().remove(destinationMarker);
        }

        destinationMarker = new Marker(map);
        destinationMarker.setPosition(point);
        destinationMarker.setTitle("Destinație");

        map.getOverlays().add(destinationMarker);

        map.invalidate();
    }

    private void calculateRoute() {

        String destination =
                destinationInput.getText().toString().trim();

        if (destinationPoint == null &&
                destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Introdu o destinație sau selecteaz-o pe hartă.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (currentLocation == null &&
                startInput.getText().toString().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "GPS-ul nu are încă o poziție. Așteaptă sau introdu plecarea.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        routeButton.setEnabled(false);
        statusText.setText("Se caută traseul...");

        executor.execute(() -> {

            try {

                GeoPoint start;

                String startAddress =
                        startInput.getText().toString().trim();

                if (startAddress.isEmpty()) {

                    start = new GeoPoint(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude()
                    );

                } else {

                    start = geocode(startAddress);
                }

                GeoPoint end;

                if (destinationPoint != null) {

                    end = destinationPoint;

                } else {

                    end = geocode(destination);
                }

                if (start == null || end == null) {
                    throw new Exception(
                            "Nu am găsit adresa introdusă."
                    );
                }

                getRoute(start, end);

            } catch (Exception e) {

                runOnUiThread(() -> {

                    statusText.setText(
                            "Eroare: " + e.getMessage()
                    );

                    routeButton.setEnabled(true);
                });
            }
        });
    }

    private GeoPoint geocode(String address)
            throws Exception {

        String encoded = URLEncoder.encode(
                address,
                "UTF-8"
        );

        String url =
                "https://nominatim.openstreetmap.org/search?q=" +
                encoded +
                "&format=json&limit=1";

        JSONArray array = new JSONArray(
                httpGet(url)
        );

        if (array.length() == 0) {
            return null;
        }

        JSONObject result = array.getJSONObject(0);

        double lat = result.getDouble("lat");
        double lon = result.getDouble("lon");

        return new GeoPoint(lat, lon);
    }

    private void getRoute(
            GeoPoint start,
            GeoPoint end
    ) throws Exception {

        String url =
                "https://router.project-osrm.org/route/v1/driving/" +
                start.getLongitude() + "," +
                start.getLatitude() + ";" +
                end.getLongitude() + "," +
                end.getLatitude() +
                "?overview=full&geometries=geojson&steps=true";

        JSONObject response =
                new JSONObject(httpGet(url));

        if (!"Ok".equals(response.optString("code"))) {
            throw new Exception(
                    "Nu s-a putut calcula traseul."
            );
        }

        JSONObject route =
                response.getJSONArray("routes")
                        .getJSONObject(0);

        double distance =
                route.getDouble("distance") / 1000.0;

        double duration =
                route.getDouble("duration") / 60.0;

        JSONArray coordinates =
                route.getJSONObject("geometry")
                        .getJSONArray("coordinates");

        ArrayList<GeoPoint> points =
                new ArrayList<>();

        for (int i = 0; i < coordinates.length(); i++) {

            JSONArray coordinate =
                    coordinates.getJSONArray(i);

            double lon = coordinate.getDouble(0);
            double lat = coordinate.getDouble(1);

            points.add(new GeoPoint(lat, lon));
        }

        ArrayList<String> newInstructions =
                new ArrayList<>();

        JSONArray legs =
                route.getJSONArray("legs");

        for (int i = 0; i < legs.length(); i++) {

            JSONArray steps =
                    legs.getJSONObject(i)
                            .getJSONArray("steps");

            for (int j = 0; j < steps.length(); j++) {

                JSONObject step =
                        steps.getJSONObject(j);

                JSONObject maneuver =
                        step.getJSONObject("maneuver");

                String type =
                        maneuver.optString("type");

                String modifier =
                        maneuver.optString("modifier");

                String road =
                        step.optString("name");

                String instruction =
                        translateManeuver(type, modifier);

                if (!road.isEmpty()) {
                    instruction += " pe " + road;
                }

                newInstructions.add(instruction);
            }
        }

        runOnUiThread(() -> {

            instructions.clear();
            instructions.addAll(newInstructions);

            if (routeLine != null) {
                map.getOverlays().remove(routeLine);
            }

            routeLine = new Polyline();
            routeLine.setPoints(points);
            routeLine.setColor(GREEN);
            routeLine.setWidth(9f);

            map.getOverlays().add(routeLine);

            setDestinationMarker(end);

            map.getController().setCenter(start);
            map.getController().zoomToSpan(
                    Math.abs(start.getLatitude() -
                            end.getLatitude()) + 0.02,
                    Math.abs(start.getLongitude() -
                            end.getLongitude()) + 0.02
            );

            distanceText.setText(
                    String.format(
                            Locale.getDefault(),
                            "Distanță: %.1f km",
                            distance
                    )
            );

            durationText.setText(
                    String.format(
                            Locale.getDefault(),
                            "Durată estimată: %.0f minute",
                            duration
                    )
            );

            if (instructions.size() > 0) {

                maneuverText.setText(
                        "Prima manevră: " +
                        instructions.get(0)
                );

            } else {

                maneuverText.setText(
                        "Nu există instrucțiuni disponibile."
                );
            }

            statusText.setText(
                    "Traseu calculat. Verifică ruta pe hartă."
            );

            routeButton.setEnabled(true);
            map.invalidate();
        });
    }

    private String translateManeuver(
            String type,
            String modifier
    ) {

        if ("depart".equals(type)) {
            return "Pornește";
        }

        if ("arrive".equals(type)) {
            return "Ai ajuns la destinație";
        }

        if ("turn".equals(type) ||
                "end of road".equals(type)) {

            if ("left".equals(modifier)) {
                return "Virează la stânga";
            }

            if ("right".equals(modifier)) {
                return "Virează la dreapta";
            }

            if ("slight left".equals(modifier)) {
                return "Ține ușor stânga";
            }

            if ("slight right".equals(modifier)) {
                return "Ține ușor dreapta";
            }

            if ("sharp left".equals(modifier)) {
                return "Virează strâns la stânga";
            }

            if ("sharp right".equals(modifier)) {
                return "Virează strâns la dreapta";
            }

            if ("uturn".equals(modifier)) {
                return "Întoarce";
            }

            return "Continuă înainte";
        }

        if ("roundabout".equals(type) ||
                "rotary".equals(type)) {

            return "Intră în sensul giratoriu";
        }

        if ("merge".equals(type)) {
            return "Intră pe drumul indicat";
        }

        if ("fork".equals(type)) {
            return "Urmează direcția indicată";
        }

        if ("continue".equals(type)) {
            return "Continuă înainte";
        }

        if ("new name".equals(type)) {
            return "Continuă pe drumul următor";
        }

        return "Continuă pe traseu";
    }

    private String httpGet(String address)
            throws Exception {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(address);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);

            connection.setRequestProperty(
                    "User-Agent",
                    "TRUCK-ALEX-COMMUNITY/1.0"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            int code = connection.getResponseCode();

            InputStream stream;

            if (code >= 200 && code < 300) {
                stream = connection.getInputStream();
            } else {
                stream = connection.getErrorStream();
                throw new Exception(
                        "Serverul a răspuns cu eroarea " + code
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    stream,
                                    "UTF-8"
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

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

    private void speakInstructions() {

        if (!ttsReady) {

            Toast.makeText(
                    this,
                    "Vocea română nu este pregătită.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (instructions.isEmpty()) {

            Toast.makeText(
                    this,
                    "Calculează mai întâi un traseu.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        for (int i = 0; i < instructions.size(); i++) {

            String text =
                    (i + 1) + ". " + instructions.get(i);

            tts.speak(
                    text,
                    i == 0
                            ? TextToSpeech.QUEUE_FLUSH
                            : TextToSpeech.QUEUE_ADD,
                    null,
                    "route_" + i
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (map != null) {
            map.onResume();
        }

        if (locationManager != null) {
            startGps();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map != null) {
            map.onPause();
        }

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }

        if (tts != null) {
            tts.stop();
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

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        runOnUiThread(() ->
                statusText.setText("GPS dezactivat.")
        );
    }

    @Override
    public void onStatusChanged(
            String provider,
            int status,
            Bundle extras
    ) {
    }
}
