package com.alex.truckcommunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {

    LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 35, 25, 25);
        layout.setBackgroundColor(Color.rgb(15, 20, 28));

        TextView title = new TextView(this);
        title.setText("TRUCK ALEX COMMUNITY");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        layout.addView(title);

        addButton("HARTA GPS", "Navigatia GPS va fi adaugata ulterior.");
        addButton("PROFIL TIR",
                "INALTIME: 4,00 m\nLATIME: 2,55 m\nLUNGIME: 16,50 m\nGREUTATE: 40 tone");

        addButton("CAMION RIGID",
                "INALTIME: 3,50 m\nLATIME: 2,55 m\nLUNGIME: 8,00 m\nGREUTATE: 18 tone\n2 axe");

        addButton("SETARI CAMION",
                "Alege profilul TIR sau camion rigid.");

        addButton("VOCE ROMANA",
                "Vocea romana va fi configurata intr-o etapa viitoare.");

        addButton("DESPRE APLICATIE",
                "TRUCK ALEX COMMUNITY\nVersiunea 1.0");

        setContentView(layout);
    }

    private void addButton(String name, String message) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(35, 100, 160));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 65);

        params.setMargins(0, 18, 0, 0);
        layout.addView(button, params);

        button.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(name)
                        .setMessage(message)
                        .setPositiveButton("INCHIDE", null)
                        .show());
    }
}
