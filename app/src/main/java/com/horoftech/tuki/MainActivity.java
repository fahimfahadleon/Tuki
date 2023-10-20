package com.horoftech.tuki;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.horoftech.tuki.databinding.ActivityMainBinding;
import com.horoftech.tuki.databinding.ShowProfileOfPartnerBinding;

import org.json.JSONObject;

import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    MainViewModel viewModel;

    FirebaseDatabase database;
    DatabaseReference reference;
    GoogleSignInClient client;
    FirebaseAuth  auth;

    User myData;
    User partnerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        viewModel = new MainViewModel();
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();


        client = GoogleSignIn.getClient(this,options);
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users");
        binding.loginbutton.setOnClickListener(v -> someActivityResultLauncher.launch(client.getSignInIntent()));
        FirebaseUser user = auth.getCurrentUser();
        if(user == null){
            binding.loginbutton.setVisibility(View.VISIBLE);
            binding.invisibleLayout.setVisibility(View.GONE);
        }else {
            binding.loginbutton.setVisibility(View.GONE);

            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    String uid;
                    if(user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()){
                        uid = user.getEmail();
                    }else {
                        uid = user.getPhoneNumber();
                    }

                    assert uid != null;
                    if(snapshot.hasChild(Utils.md5(uid))){
                        myData = snapshot.child(Utils.md5(uid)).getValue(User.class);
                        Glide.with(MainActivity.this).load(myData.getProfile()).circleCrop().into(binding.myProfilePicture);

                        assert myData != null;
                        if(myData.getPartnerID() != null){
                            partnerData = snapshot.child(Utils.md5(myData.getPartnerID())).getValue(User.class);
                            binding.invisibleLayout.setVisibility(View.GONE);
                            Glide.with(MainActivity.this).load(partnerData.getProfile()).circleCrop().into(binding.partnerProfilePicture);
                            binding.invisibleLayout2.setVisibility(View.VISIBLE);
                        }else {
                            binding.invisibleLayout.setVisibility(View.VISIBLE);
                            binding.lovepie.setVisibility(View.GONE);
                            binding.partnerProfilePicture.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


//            auth.signOut();
            // TODO: 10/18/2023
            //if already has a partner don't show
        }

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
            new SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE).setConfirmButton("Continue", sweetAlertDialog -> {
                askNotificationPermission();
                sweetAlertDialog.dismiss();
            }).setCancelButton("Cancel", sweetAlertDialog -> {
                sweetAlertDialog.dismiss();
                finish();
            }).setContentText("Application Needs Notification Permission To Perform Correctly")
                    .setTitleText("Permission")
                    .show();
        }


        binding.sendMessageButton.setOnClickListener(v -> {
            String s = Objects.requireNonNull(binding.textinputlayout2.getEditText()).getText().toString();
            if(!s.isEmpty()){
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("data",s);
                    SendNotification.sendNotification(partnerData.getToken(),jsonObject,MainActivity.this);
                }catch (Exception e){
                    //ignored
                }

            }
        });

        binding.searchButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.textinputlayout.getEditText()).getText().toString();
            if(!email.isEmpty()){
                if(email.equals(myData.getId())){
                    Toast.makeText(MainActivity.this, "Not found!", Toast.LENGTH_SHORT).show();
                    return;
                }


                showLoading();
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        dismissLoading();
                        if(snapshot.hasChild(Utils.md5(email))){
                            User user1 = snapshot.child(Utils.md5(email)).getValue(User.class);
                            showCustomDialog(myData,user1);
                        }else {
                            showError("Error","User not found!");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dismissLoading();
                    }
                });
            }
        });



    }

    AlertDialog dialog;
    void showCustomDialog(User myData,User partnerData){
        runOnUiThread(() -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            ShowProfileOfPartnerBinding showProfileBinding = ShowProfileOfPartnerBinding.inflate(getLayoutInflater());
            Glide.with(MainActivity.this).load(myData.getProfile()).circleCrop().into(showProfileBinding.firstHeart);
            Glide.with(MainActivity.this).load(partnerData.getProfile()).circleCrop().into(showProfileBinding.secondHeart);

            showProfileBinding.pair.setOnClickListener(v -> {
                showProfileBinding.pair.setVisibility(View.GONE);
                reference.child(Utils.md5(myData.getId())).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if(user!=null){
                            user.setPartnerID(partnerData.getId());
                            reference.child(Utils.md5(myData.getId())).setValue(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                reference.child(Utils.md5(partnerData.getId())).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if(user!=null){
                            user.setPartnerID(myData.getId());
                            reference.child(Utils.md5(partnerData.getId())).setValue(user);
                            runOnUiThread(() -> {
                                if(dialog!=null && dialog.isShowing()){
                                    dialog.dismiss();
                                    showSuccess("Successful","Pair with "+partnerData.getName()+" is successful.");
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            });

            builder.setView(showProfileBinding.getRoot());
            dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#00000000")));
            dialog.show();

        });

    }

    SweetAlertDialog loadingDialog;
    void showLoading(){
        runOnUiThread(()->{
            loadingDialog = new SweetAlertDialog(this,SweetAlertDialog.PROGRESS_TYPE);
            loadingDialog.show();
        });
    }

    void dismissLoading(){
        runOnUiThread(() -> {
            if(loadingDialog!=null && loadingDialog.isShowing()){
                loadingDialog.dismiss();
            }
        });
    }


    void showError(String title,String content){
        runOnUiThread(() -> {
            new SweetAlertDialog(this,SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(title)
                    .setContentText(content)
                    .setCancelButton("Cancel", Dialog::dismiss)
                    .show();
        });
    }


    void showSuccess(String title,String content){
        runOnUiThread(() -> {
            SweetAlertDialog dialog1 =  new SweetAlertDialog(MainActivity.this,SweetAlertDialog.SUCCESS_TYPE);
            dialog1.setTitleText(title).setContentText(content).show();
            new Handler(Looper.getMainLooper()).postDelayed(dialog1::dismiss,4000);
        });

    }


    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
//                        doSomeOperations();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuth(account.getIdToken());
                    } catch (ApiException e) {
                        throw new RuntimeException(e);

                    }
                }
            });
    boolean isPreviouslyLoggedIn = false;
    void firebaseAuth(String token){
        AuthCredential credential = GoogleAuthProvider.getCredential(token,null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if(task.isComplete()){
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if(firebaseUser == null){
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Something went Wrong!", Toast.LENGTH_SHORT).show());
                    return;
                }

                User user = new User();

                String phoneNumber = firebaseUser.getPhoneNumber();
                String uid;
                if(phoneNumber == null || phoneNumber.isEmpty()){
                    uid = firebaseUser.getEmail();
                }else {
                    uid = firebaseUser.getPhoneNumber();
                }

                assert uid != null;
                Log.e("uid",uid);

                user.setId(uid);
                user.setName(firebaseUser.getDisplayName());
                user.setProfile(firebaseUser.getPhotoUrl() == null?"null":firebaseUser.getPhotoUrl().toString());
                user.setToken(FirebaseMessagingService.getToken(MainActivity.this));
                reference.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(Utils.md5(uid))){
                            User user1 = snapshot.getValue(User.class);
                            assert user1 != null;
                            isPreviouslyLoggedIn = user1.getPartnerID() != null;

                            user1.setToken(FirebaseMessagingService.getToken(MainActivity.this));
                            reference.child(Utils.md5(uid)).setValue(user1);

                        }else {
                            reference.child(Utils.md5(uid)).setValue(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


                runOnUiThread(() -> {
                    binding.loginbutton.setVisibility(View.GONE);
                    if(!isPreviouslyLoggedIn){
                        binding.invisibleLayout.setVisibility(View.VISIBLE);
                    }else {
                        binding.invisibleLayout.setVisibility(View.GONE);
                    }
                });

            }else {
                Log.e("error","task not complete"+task.getResult().toString());
            }
        });

    }
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "not granted!", Toast.LENGTH_SHORT).show();
                }
            });

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
                Log.e("Success","we can post notification now");
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                Log.e("failed","permission prosponed");
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

}