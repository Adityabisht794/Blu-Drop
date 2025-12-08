package com.example.bludrop;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI components
    private TextView tvStatus, tvChatLog;
    private Button btnEnableBluetooth, btnScanConnect, btnSend;
    private EditText etMessage;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothChatService chatService;

    // Handler message types
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_STATUS = 2;

    // Permissions
    private static final int REQ_BT_PERMISSIONS = 101;
    private static final String[] BT_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // Enable BT dialog
    private static final int REQ_ENABLE_BT = 201;

    // Nearby devices scan data
    private final List<BluetoothDevice> nearbyDevices = new ArrayList<>();
    private final List<String> nearbyDeviceNames = new ArrayList<>();
    private boolean isReceiverRegistered = false;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI binding
        tvStatus = findViewById(R.id.tvStatus);
        tvChatLog = findViewById(R.id.tvChatLog);
        btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        btnScanConnect = findViewById(R.id.btnScanConnect);
        btnSend = findViewById(R.id.btnSend);
        etMessage = findViewById(R.id.etMessage);

        // chat log clean start
        tvChatLog.setText("");

        // Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Handler: Bluetooth thread → UI update
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        tvChatLog.append("\nFriend: " + readMessage);
                        break;

                    case MESSAGE_STATUS:
                        String status = (String) msg.obj;
                        tvStatus.setText(status);
                        break;
                }
            }
        };

        // Chat service
        chatService = new BluetoothChatService(bluetoothAdapter, handler);

        // Pehle permissions check karo
        requestBluetoothPermissionsIfNeeded();

        updateBluetoothStatus();

        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());

        // "Nearby scan" button
        btnScanConnect.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Turn ON Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hasBtPermissions()) {
                requestBluetoothPermissionsIfNeeded();
                return;
            }
            startDiscoveryForNearbyDevices();
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // discovery receiver unregister karo
        if (isReceiverRegistered) {
            unregisterReceiver(discoveryReceiver);
            isReceiverRegistered = false;
        }
    }

    // ---------- PERMISSIONS HELPERS ----------

    private boolean hasBtPermissions() {
        // Android 12+ : BT + location runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String perm : BT_PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(this, perm)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        } else {
            // Android 6–11 : scan ke liye location permission
            return ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissionsIfNeeded() {
        if (!hasBtPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                        this,
                        BT_PERMISSIONS,
                        REQ_BT_PERMISSIONS
                );
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQ_BT_PERMISSIONS
                );
            }
        } else {
            startServerIfPossible();
        }
    }

    private void startServerIfPossible() {
        if (!bluetoothAdapter.isEnabled()) return;

        try {
            chatService.startServer();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_BT_PERMISSIONS) {
            if (hasBtPermissions()) {
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
                updateBluetoothStatus();
            } else {
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- UI + BLUETOOTH LOGIC ----------

    private void updateBluetoothStatus() {
        if (bluetoothAdapter.isEnabled()) {
            tvStatus.setText("Status: Bluetooth ON (Waiting / Connected)");
            if (hasBtPermissions()) {
                // Apne app users ke liye Bluetooth name set karo
                updateBluetoothNameForAppUser();
                startServerIfPossible();
            }
        } else {
            tvStatus.setText("Status: Bluetooth OFF");
        }
    }

    // Apne app ke users ka naam: BLUDROP_<UserName>
    private void updateBluetoothNameForAppUser() {
        String userName = getSharedPreferences("BluDrop", MODE_PRIVATE)
                .getString("user_name", "User");

        String targetName = "BLUDROP_" + userName;

        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                // Android 12+ pe permission check
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothAdapter.setName(targetName);
            }
        } catch (SecurityException e) {
            // ignore / log
        }
    }

    // ⭐ Official way: system dialog for enabling BT
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth already ON", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBtPermissions() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissionsIfNeeded();
            return;
        }

        try {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to request Bluetooth enable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                updateBluetoothStatus();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- NEARBY DISCOVERY LOGIC ----------

    private void startDiscoveryForNearbyDevices() {
        if (!hasBtPermissions()) {
            Toast.makeText(this, "Bluetooth permissions missing", Toast.LENGTH_SHORT).show();
            return;
        }

        nearbyDevices.clear();
        nearbyDeviceNames.clear();

        try {
            bluetoothAdapter.cancelDiscovery();
            boolean started = bluetoothAdapter.startDiscovery();
            if (started) {
                tvStatus.setText("Status: Scanning for nearby devices...");
                Toast.makeText(this, "Scanning nearby devices...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to start discovery", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Missing BLUETOOTH_SCAN permission", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(discoveryReceiver, filter);
            isReceiverRegistered = true;
        }
    }

    // ye receiver har nearby device ko capture karega
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null) {
                    String name;
                    try {
                        name = device.getName();
                    } catch (SecurityException e) {
                        return;
                    }

                    if (name == null || name.isEmpty()) {
                        return;
                    }

                    // Only show devices running this app: BLUDROP_<Name>
                    if (!name.startsWith("BLUDROP_")) {
                        return;
                    }

                    String cleanName = name.replace("BLUDROP_", "");
                    String address = device.getAddress();
                    String label = cleanName + "\n" + address;

                    if (!nearbyDeviceNames.contains(label)) {
                        nearbyDevices.add(device);
                        nearbyDeviceNames.add(label);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                tvStatus.setText("Status: Scan complete");

                if (nearbyDevices.isEmpty()) {
                    Toast.makeText(context, "No nearby BluDrop users found", Toast.LENGTH_SHORT).show();
                } else {
                    showNearbyDevicesBottomSheet();
                }
            }
        }
    };

    // ---------- SNAPCHAT STYLE BOTTOM SHEET ----------

    private void showNearbyDevicesBottomSheet() {
        if (nearbyDevices.isEmpty()) {
            Toast.makeText(this, "No nearby BluDrop users found", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_nearby_devices, null);

        TextView tvSelfName = view.findViewById(R.id.tvSelfName);
        ImageView imgSelfAvatar = view.findViewById(R.id.imgSelfAvatar);
        ListView listView = view.findViewById(R.id.listNearbyDevices);

        // apna naam
        String userName = getSharedPreferences("BluDrop", MODE_PRIVATE)
                .getString("user_name", "You");
        tvSelfName.setText(userName);

        // 🎯 avatar image resource (NameActivity me save kiya tha)
        int avatarRes = getSharedPreferences("BluDrop", MODE_PRIVATE)
                .getInt("user_avatar_res", R.drawable.avatar1); // default avatar1

        imgSelfAvatar.setImageResource(avatarRes);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nearbyDeviceNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            BluetoothDevice selectedDevice = nearbyDevices.get(position);
            tvStatus.setText("Status: Connecting to " + selectedDevice.getAddress() + "...");
            chatService.connectTo(selectedDevice);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }


    // ---------- MESSAGE SEND ----------

    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();

        if (msg.isEmpty()) {
            Toast.makeText(this, "Enter message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth OFF", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBtPermissions()) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show();
            requestBluetoothPermissionsIfNeeded();
            return;
        }

        chatService.sendMessage(msg.getBytes());
        tvChatLog.append("\nMe: " + msg);
        etMessage.setText("");
    }
}
