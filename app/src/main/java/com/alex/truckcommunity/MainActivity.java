
package com.alex.truckcommunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends Activity {

    private LinearLayout home;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        showHome();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(18, 18, 18, 18);
        home.setBackgroundColor(Color.rgb(20, 28, 40));

        scroll.addView(home);

        addPanel("HARTA GPS", "Deschide harta OpenStreetMap.");
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
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 95);
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
        Configuration.getInstance().setUserAgentValue(getPackageName());

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        TextView back = new TextView(this);
        back.setText("← INAPOI");
        back.setTextSize(20);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundColor(Color.rgb(25, 105, 190));
        screen.addView(back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 60));

        mapView = new MapView(this);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(5.5);
        mapView.getController().setCenter(new GeoPoint(42.5, 12.5));

        screen.addView(mapView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        back.setOnClickListener(v -> {
            if (mapView != null) {
                mapView.onPause();
                mapView = null;
            }
            showHome();
        });

        setContentView(screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }
}
