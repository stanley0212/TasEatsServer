package com.luvtas.taseatsserver.ui.shipper;

import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.luvtas.taseatsserver.Adapter.MyShipperAdapter;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.EventBus.ChangMenuClick;
import com.luvtas.taseatsserver.EventBus.UpdateShipperEvent;
import com.luvtas.taseatsserver.Model.ShipperModel;
import com.luvtas.taseatsserver.R;

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

public class ShipperFragment extends Fragment {

    private ShipperViewModel mViewModel;
    private Unbinder unbinder;

    @BindView(R.id.recycler_shipper)
    RecyclerView recycler_shipper;
    AlertDialog dialog;
    MyShipperAdapter adapter;
    List<ShipperModel> shipperModelList,saveShipperBeforeSearchList;



    public static ShipperFragment newInstance() {
        return new ShipperFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View itemView = inflater.inflate(R.layout.fragment_shipper, container, false);
        mViewModel = ViewModelProviders.of(this).get(ShipperViewModel.class);
        unbinder = ButterKnife.bind(this,itemView);
        initViews();
        mViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(),""+s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        mViewModel.getShipperMutableList().observe(getViewLifecycleOwner(),shippers->{
            dialog.dismiss();
            shipperModelList = shippers;
            if(saveShipperBeforeSearchList == null)
                saveShipperBeforeSearchList = shippers;
            adapter = new MyShipperAdapter(getContext(),shipperModelList);
            recycler_shipper.setAdapter(adapter);

        });
        return itemView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.food_list_menu,menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        // Event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                startSearchFood(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        // Clear text click to clear button on search view
        ImageView closeButton = (ImageView)searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(view -> {
            EditText ed = (EditText)searchView.findViewById(R.id.search_src_text);
            // Clear text
            ed.setText("");
            // Clear query
            searchView.setQuery("", false);
            // Collapse the action view
            searchView.onActionViewCollapsed();
            // Collapse the search widget
            menuItem.collapseActionView();
            // Restore result to original
            if(saveShipperBeforeSearchList != null)
                mViewModel.getShipperMutableList().setValue(saveShipperBeforeSearchList);
        });

    }

    private void startSearchFood(String s) {
        List<ShipperModel> resultShipper = new ArrayList<>();
        for(int i = 0; i < shipperModelList.size(); i ++)
        {
            ShipperModel shipperModel = shipperModelList.get(i);
            if(shipperModel.getPhone().toLowerCase().contains(s.toLowerCase()))
            {
                resultShipper.add(shipperModel);
            }
        }
        mViewModel.getShipperMutableList().setValue(resultShipper);
    }

    private void initViews() {
        setHasOptionsMenu(true);
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        LinearLayoutManager layoutManager = new LinearLayoutManager((getContext()));
        recycler_shipper.setLayoutManager(layoutManager);
        recycler_shipper.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(UpdateShipperEvent.class))
            EventBus.getDefault().removeStickyEvent(UpdateShipperEvent.class);
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangMenuClick(true));
        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateShipperActive(UpdateShipperEvent event)
    {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("active", event.isActive()); // get state of button , not of shipper
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPER)
                .child(event.getShipperModel().getKey())
                .updateChildren(updateData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(event.isActive() == true)
                {
                    Toast.makeText(getContext(),event.getShipperModel().getName()+" is Online", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getContext(),event.getShipperModel().getName()+" is Offline", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
