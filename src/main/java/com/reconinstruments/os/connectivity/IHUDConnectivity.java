package com.reconinstruments.os.connectivity;

/**
 * Created by Recom3 on 31/01/2023.
 */

public interface IHUDConnectivity {
    public enum ConnectionState {
        a, b, c, d, e;
    }

    public enum NetworkEvent {
        a, b, c, d;
    }

    void onConnectionStateChanged(ConnectionState paramConnectionState);

    void onNetworkEvent(NetworkEvent paramNetworkEvent, boolean paramBoolean);

    void onDeviceName(String paramString);
}
