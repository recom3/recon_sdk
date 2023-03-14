package com.reconinstruments.os.connectivity.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.reconinstruments.os.connectivity.IHUDConnectivity;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class HUDBTBaseService implements IHUDBTService {

    public class OutputStreamContainer {

        /* renamed from: a  reason: collision with root package name */
        protected OutputStream f2730a;
        private int c = -1;
        private int d = -1;

        public OutputStreamContainer(OutputStream outputStream) {
            this.f2730a = outputStream;
        }

        public final OutputStream a() {
            return this.f2730a;
        }
    }
}
