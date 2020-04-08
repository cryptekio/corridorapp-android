package com.cryptekio.corridor.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.cryptekio.corridor.network.NetworkMethods;
import com.cryptekio.corridor.network.NetworkMethodsListener;
import com.cryptekio.corridor.R;

public class RegistrationFragment extends Fragment {
    private static final String TAG = "RegistrationFragment";

    private EditText etPhoneNumber;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.registration_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               final String phoneNumber = etPhoneNumber.getText().toString();

                String regexStr = "^[+]?[0-9]{10,13}$";
                if(phoneNumber.matches(regexStr)) {

                    Log.d(TAG,"valid number" );
                    NetworkMethods.instance.signUp(phoneNumber, new NetworkMethodsListener() {
                        @Override
                        public void onResponseSuccessfully(Object response) {
                            Bundle bundle = new Bundle();
                            bundle.putString("phone", phoneNumber);
                            NavHostFragment.findNavController(RegistrationFragment.this)
                                    .navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);
                        }

                        @Override
                        public void onResponseError(Object response, String error) {

                        }
                    });

                }else{
                    Toast.makeText(getContext(),"Please enter  valid phone number",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"invalid number" );
                }


            }
        });
    }





}
