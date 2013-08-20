package mei.tcd.smta;


import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import mei.tcd.ins.InsCalibrationActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ImageButton buttonCalibrate = (ImageButton)findViewById(R.id.btncalibrar);
        ImageButton buttonConfig = (ImageButton)findViewById(R.id.btnconfig);
        ImageButton buttonStart = (ImageButton)findViewById(R.id.btnstart);
        buttonConfig.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });
        buttonCalibrate.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, InsCalibrationActivity.class);
                startActivity(i);
            }
        });
        buttonStart.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, InsGpsActivity.class);
                startActivity(i);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
