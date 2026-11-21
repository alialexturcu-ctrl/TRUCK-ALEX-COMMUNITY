
package com.alex.truckcommunity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.Polyline;
import org.maplibre.android.annotations.PolylineOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private MapView mapView;
    private MapLibreMap map;

    private Marker truckMarker;
    private Polyline routeLine;

    private TextToSpeech voice;
    private boolean voiceReady = false;

    private boolean is3D = true;
    private boolean simulationRunning = false;

    private TextView maneuverTitle;
    private TextView maneuverDistance;
    private TextView exitPanel;
    private TextView remainingDistance;
    private TextView remainingTime;
    private TextView simulationButton;

    private int simulationStep = 0;

    private final Handler handler = new Handler();

    private final ArrayList<LatLng> demoRoute = new ArrayList<>();

    private final String[] instructions = {
        "Continua inainte",
        "Apropiere de intersectie",
        "Urmeaza indicatia spre dreapta",
        "Simulare iesire de autostrada",
        "Continua pe traseul demonstrativ",
        "Apropiere de destinatie"
    };

    private final String[] distances = {
        "2,0 km",
        "1,5 km",
        "900 m",
        "600 m",
        "250 m",
        "100 m"
    };

    private final String[] exits = {
        "A1  |  DIRECTIA INAINTE",
        "INTERSECTIE  |  DREAPTA",
        "IESIRE DEMO  |  900 m",
        "IESIRE DEMO  |  600 m",
        "TRASEU DEMONSTRATIV",
        "DESTINATIE APROAPE"
    };

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {

            if (!simulationRunning) return;

            simulationStep++;

            if (simulationStep >= instructions.length) {
                stopSimulation();
                Toast.makeText(
                    MainActivity.this,
                    "Simularea s-a terminat.",
                    Toast.LENGTH_LONG
                ).show();
                speak("Simularea s-a terminat.");
                return;
            }

            updateSimulationPanel();

            if (map != null && !demoRoute.isEmpty()) {
                int index = Math.min(
                    simulationStep,
                    demoRoute.size() - 1
                );

                LatLng point = demoRoute.get(index);

                if (truckMarker != null) {
                    map.removeMarker(truckMarker);
                }

                truckMarker = map.addMarker(
                    new MarkerOptions()
                        .position(point)
                        .title("CAMION DEMONSTRATIV")
                );

                CameraPosition camera =
                    new CameraPosition.Builder()
                        .target(point)
                        .zoom(16)
                        .tilt(is3D ? 60 : 0)
                        .bearing(is3D ? 35 : 0)
                        .build();

                map.setCameraPosition(camera);
            }

            speak(instructions[simulationStep]);

            handler.postDelayed(this, 5000);
        }
    };

    private int dp(float value) {
        return (int) (
            value * getResources()
                .getDisplayMetrics().density + 0.5f
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        initializeVoice();
        prepareDemoRoute();
        showHome();
    }

    private void initializeVoice() {

        voice = new TextToSpeech(this, result -> {

            if (result == TextToSpeech.SUCCESS) {

                int language = voice.setLanguage(
                    new Locale("ro", "RO")
                );

                voiceReady =
                    language != TextToSpeech.LANG_MISSING_DATA &&
                    language != TextToSpeech.LANG_NOT_SUPPORTED;

                if (voiceReady) {
                    voice.setSpeechRate(0.9f);
                }
            }
        });
    }

    private void speak(String message) {

        if (voice != null && voiceReady) {
            voice.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "GPS_ALEX_DEMO"
            );
        }
    }

    private void prepareDemoRoute() {

        demoRoute.clear();

        demoRoute.add(new LatLng(45.4100, 11.8700));
        demoRoute.add(new LatLng(45.4120, 11.8730));
        demoRoute.add(new LatLng(45.4140, 11.8760));
        demoRoute.add(new LatLng(45.4160, 11.8790));
        demoRoute.add(new LatLng(45.4180, 11.8820));
        demoRoute.add(new LatLng(45.4200, 11.8850));
    }

    private GradientDrawable background(
        int color,
        int radius
    ) {

        GradientDrawable drawable =
            new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        return drawable;
    }

    private TextView makeText(
        String value,
        int size,
        int color,
        boolean bold
    ) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);

        if (bold) {
            text.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            );
        }

        return text;
    }

    private TextView makeButton(String value) {

        TextView button = makeText(
            value,
            15,
            Color.WHITE,
            true
        );

        button.setBackground(background(
            Color.rgb(20, 35, 52),
            12
        ));

        button.setPadding(
            dp(8), dp(8), dp(8), dp(8)
        );

        return button;
    }

    private void showHome() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(12, 22, 35));

        TextView title = makeText(
            "GPS ALEX",
            34,
            Color.WHITE,
            true
        );

        root.addView(title);

        TextView subtitle = makeText(
            "TRUCK NAVI 2.0",
            17,
            Color.rgb(80, 190, 255),
            true
        );

        root.addView(subtitle);

        TextView description = makeText(
            "SIMULATOR GPS 3D\n\n" +
            "Harta 3D demonstrativa\n" +
            "Panouri de orientare\n" +
            "Indicatii in limba romana\n" +
            "Simulare traseu",
            17,
            Color.LTGRAY,
            false
        );

        description.setPadding(
            dp(10), dp(30), dp(10), dp(30)
        );

        root.addView(description);

        TextView start = makeButton(
            "DESCHIDE GPS ALEX"
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                -1, dp(58)
            );

        root.addView(start, params);

        start.setOnClickListener(v -> showMap());

        setContentView(root);
    }

    private void showMap() {

        stopSimulation();

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);

        mapView = new MapView(this);

        frame.addView(
            mapView,
            new FrameLayout.LayoutParams(-1, -1)
        );

        LinearLayout topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.VERTICAL);
        topPanel.setPadding(dp(12), dp(8), dp(12), dp(8));
        topPanel.setBackground(background(
            Color.rgb(15, 30, 47),
            16
        ));

        maneuverTitle = makeText(
            "SIMULARE GPS",
            22,
            Color.WHITE,
            true
        );

        maneuverDistance = makeText(
            "Apasa START SIMULARE",
            17,
            Color.rgb(100, 210, 255),
            true
        );

        topPanel.addView(maneuverTitle);
        topPanel.addView(maneuverDistance);

        FrameLayout.LayoutParams topParams =
            new FrameLayout.LayoutParams(
                -1,
                dp(105),
                Gravity.TOP
            );

        topParams.setMargins(
            dp(10), dp(10), dp(10), 0
        );

        frame.addView(topPanel, topParams);

        exitPanel = makeText(
            "A1  |  INDICATOR DEMONSTRATIV",
            17,
            Color.WHITE,
            true
        );

        exitPanel.setBackground(background(
            Color.rgb(20, 100, 65),
            12
        ));

        FrameLayout.LayoutParams exitParams =
            new FrameLayout.LayoutParams(
                -1,
                dp(58),
                Gravity.TOP
            );

        exitParams.setMargins(
            dp(10), dp(125), dp(10), 0
        );

        frame.addView(exitPanel, exitParams);

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(12), dp(10), dp(12), dp(10));
        bottomPanel.setBackground(background(
            Color.rgb(15, 30, 47),
            16
        ));

        remainingDistance = makeText(
            "DISTANTA: 2,0 km",
            18,
            Color.WHITE,
            true
        );

        remainingTime = makeText(
            "TIMP SIMULAT: 10 minute",
            16,
            Color.rgb(100, 210, 255),
            true
        );

        bottomPanel.addView(remainingDistance);
        bottomPanel.addView(remainingTime);

        simulationButton = makeButton(
            "START SIMULARE"
        );

        LinearLayout.LayoutParams buttonParams =
            new LinearLayout.LayoutParams(
                -1, dp(48)
            );

        buttonParams.setMargins(
            0, dp(10), 0, 0
        );

        bottomPanel.addView(
            simulationButton,
            buttonParams
        );

        TextView modeButton = makeButton(
            "SCHIMBA 2D / 3D"
        );

        LinearLayout.LayoutParams modeParams =
            new LinearLayout.LayoutParams(
                -1, dp(42)
            );

        modeParams.setMargins(
            0, dp(7), 0, 0
        );

        bottomPanel.addView(modeButton, modeParams);

        TextView backButton = makeButton(
            "INAPOI LA MENIU"
        );

        LinearLayout.LayoutParams backParams =
            new LinearLayout.LayoutParams(
                -1, dp(42)
            );

        backParams.setMargins(
            0, dp(7), 0, 0
        );

        bottomPanel.addView(backButton, backParams);

        FrameLayout.LayoutParams bottomParams =
            new FrameLayout.LayoutParams(
                -1,
                -2,
                Gravity.BOTTOM
            );

        bottomParams.setMargins(
            dp(10), 0, dp(10), dp(10)
        );

        frame.addView(bottomPanel, bottomParams);

        setContentView(frame);

        simulationButton.setOnClickListener(v -> {

            if (simulationRunning) {
                stopSimulation();
            } else {
                startSimulation();
            }
        });

        modeButton.setOnClickListener(v -> {

            is3D = !is3D;

            if (map != null) {
                updateDemoCamera();
            }

            Toast.makeText(
                this,
                is3D ? "MOD 3D ACTIV" : "MOD 2D ACTIV",
                Toast.LENGTH_SHORT
            ).show();
        });

        backButton.setOnClickListener(v -> {
            stopSimulation();
            destroyMap();
            showHome();
        });

        initializeMap();
    }

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

                    updateDemoCamera();
                    drawDemoRoute();

                    LatLng first = demoRoute.get(0);

                    truckMarker = map.addMarker(
                        new MarkerOptions()
                            .position(first)
                            .title("CAMION DEMONSTRATIV")
                    );
                }
            );
        });
    }

    private void updateDemoCamera() {

        if (map == null) return;

        LatLng point = demoRoute.get(
            Math.min(
                simulationStep,
                demoRoute.size() - 1
            )
        );

        CameraPosition camera =
            new CameraPosition.Builder()
                .target(point)
                .zoom(16)
                .tilt(is3D ? 60 : 0)
                .bearing(is3D ? 35 : 0)
                .build();

        map.setCameraPosition(camera);
    }

    private void drawDemoRoute() {

        if (map == null || demoRoute.size() < 2) return;

        if (routeLine != null) {
            map.removePolyline(routeLine);
        }

        routeLine = map.addPolyline(
            new PolylineOptions()
                .addAll(demoRoute)
                .color(Color.BLUE)
                .width(7)
        );
    }

    private void startSimulation() {

        if (simulationRunning) return;

        simulationStep = 0;
        simulationRunning = true;

        simulationButton.setText(
            "OPRESTE SIMULAREA"
        );

        updateSimulationPanel();

        speak("A inceput simularea GPS Alex.");

        handler.postDelayed(
            simulationRunnable,
            5000
        );
    }

    private void stopSimulation() {

        simulationRunning = false;

        handler.removeCallbacks(
            simulationRunnable
        );

        if (simulationButton != null) {
            simulationButton.setText(
                "START SIMULARE"
            );
        }

        if (voice != null) {
            voice.stop();
        }
    }

    private void updateSimulationPanel() {

        if (maneuverTitle == null) return;

        int index = Math.min(
            simulationStep,
            instructions.length - 1
        );

        maneuverTitle.setText(
            instructions[index]
        );

        maneuverDistance.setText(
            "Urmatoarea indicatie: " +
            distances[index]
        );

        exitPanel.setText(
            exits[index]
        );

        remainingDistance.setText(
            "DISTANTA SIMULATA: " +
            (instructions.length - index) +
            " km"
        );

        remainingTime.setText(
            "ETAPA " + (index + 1) +
            " DIN " + instructions.length
        );
    }

    private void destroyMap() {

        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }

        map = null;
        truckMarker = null;
        routeLine = null;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.onPause();
        }

        stopSimulation();
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

        stopSimulation();
        destroyMap();

        if (voice != null) {
            voice.stop();
            voice.shutdown();
            voice = null;
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
    public void onBackPressed() {

        stopSimulation();
        destroyMap();
        showHome();
    }
}
