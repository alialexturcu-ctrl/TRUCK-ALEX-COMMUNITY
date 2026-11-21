package com.alex.truckcommunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

public class MainActivity extends Activity {

    LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 30, 20, 30);
        layout.setBackgroundColor(Color.rgb(13, 19, 29));

        TextView title = new TextView(this);
        title.setText("TRUCK ALEX COMMUNITY");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("CAMION • GPS • NAVIGATIE");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        addPanel("HARTA GPS", "Navigatia GPS va fi adaugata ulterior.");
        addPanel("PROFIL TIR", "INALTIME: 4,00 m\nLATIME: 2,55 m\nLUNGIME: 16,50 m\nGREUTATE: 40 tone");
        addPanel("CAMION RIGID", "INALTIME: 3,50 m\nLATIME: 2,55 m\nLUNGIME: 8,00 m\nGREUTATE: 18 tone\n2 axe");
        addPanel("SETARI CAMION", "Alege profilul TIR sau camion rigid.");
        addPanel("VOCE ROMANA", "Vocea romana va fi configurata ulterior.");
        addPanel("DESPRE APLICATIE", "TRUCK ALEX COMMUNITY\nVersiunea 1.0");

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addPanel(String name, String message) {

        TextView panel = new TextView(this);
        panel.setText(name);
        panel.setTextColor(Color.WHITE);
        panel.setTextSize(21);
        panel.setTypeface(null, Typeface.BOLD);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(15, 15, 15, 15);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(30, 75, 115));
        background.setCornerRadius(20);
        background.setStroke(3, Color.rgb(70, 150, 210));

        panel.setBackground(background);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 95);

        params.setMargins(0, 18, 0, 0);
        layout.addView(panel, params);

        panel.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(message)
                .setPositiveButton("INCHIDE", null)
                .show());
    }
}
