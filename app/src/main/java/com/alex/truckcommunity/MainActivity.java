
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

    // HARTA SI INTERFATA

    private MapView map;
    private FrameLayout root;

    private LinearLayout topPanel;
    private LinearLayout bottomPanel;
    private LinearLayout sidePanel;

    private EditText startInput;
    private EditText destinationInput;

    private TextView statusText;
    private TextView speedText;
    private TextView distanceText;
    private TextView durationText;
    private TextView nextText;

    private Button routeButton;
    private Button voiceButton;
    private Button guidanceButton;

    // GPS

    private LocationManager locationManager;
    private Location currentLocation;

    private static final int LOCATION_PERMISSION = 1001;

    // VOCE

    private TextToSpeech tts;
    private boolean ttsReady = false;

    // INTERNET

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // MARKERE SI TRASEU

    private Marker gpsMarker;
    private Marker startMarker;
    private Marker destinationMarker;
    private Polyline routeLine;

    private GeoPoint destinationPoint;
    private GeoPoint lastDestinationPoint;

    // MANEVRE

    private final List<String> instructions =
            new ArrayList<>();

    private final List<ManeuverPoint> maneuverPoints =
            new ArrayList<>();

    private int currentManeuverIndex = 0;

    private static class ManeuverPoint {
        GeoPoint point;
        String instruction;
        boolean announced = false;

        ManeuverPoint(GeoPoint point, String instruction) {
            this.point = point;
            this.instruction = instruction;
        }
    }

    // SETARI

    private SharedPreferences preferences;

    private static final String PREFS =
            "truck_alex_preferences";

    private static final String FAVORITES =
            "truck_favorites";

    private static final String RECENTS =
            "truck_recents";

    private static final String TRUCK_HEIGHT =
            "truck_height";

    private static final String TRUCK_WIDTH =
            "truck_width";

    private static final String TRUCK_LENGTH =
            "truck_length";

    private static final String TRUCK_WEIGHT =
            "truck_weight";

    private static final String NIGHT_MODE =
            "night_mode";

    private static final String AUTO_GUIDANCE =
            "auto_guidance";

    private boolean nightMode = true;
    private boolean guidanceActive = true;
    private boolean changingDestination = false;
    private boolean routeCalculated = false;

    // CULORI

    private int BACKGROUND;
    private int PANEL;
    private int PANEL_LIGHT;
    private int GREEN;
    private int WHITE;
    private int BUTTON_BLACK = Color.rgb(10, 14, 18);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        preferences = getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        );

        nightMode = preferences.getBoolean(
                NIGHT_MODE,
                true
        );

        guidanceActive = preferences.getBoolean(
                AUTO_GUIDANCE,
                true
        );

        updateColors();

        Configuration.getInstance().setUserAgentValue(
                getPackageName()
        );

        createMap();
        createInterface();
        initializeVoice();
        checkLocationPermission();
    }

    // CULORI SI TEMA

    private void updateColors() {

        if (nightMode) {
            BACKGROUND = Color.rgb(13, 20, 26);
            PANEL = Color.rgb(25, 34, 42);
            PANEL_LIGHT = Color.rgb(39, 50, 59);
            GREEN = Color.rgb(0, 190, 105);
            WHITE = Color.WHITE;
        } else {
            BACKGROUND = Color.rgb(235, 240, 244);
            PANEL = Color.WHITE;
            PANEL_LIGHT = Color.rgb(225, 232, 237);
            GREEN = Color.rgb(0, 135, 75);
            WHITE = Color.rgb(25, 35, 45);
        }
    }

    private void toggleTheme() {

        nightMode = !nightMode;

        preferences.edit()
                .putBoolean(NIGHT_MODE, nightMode)
                .apply();

        updateColors();

        if (topPanel != null) {
            topPanel.setBackground(rounded(PANEL, 18));
            updateThemeViews(topPanel);
        }

        if (bottomPanel != null) {
            bottomPanel.setBackground(rounded(PANEL, 18));
            updateThemeViews(bottomPanel);
        }

        if (sidePanel != null) {
            sidePanel.setBackground(rounded(PANEL, 16));
            updateThemeViews(sidePanel);
        }

        if (map != null) {
            map.invalidate();
        }

        Toast.makeText(
                this,
                nightMode
                        ? "Mod noapte activat"
                        : "Mod zi activat",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateThemeViews(View view) {

        if (view instanceof Button) {
            return;
        }

        if (view instanceof EditText) {
            EditText edit = (EditText) view;
            edit.setTextColor(WHITE);
            edit.setHintTextColor(
                    nightMode ? Color.LTGRAY : Color.DKGRAY
            );
            edit.setBackground(rounded(PANEL_LIGHT, 10));
            return;
        }

        if (view instanceof TextView) {
            ((TextView) view).setTextColor(WHITE);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                updateThemeViews(group.getChildAt(i));
            }
        }
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

    // INTERFATA PRINCIPALA

    private void createInterface() {

        root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);

        root.addView(
                map,
                new FrameLayout.LayoutParams(-1, -1)
        );

        createTopPanel();
        createSidePanel();
        createBottomPanel();

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

    // PANOU SUS

    private void createTopPanel() {

        topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.VERTICAL);
        topPanel.setPadding(12, 7, 12, 8);
        topPanel.setBackground(rounded(PANEL, 18));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = createText(
                "TRUCK ALEX COMMUNITY"
        );

        title.setTextColor(GREEN);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);

        titleRow.addView(
                title,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView version = createText("GPS 2.0");
        version.setTextSize(10);
        version.setTextColor(GREEN);

        titleRow.addView(version);

        topPanel.addView(titleRow);

        startInput = createInput(
                "PLECARE: poziția GPS sau adresă"
        );

        destinationInput = createInput(
                "DESTINAȚIE: unde mergem?"
        );

        topPanel.addView(startInput);
        topPanel.addView(destinationInput);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button coordinateButton =
                createButton("COORDONATE");

        Button mapButton =
                createButton("ALEGE PE HARTĂ");

        LinearLayout.LayoutParams half =
                new LinearLayout.LayoutParams(0, 43, 1);

        half.setMargins(3, 2, 3, 2);

        actionRow.addView(coordinateButton, half);
        actionRow.addView(mapButton, half);

        topPanel.addView(actionRow);

        coordinateButton.setOnClickListener(
                v -> showCoordinateDialog()
        );

        mapButton.setOnClickListener(
                v -> selectDestinationOnMap()
        );

        routeButton = createButton("CALCULEAZĂ TRASEUL");
        routeButton.setTextSize(14);

        LinearLayout.LayoutParams routeParams =
                new LinearLayout.LayoutParams(-1, 48);

        routeParams.setMargins(3, 3, 3, 2);

        topPanel.addView(routeButton, routeParams);

        routeButton.setOnClickListener(
                v -> calculateRoute()
        );

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        -1,
                        -2,
                        Gravity.TOP
                );

        params.setMargins(8, 8, 8, 0);

        root.addView(topPanel, params);
    }

    // BUTOANE LATERALE

    private void createSidePanel() {

        sidePanel = new LinearLayout(this);
        sidePanel.setOrientation(LinearLayout.VERTICAL);
        sidePanel.setPadding(4, 4, 4, 4);
        sidePanel.setBackground(rounded(PANEL, 16));

        Button gpsButton = createSideButton("GPS");
        Button zoomIn = createSideButton("+");
        Button zoomOut = createSideButton("−");
        Button moreButton = createSideButton("MENIU");

        sidePanel.addView(gpsButton);
        sidePanel.addView(zoomIn);
        sidePanel.addView(zoomOut);
        sidePanel.addView(moreButton);

        gpsButton.setOnClickListener(
                v -> showMyPosition()
        );

        zoomIn.setOnClickListener(
                v -> map.getController().zoomIn()
        );

        zoomOut.setOnClickListener(
                v -> map.getController().zoomOut()
        );

        moreButton.setOnClickListener(
                v -> showMainMenu()
        );

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        76,
                        -2,
                        Gravity.RIGHT | Gravity.CENTER_VERTICAL
                );

        params.setMargins(0, 0, 7, 0);

        root.addView(sidePanel, params);
    }

    // PANOU JOS

    private void createBottomPanel() {

        bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(12, 8, 12, 8);
        bottomPanel.setBackground(rounded(PANEL, 18));

        statusText = createText("● Aștept poziția GPS...");
        statusText.setTextColor(GREEN);
        statusText.setTextSize(12);

        bottomPanel.addView(statusText);

        speedText = createText(
                "VITEZĂ GPS: -- km/h"
        );

        speedText.setTextSize(16);
        speedText.setTypeface(null, Typeface.BOLD);

        bottomPanel.addView(speedText);

        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout distanceBox =
                createInfoBox("DISTANȚĂ");

        LinearLayout durationBox =
                createInfoBox("DURATĂ ESTIMATĂ");

        distanceText =
                (TextView) distanceBox.getChildAt(1);

        durationText =
                (TextView) durationBox.getChildAt(1);

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(0, -2, 1);

        infoParams.setMargins(3, 3, 3, 3);

        infoRow.addView(distanceBox, infoParams);
        infoRow.addView(durationBox, infoParams);

        bottomPanel.addView(infoRow);

        nextText = createText(
                "URMĂTOAREA MANEVRĂ: --"
        );

        nextText.setTextSize(13);
        nextText.setTypeface(null, Typeface.BOLD);

        bottomPanel.addView(nextText);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        voiceButton = createButton("ASCULTĂ");
        guidanceButton = createButton(
                guidanceActive ? "VOCE AUTOMATĂ ON" : "VOCE AUTOMATĂ OFF"
        );

        Button maneuversButton = createButton("MANEVRE");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(0, 46, 1);

        buttonParams.setMargins(2, 3, 2, 3);

        buttonRow.addView(voiceButton, buttonParams);
        buttonRow.addView(guidanceButton, buttonParams);
        buttonRow.addView(maneuversButton, buttonParams);

        bottomPanel.addView(buttonRow);

        voiceButton.setOnClickListener(
                v -> speakInstructions()
        );

        guidanceButton.setOnClickListener(
                v -> toggleGuidance()
        );

        maneuversButton.setOnClickListener(
                v -> showManeuvers()
        );

        TextView copyright = createText(
                "OpenStreetMap • Rutare OSRM"
        );

        copyright.setTextSize(9);
        copyright.setGravity(Gravity.CENTER);

        bottomPanel.addView(copyright);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        -1,
                        -2,
                        Gravity.BOTTOM
                );

        params.setMargins(8, 0, 8, 8);

        root.addView(bottomPanel, params);
    }

    // ELEMENTE GRAFICE

    private LinearLayout createInfoBox(String label) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(8, 5, 8, 5);
        box.setBackground(rounded(PANEL_LIGHT, 10));

        TextView heading = createText(label);
        heading.setTextSize(10);
        heading.setTextColor(Color.LTGRAY);

        TextView value = createText("--");
        value.setTextSize(18);
        value.setTextColor(GREEN);
        value.setTypeface(null, Typeface.BOLD);

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
        input.setPadding(12, 0, 12, 0);
        input.setBackground(rounded(PANEL_LIGHT, 10));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 43);

        params.setMargins(0, 3, 0, 3);

        input.setLayoutParams(params);

        return input;
    }

    private TextView createText(String value) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextColor(WHITE);
        text.setTextSize(13);
        text.setPadding(4, 3, 4, 3);

        return text;
    }

    private Button createButton(String value) {

        Button button = new Button(this);

        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(4, 3, 4, 3);
        button.setMinHeight(42);
        button.setMinimumHeight(42);
        button.setBackground(rounded(BUTTON_BLACK, 10));

        return button;
    }

    private Button createSideButton(String value) {

        Button button = createButton(value);
        button.setTextSize(10);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 48);

        params.setMargins(2, 3, 2, 3);

        button.setLayoutParams(params);

        return button;
    }

    private GradientDrawable rounded(int color, int radius) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    // MENIU PRINCIPAL

    private void showMainMenu() {

        String[] options = {
                "SALVEAZĂ DESTINAȚIA LA FAVORITE",
                "DESTINAȚII FAVORITE",
                "DESTINAȚII RECENTE",
                "PROFIL CAMION",
                "MOD ZI / NOAPTE",
                "GHIDARE VOCALĂ"
        };

        new AlertDialog.Builder(this)
                .setTitle("TRUCK ALEX COMMUNITY")
                .setItems(options, (dialog, which) -> {

                    switch (which) {

                        case 0:
                            addFavorite();
                            break;

                        case 1:
                            showSavedPlaces(
                                    FAVORITES,
                                    "DESTINAȚII FAVORITE"
                            );
                            break;

                        case 2:
                            showSavedPlaces(
                                    RECENTS,
                                    "DESTINAȚII RECENTE"
                            );
                            break;

                        case 3:
                            showTruckProfile();
                            break;

                        case 4:
                            toggleTheme();
                            break;

                        case 5:
                            toggleGuidance();
                            break;
                    }
                })
                .setNegativeButton("ÎNCHIDE", null)
                .show();
    }

    // VOCE

    private void initializeVoice() {

        tts = new TextToSpeech(
                this,
                result -> {

                    if (result == TextToSpeech.SUCCESS) {

                        int language = tts.setLanguage(
                                new Locale("ro", "RO")
                        );

                        ttsReady =
                                language != TextToSpeech.LANG_MISSING_DATA &&
                                language != TextToSpeech.LANG_NOT_SUPPORTED;
                    }
                }
        );
    }

    private void toggleGuidance() {

        guidanceActive = !guidanceActive;

        preferences.edit()
                .putBoolean(AUTO_GUIDANCE, guidanceActive)
                .apply();

        guidanceButton.setText(
                guidanceActive
                        ? "VOCE AUTOMATĂ ON"
                        : "VOCE AUTOMATĂ OFF"
        );

        if (!guidanceActive && tts != null) {
            tts.stop();
        }

        Toast.makeText(
                this,
                guidanceActive
                        ? "Ghidare vocală activată"
                        : "Ghidare vocală oprită",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void speak(String message) {

        if (ttsReady && tts != null) {
            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "truck_voice"
            );
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

    // GHIDARE AUTOMATA

    private void updateAutomaticGuidance(Location location) {

        if (!guidanceActive ||
                !routeCalculated ||
                maneuverPoints.isEmpty()) {
            return;
        }

        while (currentManeuverIndex < maneuverPoints.size()) {

            ManeuverPoint maneuver =
                    maneuverPoints.get(currentManeuverIndex);

            if (maneuver.point == null) {
                currentManeuverIndex++;
                continue;
            }

            float[] distance = new float[1];

            Location.distanceBetween(
                    location.getLatitude(),
                    location.getLongitude(),
                    maneuver.point.getLatitude(),
                    maneuver.point.getLongitude(),
                    distance
            );

            int meters = (int) distance[0];

            nextText.setText(
                    "URMĂTOAREA MANEVRĂ: " +
                    meters + " m • " +
                    maneuver.instruction
            );

            if (meters <= 120 && !maneuver.announced) {

                maneuver.announced = true;

                speak(maneuver.instruction);

                currentManeuverIndex++;

            } else {
                break;
            }
        }

        if (currentManeuverIndex >= maneuverPoints.size()) {
            nextText.setText(
                    "Manevre terminate. Verifică sosirea."
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

    // GPS SI PERMISIUNI

    private void checkLocationPermission() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
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

            boolean granted = false;

            for (int result : results) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
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

            boolean fine =
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            boolean coarse =
                    checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;

            if (!fine && !coarse) {
                return;
            }

            locationManager.removeUpdates(this);

            Location last = null;

            if (fine) {
                last = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                );
            }

            if (last == null && coarse) {
                last = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                );
            }

            if (last != null) {
                updateLocation(last);
            }

            if (fine) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000,
                        5,
                        this
                );
            }

            if (coarse) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10,
                        this
                );
            }

            statusText.setText("● GPS ACTIV");

        } catch (SecurityException e) {

            statusText.setText(
                    "Permisiunea GPS trebuie verificată."
            );

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

        double speed = location.getSpeed() * 3.6;

        if (speed >= 0) {
            speedText.setText(
                    String.format(
                            Locale.getDefault(),
                            "VITEZĂ GPS: %.0f km/h",
                            speed
                    )
            );
        } else {
            speedText.setText("VITEZĂ GPS: -- km/h");
        }

        updateAutomaticGuidance(location);

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

    // DESTINATIE PE HARTA

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

    // COORDONATE

    private void showCoordinateDialog() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);

        EditText latitude = createInput("Latitudine");
        EditText longitude = createInput("Longitudine");

        latitude.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL |
                InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        longitude.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL |
                InputType.TYPE_NUMBER_FLAG_SIGNED
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
                                        lat,
                                        lon
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

    // PROFIL CAMION

    private void showTruckProfile() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(18, 10, 18, 10);

        EditText height = createInput("Înălțime camion (m)");
        EditText width = createInput("Lățime camion (m)");
        EditText length = createInput("Lungime camion (m)");
        EditText weight = createInput("Greutate totală (tone)");

        int decimalInput =
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL;

        height.setInputType(decimalInput);
        width.setInputType(decimalInput);
        length.setInputType(decimalInput);
        weight.setInputType(decimalInput);

        height.setText(preferences.getString(TRUCK_HEIGHT, ""));
        width.setText(preferences.getString(TRUCK_WIDTH, ""));
        length.setText(preferences.getString(TRUCK_LENGTH, ""));
        weight.setText(preferences.getString(TRUCK_WEIGHT, ""));

        layout.addView(height);
        layout.addView(width);
        layout.addView(length);
        layout.addView(weight);

        TextView warning = createText(
                "Dimensiunile sunt salvate în profil. " +
                "Motorul actual de rutare nu folosește aceste date " +
                "pentru calcularea restricțiilor."
        );

        warning.setTextColor(Color.rgb(220, 90, 50));
        layout.addView(warning);

        new AlertDialog.Builder(this)
                .setTitle("PROFILUL CAMIONULUI")
                .setView(layout)
                .setNegativeButton("ÎNCHIDE", null)
                .setPositiveButton("SALVEAZĂ", (dialog, which) -> {

                    preferences.edit()
                            .putString(
                                    TRUCK_HEIGHT,
                                    height.getText().toString().trim()
                            )
                            .putString(
                                    TRUCK_WIDTH,
                                    width.getText().toString().trim()
                            )
                            .putString(
                                    TRUCK_LENGTH,
                                    length.getText().toString().trim()
                            )
                            .putString(
                                    TRUCK_WEIGHT,
                                    weight.getText().toString().trim()
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            "Profil camion salvat.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    // FAVORITE SI ISTORIC

    private void savePlace(
            String key,
            String name,
            GeoPoint point
    ) {

        if (point == null) {
            return;
        }

        try {

            JSONArray old = new JSONArray(
                    preferences.getString(key, "[]")
            );

            JSONArray updated = new JSONArray();

            JSONObject item = new JSONObject();

            item.put("name", name);
            item.put("lat", point.getLatitude());
            item.put("lon", point.getLongitude());

            updated.put(item);

            for (int i = 0;
                 i < old.length() && updated.length() < 10;
                 i++) {

                JSONObject existing = old.getJSONObject(i);

                double lat = existing.optDouble("lat");
                double lon = existing.optDouble("lon");

                boolean duplicate =
                        Math.abs(lat - point.getLatitude()) < 0.0001 &&
                        Math.abs(lon - point.getLongitude()) < 0.0001;

                if (!duplicate) {
                    updated.put(existing);
                }
            }

            preferences.edit()
                    .putString(key, updated.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private void addFavorite() {

        GeoPoint point = destinationPoint != null
                ? destinationPoint
                : lastDestinationPoint;

        if (point == null) {
            Toast.makeText(
                    this,
                    "Selectează mai întâi o destinație.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String name =
                destinationInput.getText().toString().trim();

        if (name.isEmpty()) {
            name = "Destinație salvată";
        }

        savePlace(FAVORITES, name, point);

        Toast.makeText(
                this,
                "Destinație adăugată la favorite.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showSavedPlaces(
            String key,
            String title
    ) {

        try {

            JSONArray saved = new JSONArray(
                    preferences.getString(key, "[]")
            );

            if (saved.length() == 0) {
                Toast.makeText(
                        this,
                        "Nu există destinații salvate.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String[] names = new String[saved.length()];

            for (int i = 0; i < saved.length(); i++) {
                names[i] = saved.getJSONObject(i)
                        .optString("name", "Destinație");
            }

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setItems(names, (dialog, which) -> {

                        try {

                            JSONObject item =
                                    saved.getJSONObject(which);

                            String name =
                                    item.optString("name");

                            double lat =
                                    item.getDouble("lat");

                            double lon =
                                    item.getDouble("lon");

                            GeoPoint point =
                                    new GeoPoint(lat, lon);

                            destinationPoint = point;

                            changingDestination = true;
                            destinationInput.setText(name);
                            changingDestination = false;

                            setDestinationMarker(point);

                            map.getController().animateTo(point);
                            map.getController().setZoom(13.0);

                            statusText.setText(
                                    "Destinație selectată."
                            );

                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton("ÎNCHIDE", null)
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Nu am putut încărca destinațiile.",
                    Toast.LENGTH_SHORT
            ).show();
        }
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

        ArrayList<ManeuverPoint> newManeuvers =
                new ArrayList<>();

        JSONArray legs = route.getJSONArray("legs");

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

                String instruction =
                        translateManeuver(
                                type,
                                maneuver.optString("modifier")
                        );

                String road = step.optString("name");

                if (!road.isEmpty()) {
                    instruction += " pe " + road;
                }

                newInstructions.add(instruction);

                if (!"depart".equals(type)) {

                    GeoPoint maneuverLocation = null;

                    JSONArray location =
                            maneuver.optJSONArray("location");

                    if (location != null &&
                            location.length() >= 2) {

                        maneuverLocation = new GeoPoint(
                                location.getDouble(1),
                                location.getDouble(0)
                        );
                    }

                    newManeuvers.add(
                            new ManeuverPoint(
                                    maneuverLocation,
                                    instruction
                            )
                    );
                }
            }
        }

        runOnUiThread(() -> {

            instructions.clear();
            instructions.addAll(newInstructions);

            maneuverPoints.clear();
            maneuverPoints.addAll(newManeuvers);

            currentManeuverIndex = 0;
            routeCalculated = true;

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

            destinationPoint = end;
            lastDestinationPoint = end;

            String destinationName =
                    destinationInput.getText().toString().trim();

            if (destinationName.isEmpty()) {
                destinationName = "Destinație";
            }

            savePlace(
                    RECENTS,
                    destinationName,
                    end
            );

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
                        "PRIMA MANEVRĂ: " +
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

            Toast.makeText(
                    this,
                    "Traseu calculat.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // TRADUCERE MANEVRE

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

    // CONEXIUNE INTERNET

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
                    "TRUCK-ALEX-COMMUNITY/2.0"
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
