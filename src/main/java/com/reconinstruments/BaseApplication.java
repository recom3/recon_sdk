package com.reconinstruments;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.reconinstruments.hudserver.HUDWebService;
import com.reconinstruments.os.connectivity.HUDConnectivityManager;

/**
 * Created by Recom3 on 02/01/2025.
 */

public class BaseApplication extends Application {

    private HUDWebService mHUDWebService;
    private HUDConnectivityManager mHUDConnectivityManager;
    private boolean isServiceBound = false;
    private OnServiceBoundListener serviceBoundListener;

    public interface OnServiceBoundListener {
        void onServiceBound();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindServices();
    }

    private void bindServices() {
        // Your binding logic here
        Intent serviceIntent = new Intent(this, HUDWebService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Handle service connection

            HUDWebService.LocalBinder binder = (HUDWebService.LocalBinder) service;
            BaseApplication.this.mHUDWebService = binder.getService();

            BaseApplication.this.mHUDConnectivityManager = BaseApplication.this.mHUDWebService.hudConnectivityManager;

            isServiceBound = true;

            // Notify the listener
            if (serviceBoundListener != null) {
                serviceBoundListener.onServiceBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Handle service disconnection
            isServiceBound = false;
        }
    };

    public void setOnServiceBoundListener(OnServiceBoundListener listener) {
        this.serviceBoundListener = listener;

        // If already bound, notify immediately
        if (isServiceBound && listener != null) {
            listener.onServiceBound();
        }
    }

    public HUDWebService getmHUDWebService() {
        return mHUDWebService;
    }

    public void setmHUDWebService(HUDWebService mHUDWebService) {
        this.mHUDWebService = mHUDWebService;
    }

    public HUDConnectivityManager getmHUDConnectivityManager() {
        return mHUDConnectivityManager;
    }

    public void setmHUDConnectivityManager(HUDConnectivityManager mHUDConnectivityManager) {
        this.mHUDConnectivityManager = mHUDConnectivityManager;
    }
}

