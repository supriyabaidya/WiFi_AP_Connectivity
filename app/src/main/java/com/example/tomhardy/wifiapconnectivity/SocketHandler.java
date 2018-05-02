package com.example.tomhardy.wifiapconnectivity;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by TomHardy on 18-02-2018.
 */

public class SocketHandler {

    private static Socket socket;

    public static synchronized Socket getSocket()
    {
        return socket;
    }

    public static synchronized void setSocket(Socket socket)
    {
        SocketHandler.socket = socket;
    }

    public static synchronized void close()
    {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}