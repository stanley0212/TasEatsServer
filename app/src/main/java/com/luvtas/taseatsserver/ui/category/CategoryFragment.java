package com.luvtas.taseatsserver.ui.category;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.luvtas.taseatsserver.Adapter.MyCategoriesAdapter;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.Common.MySwipeHelper;
import com.luvtas.taseatsserver.Common.SpacesItemDecoration;
import com.luvtas.taseatsserver.EventBus.ToastEvent;
import com.luvtas.taseatsserver.Model.CategoryModel;
import com.luvtas.taseatsserver.R;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class CategoryFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1234;
    private CategoryViewModel categoryViewModel;

    Unbinder unbinder;
    @BindView(R.id.recycler_menu)
    RecyclerView recycler_menu;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyCategoriesAdapter adapter;
    List<CategoryModel> categoryModels;
    ImageView img_category;
    private Uri imageUrl = null;
    FirebaseStorage storage;
    StorageReference storageReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        categoryViewModel =
                ViewModelProviders.of(this).get(CategoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_category, container, false);
        unbinder = ButterKnife.bind(this,root);
        initViews();
        categoryViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(),""+s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        categoryViewModel.getCategoryListMultable().observe(getViewLifecycleOwner(), categoryModelList -> {
            dialog.dismiss();
            categoryModels = categoryModelList;
            adapter = new MyCategoriesAdapter(getContext(),categoryModels);
            recycler_menu.setAdapter(adapter);

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
        recycler_menu.setLayoutManager(layoutManager);
        recycler_menu.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_menu, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(),"Update",30,0, Color.parseColor("#560027"),
                        pos -> {
                                Common.categorySelected = categoryModels.get(pos);
                                showUpdateDialog();
                        }));

                buf.add(new MyButton(getContext(),"Delete",30,0, Color.parseColor("#333639"),
                        pos -> {
                            Common.categorySelected = categoryModels.get(pos);
                            showDeleteDialog();
                        }));
            }
        };

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.action_bar_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_create)
        {
            showAddDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Create");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = (EditText)itemView.findViewById(R.id.edt_category_name);
        img_category = (ImageView)itemView.findViewById(R.id.img_category);

        // Set Data
       Glide.with(getContext()).load(R.drawable.ic_image_gray_24dp).into(img_category);

        // Set event
        img_category.setOnClickListener(view -> {
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

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                CategoryModel categoryModel = new CategoryModel();
                categoryModel.setName(edt_category_name.getText().toString());
                categoryModel.setFoods(new ArrayList<>()); // create empty list for food list

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
                            categoryModel.setImage(uri.toString());
                            addCategory(categoryModel);
                        });
                    }).addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        dialog.setMessage(new StringBuilder("Uploading...").append(progress).append("%"));
                    });
                }
                else
                {
                    addCategory(categoryModel);
                }
            }
        });
        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addCategory(CategoryModel categoryModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.CATEGORY_REF)
                .push()
                .setValue(categoryModel)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                categoryViewModel.loadCategories();
                Toast.makeText(getContext(),"Create success", Toast.LENGTH_SHORT).show();
                EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.CREATE,false));
                dialog.dismiss();
            }
        });
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
                deleteCategory();
            }
        });
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteCategory() {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .removeValue()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                categoryViewModel.loadCategories();
                Toast.makeText(getContext(),"Delete success", Toast.LENGTH_SHORT).show();
                //EventBus.getDefault().postSticky(new ToastEvent(false,false));
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
        img_category = (ImageView)itemView.findViewById(R.id.img_category);

        // Set Data
        edt_category_name.setText(new StringBuilder("").append(Common.categorySelected.getName()));
        Glide.with(getContext()).load(Common.categorySelected.getImage()).into(img_category);

        // Set event
        img_category.setOnClickListener(view -> {
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
                                    updateCategory(updateData);
                                });
                            }).addOnProgressListener(taskSnapshot -> {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                dialog.setMessage(new StringBuilder("Uploading...").append(progress).append("%"));
                    });
                }
                else
                {
                    updateCategory(updateData);
                }
            }
        });
        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateCategory(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .updateChildren(updateData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                categoryViewModel.loadCategories();
                Toast.makeText(getContext(),"Update success", Toast.LENGTH_SHORT).show();
                //EventBus.getDefault().postSticky(new ToastEvent(true,false));
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
                img_category.setImageURI(imageUrl);
            }
        }
    }
}
