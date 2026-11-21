
package com.alex.truckcommunity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

public class MainActivity extends Activity {

    private MapView mapView;
    private MapLibreMap map;
    private LocationManager locationManager;
    private double latitude = 0;
    private double longitude = 0;
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.setPadding(12, 12, 12, 12);

        TextView title = new TextView(this);
        title.setText("GPS ALEX TRUCK NAVI");
        title.setTextSize(25);
        title.setTextColor(0xFF111111);
        title.setGravity(Gravity.CENTER);
        title.setPadding(5, 15, 5, 15);
        root.addView(title);

        TextView truck = new TextView(this);
        truck.setText("🚛 VOLVO AERO 2026");
        truck.setTextSize(27);
        truck.setTextColor(0xFF222222);
        truck.setGravity(Gravity.CENTER);
        truck.setPadding(5, 35, 5, 35);
        root.addView(truck);

        TextView subtitle = new TextView(this);
        subtitle.setText("TRUCK NAVIGATION");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFF555555);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(0, 25, 0, 15);

        Button parking = makeButton("PARCĂRI TIR");
        Button fuel = makeButton("MOTORINĂ");
        Button service = makeButton("SERVICE CAMIOANE");

        buttons.addView(parking);
        buttons.addView(fuel);
        buttons.addView(service);

        root.addView(buttons);

        status = new TextView(this);
        status.setText("Pregătit pentru navigare");
        status.setTextSize(15);
        status.setTextColor(0xFF222222);
        status.setGravity(Gravity.CENTER);
        status.setPadding(5, 10, 5, 10);
        root.addView(status);

        mapView = new MapView(this);
        root.addView(mapView,
                new LinearLayout.LayoutParams(-1, 0, 1));

        mapView.onCreate(savedInstanceState);
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
                        }
                    });
        });

        parking.setOnClickListener(v ->
                searchGoogle("truck parking"));

        fuel.setOnClickListener(v ->
                searchGoogle("diesel fuel stations"));

        service.setOnClickListener(v ->
                searchGoogle("truck repair service"));

        setContentView(root);

        getGPS();
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF0877CC));
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, 65);
        params.setMargins(0, 6, 0, 6);
        button.setLayoutParams(params);
        return button;
    }

    private void searchGoogle(String query) {
        String url;

        if (latitude != 0 && longitude != 0) {
            url = "https://www.google.com/maps/search/"
                    + Uri.encode(query)
                    + "/@" + latitude + "," + longitude + ",12z";
        } else {
            url = "https://www.google.com/maps/search/"
                    + Uri.encode(query);
        }

        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            status.setText("Căutare deschisă în Google Maps.");
        } catch (Exception e) {
            status.setText("Nu s-a putut deschide Google Maps.");
        }
    }

    private void getGPS() {
        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 100);
            return;
        }

        try {
            Location location = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    10,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                        }
                    });

        } catch (SecurityException e) {
            status.setText("Permisiunea GPS nu este disponibilă.");
        } catch (Exception e) {
            status.setText("GPS indisponibil.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 100
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getGPS();
        }
    }
}
