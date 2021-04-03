package com.luvtas.taseatsserver.Remote;

import com.luvtas.taseatsserver.Model.FCMResponse;
import com.luvtas.taseatsserver.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAcCrey8Q:APA91bHS5mz1AOIMehpnEJ9YO-D20wIMalJEenOgiGEuV1tCJQVRDejiYc1wKfRmDt2XeUR-dw9ty9vR0xwZ8e9qdNVOa5JQve8FsN5XyqINWlBCmXM0B03mFXAO_441W0St36INiIw8"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}