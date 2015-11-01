package com.studio.arm.wink;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.studio.arm.R;

import java.util.Locale;

public class Setting extends AppCompatActivity {

    private TextView timeOut;
    private static final String FILENAME = "setting_file";
    private SharedPreferences sharedPref;
    public static final String PREF_FILE_NAME = "PrefFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        timeOut = (TextView) findViewById(R.id.editTimeOut);
        sharedPref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        timeOut.setText(String.format("%.2f",sharedPref.getFloat(FILENAME, 1f)));
    }

    @Override
    public void onBackPressed() {
        //    moveTaskToBack(true);
        sharedPref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        try {
            editor.putFloat(FILENAME, Float.parseFloat(timeOut.getText().toString()));
            editor.apply();
        }catch (NumberFormatException n){
            Log.d("NumberEx", "parse string to number exception");
        }
        finish();
        overridePendingTransition(R.animator.back_in, R.animator.back_out);
    }
}
