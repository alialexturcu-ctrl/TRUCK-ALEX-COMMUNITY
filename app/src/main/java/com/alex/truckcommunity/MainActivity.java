
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
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity
        implements LocationListener, MapEventsReceiver {

    private MapView map;

    private EditText startInput;
    private EditText destinationInput;

    private TextView statusText;
    private TextView distanceText;
    private TextView durationText;
    private TextView nextText;

    private Button routeButton;
    private Button voiceButton;

    private LocationManager locationManager;
    private Location currentLocation;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private Marker gpsMarker;
    private Marker startMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private GeoPoint destinationPoint;

    private final List<String> instructions =
            new ArrayList<>();

    private static final int LOCATION_PERMISSION = 1001;

    private final int BACKGROUND =
            Color.rgb(13, 20, 26);

    private final int PANEL =
            Color.rgb(25, 34, 42);

    private final int PANEL_LIGHT =
            Color.rgb(39, 50, 59);

    private final int GREEN =
            Color.rgb(0, 190, 105);

    private final int WHITE = Color.WHITE;

    private final int BUTTON_BLACK =
            Color.rgb(10, 14, 18);

    private boolean changingDestination = false;

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

    // HARTA

    private void createMap() {

        map = new MapView(this);

        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);

        map.getController().setZoom(6.0);

        map.getController().setCenter(
                new GeoPoint(45.0, 10.0)
        );

        map.getOverlays().add(
                new MapEventsOverlay(this)
        );
    }

    // INTERFATA

    private void createInterface() {

        FrameLayout root = new FrameLayout(this);

        root.setBackgroundColor(BACKGROUND);

        FrameLayout.LayoutParams mapParams =
                new FrameLayout.LayoutParams(-1, -1);

        root.addView(map, mapParams);

        // PANOU SUPERIOR

        LinearLayout topPanel = new LinearLayout(this);

        topPanel.setOrientation(LinearLayout.VERTICAL);
        topPanel.setPadding(14, 10, 14, 10);
        topPanel.setBackground(rounded(PANEL, 20));

        TextView title = new TextView(this);

        title.setText("TRUCK ALEX COMMUNITY");
        title.setTextColor(GREEN);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);

        topPanel.addView(title);

        TextView subtitle = new TextView(this);

        subtitle.setText("YOUR COMMUNITY NAVIGATION");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(10);
        subtitle.setGravity(Gravity.CENTER);

        topPanel.addView(subtitle);

        startInput = createInput(
                "●  Plecare: poziția GPS sau adresă"
        );

        destinationInput = createInput(
                "●  Unde mergem? Introdu destinația"
        );

        topPanel.addView(startInput);
        topPanel.addView(destinationInput);

        LinearLayout searchButtons =
                new LinearLayout(this);

        searchButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button coordinateButton =
                createButton("COORDONATE");

        Button mapButton =
                createButton("ALEGE PE HARTĂ");

        LinearLayout.LayoutParams half =
                new LinearLayout.LayoutParams(
                        0, 54, 1
                );

        half.setMargins(3, 3, 3, 3);

        searchButtons.addView(coordinateButton, half);
        searchButtons.addView(mapButton, half);

        topPanel.addView(searchButtons);

        coordinateButton.setOnClickListener(v ->
                showCoordinateDialog()
        );

        mapButton.setOnClickListener(v ->
                selectDestinationOnMap()
        );

        routeButton =
                createButton("CALCULEAZĂ TRASEUL");

        routeButton.setTextSize(15);

        topPanel.addView(routeButton);

        routeButton.setOnClickListener(v ->
                calculateRoute()
        );

        FrameLayout.LayoutParams topParams =
                new FrameLayout.LayoutParams(
                        -1, -2, Gravity.TOP
                );

        topParams.setMargins(10, 10, 10, 0);

        root.addView(topPanel, topParams);

        // BUTOANE LATERALE

        LinearLayout sidePanel =
                new LinearLayout(this);

        sidePanel.setOrientation(
                LinearLayout.VERTICAL
        );

        sidePanel.setPadding(4, 4, 4, 4);
        sidePanel.setBackground(rounded(PANEL, 16));

        Button zoomIn = createSideButton("+");
        Button zoomOut = createSideButton("−");
        Button gpsButton = createSideButton("GPS");

        sidePanel.addView(zoomIn);
        sidePanel.addView(zoomOut);
        sidePanel.addView(gpsButton);

        zoomIn.setOnClickListener(v ->
                map.getController().zoomIn()
        );

        zoomOut.setOnClickListener(v ->
                map.getController().zoomOut()
        );

        gpsButton.setOnClickListener(v ->
                showMyPosition()
        );

        FrameLayout.LayoutParams sideParams =
                new FrameLayout.LayoutParams(
                        90,
                        -2,
                        Gravity.RIGHT | Gravity.CENTER_VERTICAL
                );

        sideParams.setMargins(0, 0, 9, 0);

        root.addView(sidePanel, sideParams);

        // PANOU INFERIOR

        LinearLayout bottomPanel =
                new LinearLayout(this);

        bottomPanel.setOrientation(
                LinearLayout.VERTICAL
        );

        bottomPanel.setPadding(14, 10, 14, 10);
        bottomPanel.setBackground(rounded(PANEL, 20));

        statusText = createText(
                "●  Aștept poziția GPS..."
        );

        statusText.setTextColor(GREEN);

        bottomPanel.addView(statusText);

        LinearLayout infoRow =
                new LinearLayout(this);

        infoRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        LinearLayout distanceBox =
                createInfoBox("DISTANȚĂ");

        LinearLayout durationBox =
                createInfoBox("DURATĂ ESTIMATĂ");

        distanceText =
                (TextView) distanceBox.getChildAt(1);

        durationText =
                (TextView) durationBox.getChildAt(1);

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0, -2, 1
                );

        infoParams.setMargins(3, 5, 3, 5);

        infoRow.addView(distanceBox, infoParams);
        infoRow.addView(durationBox, infoParams);

        bottomPanel.addView(infoRow);

        nextText = createText(
                "Următoarea manevră: --"
        );

        bottomPanel.addView(nextText);

        LinearLayout bottomButtons =
                new LinearLayout(this);

        bottomButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        voiceButton =
                createButton("ASCULTĂ");

        Button maneuversButton =
                createButton("MANEVRE");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0, 54, 1
                );

        buttonParams.setMargins(3, 3, 3, 3);

        bottomButtons.addView(voiceButton, buttonParams);
        bottomButtons.addView(maneuversButton, buttonParams);

        bottomPanel.addView(bottomButtons);

        voiceButton.setOnClickListener(v ->
                speakInstructions()
        );

        maneuversButton.setOnClickListener(v ->
                showManeuvers()
        );

        TextView copyright = createText(
                "© OpenStreetMap | Rutare OSRM"
        );

        copyright.setTextSize(9);
        copyright.setGravity(Gravity.CENTER);

        bottomPanel.addView(copyright);

        FrameLayout.LayoutParams bottomParams =
                new FrameLayout.LayoutParams(
                        -1, -2, Gravity.BOTTOM
                );

        bottomParams.setMargins(10, 0, 10, 10);

        root.addView(bottomPanel, bottomParams);

        setContentView(root);

        destinationInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        if (!changingDestination) {
                            destinationPoint = null;
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );
    }

    private LinearLayout createInfoBox(String label) {

        LinearLayout box = new LinearLayout(this);

        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(8, 8, 8, 8);
        box.setBackground(rounded(PANEL_LIGHT, 12));

        TextView heading = createText(label);

        heading.setTextSize(10);
        heading.setTextColor(Color.LTGRAY);

        TextView value = createText("--");

        value.setTextSize(18);
        value.setTypeface(null, Typeface.BOLD);
        value.setTextColor(GREEN);

        box.addView(heading);
        box.addView(value);

        return box;
    }

    private EditText createInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setHintTextColor(Color.LTGRAY);
        input.setTextColor(WHITE);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setPadding(12, 2, 12, 2);

        input.setBackground(rounded(PANEL_LIGHT, 12));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 45);

        params.setMargins(0, 4, 0, 4);

        input.setLayoutParams(params);

        return input;
    }

    private TextView createText(String value) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextColor(WHITE);
        text.setTextSize(13);
        text.setPadding(4, 4, 4, 4);

        return text;
    }

    // BUTOANE NEGRE CU TEXT ALB

    private Button createButton(String value) {

        Button button = new Button(this);

        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(10, 8, 10, 8);
        button.setMinHeight(54);
        button.setMinimumHeight(54);

        button.setBackground(
                rounded(BUTTON_BLACK, 12)
        );

        return button;
    }

    private Button createSideButton(String value) {

        Button button = createButton(value);

        button.setTextSize(15);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 58);

        params.setMargins(3, 4, 3, 4);

        button.setLayoutParams(params);

        return button;
    }

    private GradientDrawable rounded(
            int color,
            int radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    // VOCE

    private void initializeVoice() {

        tts = new TextToSpeech(
                this,
                result -> {

                    if (result == TextToSpeech.SUCCESS) {

                        int language =
                                tts.setLanguage(
                                        new Locale("ro", "RO")
                                );

                        ttsReady =
                                language != TextToSpeech.LANG_MISSING_DATA &&
                                language != TextToSpeech.LANG_NOT_SUPPORTED;
                    }
                }
        );
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

            String sentence =
                    (i + 1) + ". " + instructions.get(i);

            tts.speak(
                    sentence,
                    i == 0
                            ? TextToSpeech.QUEUE_FLUSH
                            : TextToSpeech.QUEUE_ADD,
                    null,
                    "manevra_" + i
            );
        }
    }

    private void showManeuvers() {

        if (instructions.isEmpty()) {

            Toast.makeText(
                    this,
                    "Nu există încă un traseu.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        StringBuilder message = new StringBuilder();

        for (int i = 0; i < instructions.size(); i++) {

            message.append(i + 1)
                    .append(". ")
                    .append(instructions.get(i))
                    .append("\n\n");
        }

        ScrollView scroll = new ScrollView(this);

        TextView text = createText(message.toString());

        text.setTextSize(14);
        text.setPadding(18, 15, 18, 15);

        scroll.addView(text);

        new AlertDialog.Builder(this)
                .setTitle("MANEVRELE TRASEULUI")
                .setView(scroll)
                .setPositiveButton("ÎNCHIDE", null)
                .show();
    }

    // GPS

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
            int[] results
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == LOCATION_PERMISSION) {

            if (results.length > 0 &&
                    results[0] == PackageManager.PERMISSION_GRANTED) {

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

            locationManager.removeUpdates(this);

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

            statusText.setText("● GPS ACTIV");

        } catch (Exception e) {

            statusText.setText(
                    "GPS indisponibil. Verifică setările."
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
                String.format(
                        Locale.US,
                        "● GPS ACTIV  %.5f, %.5f",
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
                    "Așteaptă poziția GPS.",
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

    // SELECTARE DESTINATIE

    private void selectDestinationOnMap() {

        Toast.makeText(
                this,
                "Atinge punctul dorit pe hartă.",
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint point) {

        destinationPoint = point;

        changingDestination = true;

        destinationInput.setText(
                String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        point.getLatitude(),
                        point.getLongitude()
                )
        );

        changingDestination = false;

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

    private void showCoordinateDialog() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);

        EditText latitude = createInput("Latitudine");
        EditText longitude = createInput("Longitudine");

        latitude.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        longitude.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        layout.addView(latitude);
        layout.addView(longitude);

        new AlertDialog.Builder(this)
                .setTitle("DESTINAȚIE GPS")
                .setView(layout)
                .setNegativeButton("ANULEAZĂ", null)
                .setPositiveButton("CONFIRMĂ", (dialog, which) -> {

                    try {

                        double lat = Double.parseDouble(
                                latitude.getText().toString().trim()
                        );

                        double lon = Double.parseDouble(
                                longitude.getText().toString().trim()
                        );

                        if (lat < -90 || lat > 90 ||
                                lon < -180 || lon > 180) {
                            throw new Exception();
                        }

                        GeoPoint point = new GeoPoint(lat, lon);

                        destinationPoint = point;

                        changingDestination = true;

                        destinationInput.setText(
                                String.format(
                                        Locale.US,
                                        "%.6f, %.6f",
                                        lat, lon
                                )
                        );

                        changingDestination = false;

                        setDestinationMarker(point);

                        map.getController().animateTo(point);
                        map.getController().setZoom(13.0);

                        statusText.setText(
                                "Destinație GPS selectată."
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Coordonate incorecte.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .show();
    }

    // CALCUL TRASEU

    private void calculateRoute() {

        String destination =
                destinationInput.getText().toString().trim();

        if (destinationPoint == null && destination.isEmpty()) {

            Toast.makeText(
                    this,
                    "Introdu destinația.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Location locationSnapshot = currentLocation;

        String startAddress =
                startInput.getText().toString().trim();

        if (locationSnapshot == null && startAddress.isEmpty()) {

            Toast.makeText(
                    this,
                    "Așteaptă GPS-ul sau introdu plecarea.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        GeoPoint selectedDestination = destinationPoint;

        routeButton.setEnabled(false);

        statusText.setText("Se calculează traseul...");

        executor.execute(() -> {

            try {

                GeoPoint start;

                if (startAddress.isEmpty()) {

                    start = new GeoPoint(
                            locationSnapshot.getLatitude(),
                            locationSnapshot.getLongitude()
                    );

                } else {

                    start = findPlace(startAddress);
                }

                GeoPoint end;

                if (selectedDestination != null) {

                    end = selectedDestination;

                } else {

                    end = parseCoordinates(destination);

                    if (end == null) {
                        end = findPlace(destination);
                    }
                }

                if (start == null || end == null) {
                    throw new Exception(
                            "Nu am găsit una dintre adrese."
                    );
                }

                requestRoute(start, end);

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

    private GeoPoint parseCoordinates(String value) {

        try {

            String[] parts = value.split(",");

            if (parts.length != 2) {
                return null;
            }

            double lat = Double.parseDouble(
                    parts[0].trim()
            );

            double lon = Double.parseDouble(
                    parts[1].trim()
            );

            if (lat < -90 || lat > 90 ||
                    lon < -180 || lon > 180) {
                return null;
            }

            return new GeoPoint(lat, lon);

        } catch (Exception e) {
            return null;
        }
    }

    private GeoPoint findPlace(String address)
            throws Exception {

        String encoded =
                URLEncoder.encode(address, "UTF-8");

        String url =
                "https://nominatim.openstreetmap.org/search?q=" +
                encoded +
                "&format=json&limit=1";

        JSONArray results =
                new JSONArray(httpGet(url));

        if (results.length() == 0) {
            return null;
        }

        JSONObject place =
                results.getJSONObject(0);

        return new GeoPoint(
                place.getDouble("lat"),
                place.getDouble("lon")
        );
    }

    private void requestRoute(
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
                    "Serverul nu a găsit traseul."
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

            JSONArray item =
                    coordinates.getJSONArray(i);

            points.add(
                    new GeoPoint(
                            item.getDouble(1),
                            item.getDouble(0)
                    )
            );
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

                String instruction =
                        translateManeuver(
                                maneuver.optString("type"),
                                maneuver.optString("modifier")
                        );

                String road = step.optString("name");

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
            routeLine.setWidth(10f);

            map.getOverlays().add(routeLine);

            if (startMarker != null) {
                map.getOverlays().remove(startMarker);
            }

            startMarker = new Marker(map);
            startMarker.setPosition(start);
            startMarker.setTitle("Plecare");

            map.getOverlays().add(startMarker);

            setDestinationMarker(end);

            double latSpan = Math.max(
                    0.01,
                    Math.abs(
                            start.getLatitude() -
                            end.getLatitude()
                    ) + 0.03
            );

            double lonSpan = Math.max(
                    0.01,
                    Math.abs(
                            start.getLongitude() -
                            end.getLongitude()
                    ) + 0.03
            );

            map.getController().setCenter(start);

            map.getController().zoomToSpan(
                    latSpan,
                    lonSpan
            );

            distanceText.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f km",
                            distance
                    )
            );

            durationText.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.0f min",
                            duration
                    )
            );

            if (!instructions.isEmpty()) {

                nextText.setText(
                        "Prima manevră: " +
                        instructions.get(0)
                );

            } else {

                nextText.setText(
                        "Nu există manevre disponibile."
                );
            }

            statusText.setText(
                    "● TRASEU CALCULAT"
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

        if ("on ramp".equals(type)) {
            return "Intră pe bretea";
        }

        if ("off ramp".equals(type)) {
            return "Ieși de pe bretea";
        }

        return "Continuă pe traseu";
    }

    // INTERNET

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

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "Eroare server: " + code
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(),
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

    // CICLU DE VIATA

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
                statusText.setText("GPS DEZACTIVAT")
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
