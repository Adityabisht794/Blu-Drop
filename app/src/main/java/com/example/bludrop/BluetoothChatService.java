package com.example.bludrop;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.example.bludrop.MainActivity.MESSAGE_READ;
import static com.example.bludrop.MainActivity.MESSAGE_STATUS;

import androidx.annotation.RequiresPermission;

public class BluetoothChatService {

    private static final String TAG = "BluetoothChatService";
    private static final String APP_NAME = "BluDropChat";

    // Same UUID on both devices
    private static final UUID APP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter adapter;
    private final Handler handler;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public BluetoothChatService(BluetoothAdapter adapter, Handler handler) {
        this.adapter = adapter;
        this.handler = handler;
    }

    // Server: wait for incoming connections
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public synchronized void startServer() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        acceptThread = new AcceptThread();
        acceptThread.start();

        sendStatus("Status: Listening for connections...");
    }

    // Client: connect to selected device
    public synchronized void connectTo(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    // Called when connection is established
    private synchronized void manageConnectedSocket(BluetoothSocket socket, BluetoothDevice device) {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // 🔐 Safely get device name (Android 12+ BLUETOOTH_CONNECT permission issue solve)
        String name = safeDeviceName(device);
        sendStatus("Status: Connected to " + name);
    }

    // Helper: safely read device name without crash if permission missing
    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();   // may throw SecurityException on Android 12+

            if (name == null || name.isEmpty()) {
                return "Unknown device";
            }

            // agar hamara app-user hai to prefix hata do
            if (name.startsWith("BLUDROP_")) {
                name = name.replace("BLUDROP_", "");
            }

            return name;

        } catch (SecurityException e) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, using fallback name");
            return "Unknown device";
        }
    }


    public void sendMessage(byte[] data) {
        ConnectedThread ct;
        synchronized (this) {
            ct = connectedThread;
        }
        if (ct != null) {
            ct.write(data);
        } else {
            sendStatus("Status: Not connected to any device");
        }
    }

    private void sendStatus(String text) {
        Message msg = handler.obtainMessage(MESSAGE_STATUS, -1, -1, text);
        handler.sendMessage(msg);
    }

    // ----------------- AcceptThread (Server) -----------------

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: listen() failed", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;

            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed", e);
                    break;
                }

                if (socket != null) {
                    BluetoothDevice device = socket.getRemoteDevice();
                    manageConnectedSocket(socket, device);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close server socket", e);
                    }
                    // ek connection ke baad break (disconnect hone par ConnectedThread dobara startServer() karega)
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread cancel: close() failed", e);
            }
        }
    }

    // ----------------- ConnectThread (Client) -----------------

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: create() failed", e);
            }
            socket = tmp;
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        public void run() {

            // discovery cancel with safety
            try {
                adapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "Missing BLUETOOTH_SCAN permission for cancelDiscovery");
            }

            try {
                socket.connect();

                // ⭐ Small delay: Android 13/14 me kabhi kabhi immediately data read/write se issue aata hai
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }

            } catch (IOException connectException) {
                Log.e(TAG, "ConnectThread: connect() failed", connectException);
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "ConnectThread: could not close socket", closeException);
                }
                sendStatus("Status: Connection failed");
                return;
            }

            manageConnectedSocket(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread cancel: close() failed", e);
            }
        }
    }

    // ----------------- ConnectedThread (Data transfer) -----------------

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: temp sockets not created", e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer.clone())
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread: disconnected", e);
                    sendStatus("Status: Disconnected");

                    // ⭐⭐ IMPORTANT: disconnect ke baad phir se server mode me chale jao
                    if (adapter != null && adapter.isEnabled()) {
                        try {
                            startServer();
                        } catch (SecurityException ex) {
                            Log.e(TAG, "No permission to restart server", ex);
                        }
                    }

                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Exception during write", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread cancel: close() failed", e);
            }
        }
    }
}
