package com.reconinstruments.os.connectivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Recom3 on 31/01/2023.
 */

public class HUDConnectivityManager implements IHUDConnectivity {

    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private ConnectivityHandler mHandler = null;
    private IHUDConnectivityConnection mHUDConnectivityConnection = null;

    public static final int MESSAGE_BT_STATE_CHANGE = 1;
    public static final int MESSAGE_NETWORK_EVENT = 2;
    public static final int MESSAGE_READ = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;

    static Queue<QueueMessage> mCommandQueue = new LinkedList<QueueMessage>();

    static Queue<QueueMessage> mFileQueue;

    static Queue<QueueMessage> mObjectQueue = new LinkedList<QueueMessage>();

    // Key names sent to the Handler
    private static final String DEVICE_NAME = "device_name";

    private String mDeviceName = "";
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    public boolean mHasLocalWeb = false;
    public boolean mHasRemoteWeb = false;

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        if(DEBUG) Log.d(TAG, "onConnectionStateChanged:" + state);
        mConnectionState = state;
        mHandler.obtainMessage(MESSAGE_BT_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        int i = 1;
        new StringBuilder("onNetworkEvent:").append(networkEvent).append(" hasNetworkAccess:").append(hasNetworkAccess);
        switch (networkEvent) {
            case LOCAL_WEB_GAINED:
                this.mHasLocalWeb = true;
                break;
            case LOCAL_WEB_LOST:
                this.mHasLocalWeb = false;
                break;
            case REMOTE_WEB_GAINED:
                this.mHasRemoteWeb = true;
                break;
            case REMOTE_WEB_LOST:
                this.mHasRemoteWeb = false;
                break;
        }
        ConnectivityHandler connectivityHandler = this.mHandler;
        int ordinal = networkEvent.ordinal();
        if (!hasWebConnection()) {
            i = 0;
        }
        connectivityHandler.obtainMessage(2, ordinal, i).sendToTarget();
    }

    @Override
    public void onDeviceName(String deviceName) {
        if(DEBUG) Log.d(TAG, "onDeviceName:" + deviceName);
        mDeviceName = deviceName;
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    public final boolean hasWebConnection() {
        boolean z;
        try {
            z = this.mHUDConnectivityConnection.hasWebConnection();
        } catch (Exception e) {
            z = false;
        }
        return z;
    }

    static class ConnectivityHandler extends Handler {
        private ArrayList<WeakReference<IHUDConnectivity>> mWeakHUDConnectivities = new ArrayList<WeakReference<IHUDConnectivity>>();

        private WeakReference<IHUDConnectivity> get(IHUDConnectivity hudConnectivity) {
            synchronized (mWeakHUDConnectivities) {
                for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                    if(mWeakHUDConnectivities.get(i).get() == hudConnectivity) {
                        return mWeakHUDConnectivities.get(i);
                    }
                }
            }
            return null;
        }

        public boolean register(IHUDConnectivity hudConnectivity) {
            if(hudConnectivity == null) {
                return false;
            }

            WeakReference<IHUDConnectivity> weakHUDConnectivity = get(hudConnectivity);
            if(weakHUDConnectivity != null) {
                Log.w(TAG, "IHUDConnectivity already registered");
                return false;
            }

            synchronized (mWeakHUDConnectivities) {
                mWeakHUDConnectivities.add(new WeakReference<IHUDConnectivity>(hudConnectivity));
            }
            return true;
        }

        public void unregister(IHUDConnectivity hudConnectivity) {
            if(hudConnectivity == null) {
                return;
            }

            WeakReference<IHUDConnectivity> weakHUDConnectivity = get(hudConnectivity);
            if(weakHUDConnectivity == null) {
                Log.w(TAG, "IHUDConnectivity is not registered");
                return;
            }

            synchronized (mWeakHUDConnectivities) {
                mWeakHUDConnectivities.remove(weakHUDConnectivity);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakHUDConnectivities.size() == 0) {
                Log.d(TAG, "No IHUDConnectivity registered in ConnectivityHandler");
                return;
            }

            switch (msg.what) {
                case MESSAGE_BT_STATE_CHANGE:
                    if (DEBUG) Log.i(TAG, "MESSAGE_BT_STATE_CHANGE: " + msg.arg1);
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onConnectionStateChanged(ConnectionState.values()[msg.arg1]);
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
                case MESSAGE_NETWORK_EVENT:
                    if (DEBUG) Log.i(TAG, "MESSAGE_NETWORK_EVENT: " + msg.arg1);
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onNetworkEvent(NetworkEvent.values()[msg.arg1], (msg.arg2 == 1));
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    if (DEBUG) Log.i(TAG, "MESSAGE_DEVICE_NAME: " + msg.getData().getString(DEVICE_NAME));
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onDeviceName(msg.getData().getString(DEVICE_NAME));
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
            }
        }
    }
}
