package com.horoftech.tuki;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendNotification {

    public static void sendNotification(String token, JSONObject body, Context context){

        Log.e("SendingNotification",body.toString());
        StrictMode.ThreadPolicy builder = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(builder);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("to",token);
            jsonObject.put("data",body);
            jsonObject.put("priority","high");

            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("priority","high");
            jsonObject.put("android",jsonObject1);

            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("apns-priority","10");
            JSONObject jsonObject3 = new JSONObject();
            jsonObject3.put("headers",jsonObject2);
            jsonObject.put("apns",jsonObject3);

            JSONObject jsonObject4 = new JSONObject();
            jsonObject4.put("Urgency","high");

            JSONObject jsonObject5 = new JSONObject();
            jsonObject5.put("headers",jsonObject4);
            jsonObject.put("webpush",jsonObject5);
            OkHttpClient client = getUnsafeOkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");

            RequestBody requestBody = RequestBody.create( jsonObject.toString(),mediaType);

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .method("POST", requestBody)
                    .addHeader("Authorization","key="+context.getResources().getString(R.string.firebase_server_key))
                    .addHeader("Content-Type", "application/json")
                    .build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("failed",e.toString());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String s = response.body() == null?"null":response.body().string();
                    Log.e("successNotification",s);


                }
            });




        }catch (Exception e){
            e.printStackTrace();
        }



    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            @SuppressLint("CustomX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
