package com.mch.helloble;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final long SCAN_PERIOD = 30 * 1000;

    public ArrayList<BluetoothDevice> mList = new ArrayList<>();

    private RecyclerView.Adapter mAdapter;

    private OnListInteractionListener mListener = new OnListInteractionListener() {
        @Override
        public void onListInteraction(BluetoothDevice device) {
            Log.d(TAG, "onListInteraction: " + device.getAddress());

            startActivity(DeviceActivity.newIntent(getApplicationContext(), device));
        }
    };

    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mScanning;

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult: " + callbackType + " " + result);

            BluetoothDevice device = result.getDevice();
            if (!mList.contains(device)) {
                mList.add(result.getDevice());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: " + results.size());
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed: " + errorCode);
        }
    };

    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Scanning", ((int) SCAN_PERIOD)).show();
                scanLeDevice(true);
            }
        });

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scanLeDevice(true);
            }
        });

        mAdapter = new MyItemRecyclerViewAdapter(mList, mListener);

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(mAdapter);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothLeScanner = bluetoothManager.getAdapter().getBluetoothLeScanner();

        scanLeDevice(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mSwipeRefreshLayout.setRefreshing(true);
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mSwipeRefreshLayout.setRefreshing(false);
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    public interface OnListInteractionListener {
        void onListInteraction(BluetoothDevice device);
    }

}
