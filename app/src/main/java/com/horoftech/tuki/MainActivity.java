package com.horoftech.tuki;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

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

import java.util.HashMap;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    MainViewModel viewModel;

    FirebaseDatabase database;
    DatabaseReference reference;
    GoogleSignInClient client;
    FirebaseAuth  auth;

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
            binding.textinputlayout.setVisibility(View.GONE);
        }else {
            binding.loginbutton.setVisibility(View.GONE);

            binding.textinputlayout.setVisibility(View.VISIBLE);

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

    void firebaseAuth(String token){
        AuthCredential credential = GoogleAuthProvider.getCredential(token,null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if(task.isComplete()){
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if(firebaseUser == null){
                    Toast.makeText(this, "Something went Wrong!", Toast.LENGTH_SHORT).show();
                    return;
                }
                HashMap<String,User> map = new HashMap<>();
                User user = new User();
                user.setId(firebaseUser.getPhoneNumber() == null?firebaseUser.getEmail():firebaseUser.getPhoneNumber());
                user.setName(firebaseUser.getDisplayName());
                user.setProfile(firebaseUser.getPhotoUrl() == null?"null":firebaseUser.getPhotoUrl().toString());
                user.setToken(FirebaseMessagingService.getToken(MainActivity.this));
                map.put(firebaseUser.getPhoneNumber() == null?firebaseUser.getEmail():firebaseUser.getPhoneNumber(),user);
                reference.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(firebaseUser.getPhoneNumber() == null?firebaseUser.getEmail():firebaseUser.getPhoneNumber())){
                            User user1 = snapshot.getValue(User.class);
                            if(user1 == null){
                                reference.setValue(map);
                            }else {
                                user1.setToken(FirebaseMessagingService.getToken(MainActivity.this));
                                HashMap<String,User> userHashMap = new HashMap<>();
                                userHashMap.put(firebaseUser.getPhoneNumber() == null?firebaseUser.getEmail():firebaseUser.getPhoneNumber(),user);
                                reference.setValue(userHashMap);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.loginbutton.setVisibility(View.GONE);


                    }
                });

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