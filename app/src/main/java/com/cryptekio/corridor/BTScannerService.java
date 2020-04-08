package com.cryptekio.corridor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.cryptekio.corridor.entity.BTPeripheral;
import com.cryptekio.corridor.network.NetworkMethods;
import com.cryptekio.corridor.network.NetworkMethodsListener;

import org.json.JSONException;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class BTScannerService extends Service {

    private static final String  TAG = "BTScannerService";
    private static final String OUR_MAINSERVICE_UUID = "b4250400-fb4b-4746-b2b0-93f0e61122c6";
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanFilter scanFilterByServuceUuid;
    private ScanFilter scanFilterByApple;
    private int rssiDropThreshold;
    private int uploadReq;

    private HashMap<String, BTPeripheral> devicesMap = new HashMap<>();

    private  ScanSettings settings = new ScanSettings.Builder()
            .setScanMode( ScanSettings.SCAN_MODE_LOW_POWER)//SCAN_MODE_BALANCED )
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build();
    private Handler uploadDataHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand");
        initSettings();
        uploadDataWithDelay(uploadReq);
        return START_NOT_STICKY;
    }

    private void initSettings(){

       this.uploadReq =  DataManager.getDataUploadFreq(getApplicationContext());
       this.rssiDropThreshold = DataManager.getRSSIDropThreshold(getApplicationContext());

    }

    private void uploadDataWithDelay(int secDelay){

        if(uploadDataHandler != null){
            uploadDataHandler.removeCallbacksAndMessages(null);
        }
        uploadDataHandler = new Handler();
        uploadDataHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                uploadData();

                for (String key:devicesMap.keySet() ) {
                    BTPeripheral peripheral = devicesMap.get(key);
                    peripheral.clearRSSIValues();
                }
                uploadDataWithDelay(uploadReq);
            }

        }, secDelay * 1000);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        initSettings();
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        scanFilterByServuceUuid = new ScanFilter.Builder()
                .setServiceUuid( new ParcelUuid(UUID.fromString(OUR_MAINSERVICE_UUID ) ) )
                .build();

        //0x004C(76) -  manufacturer code(Apple Inc)
        scanFilterByApple = new ScanFilter.Builder().setManufacturerData(0x004C,null,null).build();
        if( BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported() ) {
            startAdvertise();
        }

        String model = Build.MODEL;
        String deviceName  = "1"+ model.charAt(model.length() - 1) ;
        BluetoothAdapter.getDefaultAdapter().setName(deviceName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(1, new Notification());
        }

        startScan();
    }

    void startScan(){

        Log.d(TAG, "start scan" );
        if(bluetoothLeScanner != null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(this.scanFilterByApple);
            filters.add(this.scanFilterByServuceUuid);
            bluetoothLeScanner.startScan(filters, settings, mScanCallback);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, 10000);
        }else {
            stopThisService();
        }
    }

    void stopScan(){
        Log.d(TAG, "stopScan");

        try {

            if (bluetoothLeScanner != null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {

                bluetoothLeScanner.stopScan(mScanCallback);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startScan();
                    }
                }, 5000);
            } else {
                stopThisService();
            }

        }catch (IllegalStateException e){
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = getApplicationContext().getPackageName();
        String channelName = "background_sevice";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    void stopThisService(){
        Log.d(TAG, "stopThisService");
        stopForeground(true);
        stopSelf();
    }

    void  handleScanResult(ScanResult result){
        SparseArray<byte[]> manData = null;
        if( result == null || result.getDevice() == null || result.getRssi() < rssiDropThreshold)
            return;

        SparseArray<byte[]> manufacturerData =  result.getScanRecord().getManufacturerSpecificData();
        int manKey=   manufacturerData.keyAt(0);
        List<ParcelUuid> uuids =  result.getScanRecord().getServiceUuids();


        if(uuids != null && !uuids.isEmpty()){

            if(uuids.get(0).toString().equalsIgnoreCase(OUR_MAINSERVICE_UUID)){
                handleDevice(false, result);
                manData =  result.getScanRecord().getManufacturerSpecificData();
            }

        }else if(manKey == 0x004C){
            //0x004C(76) -  manufacturer code(Apple Inc)
            handleDevice(false, result);
        }

        if(manData != null){
            printManData(manData);
        }
    }

    void printManData( SparseArray<byte[]> manData){
        for (int i = 0; i<manData.size(); i++){
            byte[] arr = manData.valueAt(i);
            Log.d(TAG,  byteArrayToHex(arr));
        }
    }

    void handleDevice(Boolean byManufacturer, ScanResult result){

        Log.d(TAG,"handleDevice");

        String address = result.getDevice().getAddress();
        BTPeripheral device = devicesMap.get(address);
        if(device != null ){
            if( device.isOurDevice()) {

                device.addRSSIValue(result.getRssi());
                Log.d(TAG, "RSSI  = " + result.getRssi());
                Log.d(TAG, "sender = " + device.getUserId());

            }

        }else{
            BTPeripheral peripheral = new BTPeripheral(address);
            devicesMap.put(address,peripheral);
            Log.d(TAG, "try to connect");
            result.getDevice().connectGatt(getBaseContext(), false, new BleGattCallback(),BluetoothDevice.TRANSPORT_LE);
        }
    }




    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult");
            handleScanResult(result);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults, count = " + results.size());
            for (ScanResult result:results ) {
                handleScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e( TAG, "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
            /*final BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
            bAdapter.disable();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bAdapter.enable();
                   stopThisService();

                }
            }, 1000);*/
        }
    };

    void startAdvertise(){
        Log.d(TAG, "startAdvertise");
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( true )
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.fromString(OUR_MAINSERVICE_UUID) );

        byte manData[] = {(byte)0xB5,(byte)0xB5,(byte)0xB5,(byte)0xB5,(byte)0xB5,(byte)0xB5};

        String charId = DataManager.getUuid(getApplicationContext());
       /* UUID id =   UUID.fromString(charId);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        byte[] manDaya = bb.array();*/

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
              //  .setIncludeTxPowerLevel(true)
                .addManufacturerData(223,manData)
                .addServiceUuid( pUuid )
                .build();


        advertiser.startAdvertising( settings, advertiseData, advertisingCallback );

        BluetoothManager btManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothGattServer bluetoothGattServer = btManager.openGattServer(getApplication(), callback);
        BluetoothGattService deviceIdService = new BluetoothGattService(UUID.fromString(OUR_MAINSERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(charId), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        deviceIdService.addCharacteristic(characteristic);
        bluetoothGattServer.addService(deviceIdService);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
      uploadData();
    }

    private void uploadData(){
        try {
            JSONObject data =    DataManager.generateEncodedData(devicesMap, DataManager.getSecretKey(getApplicationContext()));
            String uuid = DataManager.getUuid(getApplicationContext());
            if (data != null) {
                NetworkMethods.instance.postBTValues(uuid, data, new NetworkMethodsListener() {
                    @Override
                    public void onResponseSuccessfully(Object response) {
                        Log.d(TAG, "onResponseSuccessfully: " + response);
                    }

                    @Override
                    public void onResponseError(Object response, String error) {
                        Log.d(TAG, "onResponseError: " + error);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        private static final String TAG = "AdvertiseCallback";
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d( TAG, "Advertising onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( TAG, "onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };

    BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
        String TAG = "BluetoothGattServerCallback";
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG, "onConnectionStateChange");
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(TAG, "onServiceAdded");
        }
    };


    class BleGattCallback extends BluetoothGattCallback{
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered, address:" + gatt.getDevice().getAddress() + ", deviceName = " + gatt.getDevice().getName());

            List<BluetoothGattService> services =  gatt.getServices();
            String userId;

            for (BluetoothGattService service:services) {

                Log.d(TAG, "discovered service: " +  service.getUuid());
                if(service.getUuid().toString().equalsIgnoreCase(OUR_MAINSERVICE_UUID)){
                   List<BluetoothGattCharacteristic> list =  service.getCharacteristics();

                   userId = list.get(0).getUuid().toString();
                   Log.d(TAG, "Characteristic  = " + userId);
                   Log.d(TAG, "Characteristic value = " + list.get(0).getValue());

                  String address =  gatt.getDevice().getAddress();
                  BTPeripheral device = devicesMap.get(address);
                  device.setOurDevice(true);
                  device.setUserId(userId);

                }
            }
            gatt.close();
            gatt.disconnect();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead");

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (gatt != null) {
                Log.d(TAG, "onConnectionStateChange, deviceName = " + gatt.getDevice().getName());
                Log.d(TAG, "status = " + status + ", new state = " + newState);

                if (newState == BluetoothProfile.STATE_CONNECTED && status ==  BluetoothGatt.GATT_SUCCESS ) {
                    gatt.discoverServices();
                }
            }

        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite");

        }
    }


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
