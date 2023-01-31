package com.reconinstruments.os.connectivity;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Recom3 on 31/01/2023.
 */

public class HUDConnectivityManager implements IHUDConnectivity {

    static Queue<QueueMessage> mCommandQueue = new LinkedList<QueueMessage>();

    static Queue<QueueMessage> mFileQueue;

    static Queue<QueueMessage> mObjectQueue = new LinkedList<QueueMessage>();

    @Override
    public void onConnectionStateChanged(ConnectionState paramConnectionState) {

    }

    @Override
    public void onNetworkEvent(NetworkEvent paramNetworkEvent, boolean paramBoolean) {

    }

    @Override
    public void onDeviceName(String paramString) {

    }
}
