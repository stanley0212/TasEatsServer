package com.luvtas.taseatsserver.Callback;

public interface ILoadTimeFromFirebaseListener {
    void onLoadOnlyTimeSuccess(long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
