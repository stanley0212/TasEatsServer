package com.luvtas.taseatsserver.Callback;

import com.luvtas.taseatsserver.Model.ShippingOrder;

public interface ISingleShippingOrderCallbackListener {
    void onSingleShippingOrderLoadSuccess(ShippingOrder shippingOrder);
}
