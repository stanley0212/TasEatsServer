package com.luvtas.taseatsserver.Callback;

import com.luvtas.taseatsserver.Model.BestDealsModel;

import java.util.List;

public interface IBestDealsCallbackListener {
    void onListBestDealsLoadSuccess(List<BestDealsModel> bestDealsModels);
    void onListBestDealsLoadFailed(String message);
}
