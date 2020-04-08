package com.cryptekio.corridor.network;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkMethods {
    private static final String TAG = "NetworkMethods";
    private static final String HOST_URL = "https://dev.cryptek.io";
    private static final String SIGNUP_ENDPOINT = "/signup";
    private static final String VERIFY_ENDPOINT = "/verify";
    private static final String POST_VALUES_ENDPOINT = "/upload";
    private static final String GET_OPTIONS_ENDPOINT = "/options";

    public static final MediaType JSON = MediaType.parse("application/json");
    OkHttpClient client = new OkHttpClient();

    public static final NetworkMethods instance = new NetworkMethods();
    private NetworkMethods() {}

    public void postBTValues(String uuid, JSONObject emitters, @NonNull NetworkMethodsListener listener  ){
        try {
            emitters.put("owner", uuid);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String s = emitters.toString();
        Log.d(TAG, s);

        doPostRequest(POST_VALUES_ENDPOINT, emitters.toString(), listener);
    }


    public void signUp(String phoneNumber, @NonNull NetworkMethodsListener listener) {


        JSONObject json = new JSONObject();
        try {
            json.put("phone", phoneNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doPostRequest(SIGNUP_ENDPOINT, json.toString(), listener);
    }


    public void verifyNumber(String verifyCode, String number, @NonNull NetworkMethodsListener listener) {
        JSONObject json = new JSONObject();
        try {
            json.put("phone", number);
            json.put("code",verifyCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        doPostRequest(VERIFY_ENDPOINT, json.toString(), listener);
    }

    public void fetchOptions(NetworkMethodsListener listener){
        this.doGetRequest(GET_OPTIONS_ENDPOINT, listener);
    }

    private void doGetRequest(String endpoint, final NetworkMethodsListener listener){
        Request request = new Request.Builder()
                .url(HOST_URL + endpoint)
                .header("Content-Type","application/json")
                .header("Accept", "application/json")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {

                listener.onResponseSuccessfully(response.body().string());
            }

            public void onFailure(Call call, IOException e) {
                listener.onResponseError(null, e.getLocalizedMessage());
            }
        });
    }

    private void doPostRequest(String endpoint, String json, final NetworkMethodsListener listener) {

        RequestBody body = RequestBody.create(json,JSON);


        Request request = new Request.Builder()
                .url(HOST_URL + endpoint)
                .header("Content-Type","application/json")
                .header("Accept", "application/json")
                .post(body)
                .build();



        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.onResponseError(null, e.getLocalizedMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                listener.onResponseSuccessfully(response.body().string());
            }


        });

    }
}
