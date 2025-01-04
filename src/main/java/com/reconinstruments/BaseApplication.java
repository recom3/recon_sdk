package com.reconinstruments;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.reconinstruments.hudserver.HUDWebService;

/**
 * Created by Recom3 on 02/01/2025.
 */

public class BaseApplication extends Application {
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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Handle service disconnection
        }
    };
}

