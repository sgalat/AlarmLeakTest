package org.sgalat.alarmleaktest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;

public class MainActivity
    extends AppCompatActivity
{
    private final static int NUM_BATCH_ALARMS = 10;
    private final static long SCHEDULING_PERIOD_MS = 100;
    private final static long ALARM_DELAY = 5000;
    private final static String ALARM_TAG = "TestAlarm1";

    private AlarmManager alarmManager;
    private Handler handler = new Handler();
    private int numAlarmsScheduled;
    private final AlarmListener[] listeners = new AlarmListener[NUM_BATCH_ALARMS];
    private TextView tv;

    private final Runnable alarmSchedulingRunnable = new Runnable() {
        @Override
        public void run()
        {
            final long alarmTime = SystemClock.elapsedRealtime() + ALARM_DELAY;
            for (int i = 0; i < NUM_BATCH_ALARMS; ++i) {
                alarmManager.cancel(listeners[i]);
                alarmManager.setExact(
                    ELAPSED_REALTIME_WAKEUP, alarmTime, ALARM_TAG, listeners[i], handler);
            }

            numAlarmsScheduled += NUM_BATCH_ALARMS;
            updateUi();

            handler.postDelayed(alarmSchedulingRunnable, SCHEDULING_PERIOD_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        tv = findViewById(R.id.tv);

        for (int i = 0; i < NUM_BATCH_ALARMS; ++i) {
            listeners[i] = new AlarmListener();
        }
    }

    public void onStartTest(View view) {
        numAlarmsScheduled = 0;
        updateUi();
        handler.post(alarmSchedulingRunnable);
    }

    public void onStopTest(View view) {
        handler.removeCallbacks(alarmSchedulingRunnable);
    }

    private void updateUi() {
        tv.setText(Integer.toString(numAlarmsScheduled));
    }

    private class AlarmListener implements AlarmManager.OnAlarmListener {
        @Override
        public void onAlarm() {
            // no-op
        }
    }
}
