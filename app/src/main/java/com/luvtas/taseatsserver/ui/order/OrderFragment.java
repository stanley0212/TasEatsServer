package com.luvtas.taseatsserver.ui.order;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.luvtas.taseatsserver.Adapter.MyOrderAdapter;
import com.luvtas.taseatsserver.Adapter.MyShipperSelectionAdapter;
import com.luvtas.taseatsserver.Callback.IShipperLoadCallbackListener;
import com.luvtas.taseatsserver.Common.BottomSheetOrderFragment;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.Common.MySwipeHelper;
import com.luvtas.taseatsserver.EventBus.AddonSizeEditEvent;
import com.luvtas.taseatsserver.EventBus.ChangMenuClick;
import com.luvtas.taseatsserver.EventBus.LoadOrderEvent;
import com.luvtas.taseatsserver.EventBus.PrintOrderEvent;
import com.luvtas.taseatsserver.Model.FCMResponse;
import com.luvtas.taseatsserver.Model.FCMSendData;
import com.luvtas.taseatsserver.Model.FoodModel;
import com.luvtas.taseatsserver.Model.OrderModel;
import com.luvtas.taseatsserver.Model.ShipperModel;
import com.luvtas.taseatsserver.Model.ShippingOrder;
import com.luvtas.taseatsserver.Model.TokenModel;
import com.luvtas.taseatsserver.R;
import com.luvtas.taseatsserver.Remote.IFCMService;
import com.luvtas.taseatsserver.Remote.RetrofitFCMClient;
import com.luvtas.taseatsserver.SizeAddonEditActivity;
import com.luvtas.taseatsserver.TrackingOrderActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class OrderFragment extends Fragment implements IShipperLoadCallbackListener {

    private OrderViewModel orderViewModel;

    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    @BindView(R.id.txt_order_filter)
    TextView txt_order_filter;

    RecyclerView recycler_shipper;

    Unbinder unbinder;

    MyOrderAdapter adapter;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;
    private MyShipperSelectionAdapter myShipperSelectedAdapter;
    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    public static OrderFragment newInstance() {
        return new OrderFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        orderViewModel = ViewModelProviders.of(this).get(OrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_order, container,false);
        unbinder = ButterKnife.bind(this,root);
        initViews();
        orderViewModel.getMessageError().observe(getViewLifecycleOwner(),s -> {
            Toast.makeText(getContext(),s, Toast.LENGTH_SHORT).show();
        });
        orderViewModel.getOrderModelMutableLiveData().observe(getViewLifecycleOwner(),orderViewModel -> {
            if(orderViewModel != null)
            {
                adapter = new MyOrderAdapter(getContext(),orderViewModel);

                recycler_order.setAdapter(adapter);
                updateTextCounter();
            }
        });
        return root;
    }

    private void initViews() {

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        shipperLoadCallbackListener = this;

        setHasOptionsMenu(true);
        recycler_order.setHasFixedSize(true);
        recycler_order.setLayoutManager(new LinearLayoutManager(getContext()));

        //Get Size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_order, width / 6) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                buf.add(new MyButton(getContext(),"Print",30,0, Color.parseColor("#89b010"),
                        pos -> {

                            Dexter.withActivity(getActivity())
                                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse response) {
                                            EventBus.getDefault().postSticky(new PrintOrderEvent(new StringBuilder(Common.getAppPath(getActivity()))
                                                    .append(Common.FILE_PRINT).toString(),
                                                    adapter.getItemAtPosition(pos)));
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse response) {
                                            Toast.makeText(getContext(),"Please accept this permission", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                        }
                                    }).check();

                        }));

                buf.add(new MyButton(getContext(),"Directions",30,0, Color.parseColor("#9b0000"),
                        pos -> {

                                OrderModel orderModel = ((MyOrderAdapter)recycler_order.getAdapter())
                                        .getItemAtPosition(pos);
                                if(orderModel.getOrderStatus() == 1)
                                {
                                    Common.currentOrderSelected = orderModel;
                                    startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                }
                                else
                                {
                                    Toast.makeText(getContext(), new StringBuilder("Your order is ")
                                    .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                                    .append(". So you can't track directions"), Toast.LENGTH_SHORT).show();
                                }

                        }));
                buf.add(new MyButton(getContext(),"Call",30,0, Color.parseColor("#560027"),
                        pos -> {

                            Dexter.withActivity(getActivity())
                                    .withPermission(Manifest.permission.CALL_PHONE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse response) {
                                            OrderModel orderModel = adapter.getItemAtPosition(pos);
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_DIAL);
                                            intent.setData(Uri.parse(new StringBuilder("tel: ")
                                                    .append(orderModel.getUserPhone()).toString()));
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse response) {
                                            Toast.makeText(getContext(),"You must accept "+response.getPermissionName(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                        }
                                    }).check(); // don't forgot call check()

                        }));
                buf.add(new MyButton(getContext(),"Remove",30,0, Color.parseColor("#12005e"),
                        pos -> {

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                                    .setTitle("Delete")
                                    .setMessage("Do you really want to delete this order?")
                                    .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("DELETE", (dialogInterface, i) -> {
                                        OrderModel orderModel = adapter.getItemAtPosition(pos);
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.RESTAURANT_REF)
                                                .child(Common.currentServerUser.getRestaurant())
                                                .child(Common.ORDER_REF)
                                                .child(orderModel.getKey())
                                                .removeValue()
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }).addOnSuccessListener(aVoid -> {
                                                    adapter.removeItem(pos);
                                                    adapter.notifyItemRemoved(pos);
                                                    updateTextCounter();
                                                    dialogInterface.dismiss();
                                                    Toast.makeText(getContext(),"Order has been delete!", Toast.LENGTH_SHORT).show();
                                        });

                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                            negativeButton.setTextColor(Color.GRAY);
                            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setTextColor(Color.RED);

                        }));
                buf.add(new MyButton(getContext(),"Update",30,0, Color.parseColor("#336699"),
                        pos -> {

                                showEditDialog(adapter.getItemAtPosition(pos),pos);

                        }));
            }
        };


    }

    private void showEditDialog(OrderModel orderModel, int pos) {
        View layout_dialog;
        AlertDialog.Builder builder;
        if(orderModel.getOrderStatus() == 0)
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipping, null);

            recycler_shipper = layout_dialog.findViewById(R.id.recycler_shippers);

            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen).setView(layout_dialog);
        }
        else if(orderModel.getOrderStatus() == -1)  // Cancelled
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_cancelled, null);
            builder = new AlertDialog.Builder(getContext()).setView(layout_dialog);
        }
        else // Shipped
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipped, null);
            builder = new AlertDialog.Builder(getContext()).setView(layout_dialog);
        }

        // View
        Button btn_ok = (Button)layout_dialog.findViewById(R.id.btn_ok);
        Button btn_cancel = (Button)layout_dialog.findViewById(R.id.btn_cancel);

        RadioButton rdi_shipping = (RadioButton)layout_dialog.findViewById(R.id.rdi_shipping);
        RadioButton rdi_shipped = (RadioButton)layout_dialog.findViewById(R.id.rdi_shipped);
        RadioButton rdi_cancelled = (RadioButton)layout_dialog.findViewById(R.id.rdi_cancelled);
        RadioButton rdi_delete = (RadioButton)layout_dialog.findViewById(R.id.rdi_delete);
        RadioButton rdi_restore_placed = (RadioButton)layout_dialog.findViewById(R.id.rdi_restore_placed);

        TextView txt_status = (TextView)layout_dialog.findViewById(R.id.txt_status);

        // Set data
        txt_status.setText(new StringBuilder("Order Status(").append(Common.convertStatusToString(orderModel.getOrderStatus())));

        // Create Dialog
        AlertDialog dialog = builder.create();

        if(orderModel.getOrderStatus() == 0) // Shipping
            loadShipperList(pos, orderModel, dialog, btn_ok, btn_cancel,
                    rdi_shipping, rdi_shipped, rdi_cancelled,rdi_delete,rdi_restore_placed);
        else
            showDialog(pos, orderModel, dialog, btn_ok, btn_cancel,
                    rdi_shipping, rdi_shipped, rdi_cancelled,rdi_delete,rdi_restore_placed);



    }

    private void loadShipperList(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        List<ShipperModel> tempList = new ArrayList<>();
        DatabaseReference shipperRef = FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPER);
        Query shipperActive = shipperRef.orderByChild("active").equalTo(true); // Load only shipper active by server app
        shipperActive.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot shipperSnapShot: dataSnapshot.getChildren())
                {
                    ShipperModel shipperModel = shipperSnapShot.getValue(ShipperModel.class);
                    shipperModel.setKey(shipperSnapShot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos,orderModel,tempList,
                        dialog,
                        btn_ok,btn_cancel,
                        rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(databaseError.getMessage());
            }
        });
    }

    private void showDialog(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        dialog.show();
        //Log.d("Dialog", "OK");
        //Custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_cancel.setOnClickListener(view -> dialog.dismiss());
        btn_ok.setOnClickListener(view -> {

            if(rdi_cancelled != null && rdi_cancelled.isChecked())
                {
                    updateOrder(pos,orderModel,-1);
                    dialog.dismiss();
                }
            else if(rdi_shipping != null && rdi_shipping.isChecked())
            {
                //updateOrder(pos,orderModel,1);
                ShipperModel shipperModel = null;
                if(myShipperSelectedAdapter != null)
                {
                    shipperModel = myShipperSelectedAdapter.getSelectedShipper();
                    if(shipperModel != null)
                    {
//                        Toast.makeText(getContext(),shipperModel.getName()+" will delivery",Toast.LENGTH_SHORT).show();
//                        dialog.dismiss();

                        createShippingOrder(pos,shipperModel,orderModel,dialog);

                    }
                    else
                    {
                        Toast.makeText(getContext(),"Please select shipper",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if(rdi_shipped != null && rdi_shipped.isChecked())
            {
                updateOrder(pos,orderModel,2);
                dialog.dismiss();
            }
            else if(rdi_restore_placed != null && rdi_restore_placed.isChecked())
            {
                updateOrder(pos,orderModel,0);
                dialog.dismiss();
            }
            else if(rdi_delete != null && rdi_delete.isChecked())
            {
                deleteOrder(pos,orderModel);
                dialog.dismiss();
            }
        });

    }

    private void createShippingOrder(int pos, ShipperModel shipperModel, OrderModel orderModel, AlertDialog dialog) {
        ShippingOrder shippingOrder = new ShippingOrder();
        shippingOrder.setShipperPhone(shipperModel.getPhone());
        shippingOrder.setShipperName(shipperModel.getName());
        shippingOrder.setOrderModel(orderModel);
        shippingOrder.setStartTrip(false);
        shippingOrder.setCurrentLat(-37.840346);
        shippingOrder.setCurrentLng(145.1171413);

        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPING_ORDER_REF)
                .child(orderModel.getKey())
                .setValue(shippingOrder)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        dialog.dismiss();
                        //Toast.makeText(getContext(), "Order has been sent to "+shipperModel.getName(), Toast.LENGTH_SHORT).show();
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(shipperModel.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists())
                                        {
                                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "Your order new order to delivery");
                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("Your have new order to delivery to ")
                                                    .append(orderModel.getShippingAddress()).toString());

                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(), notiData);
                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if(fcmResponse.getSuccess() == 1)
                                                        {
                                                            Log.d("Nofi","OK");
                                                            updateOrder(pos, orderModel,1);
                                                        }
                                                        else
                                                        {
                                                            Toast.makeText(getContext(),"Failed to send to shipper! Order wasn't update", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(),""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }));
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(),"Token not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(),""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void deleteOrder(int pos, OrderModel orderModel) {
        if(!TextUtils.isEmpty(orderModel.getKey()))
        {
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .removeValue()
                    .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {
                        adapter.removeItem(pos);
                        adapter.notifyItemRemoved(pos);
                        updateTextCounter();
                        Toast.makeText(getContext(),"Delete order success", Toast.LENGTH_SHORT).show();
                    });
        }
        else
        {
            Toast.makeText(getContext(),"Order number must not be null or empty",Toast.LENGTH_SHORT).show();
        }

    }

    private void updateOrder(int pos, OrderModel orderModel, int status)
    {
        if(!TextUtils.isEmpty(orderModel.getKey()))
        {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("orderStatus", status);

            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .updateChildren(updateData)
                    .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {

                        // Show dialog
                        android.app.AlertDialog dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
                        dialog.show();

                        // First, get token of user
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.TOKEN_REF)
                                    .child(orderModel.getUserId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                                Map<String, String> notiData = new HashMap<>();
                                                notiData.put(Common.NOTI_TITLE, "Your order was update");
                                                notiData.put(Common.NOTI_CONTENT, new StringBuilder("Your order ")
                                                .append(orderModel.getKey())
                                                .append(" Was update to ")
                                                .append(Common.convertStatusToString(status)).toString());

                                                FCMSendData sendData = new FCMSendData(tokenModel.getToken(), notiData);
                                                compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(fcmResponse -> {
                                                    dialog.dismiss();
                                                    if(fcmResponse.getSuccess() == 1)
                                                    {
                                                        Toast.makeText(getContext(),"Update order success", Toast.LENGTH_SHORT).show();
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(getContext(),"Update order success but failed to send notification", Toast.LENGTH_SHORT).show();
                                                    }
                                                }, throwable -> {
                                                    dialog.dismiss();
                                                    Toast.makeText(getContext(),""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }));
                                            }
                                            else
                                            {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(),"Token not found", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(),""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });



                           adapter.removeItem(pos);
                           adapter.notifyItemRemoved(pos);
                           updateTextCounter();

                    });
        }
        else
        {
            Toast.makeText(getContext(),"Order number must not be null or empty",Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTextCounter() {
        txt_order_filter.setText(new StringBuilder("Orders (").append(adapter.getItemCount()).append(")"));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.order_filter_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_filter)
        {
            BottomSheetOrderFragment bottomSheetOrderFragment = BottomSheetOrderFragment.getInstance();
            bottomSheetOrderFragment.show(getActivity().getSupportFragmentManager(),"OrderFilter");
            return true;
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent.class);
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangMenuClick(true));
        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoadOrderEvent(LoadOrderEvent event)
    {
        orderViewModel.loadOrderByStatus(event.getStatus());
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {

    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        if(recycler_shipper != null)
        {
            recycler_shipper.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recycler_shipper.setLayoutManager(layoutManager);
            recycler_shipper.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

            myShipperSelectedAdapter = new MyShipperSelectionAdapter(getContext(), shipperModels);
            recycler_shipper.setAdapter(myShipperSelectedAdapter);
        }
        showDialog(pos, orderModel, dialog, btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
    }

    @Override
    public void onShipperLoadFailed(String message) {
        Toast.makeText(getContext(),""+message, Toast.LENGTH_SHORT).show();
    }
}
