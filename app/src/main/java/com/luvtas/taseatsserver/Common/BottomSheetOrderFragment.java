package com.luvtas.taseatsserver.Common;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.luvtas.taseatsserver.EventBus.LoadOrderEvent;
import com.luvtas.taseatsserver.R;

import org.greenrobot.eventbus.EventBus;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class BottomSheetOrderFragment extends BottomSheetDialogFragment {

    @OnClick(R.id.placed_filter)
    public void onPlaceFilterClick(){
        EventBus.getDefault().postSticky(new LoadOrderEvent(0));
        dismiss();
    }

    @OnClick(R.id.shipping_filter)
    public void onShippingFilterClick(){
        EventBus.getDefault().postSticky(new LoadOrderEvent(1));
        dismiss();
    }

    @OnClick(R.id.shipped_filter)
    public void onShippedFilterClick(){
        //Log.d("Shipped","OK");
        EventBus.getDefault().postSticky(new LoadOrderEvent(2));
        dismiss();
    }

    @OnClick(R.id.cancelled_filter)
    public void onCancelledFilterClick(){
        //Log.d("Cancel", "OK");
        EventBus.getDefault().postSticky(new LoadOrderEvent(-1));
        dismiss();
    }


    private static BottomSheetOrderFragment instance;
    private Unbinder unbinder;

    public static BottomSheetOrderFragment getInstance(){
        return instance == null ? new BottomSheetOrderFragment() : instance;
    }

    public BottomSheetOrderFragment() {
    }


    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View itemView = inflater.inflate(R.layout.fragment_order_filter, container, false);
        unbinder = ButterKnife.bind(this, itemView);
        return itemView;
    }
}
