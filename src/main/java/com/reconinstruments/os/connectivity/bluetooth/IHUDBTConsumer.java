package com.reconinstruments.os.connectivity.bluetooth;

/**
 * Created by Recom3 on 14/03/2023.
 */

public interface IHUDBTConsumer {
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body);
}
