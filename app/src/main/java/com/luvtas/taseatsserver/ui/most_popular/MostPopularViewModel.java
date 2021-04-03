package com.luvtas.taseatsserver.ui.most_popular;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.luvtas.taseatsserver.Callback.IMostPopularCallbackListener;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.Model.MostPopularModel;

import java.util.ArrayList;
import java.util.List;

public class MostPopularViewModel extends ViewModel implements IMostPopularCallbackListener {
    private MutableLiveData<String> messageError = new MutableLiveData<>();
    private MutableLiveData<List<MostPopularModel>>  mostPopularListMutable;
    private IMostPopularCallbackListener iMostPopularCallbackListener;

    public MostPopularViewModel() {
        iMostPopularCallbackListener = this;
    }

    public MutableLiveData<List<MostPopularModel>> getMostPopularListMutable() {
        if(mostPopularListMutable == null)
            mostPopularListMutable = new MutableLiveData<>();
        loadMostPopular();
        return mostPopularListMutable;
    }

    public void loadMostPopular() {
        List<MostPopularModel> temp = new ArrayList<>();
        DatabaseReference mostPopularRef = FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.MOST_POPULAR);

        mostPopularRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot mostPopularSnapShot:dataSnapshot.getChildren())
                {
                    MostPopularModel mostPopularModel = mostPopularSnapShot.getValue(MostPopularModel.class);
                    mostPopularModel.setKey(mostPopularSnapShot.getKey());
                    temp.add(mostPopularModel);
                }
                iMostPopularCallbackListener.onListMostPopularLoadSuccess(temp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                iMostPopularCallbackListener.onListMostPopularLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onListMostPopularLoadSuccess(List<MostPopularModel> mostPopularModels) {
        mostPopularListMutable.setValue(mostPopularModels);
    }

    @Override
    public void onListMostPopularLoadFailed(String message) {
        messageError.setValue(message);
    }
}
