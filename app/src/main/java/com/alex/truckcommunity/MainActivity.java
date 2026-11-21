
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
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;

public class MainActivity extends Activity implements OnMapReadyCallback {

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
        root.setPadding(12, 12, 12, 12);

        TextView title = new TextView(this);
        title.setText("GPS ALEX TRUCK NAVI");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        mapView = new MapView(this);
        root.addView(mapView, new LinearLayout.LayoutParams(
                -1, 0, 1));

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        Button parking = makeButton("PARCĂRI TIR");
        Button fuel = makeButton("MOTORINĂ");
        Button service = makeButton("SERVICE CAMIOANE");

        root.addView(parking);
        root.addView(fuel);
        root.addView(service);

        status = new TextView(this);
        status.setText("Apasă un buton pentru căutare în Google Maps.");
        status.setTextSize(15);
        status.setPadding(8, 12, 8, 12);
        root.addView(status);

        setContentView(root);

        parking.setOnClickListener(v ->
                searchGoogle("truck parking"));

        fuel.setOnClickListener(v ->
                searchGoogle("diesel fuel stations"));

        service.setOnClickListener(v ->
                searchGoogle("truck repair service"));

        getGPS();
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);
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

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
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
                    5000, 10,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                        }
                    });

        } catch (Exception e) {
            status.setText("GPS indisponibil.");
        }
    }

    @Override
    public void onMapReady(MapLibreMap mapLibreMap) {
        map = mapLibreMap;

        map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty",
                style -> {
                    if (latitude != 0 && longitude != 0) {
                        map.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(latitude, longitude))
                                .zoom(12)
                                .build());
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getGPS();
        }
    }
}
