package com.andela.gkuti.mipista.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.andela.gkuti.mipista.dal.Datastore;
import com.andela.gkuti.mipista.dal.UserData;
import com.andela.gkuti.mipista.util.Constants;
import com.andela.gkuti.mipista.util.Date;

import java.util.concurrent.TimeUnit;

public class Tracker extends BroadcastReceiver {
    private BroadcastReceiver updateListReceiver;
    private Thread thread;
    private String activity;
    private Context context;
    private boolean isRunning = false;
    private Datastore datastore;
    private String startTime;
    private String endTime;
    private String date;
    private int duration;
    private UserData userData;
    private long stime;
    private long etime;
    private IntentFilter activityFilter;
    private String location = "unknown";
    private boolean stop;
    private BroadcastReceiver locationUpdate;

    public Tracker(Context context) {
        this.context = context;
        datastore = new Datastore(context);
        userData = new UserData(context);
        activity = "STI";
        updateListReceiver = this;
        locationUpdate = this;
        activityFilter = new IntentFilter();
        activityFilter.addAction(Constants.ACTION.getValue());
        registerLocationUpdates();
    }

    private void registerLocationUpdates() {
        locationUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                location = intent.getStringExtra("Location");
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("location");
        context.registerReceiver(locationUpdate, filter);
    }

    public void startTracker() {
        stop = false;
        start();
        context.registerReceiver(updateListReceiver, activityFilter);
    }

    private void start() {
        if (!isRunning) {
            initthread();
            initializeTracking();
            thread.start();
        }
    }

    private void initthread() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (activity.equals("STI") && !stop) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                endTracking();
            }
        });
    }

    private void save() {
        if (shouldSave()) {
            datastore.saveData(location, startTime, endTime, date, duration);
        }
    }

    private boolean shouldSave() {
        if (isUnknown()) {
            return checkTime();
        } else if (!location.equals("unknown")) {
            return checkTime();
        }
        return false;
    }

    private boolean isUnknown() {
        if (location.equals("unknown") && userData.getData("unknown") == 0) {
            return true;
        }
        return false;
    }

    private boolean checkTime() {
        duration = (int) TimeUnit.MILLISECONDS.toSeconds(etime - stime);
        return duration >= (userData.getData("delay") * 60);
    }

    private void initializeTracking() {
        isRunning = true;
        date = Date.getDate();
        startTime = Date.getTime();
        stime = System.currentTimeMillis();
    }

    private void endTracking() {
        isRunning = false;
        etime = System.currentTimeMillis();
        endTime = Date.getTime();
        save();
    }

    public void stopTracker() {
        try {
            stop = true;
            context.unregisterReceiver(updateListReceiver);
        } catch (Exception e) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        activity = intent.getStringExtra("Activity");
        start();
    }
}
