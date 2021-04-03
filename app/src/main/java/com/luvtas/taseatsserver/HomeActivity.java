package com.luvtas.taseatsserver;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.luvtas.taseatsserver.Adapter.PdfDocumentAdapter;
import com.luvtas.taseatsserver.Common.Common;
import com.luvtas.taseatsserver.Common.PDFUtils;
import com.luvtas.taseatsserver.EventBus.CategoryClick;
import com.luvtas.taseatsserver.EventBus.ChangMenuClick;
import com.luvtas.taseatsserver.EventBus.PrintOrderEvent;
import com.luvtas.taseatsserver.EventBus.ToastEvent;
import com.luvtas.taseatsserver.Model.FCMResponse;
import com.luvtas.taseatsserver.Model.FCMSendData;
import com.luvtas.taseatsserver.Model.OrderModel;
import com.luvtas.taseatsserver.Remote.IFCMService;
import com.luvtas.taseatsserver.Remote.RetrofitFCMClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 7171;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private int menuClick = -1;

    private ImageView img_upload;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;
    private Uri imgUri = null;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private AlertDialog dialog;

    @OnClick(R.id.fab_chat)
    void onOpenChatList(){
        startActivity(new Intent(this, ChatListActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        init();



//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_category, R.id.nav_food_list, R.id.nav_order, R.id.nav_shipper, R.id.nav_sign_out )
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = (TextView)headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hey ", Common.currentServerUser.getName(),txt_user);

        menuClick = R.id.nav_category;

        checkIsOpenFromActivity();
    }

    private void init() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        subscribeToTopic(Common.createTopicOrder());
        updateToken();

        dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setMessage("Please wait...")
                .create();
    }

    private void checkIsOpenFromActivity() {
        boolean isOpenFromNewOrder = getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false);
        if(isOpenFromNewOrder)
        {
            navController.popBackStack();
            navController.navigate(R.id.nav_order);
            menuClick = R.id.nav_order;
        }
    }

    private void updateToken() {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        Common.updateToken(HomeActivity.this, instanceIdResult.getToken(),
                                true,false);
                    }


                });
    }

    private void subscribeToTopic(String topicOrder) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topicOrder)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful())
                    Toast.makeText(HomeActivity.this,"Failed: "+task.isSuccessful(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategoryClick(CategoryClick event)
    {
        if(event.isSuccess())
        {
            if(menuClick != R.id.nav_food_list)
            {
                navController.navigate(R.id.nav_food_list);
                menuClick = R.id.nav_food_list;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onChangMenuClick(ChangMenuClick event)
    {
        if(event.isFromFoodList())
        {
            // Clear
            navController.popBackStack(R.id.nav_category, true);
            navController.navigate(R.id.nav_category);
        }
        else
        {
            navController.popBackStack(R.id.nav_food_list, true);
            navController.navigate(R.id.nav_food_list);
        }
        
        menuClick = -1;
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onToastEvent(ToastEvent event)
    {
        if(event.getAction() == Common.ACTION.CREATE)
        {
            Toast.makeText(HomeActivity.this,"Create success", Toast.LENGTH_SHORT).show();

        }
        else if(event.getAction() == Common.ACTION.UPDATE)
        {
            Toast.makeText(HomeActivity.this,"update success", Toast.LENGTH_SHORT).show();

        }
        else
        {
            Toast.makeText(HomeActivity.this,"Delete success", Toast.LENGTH_SHORT).show();
        }
            EventBus.getDefault().postSticky(new ChangMenuClick(event.isFromFoodList()));
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);
        drawer.closeDrawers();
        switch (menuItem.getItemId())
        {
            case R.id.nav_category:
                if(menuItem.getItemId() != menuClick)
                {
                    navController.popBackStack(); //remove all back stack
                    navController.navigate(R.id.nav_category);
                }
                break;
            case R.id.nav_order:
                if(menuItem.getItemId() != menuClick)
                {
                    //navController.popBackStack(); //remove all back stack
                    navController.navigate(R.id.nav_order);
                }
                break;
            case R.id.nav_shipper:
                if(menuItem.getItemId() != menuClick)
                {
                    //navController.popBackStack(); //remove all back stack
                    navController.navigate(R.id.nav_shipper);
                }
                break;
            case R.id.nav_best_deals:
                if(menuItem.getItemId() != menuClick)
                {
                    //navController.popBackStack(); //remove all back stack
                    navController.navigate(R.id.nav_best_deals);
                }
                break;
            case R.id.nav_most_popular:
                if(menuItem.getItemId() != menuClick)
                {
                    //navController.popBackStack(); //remove all back stack
                    navController.navigate(R.id.nav_most_popular);
                }
                break;
            case R.id.nav_send_news:
                showNewsDialog();
                break;
            case R.id.nav_sign_out:
                signOut();
                break;
            default:
                menuClick = -1;
                break;
        }
        menuClick = menuItem.getItemId();
        return true;
    }

    private void showNewsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("New Shop");
        builder.setMessage("Send news notification to all client");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_news_system,null);

        EditText edt_title = (EditText)itemView.findViewById(R.id.edt_title);
        EditText edt_content = (EditText)itemView.findViewById(R.id.edt_content);
        EditText edt_link = (EditText)itemView.findViewById(R.id.edt_link);

        img_upload = (ImageView)itemView.findViewById(R.id.img_upload);
        RadioButton rdi_none = (RadioButton)itemView.findViewById(R.id.rdi_none);
        RadioButton rdi_link = (RadioButton)itemView.findViewById(R.id.rdi_link);
        RadioButton rdi_upload = (RadioButton)itemView.findViewById(R.id.rdi_image);

        // Event
        rdi_none.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_link.setOnClickListener(view -> {
            edt_link.setVisibility(View.VISIBLE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_upload.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.VISIBLE);
        });

        img_upload.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE_REQUEST);
        });

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("SEND", (dialogInterface, i) -> {
            if(rdi_none.isChecked())
            {
                sendNews(edt_title.getText().toString(),edt_content.getText().toString());
            }
            else if(rdi_link.isChecked())
            {
                sendNews(edt_title.getText().toString(),edt_content.getText().toString(), edt_link.getText().toString());
            }
            else if(rdi_upload.isChecked())
            {
                if(imgUri != null)
                {
                    AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Uploading...").create();
                    dialog.show();

                    String file_name = UUID.randomUUID().toString();
                    StorageReference newsImages = storageReference.child("news/"+file_name);
                    newsImages.putFile(imgUri)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            dialog.dismiss();
                            newsImages.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                         sendNews(edt_title.getText().toString(), edt_content.getText().toString(), uri.toString());
                                }
                            });
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = Math.round((100.0 * taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount()));
                            dialog.setMessage(new StringBuilder("Uploading...").append(progress).append("%"));
                        }
                    });
                }
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendNews(String title, String content, String url) {
        Map<String, String> notificationData = new HashMap<String, String>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE,"true");
        notificationData.put(Common.IMAGE_URL,url);

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();

                    Log.d("ID", String.valueOf(fcmResponse.getMessage_id()));

                    if(fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this,"News has been sent", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "News send failed", Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, ""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                }));
    }

    private void sendNews(String title, String content) {
        Map<String, String> notificationData = new HashMap<>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE,"false");

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(fcmResponse -> {
                dialog.dismiss();
                if(fcmResponse.getMessage_id() != 0)
                    Toast.makeText(this,"News has been sent", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "News send failed", Toast.LENGTH_SHORT).show();
        }, throwable -> {
            dialog.dismiss();
            Toast.makeText(this, ""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
        }));
    }

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Do you want to sign out?")
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Common.selectedFood = null;
                Common.categorySelected = null;
                Common.currentServerUser = null;
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data != null && data.getData() != null)
            {
                imgUri = data.getData();
                img_upload.setImageURI(imgUri);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPrintEventListener(PrintOrderEvent event)
    {
        createPDFFile(event.getPath(), event.getOrderModel());
    }

    private void createPDFFile(String path, OrderModel orderModel) {
        dialog.show();

        if(new File(path).exists())
            new File(path).delete();
        try{
            Document document = new Document();
            // Save
            PdfWriter.getInstance(document, new FileOutputStream(path));
            // Open
            document.open();

            // Setting
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            document.addAuthor("TasEats");
            document.addCreator(Common.currentServerUser.getName());

            // Font Setting
            BaseColor colorAccent = new BaseColor(0,153,204,255);
            float fontSize = 20.0f;

            // Custom font
            BaseFont fontName = BaseFont.createFont("assets/fonts/brandon_medium.otf","UTF-8", BaseFont.EMBEDDED);

            // Create title of Document
            Font titleFont = new Font(fontName, 36.0f, Font.NORMAL, BaseColor.BLACK);
            PDFUtils.addNewItem(document, "Order Details", Element.ALIGN_CENTER, titleFont);

            // Add more
            Font orderNumberFont = new Font(fontName, fontSize, Font.NORMAL, colorAccent);
            PDFUtils.addNewItem(document, "Order No: ", Element.ALIGN_LEFT, orderNumberFont);
            Font orderNumberValueFont = new Font(fontName, 20, Font.NORMAL, BaseColor.BLACK);
            PDFUtils.addNewItem(document,orderModel.getKey(), Element.ALIGN_LEFT,orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            // Date
            PDFUtils.addNewItem(document, "Order Date", Element.ALIGN_LEFT, orderNumberFont);
            PDFUtils.addNewItem(document, new SimpleDateFormat("dd/MM/yyyy").format(orderModel.getCreateDate()), Element.ALIGN_LEFT,orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            // Account name

            PDFUtils.addNewItem(document,"Account Name:",Element.ALIGN_LEFT,orderNumberFont);
            PDFUtils.addNewItem(document,orderModel.getUserName(), Element.ALIGN_LEFT,orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            // Add product add detail
            PDFUtils.addLinSpace(document);
            PDFUtils.addNewItem(document, "Product Detail", Element.ALIGN_CENTER, titleFont);

            PDFUtils.addLineSeparator(document);

            // Use Rxjava, fetch image internet and add to PDF
            Observable.fromIterable(orderModel.getCartItemList())
                    .flatMap(cartItem -> Common.getBitmapFromUrl(HomeActivity.this, cartItem,document))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(cartItem -> { // on Next
                        // Each time , we will add detail
                        PDFUtils.addNewItemWithLeftAndRight(document, cartItem.getFoodName(),
                                ("(0.0%)"),
                                titleFont,
                                orderNumberValueFont);

                        // Food Size and Addon
                        PDFUtils.addNewItemWithLeftAndRight(document,
                                "Size",
                                Common.formatSizeJsonToString(cartItem.getFoodSize()),
                                titleFont,
                                orderNumberValueFont);

                        PDFUtils.addNewItemWithLeftAndRight(document,
                                "Addon",
                                Common.formatAddonJsonToString(cartItem.getFoodAddon()),
                                titleFont,
                                orderNumberValueFont);

                        PDFUtils.addNewItemWithLeftAndRight(document,
                                new StringBuilder()
                        .append(cartItem.getFoodQuantity())
                        .append("*")
                        .append(cartItem.getFoodPrice() + cartItem.getFoodExtraPrice())
                        .toString(),
                                new StringBuilder()
                        .append(cartItem.getFoodQuantity() * (cartItem.getFoodExtraPrice() + cartItem.getFoodPrice()))
                        .toString(),
                                titleFont,
                                orderNumberValueFont);

                        PDFUtils.addLineSeparator(document);


                    }, throwable -> { // on error
                        dialog.dismiss();
                        Toast.makeText(this, throwable.getMessage(),Toast.LENGTH_SHORT).show();

                    }, () -> { // on complete
                        PDFUtils.addLinSpace(document);
                        PDFUtils.addLinSpace(document);

                        PDFUtils.addNewItemWithLeftAndRight(document, "Total",
                                new StringBuilder()
                        .append(orderModel.getTotalPayment()).toString(),
                                titleFont,
                                titleFont);

                        document.close();
                        dialog.dismiss();
                        Toast.makeText(this,"Success", Toast.LENGTH_SHORT).show();

                        printPDF();
                    });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void printPDF() {
        PrintManager printManager = (PrintManager)getSystemService(Context.PRINT_SERVICE);
        try{
            PrintDocumentAdapter printDocumentAdapter = new PdfDocumentAdapter(this, new StringBuilder(Common.getAppPath(this))
            .append(Common.FILE_PRINT).toString());
            printManager.print("Document", printDocumentAdapter, new PrintAttributes.Builder().build());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
