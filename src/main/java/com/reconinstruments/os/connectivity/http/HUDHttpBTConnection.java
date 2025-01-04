package com.reconinstruments.os.connectivity.http;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTBaseService;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTHeaderFactory;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTMessage;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTMessageCollectionManager;
import com.reconinstruments.os.connectivity.bluetooth.HUDSPPService;
import com.reconinstruments.os.connectivity.bluetooth.IHUDBTConsumer;
import com.reconinstruments.os.connectivity.bluetooth.IHUDBTService;

import java.io.IOException;

import static com.reconinstruments.os.connectivity.bluetooth.HUDBTHeaderFactory.APPLICATION__PHONE;
import static com.reconinstruments.os.connectivity.bluetooth.HUDBTHeaderFactory.APPLICATION__WEB;

/**
 * Class to send http request via BT
 * Is it using SPP class: HUDSPPService (via IHUDConnectivity)
 * HUDConnectivityPhoneConnection contains:
 * HUDSPPService
 * HUDHttpBTConnection
 *
 * HUDConnectivityPhoneConnection is contained in HUDWebService
 * HUDWebService contains HUDConnectivityManager that is used to send the messages
 *
 * HUDWebService is declared in the manifest
 *
 * This class HUDHttpBTConnection
 * Uses: URLConnectionHUDAdaptor
 */
public class HUDHttpBTConnection implements IHUDBTConsumer {

    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;
    private static final int MAX_WAIT_RESPONSE = 300000; // 300 seconds = 5 minutes

    //Check repeated member
    private boolean mIsHUD = true;
    private boolean h = true;

    private Context mContext = null;

    public IHUDBTService mHUDBTService = null;
    public static IHUDBTService ihudbtService = null;

    private IHUDConnectivity mHUDConnectivity = null;

    private static IHUDConnectivity e = null;

    private static boolean mHasLocalWebConnection = false;
    private static boolean mHasRemoteWebConnection = false;

    public Context context = null;

    public BroadcastReceiver c = new BroadcastReceiver() {
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                HUDHttpBTConnection.this.updateLocalWebConnection(context);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    };

    public HUDHttpBTConnection(Context paramContext, IHUDConnectivity paramIHUDConnectivity) {
        if (paramIHUDConnectivity == null)
            throw new NullPointerException("HUDHttpBTConnection hudConnectivity can't be null");
        this.h = false;
        this.context = paramContext;
        e = paramIHUDConnectivity;
    }

    public HUDHttpBTConnection(Context context, IHUDConnectivity hudConnectivity, boolean isHUD, IHUDBTService hudBTService) {
        if (hudBTService == null || hudConnectivity == null) {
            throw new NullPointerException("HUDHttpBTConnection hudConnectivity or hudBTService can't have null values");
        }

        mIsHUD = isHUD;
        mContext = context;
        mHUDConnectivity = hudConnectivity;

        mHUDBTService = hudBTService;

        mHUDBTService.addConsumer(this);
    }

    /**
     * This method is used to send and array of bytes
     * Possiby this method is invoqued when getting an http request from a message comming from the Goggles?
     * @param paramArrayOfbyte
     * @return
     */
    private boolean sendByteArr(byte[] paramArrayOfbyte) {
        boolean bool = true;
        try {
            IHUDConnectivity.NetworkEvent networkEvent = null;
            if (HUDBTHeaderFactory.getApplication(paramArrayOfbyte) == 3) {
                if (HUDBTHeaderFactory.getCmd(paramArrayOfbyte) == 2) {
                    boolean bool1 = bool;
                    if (this.h) {
                        boolean bool2;
                        if (HUDBTHeaderFactory.getArg1(paramArrayOfbyte) == 1) {
                            bool2 = true;
                        } else {
                            bool2 = false;
                        }
                        bool1 = bool;
                        if (bool2 != mHasRemoteWebConnection) {
                            StringBuilder stringBuilder = new StringBuilder("Network state went from ");
                            stringBuilder.append(mHasRemoteWebConnection).append(" to ").append(bool2);
                            mHasRemoteWebConnection = bool2;
                            IHUDConnectivity iHUDConnectivity = e;
                            if (mHasRemoteWebConnection) {
                                networkEvent = IHUDConnectivity.NetworkEvent.REMOTE_WEB_GAINED;
                            } else {
                                networkEvent = IHUDConnectivity.NetworkEvent.REMOTE_WEB_LOST;
                            }
                            //HUDSPPService implements IHUDConnectivity
                            //iHUDConnectivity.a(networkEvent, a());
                            iHUDConnectivity.onNetworkEvent(networkEvent, HUDHttpBTConnection.this.hasWebConnection());
                            bool1 = bool;
                        }
                    }
                    return bool1;
                }
                return false;
            }

            if (HUDBTHeaderFactory.getApplication((byte[])paramArrayOfbyte) == 2) {
                if (HUDBTHeaderFactory.getCmd((byte[])paramArrayOfbyte) == 1) {
                    boolean bool1 = bool;
                    if (!this.h) {
                        a(HUDBTHeaderFactory.getRequestID((byte[])paramArrayOfbyte));
                        bool1 = bool;
                    }
                    return bool1;
                }
                return false;
            }
        } catch (Exception exception) {
            return false;
        }
        return false;
    }

    private void waitForResponseComplete(HUDBTMessage btResponse) throws Exception {
        // Waiting for the incoming stream to complete a full HUDBTMessage (response)
        synchronized (btResponse) {
            btResponse.wait(MAX_WAIT_RESPONSE);
        }

        if (!btResponse.isComplete()) {
            HUDBTMessageCollectionManager.recycleIncompleteRequestID(btResponse);
            if(DEBUG){
                int headSize = (btResponse.getHeader() == null ? 0 : btResponse.getHeader().length);
                int payloadSize = (btResponse.getPayload() == null ? 0 : btResponse.getPayload().length);
                int bodySize = (btResponse.getBody() == null ? 0 : btResponse.getBody().length);
                Log.e(TAG,"Incomplete Messsage: HeaderSize:" + headSize + " PayloadSize:" + payloadSize + " BodySize:" + bodySize );
            }
            throw new Exception("HUDBTMessage came back with an incomplete response");
        }
    }

    private HUDHttpResponse getHttpResponse(HUDBTMessage btResponse) throws Exception {
        byte[] responseHeader = btResponse.getHeader();
        if (HUDBTHeaderFactory.getCode(responseHeader) == HUDBTHeaderFactory.CODE__ERROR) {
            throw new IOException("sendWebRequest web request has failed, no data");
        }

        if (DEBUG)
            Log.d(TAG, "Payload Length=" + HUDBTHeaderFactory.getPayloadLength(responseHeader) + " Body Length=" + HUDBTHeaderFactory.getBodyLength(responseHeader));

        HUDHttpResponse hudHTTPResponse = new HUDHttpResponse(btResponse.getPayload());

        if (HUDBTHeaderFactory.hasBody(btResponse.getHeader())) {
            hudHTTPResponse.setBody(btResponse.getBody());
        }

        return hudHTTPResponse;
    }

    private void streamData(byte[] header, byte[] payload, byte[] body) throws Exception {
        if (DEBUG) {
            if (header != null) {
                Log.d(TAG, "header size:" + header.length);
            } else {
                Log.d(TAG, "header is null");
            }

            if (payload != null) {
                Log.d(TAG, "payload size:" + payload.length);
            } else {
                Log.d(TAG, "payload is null");
            }

            if (body != null) {
                Log.d(TAG, "body size:" + body.length);
            } else {
                Log.d(TAG, "body is null");
            }

        }
        if (mHUDBTService == null) {
            throw new Exception("sendData: HUDBTService is null");
        }
        HUDBTBaseService.OutputStreamContainer osContainer = mHUDBTService.obtainOutputStreamContainer();

        if(osContainer == null) {
            throw new Exception(TAG + ":Couldn't obtain a new OutputSreamContainer");
        }

        try {
            if(header != null && header.length > 0) {
                // Stream the Transaction Header
                mHUDBTService.write(osContainer, header);
            }

            if(payload != null && payload.length > 0) {
                // Stream the Request
                mHUDBTService.write(osContainer, payload);
            }

            if(body != null && body.length > 0) {
                // Stream the body
                mHUDBTService.write(osContainer, body);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            mHUDBTService.releaseOutputStreamContainer(osContainer);
        }
    }

    private void streamHeader(byte[] header) throws Exception {
        streamData(header, null, null);
    }

    /**
     * Why using a paramByte, because there is a
     * method with byte array, but this a method with byte array uses this
     * @param b2
     */
    private void a(byte b2) {
        synchronized (this) {
            if (ihudbtService != null && !this.h && ihudbtService.getState() == IHUDConnectivity.ConnectionState.CONNECTED) {
                try {
                    synchronized (ihudbtService) {
                        sendHttpResponseToBT(HUDBTHeaderFactory.getUpdateNetworkHeaderResponse(mHasLocalWebConnection, b2), null, null);
                    }
                } catch (Exception e2) {
                }
            }
        }
    }

    public boolean hasLocalWebConnection() {
        return mHasLocalWebConnection;
    }

    public boolean hasRemoteWebConnection() {
        return mHasRemoteWebConnection;
    }

    public static boolean hasWebConnection() {
        return (mHasLocalWebConnection || mHasRemoteWebConnection);
    }

    //private synchronized void sendUpdateNetworkHeader(HUDBTService.TransactionType transactionType) {?
    private void sendUpdateNetworkHeader() {
        synchronized (this) {
            if (ihudbtService != null && !this.h && ihudbtService.getState() == IHUDConnectivity.ConnectionState.CONNECTED) {
                try {
                    synchronized (ihudbtService) {
                        sendHttpResponseToBT(HUDBTHeaderFactory.getUpdateNetworkHeaderOneWay(mHasLocalWebConnection), null, null);
                    }
                } catch (Exception e2) {
                }
            }
        }
    }

    /**
     * This is the sending function. Start analysis with this one
     * @param header
     * @param payload
     * @param body
     */
    private void sendHttpResponseToBT(byte[] header, byte[] payload, byte[] body) throws Exception {
        if (header != null) {
            StringBuilder sb = new StringBuilder("header size:").append(header.length);
        }
        if (payload != null) {
            StringBuilder sb = new StringBuilder("payload size:").append(payload.length);
        }
        if (body != null) {
            StringBuilder sb = new StringBuilder("body size:").append(body.length);
        }
        if (ihudbtService == null) {
            throw new Exception("sendData: HUDBTService is null");
        }

        //This is the point where sends data
        HUDBTBaseService.OutputStreamContainer outputStreamContainer=ihudbtService.obtainOutputStreamContainer();

        if (TAG == null) {
            throw new Exception(this.TAG + ":Couldn't obtain a new OutputSreamContainer");
        }

        try {
            if (header != null) {
                try {
                    if (header.length > 0) {
                        ihudbtService.write(outputStreamContainer, header);
                    }
                } catch (Exception e2) {
                    throw e2;
                }
            }
            if (payload != null && payload.length > 0) {
                ihudbtService.write(outputStreamContainer, payload);
            }
            if (body != null && body.length > 0) {
                ihudbtService.write(outputStreamContainer, body);
            }
            ihudbtService.releaseOutputStreamContainer(outputStreamContainer);
        } catch (Throwable th) {
            ihudbtService.releaseOutputStreamContainer(outputStreamContainer);
            throw th;
        }

    }

    /**
     * private boolean consumeApplicationWeb(byte[] header, byte[] payload, byte[] body) {?
     * @param bArr
     * @param bArr2
     * @param bArr3
     * @return
     */
    private boolean callHttp(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        boolean z;
        int i = 0;
        if (HUDBTHeaderFactory.getApplication(bArr) == APPLICATION__WEB) {
            try {
                HUDHttpRequest hUDHttpRequest = new HUDHttpRequest(bArr2);
                //!!!
                hUDHttpRequest.setBody(bArr3);
                if (HUDBTHeaderFactory.getApplication(bArr) == 3) {
                    URLConnectionHUDAdaptor.sendWebRequest(hUDHttpRequest);
                    z = true;
                } else {
                    byte b2 = HUDBTHeaderFactory.getRequestID(bArr);
                    HUDHttpResponse response = URLConnectionHUDAdaptor.sendWebRequest(hUDHttpRequest);
                    z = true;
                    if (!this.h) {
                        z = true;
                        if (ihudbtService != null) {
                            byte[] bytes = response.toString().getBytes();
                            int length = bytes.length;
                            if (response.hasBody()) {
                                //!!!
                                i = response.getBody().length;
                            }
                            byte[] a3 = HUDBTHeaderFactory.getInternetResponseHeader(length, i);
                            HUDBTHeaderFactory.setRequestID(a3, b2);
                            sendHttpResponseToBT(a3, bytes, response.getBody());
                            z = true;
                        }
                    }
                }
            } catch (Exception e2) {
                z = true;
                if (HUDBTHeaderFactory.getApplication(bArr) == APPLICATION__WEB) {
                    z = true;
                    if (ihudbtService != null) {
                        try {
                            sendHttpResponseToBT(HUDBTHeaderFactory.getErrorHeader(), null, null);
                            z = true;
                        } catch (Exception e3) {
                            z = true;
                        }
                    }
                }
            }
        } else {
            z = true;
            if (HUDBTHeaderFactory.getApplication(bArr) != APPLICATION__PHONE) {
                z = false;
            }
        }
        return z;

    }

    /**
     * New function
     * @param hUDHttpRequest
     * @return
     */
    public boolean callHttp(HUDHttpRequest hUDHttpRequest, byte[] msgId) {
        boolean z;
        int i = 0;
        //if (HUDBTHeaderFactory.c(bArr) == 2) {
        try {
            Log.i(this.getClass().getName(), "callHttp enter");
            byte[] header = new byte[2];
            header[1] = 0;
            byte b2 = HUDBTHeaderFactory.getRequestID(header);
            msgId[0] = b2;

            //HUDHttpResponse a2 = URLConnectionHUDAdaptor.a(hUDHttpRequest);
            z = true;
            if (!this.h) {
                z = true;
                if (ihudbtService != null) {
                    byte[] bytes = hUDHttpRequest.toString().getBytes();
                    int length = bytes.length;
                    if (hUDHttpRequest.hasBody()) {
                        i = hUDHttpRequest.getBody().length;
                    }
                    byte[] a3 = HUDBTHeaderFactory.getInternetResponseHeader(length, i);
                    HUDBTHeaderFactory.setRequestID(a3, b2);

                    Log.i(this.getClass().getName(), "Header created");
                    //sendHttpResponseToBT(a3, bytes, a2.b);
                    //sendHttpResponseToBT(null, bytes, null);
                    sendHttpResponseToBT(a3, bytes, null);
                    z = true;
                }
            }
            //}
        } catch (Exception e2) {
            z = true;
            //if (HUDBTHeaderFactory.c(bArr) == 2) {
            z = true;
            if (ihudbtService != null) {
                try {
                    sendHttpResponseToBT(HUDBTHeaderFactory.getErrorHeader(), null, null);
                    z = true;
                } catch (Exception e3) {
                    z = true;
                }
            }
            //}
        }
        //}
        //} else {
        //    z = true;
        //    if (HUDBTHeaderFactory.c(bArr) != 1) {
        //        z = false;
        //    }
        //}
        return z;

    }

    public final void updateLocalWebConnection(Context paramContext) {
        boolean bool1;
        boolean bool = mHasLocalWebConnection;

        NetworkInfo networkInfo = ((ConnectivityManager)paramContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            bool1 = true;
        } else {
            bool1 = false;
        }
        mHasLocalWebConnection = bool1;
        if (bool != mHasLocalWebConnection && e != null) {
            IHUDConnectivity.NetworkEvent networkEvent;
            IHUDConnectivity iHUDConnectivity = e;
            if (mHasLocalWebConnection) {
                networkEvent = IHUDConnectivity.NetworkEvent.LOCAL_WEB_GAINED;
            } else {
                networkEvent = IHUDConnectivity.NetworkEvent.LOCAL_WEB_LOST;
            }

            iHUDConnectivity.onNetworkEvent(networkEvent, hasWebConnection());
            sendUpdateNetworkHeader();
        }
    }

    /**
     * This is the public function that is calling callHttp
     * @param paramArrayOfbyte1 header
     * @param paramArrayOfbyte2 payload
     * @param paramArrayOfbyte3 body
     * @return
     */
    public final boolean sendHttpRequest(byte[] paramArrayOfbyte1, byte[] paramArrayOfbyte2, byte[] paramArrayOfbyte3) {
        boolean bool1 = false;
        if (paramArrayOfbyte1 == null)
            return bool1;
        if (HUDBTHeaderFactory.getCmd(paramArrayOfbyte1) == 3)
            return sendByteArr(paramArrayOfbyte1);
        boolean bool2 = bool1;
        if (HUDBTHeaderFactory.getCmd(paramArrayOfbyte1) == 2) {
            bool2 = bool1;
            if (paramArrayOfbyte2.length > 0)
                bool2 = callHttp(paramArrayOfbyte1, paramArrayOfbyte2, paramArrayOfbyte3);
        }
        return bool2;
    }

    /*
    private boolean consumeApplicationCMD(byte[] header, byte[] payload, byte[] body) {
        try {
            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) {
                if (HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                    if (mIsHUD) {
                        setRemoteWebConnection(HUDBTHeaderFactory.getArg1(header) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
                    } else {
                        Log.w(TAG, "Non-HUD received an Update Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }

            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__RESPONSE) {
                if (HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__CHECK_REMOTE_NETWORK) {
                    if (!mIsHUD) {
                        sendUpdateNetworkHeader(TransactionType.RESPONSE);
                    } else {
                        Log.w(TAG, "HUD has been requested to send a Check Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "updateRemoteWebConnection failed", e);
            return false;
        }

        return false;
    }
    */

    /**
     * From the reconsdk lib
     * @param header
     * @param payload
     * @param body
     * @return
     */
    @Override
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body) {
        if (header == null) {
            Log.e(TAG, "consumeBTData: Response is null");
            return false;
        }

        if (HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__CMD) {
            //return consumeApplicationCMD(header, payload, body);
            return callHttp(header, payload, body);
        }

        if ((HUDBTHeaderFactory.getApplication(header) == APPLICATION__WEB) && (payload.length > 0)) {
            //return consumeApplicationWeb(header, payload, body);
            return callHttp(header, payload, body);
        }

        return false;
    }

    /**
     * REcom3 original implementation: obsolete
     * Function to send a web request via HUDBTService
     * @param request
     * @return
     * @throws Exception
     */
    /*
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {

        int i = 0;

        // Should only be called from inside the HUD
        if (!mIsHUD) {
            Log.e(TAG, "sendWebRequest: Phone should not try to access the network through the HUD");
            return null;
        }

        synchronized (mHUDBTService) {
            byte[] data = request.getByteArray();
            byte[] header = HUDBTHeaderFactory.getInternetRequestHeader(request.getDoInput(), data.length, request.hasBody() ? request.getBody().length : 0);

            boolean testCode = true;
            if(testCode) {
                byte[] headerTest = new byte[2];
                headerTest[1] = 0;
                byte b2 = HUDBTHeaderFactory.getRequestID(headerTest);
                //msgId[0] = b2;

                data = request.toString().getBytes();
                int length = data.length;
                if (request.hasBody()) {
                    i = request.getBody().length;
                }
                byte[] a3 = HUDBTHeaderFactory.getInternetResponseHeader(length, i);
                HUDBTHeaderFactory.setRequestID(a3, b2);

                header = a3;
            }

            Log.i(this.getClass().getName(), "Header created");

            // Stream the Transaction Header
            try {
                mHUDBTService.write(header, HUDSPPService.TransactionType.REQUEST);
            } catch (IOException e) {
                Log.e(TAG, "sendWebRequest: updateRemoteWebConnection: Couldn't writeHeader", e);
                return null;
            }

            // Stream the Request
            mHUDBTService.write(data, HUDSPPService.TransactionType.REQUEST);

            // Stream the body
            if (request.hasBody()) {
                mHUDBTService.write(request.getBody(), HUDSPPService.TransactionType.REQUEST);
            }

            // Do we expect a response?
            if (!request.getDoInput()) {
                return null;
            }

            header = mHUDBTService.read(HUDBTHeaderFactory.HEADER_LENGTH);

            if (header.length != HUDBTHeaderFactory.HEADER_LENGTH) {
                throw new IOException("sendWebRequest Header expected to have " + HUDBTHeaderFactory.HEADER_LENGTH + " bytes, but got " + header.length + " bytes");
            }

            if (HUDBTHeaderFactory.getCode(header) == HUDBTHeaderFactory.CODE__ERROR) {
                throw new IOException("sendWebRequest web request has failed, no data");
            }

            //if (DEBUG)
            //    Log.i(TAG, "Payload Length=" + HUDBTHeaderFactory.getPayloadLength(header) + " Body Length=" + HUDBTHeaderFactory.getBodyLength(header));

            int lenPayload = HUDBTHeaderFactory.getPayloadLength(header);
            data = mHUDBTService.read(lenPayload);
            HUDHttpResponse hudHTTPResponse = new HUDHttpResponse(data);

            if (HUDBTHeaderFactory.hasBody(header)) {
                data = mHUDBTService.read(HUDBTHeaderFactory.getBodyLength(header));
                if (data.length != HUDBTHeaderFactory.getBodyLength(header)) {
                    Log.e(TAG, "Read " + data.length + " expected " + HUDBTHeaderFactory.getBodyLength(header) + " bytes");
                }
                hudHTTPResponse.setBody(data);
                data = null;
            }

            return hudHTTPResponse;
        }

        //return null;
    }
    */

    private HUDHttpResponse __sendWebRequest(HUDHttpRequest request) throws Exception {
        if(mHUDBTService == null) {
            Log.e(TAG, "__sendWebRequest: HUDBTService is null");
            return null;
        }

        boolean hasResponse = request.getDoInput();

        byte[] payload = request.getByteArray();
        byte[] header = HUDBTHeaderFactory.getInternetRequestHeader(hasResponse, payload.length, request.hasBody() ? request.getBody().length : 0);

        HUDBTMessage btResponse = null;
        if(hasResponse) {
            btResponse = HUDBTMessageCollectionManager.createPendingBTResponse(header);
        }

        streamData(header, payload, request.getBody());

        // Do we expect a response?
        if(!hasResponse || (btResponse == null)) {
            return null;
        }

        waitForResponseComplete(btResponse);

        return getHttpResponse(btResponse);
    }

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {
        // Should only be called from inside the HUD
        if(!mIsHUD) {
            Log.e(TAG, "sendWebRequest: Phone should not try to access the network through the HUD");
            return null;
        }

        if(mHUDBTService == null) {
            Log.e(TAG, "sendWebRequest: HUDBTService is null");
            return null;
        }

        HUDHttpResponse hudHTTPResponse;
        try {
            hudHTTPResponse = __sendWebRequest(request);
        } catch (Exception e) {
            throw e;
        }

        return hudHTTPResponse;
    }

    public void sendWebResponse(HUDHttpResponse response, byte requestId) throws Exception {
        // Should only be called from inside the HUD
        if(mIsHUD) {
            Log.e(TAG, "sendWebResponse: Phone should not try to access the network through the HUD");
            return;
        }

        if(mHUDBTService == null) {
            Log.e(TAG, "sendWebResponse: HUDBTService is null");
            return;
        }

        byte[] data = response.getByteArray();
        byte[] header = HUDBTHeaderFactory.getInternetResponseHeader(data.length, response.hasBody() ? response.getBody().length : 0);
        HUDBTHeaderFactory.setRequestID(header, requestId);
        streamData(header, data, response.getBody());

        return;
    }

    public void setBTService(IHUDBTService hudBTService) {
        mHUDBTService = hudBTService;
        mHUDBTService.addConsumer(this);
    }

    /**
     * In order to check for remote web connection, we will send a "Check Network Header" to the phone/tablet<br>
     * We will read a response right away and check for "Has Network Code"<br>
     * We will call {@link #setRemoteWebConnection(boolean) setRemoteWebConnection} to update the local variable and any other client
     */
    /*
    public void updateRemoteWebConnection() {
        if (mHUDBTService.getState() != ConnectionState.CONNECTED) {
            setRemoteWebConnection(false);
            return;
        }

        // Should only be called from inside the HUD
        if (!mIsHUD) {
            Log.e(TAG, "No Need to check remote web connection on non HUD device");
            return;
        }

        synchronized (mHUDBTService) {
            byte[] responseHeader = null;

            try {
                responseHeader = mHUDBTService.writeHeader(HUDBTHeaderFactory.getCheckNetworkHeader(), TransactionType.REQUEST);
            } catch (IOException e) {
                Log.e(TAG, "updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }

            try {
                if (responseHeader == null) {
                    Log.e(TAG, "updateRemoteWebConnection: Response is null");
                    return;
                }

                if (HUDBTHeaderFactory.getRequestHeaderType(responseHeader) != HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) {
                    Log.e(TAG, "updateRemoteWebConnection: Expected a oneway response, got " + HUDBTHeaderFactory.getRequestHeaderType(responseHeader));
                    return;
                }

                if (HUDBTHeaderFactory.getCmd(responseHeader) != HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                    Log.e(TAG, "updateRemoteWebConnection: Expected Check Network Resonse, got " + HUDBTHeaderFactory.getCmd(responseHeader));
                    return;
                }

                setRemoteWebConnection(HUDBTHeaderFactory.getArg1(responseHeader) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
            } catch (Exception e) {
                Log.e(TAG, "updateRemoteWebConnection failed", e);
                return;
            }
        }
    }
    */
}

