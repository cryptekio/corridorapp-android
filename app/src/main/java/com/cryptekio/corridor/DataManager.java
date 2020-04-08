package com.cryptekio.corridor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import com.cryptekio.corridor.entity.BTPeripheral;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class DataManager {

    public static void saveSignupData(String secret, String uuid, Context context){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("secret", secret);
        editor.putString("uuid",uuid);
        editor.commit();
        editor.apply();
    }

    public static String getUuid(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return  preferences.getString("uuid", null);
    }

    public static String getSecretKey(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return  preferences.getString("secret", null);
    }

    public static void saveOptions(int  rssiDropThreshold, int dataUploadFreq, Context context){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("rssiDropThreshold", rssiDropThreshold);
        editor.putInt("dataUploadFreq",dataUploadFreq);
        editor.commit();
        editor.apply();
    }

    public static int getRSSIDropThreshold(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return  preferences.getInt("rssiDropThreshold", -80);
    }

    public static int getDataUploadFreq(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return  preferences.getInt("dataUploadFreq", 1800);
    }

   public static  JSONObject generateEncodedData(HashMap<String, BTPeripheral> values,String secret) throws JSONException {

        HashMap<String, ArrayList<Pair<Integer,Integer>>> emitters = buildEmittersMap(values);
        JSONArray jsonEmitters = new JSONArray();

        for (String emitter: emitters.keySet()) {

            JSONObject jsonEmitter = new JSONObject();
            jsonEmitter.put("emitter", emitter);
            JSONArray jsonData = new JSONArray();
            ArrayList<Pair<Integer,Integer>> btValues = emitters.get(emitter);

            for (Pair<Integer,Integer> pair:btValues) {
                JSONObject btValueJson = new JSONObject();
                btValueJson.put(String.valueOf(pair.first),String.valueOf(pair.second));
                jsonData.put(btValueJson);
            }

            jsonEmitter.put("data",jsonData);
            jsonEmitters.put(jsonEmitter);
        }

        JSONObject json = new JSONObject();
        json.put("metrics", jsonEmitters);
        String jsonStr = json.toString();
        Log.d("JSON: ",jsonStr);

       return json;
    }

   static HashMap<String, ArrayList<Pair<Integer,Integer>>> buildEmittersMap(HashMap<String, BTPeripheral> values){
        HashMap<String, ArrayList<Pair<Integer,Integer>>> emmiters = new HashMap<>();
        for (String key:values.keySet() ) {

            BTPeripheral peripheral = values.get(key);

            if(peripheral.isOurDevice()){

                ArrayList<Pair<Integer,Integer>> uuids =   emmiters.get(peripheral.getUserId());

                if(uuids != null){
                    uuids.addAll(peripheral.getRSSIs());
                }else{
                    emmiters.put(peripheral.getUserId(), peripheral.getRSSIs());
                }
            }

        }

        return emmiters;
    }

    private static  String generateHashWithHmac256(String message, String key) {
        try {
            final String hashingAlgorithm = "HmacSHA256"; //or "HmacSHA1", "HmacSHA512"

            byte[] bytes = hmac(hashingAlgorithm, key.getBytes(), message.getBytes());

            final String messageDigest = bytesToHex(bytes);

            return messageDigest;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] hmac(String algorithm, byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(message);
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
