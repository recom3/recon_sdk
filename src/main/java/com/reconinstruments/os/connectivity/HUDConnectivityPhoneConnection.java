package com.reconinstruments.os.connectivity;

import java.io.IOException;

import android.accounts.NetworkErrorException;
import android.content.Context;

import com.reconinstruments.os.connectivity.bluetooth.HUDSPPService;
import com.reconinstruments.os.connectivity.http.HUDHttpBTConnection;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;
import com.reconinstruments.os.connectivity.http.URLConnectionHUDAdaptor;

/** {@hide} */
public class HUDConnectivityPhoneConnection implements IHUDConnectivityConnection {
    private HUDHttpBTConnection mHUDHttpBTConnection = null;
    private HUDSPPService mHUDBTService = null;

    public HUDConnectivityPhoneConnection(Context context, IHUDConnectivity hudConnectivity, int socketCount) throws Exception {
        mHUDBTService = new HUDSPPService(hudConnectivity, socketCount);
        //!!!This is redundant as we set the mHUDBTService after
        mHUDHttpBTConnection = new HUDHttpBTConnection(context, hudConnectivity, false, mHUDBTService);
        mHUDHttpBTConnection.setBTService(mHUDBTService);
    }

    @Override
    public boolean hasWebConnection() {
        return mHUDHttpBTConnection.hasWebConnection();
    }

    @Override
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws IOException, NetworkErrorException {
        if (mHUDHttpBTConnection.hasLocalWebConnection()) {
            return URLConnectionHUDAdaptor.sendWebRequest(request);
        }

        throw new NetworkErrorException("Phone is not connected to the internet");
    }

    @Override
    public void start() throws IOException, InterruptedException {
        //!!!Commented as we are not in phone
        /*
        mHUDHttpBTConnection.startListening();
        mHUDBTService.startListening();
        mHUDHttpBTConnection.updateLocalWebConnection();
        */
    }

    @Override
    public void stop() throws IOException {
        //!!!Commented as we are not in phone
        /*
        mHUDHttpBTConnection.stopListening();
        mHUDBTService.stopListening();
        */
    }
}
