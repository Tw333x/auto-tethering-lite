package com.labs.dm.auto_tethering_lite.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.labs.dm.auto_tethering_lite.BuildConfig;
import com.labs.dm.auto_tethering_lite.R;
import com.labs.dm.auto_tethering_lite.Utils;
import com.labs.dm.auto_tethering_lite.receiver.BootCompletedReceiver;
import com.labs.dm.auto_tethering_lite.receiver.TetheringWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.labs.dm.auto_tethering_lite.AppProperties.AUTO_START;
import static com.labs.dm.auto_tethering_lite.AppProperties.DONT_REMIND;
import static com.labs.dm.auto_tethering_lite.AppProperties.EDIT_MODE;

/**
 * Created by Daniel Mroczka on 2016-01-09.
 */
public class ConfigurationActivity extends Activity {

    private int mAppWidgetId;
    private boolean editMode;
    private boolean activityMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        setResult(RESULT_CANCELED);
        init();
    }

    private void init() {
        activityMode = "android.intent.action.MAIN".equals(getIntent().getAction());
        editMode = getIntent().getExtras() != null && getIntent().getExtras().getBoolean(EDIT_MODE, false);
        mAppWidgetId = Utils.getWidgetId(getIntent());
        if (mAppWidgetId == INVALID_APPWIDGET_ID) {
            Log.e("WidgetAdd", "Cannot continue. Widget ID incorrect");
        }

        CheckBox autoStart = (CheckBox) findViewById(R.id.chkAutoStart);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        autoStart.setChecked(prefs.getBoolean(AUTO_START, false));
        autoStart.setMaxLines(2);

        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setText(activityMode ? getString(R.string.save_settings) : (editMode ? getString(R.string.modify_widget) : getString(R.string.add_widget)));
        okButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleOkButton();
            }
        });

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TextView textView = (TextView) findViewById(R.id.label);
        String buildTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(BuildConfig.buildTime);
        textView.setText(String.format("%s.%s build: %s", pInfo != null ? pInfo.versionName : null, BuildConfig.BUILD_TYPE.toUpperCase(), buildTime));

        checkIfNotlocked();
    }

    private void handleOkButton() {
        saveWidget();
    }

    private void saveWidget() {
        CheckBox autoStart = (CheckBox) findViewById(R.id.chkAutoStart);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putBoolean(AUTO_START, autoStart.isChecked()).apply();

        if (!editMode && !activityMode) {
            Toast.makeText(this, R.string.tap_modify_widget, Toast.LENGTH_LONG).show();
        }

        Intent serviceIntent = new Intent(ConfigurationActivity.this, TetheringWidgetProvider.class);
        serviceIntent.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, serviceIntent);
        startService(serviceIntent);
        finish();
    }

    /**
     * Checks if app is not blocked on startup. If yes it could unblock and force startup.
     */
    private void checkIfNotlocked() {
        final ComponentName componentName = new ComponentName(this, BootCompletedReceiver.class);
        int state = getPackageManager().getComponentEnabledSetting(componentName);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !prefs.getBoolean(DONT_REMIND, false)) {
            new AlertDialog.Builder(ConfigurationActivity.this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.startup_blocked_prompt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CheckBox activationStartup = (CheckBox) findViewById(R.id.chkAutoStart);
                            activationStartup.setChecked(true);
                            getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                            Toast.makeText(getApplicationContext(), R.string.on_startup_enable, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNeutralButton(R.string.donot_remind, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putBoolean(DONT_REMIND, true).apply();
                        }
                    })
                    .setNegativeButton(R.string.no, null
                    ).show();
        }
    }
}
