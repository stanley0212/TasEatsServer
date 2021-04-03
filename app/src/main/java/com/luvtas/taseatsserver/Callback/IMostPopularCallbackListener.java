package com.luvtas.taseatsserver.Callback;

import com.luvtas.taseatsserver.Model.MostPopularModel;

import java.util.List;

public interface IMostPopularCallbackListener {
    void onListMostPopularLoadSuccess(List<MostPopularModel> mostPopularModels);
    void onListMostPopularLoadFailed(String message);
}
