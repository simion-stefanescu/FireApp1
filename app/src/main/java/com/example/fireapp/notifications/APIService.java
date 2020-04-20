package com.example.fireapp.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers({
            "Content-Type:aplication/json",
            "Authorization:key = AAAA9B9nyh4:APA91bFvSc8AbN1MqFVDyCk7aCnY_UVTpE7HCvnb1d2KbOMh2Pk2GZbRUjk-mJZQDfe-h4yIMPEjD0vJ9Kb8prAQsUInMJ6R9GgZoHqXbTz9BEEiKxXGOBZzi-vEr_wgI2fZk7GSi4w3"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);

}
