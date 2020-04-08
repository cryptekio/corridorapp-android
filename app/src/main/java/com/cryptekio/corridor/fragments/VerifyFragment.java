package com.cryptekio.corridor.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.cryptekio.corridor.DataManager;
import com.cryptekio.corridor.network.NetworkMethods;
import com.cryptekio.corridor.network.NetworkMethodsListener;
import com.cryptekio.corridor.R;

import org.json.JSONException;
import org.json.JSONObject;

public class VerifyFragment extends Fragment {
    EditText etVerifyCode;
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.verify_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etVerifyCode = view.findViewById(R.id.etVerify);

        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String code = etVerifyCode.getText().toString();
                String number = getArguments().getString("phone");

                if(code.length() > 0){
                    NetworkMethods.instance.verifyNumber(code, number, new NetworkMethodsListener() {
                        @Override
                        public void onResponseSuccessfully(Object response) {

                            if(response.toString().length() > 0) {

                                try {
                                    JSONObject json = new JSONObject(response.toString());

                                    String status = json.optString("status");
                                    if(status.equals("OK")){
                                        JSONObject reponse = json.optJSONObject("response");

                                       String secret =  reponse.optString("secret");
                                       String uuid = reponse.optString("uuid");

                                        DataManager.saveSignupData(secret,uuid, getActivity().getApplicationContext());
                                        NavHostFragment.findNavController(VerifyFragment.this)
                                                .navigate(R.id.action_SecondFragment_to_mainFragment);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        }

                        @Override
                        public void onResponseError(Object response, String error) {
                            Log.d("tag","");
                        }
                    });

                }


            }
        });
    }


}
