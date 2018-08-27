package com.mch.helloble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {

    public static final String TAG = "DeviceActivity";

    public static final String ARG_DEVICE = "device";

    public static Intent newIntent(Context context, BluetoothDevice device) {
        return new Intent(context, DeviceActivity.class).putExtra(ARG_DEVICE, device);
    }

    public static final String ACTION_GATT_CONNECTED = "com.mch.helloble.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.mch.helloble.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.mch.helloble.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.mch.helloble.ACTION_DATA_AVAILABLE";
    
    public static final String EXTRA_DATA = "com.mch.helloble.EXTRA_DATA";
    public static final String EXTRA_TEMP = "com.mch.helloble.EXTRA_TEMP";
    public static final String EXTRA_HUMILITY = "com.mch.helloble.EXTRA_HUMILITY";
    public static final String EXTRA_AIR = "com.mch.helloble.EXTRA_AIR";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final String LIST_SERVICE = "service";
    private static final String LIST_CHARACTERISTIC = "characteristic";

    private static final UUID UUID_COMBINE_DATA_CHAR = UUID.fromString("3BD91530-EC56-9CF3-B2DF-F2E239D01013");

    public static final UUID UUID_HEART_RATE_MEASUREMENT = UUID.randomUUID();

    private BluetoothDevice mDevice;

    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    private ExpandableListView mExpandableListView;

    private SimpleExpandableListAdapter mAdapter;

    ArrayList<HashMap<String, String>> mGroupData = new ArrayList<>();

    ArrayList<ArrayList<HashMap<String, String>>> mChildData = new ArrayList<>();

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead: " + characteristic.getUuid() + " status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private boolean mConnected;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "onReceive: " + action);
                mConnected = true;
                Snackbar.make(findViewById(R.id.expandable_list_view), "connected", Snackbar.LENGTH_LONG).show();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "onReceive: " + action);
                mConnected = false;
                Snackbar.make(findViewById(R.id.expandable_list_view), "disconnected", Snackbar.LENGTH_LONG).show();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "onReceive: " + action);
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothGatt.getServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: " + action);
                displayData(
                        intent.getDoubleExtra(EXTRA_TEMP, -1),
                        intent.getIntExtra(EXTRA_HUMILITY, -1),
                        intent.getIntExtra(EXTRA_AIR, -1)
                );
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mDevice = getIntent().getParcelableExtra(ARG_DEVICE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "connectGatt", Snackbar.LENGTH_LONG).show();
                mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mGattCallback);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(mDevice.getName());

        mAdapter = new SimpleExpandableListAdapter(getApplicationContext(),
                mGroupData,
                R.layout.service_item,
                new String[]{LIST_SERVICE},
                new int[]{R.id.service},
                mChildData,
                R.layout.characteristic_item,
                new String[]{LIST_CHARACTERISTIC},
                new int[]{R.id.characteristic}
        );

        mExpandableListView = findViewById(R.id.expandable_list_view);
        mExpandableListView.setAdapter(mAdapter);
        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d(TAG, "onChildClick: " + groupPosition + " " + childPosition);

                BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);

                Log.d(TAG, "onChildClick: read" + characteristic.getUuid());

                mBluetoothGatt.readCharacteristic(characteristic);

                mBluetoothGatt.setCharacteristicNotification(characteristic, true);

                return true;
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        registerReceiver(mGattUpdateReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mBluetoothGatt = mDevice.connectGatt(getApplicationContext(), false, mGattCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();

        close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mGattUpdateReceiver);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (UUID_COMBINE_DATA_CHAR.equals(characteristic.getUuid())) {
            double temperature = extractCombinedTemperature(characteristic);
            int humidity = extractCombinedHumidity(characteristic);
            int airQuality = extractCombinedAirQuality(characteristic);
            intent.putExtra(EXTRA_TEMP, temperature);
            intent.putExtra(EXTRA_HUMILITY, humidity);
            intent.putExtra(EXTRA_AIR, airQuality);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        mGroupData.clear();
        mChildData.clear();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            Log.d(TAG, "displayGattServices: gattService = " + gattService.getUuid());

            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put(LIST_SERVICE, uuid);

            gattServiceData.add(currentServiceData);
            mGroupData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                Log.d(TAG, "displayGattServices: gattCharacteristic = " + gattCharacteristic.getUuid());

                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();

                if (gattCharacteristic.getUuid().equals(UUID_COMBINE_DATA_CHAR)) {
                    uuid += " ! ";
                }

                currentCharaData.put(LIST_CHARACTERISTIC, uuid);

                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
            mChildData.add(gattCharacteristicGroupData);
        }

        mAdapter.notifyDataSetChanged();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public static double extractCombinedTemperature(BluetoothGattCharacteristic c) {
        double temp = shortUnsignedAtOffset(c, 0);
        temp = (-27315f + temp) / 100f;
        return temp;
    }

    public static int extractCombinedHumidity(BluetoothGattCharacteristic c) {
        int humidity = shortUnsignedAtOffset(c, 2);
        humidity = humidity / 100;
        return humidity;
    }

    public static int extractCombinedAirQuality(BluetoothGattCharacteristic c) {
        return shortUnsignedAtOffset(c, 4);
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }
 
    private void displayData(double temperature, int humidity, int airQuality) {
        Log.d(TAG, "displayGattServices: Hello Sensor!!");

        Log.d(TAG, "displayGattServices: T " + temperature);
        Log.d(TAG, "displayGattServices: H " + humidity);
        Log.d(TAG, "displayGattServices: A " + airQuality);

        TextView textView = findViewById(R.id.temperature);
        textView.setText("Temperature: \n" + temperature);
        textView = findViewById(R.id.humidity);
        textView.setText("Humility: \n" + humidity);
        textView = findViewById(R.id.air_quality);
        textView.setText("Air Quality: \n" + airQuality);
    }
}
