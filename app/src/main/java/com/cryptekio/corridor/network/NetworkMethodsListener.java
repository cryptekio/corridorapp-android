package com.cryptekio.corridor.network;

public interface  NetworkMethodsListener{
    void onResponseSuccessfully(Object response);
    void onResponseError(Object response, String error);

}
