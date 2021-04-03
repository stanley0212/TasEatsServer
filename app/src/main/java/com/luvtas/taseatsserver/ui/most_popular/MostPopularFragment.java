package com.luvtas.taseatsserver.ui.most_popular;

import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.luvtas.taseatsserver.Adapter.MyBestDealsAdapter;
import com.luvtas.taseatsserver.Adapter.MyMostPopularAdapter;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.Common.MySwipeHelper;
import com.luvtas.taseatsserver.Model.MostPopularModel;
import com.luvtas.taseatsserver.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class MostPopularFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1234;
    private MostPopularViewModel mostPopularViewModel;
    Unbinder unbinder;
    @BindView(R.id.recycler_most_popular)
    RecyclerView recycler_most_popular;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyMostPopularAdapter adapter;
    List<MostPopularModel> mostPopularModels;

    ImageView img_most_popular;
    private Uri imageUrl = null;
    FirebaseStorage storage;
    StorageReference storageReference;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mostPopularViewModel =
                ViewModelProviders.of(this).get(MostPopularViewModel.class);
        View root = inflater.inflate(R.layout.most_popular_fragment, container, false);
        unbinder = ButterKnife.bind(this,root);
        initViews();
        mostPopularViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(),""+s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        mostPopularViewModel.getMostPopularListMutable().observe(getViewLifecycleOwner(), list -> {
            dialog.dismiss();
            mostPopularModels = list;
            adapter = new MyMostPopularAdapter(getContext(),mostPopularModels);
            recycler_most_popular.setAdapter(adapter);

            // 取消滑動動畫
            //recycler_menu.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        //dialog.show();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_most_popular.setLayoutManager(layoutManager);
        recycler_most_popular.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_most_popular, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(),"Update",30,0, Color.parseColor("#560027"),
                        pos -> {
                            Common.mostPopularSelected = mostPopularModels.get(pos);
                            showUpdateDialog();
                        }));

                buf.add(new MyButton(getContext(),"Delete",30,0, Color.parseColor("#333639"),
                        pos -> {
                            Common.mostPopularSelected = mostPopularModels.get(pos);
                            showDeleteDialog();
                        }));
            }
        };
    }

    private void showDeleteDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Delete");
        builder.setMessage("Do you really want to delete this item?");
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteMostPopular();
            }
        });
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteMostPopular() {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.MOST_POPULAR)
                .child(Common.mostPopularSelected.getKey())
                .removeValue()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mostPopularViewModel.loadMostPopular();
                //Toast.makeText(getContext(),"Update success", Toast.LENGTH_SHORT).show();
                //EventBus.getDefault().postSticky(new ToastEvent(false,true));
                dialog.dismiss();
            }
        });
    }

    private void showUpdateDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = (EditText)itemView.findViewById(R.id.edt_category_name);
        img_most_popular = (ImageView)itemView.findViewById(R.id.img_category);

        // Set Data
        edt_category_name.setText(new StringBuilder("").append(Common.mostPopularSelected.getName()));
        Glide.with(getContext()).load(Common.mostPopularSelected.getImage()).into(img_most_popular);

        // Set event
        img_most_popular.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("name", edt_category_name.getText().toString());

                if(imageUrl != null)
                {
                    // In this, we will use firebase storage to upload image
                    dialog.setMessage("Uploading...");
                    dialog.show();

                    String unique_name = UUID.randomUUID().toString();
                    StorageReference imageFolder = storageReference.child("image/"+unique_name);

                    imageFolder.putFile(imageUrl)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }).addOnCompleteListener(task -> {
                        dialog.dismiss();
                        imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                            updateData.put("image", uri.toString());
                            updateMostPopular(updateData);
                        });
                    }).addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        dialog.setMessage(new StringBuilder("Uploading...").append(progress).append("%"));
                    });
                }
                else
                {
                    updateMostPopular(updateData);
                }
            }
        });
        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateMostPopular(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.MOST_POPULAR)
                .child(Common.mostPopularSelected.getKey())
                .updateChildren(updateData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mostPopularViewModel.loadMostPopular();
                //Toast.makeText(getContext(),"Update success", Toast.LENGTH_SHORT).show();
                //EventBus.getDefault().postSticky(new ToastEvent(true,true));
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data != null && data.getData() != null)
            {
                imageUrl = data.getData();
                img_most_popular.setImageURI(imageUrl);
            }
        }
    }

}
