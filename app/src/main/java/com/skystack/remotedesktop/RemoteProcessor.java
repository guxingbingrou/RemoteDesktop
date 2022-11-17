package com.skystack.remotedesktop;

import java.nio.ByteBuffer;

/**
 * Time: 2022/11/17
 * Author: zou
 * Description:
 */
public interface RemoteProcessor {
    void ProcessData(byte[] data);
}
