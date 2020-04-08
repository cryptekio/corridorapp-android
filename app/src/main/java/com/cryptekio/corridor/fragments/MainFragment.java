package com.cryptekio.corridor.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.cryptekio.corridor.DataManager;
import com.cryptekio.corridor.MainActivity;
import com.cryptekio.corridor.network.NetworkMethods;
import com.cryptekio.corridor.network.NetworkMethodsListener;
import com.cryptekio.corridor.R;
import org.json.JSONException;
import org.json.JSONObject;


public class MainFragment extends BaseFragment {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;

    private TextView tvUuid;

    public MainFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        tvUuid.setText(DataManager.getUuid(mActivity));

        NetworkMethods.instance.fetchOptions(new NetworkMethodsListener() {
            @Override
            public void onResponseSuccessfully(Object response) {

                try {
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONObject optionsJson = jsonObject.getJSONObject("response").getJSONObject("options");
                   int rssiDrop =  optionsJson.optInt("rssi_drop_t");
                   int uploadFreq =  optionsJson.optInt("data_upload_f");
                   DataManager.saveOptions(rssiDrop,uploadFreq,mActivity.getApplicationContext());

                     if(ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED){

                         mActivity.runOnUiThread(new Runnable() {
                             @Override
                             public void run() {

                                 final Handler handler = new Handler();
                                 handler.postDelayed(new Runnable() {
                                     @Override
                                     public void run() {
                                         if(mActivity != null){
                                             ((MainActivity)mActivity).startMyService();
                                         }
                                     }
                                 }, 300);
                             }
                         });
                     }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onResponseError(Object response, String error) {

            }
        });

    }


    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        requestMyPermissions();
        View root =  inflater.inflate(R.layout.fragment_main, container, false);
        tvUuid = root.findViewById(R.id.tvUuid);
        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity activity = (MainActivity) mActivity;
                    activity.startMyService();
                }
                return;
            }

            case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
        }
    }

    void requestMyPermissions(){


        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessCoarseLocationApproved) {
            ((MainActivity)mActivity).startMyService();
            boolean backgroundLocationPermissionApproved =
                    ActivityCompat.checkSelfPermission(mActivity,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            if (backgroundLocationPermissionApproved) {

            } else {

                ActivityCompat.requestPermissions(mActivity, new String[] {
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_REQUEST_BACKGROUND_LOCATION);
            }
        } else {

            ActivityCompat.requestPermissions(mActivity, new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    PERMISSION_REQUEST_FINE_LOCATION);        }
    }
}
