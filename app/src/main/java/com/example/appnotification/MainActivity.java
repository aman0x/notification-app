package com.example.appnotification;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.appnotification.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.google.firebase.messaging.FirebaseMessaging;


public class MainActivity extends AppCompatActivity {
    Button b1;
    private RequestQueue mRequestQueue;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private TextView responseResult;
    private String token;

    private ProgressBar loadingPB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        handleSSLHandshake();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intentBackgroundService = new Intent(this,FirebasePushNotificationClass.class);
        startService(intentBackgroundService);

        b1= (Button)findViewById(R.id.notificationbtn);
        EditText myEditText = findViewById(R.id.myEditText);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel nc=new NotificationChannel("01","Xyz", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(nc);
        }
        b1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void  onClick(View v){
                NotificationCompat.Builder ncb =new NotificationCompat.Builder(getApplicationContext(),"01");
                ncb.setSmallIcon(R.drawable.ic_android_black_24dp);
                ncb.setContentTitle("Notification");
                ncb.setContentText("Feedback is essential for you to improve the user experience.");
                NotificationManagerCompat nmc=NotificationManagerCompat.from(MainActivity.this);
                nmc.notify('1',ncb.build());
            }
        });

        mRequestQueue = Volley.newRequestQueue(this);

        // Find button in layout
        Button buttonPostDeviceId = findViewById(R.id.button_post_device_id);
        loadingPB = findViewById(R.id.idLoadingPB);
        responseResult = findViewById(R.id.response);
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {

            if (task.isSuccessful()){
                token = task.getResult();

                Log.d("FCM","FCM token:\n"+token);
            }else{
                Log.e("FCM", "Failed");
            }
        });


        // Set button onClickListener
        buttonPostDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Log.d("FCM","FCM token test:\n"+token);
                loadingPB.setVisibility(View.VISIBLE);
                // Get device ID
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String text = myEditText.getText().toString();
                // Define POST parameters
                Map<String, String> params = new HashMap<>();
                params.put("id", deviceId);
                params.put("name",text);
                params.put("registration_id", token);
                params.put("device_id", deviceId);
                params.put("active", "true");
                params.put("date_created", String.valueOf(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")));
                params.put("type", "android");

                Gson gson = new Gson();
                Type gsonType = new TypeToken<HashMap>(){}.getType();
                String gsonString = gson.toJson(params,gsonType);
                // Define POST request URL
                String url = "https://finlogix.co.in:8081/devices/";

                // Create Volley StringRequest object
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Handle successful response
                                loadingPB.setVisibility(View.GONE);
                                myEditText.setText("");
                                Toast.makeText(getApplicationContext(), "Device ID posted successfully", Toast.LENGTH_SHORT).show();
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingPB.setVisibility(View.GONE);
                        responseResult.setText("This Device is already Registered");
                        // Handle error response
                        Toast.makeText(getApplicationContext(), "Error posting device ID"+ error, Toast.LENGTH_SHORT).show();
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        return params;
                    }
                };

                // Add StringRequest object to Volley request queue
                stringRequest.setRetryPolicy(new RetryPolicy() {
                    @Override
                    public int getCurrentTimeout() {
                        return 30000;
                    }

                    @Override
                    public int getCurrentRetryCount() {
                        return 30000;
                    }

                    @Override
                    public void retry(VolleyError error) throws VolleyError {

                    }
                });

                mRequestQueue.add(stringRequest);
            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }
}