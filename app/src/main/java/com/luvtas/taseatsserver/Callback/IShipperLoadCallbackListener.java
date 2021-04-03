package com.luvtas.taseatsserver.Callback;


import android.widget.Button;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;

import com.luvtas.taseatsserver.Model.OrderModel;
import com.luvtas.taseatsserver.Model.ShipperModel;

import java.util.List;

public interface IShipperLoadCallbackListener {
    void onShipperLoadSuccess(List<ShipperModel> shipperModelList);
    void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels,
                              AlertDialog dialog,
                              Button btn_ok, Button btn_cancel,
                              RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed);
    void onShipperLoadFailed(String message);
}
