package org.example.android.weatherwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;
    private CurrentLocation mCurrentLocation;
    private double latitude;
    private double longitude;
    public SharedPreferences sharedPref;

    @InjectView(R.id.timeLable)
    TextView mTimeLable;
    @InjectView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue)
    TextView mHumidityValue;
    @InjectView(R.id.precipValue)
    TextView mPrecipValue;
    @InjectView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @InjectView(R.id.iconImageView)
    ImageView mIconImageView;
    @InjectView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel)
    TextView mLocationLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        ScaleAnimation anim = new ScaleAnimation(0,1,0,1);
        anim.setFillBefore(true);
        anim.setFillAfter(true);
        anim.setFillEnabled(true);
        anim.setDuration(800);
        anim.setInterpolator(new OvershootInterpolator());
        fab.startAnimation(anim);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ActionButtonAnimation.class);
                startActivityForResult(intent,2);
            }
        });

        ButterKnife.inject(this);
        mProgressBar.setVisibility(View.INVISIBLE);
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callCurrentLocationClass();
            }
        });

        callCurrentLocationClass();
        Log.d(TAG, "Main UI code running !!");
    }

    private void callCurrentLocationClass() {
        if(checkPlayServices()) {
            Intent intent = new Intent(this, GetCurrentLocation.class);
            startActivityForResult(intent, 1);
        }else
            Toast.makeText(this,"Please install PlayServices",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                latitude = data.getDoubleExtra("latitude",0.0);
                longitude = data.getDoubleExtra("longitude",0.0);
                Log.d(TAG,"onactivityresult1() lat= "+latitude+"long = "+longitude);
                getForecast();
            }
        }
        if(requestCode == 2){
            if(resultCode == Activity.RESULT_OK){
                latitude = data.getDoubleExtra("latitude",0.0);
                longitude = data.getDoubleExtra("longitude",0.0);
                Log.d(TAG,"onactivityresult2() lat= "+latitude+"long = "+longitude);
                getForecast();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        return false;
    }

    private void getForecast() {
        String apiKey = "590e9dc8390b743f0c3c45db4b3a801a";
        String MAP ="";

        Log.d(TAG, "Returned lat & long : " + latitude + "," + longitude);

        if(latitude == 0.00 && longitude ==0.00){
            Toast.makeText(this, "Could not find location, try again !"
                    , Toast.LENGTH_SHORT).show();
        }else{

        //Forecast API provides weather updates for provided latitude and longitude
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;
        Log.d(MAP, "Generated URL = "+forecastUrl);

        if (isnetworkavailable()) {
            toggleRefresh();
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                            Log.d(TAG,"OkHttp forecast connection failed");
                            alertUserAboutError();
                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleRefresh();
                            }
                        });

                        String jsonData = response.body().string();
                        Log.v(TAG,"Forecast api response:"+response.isSuccessful());
//                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else
                            Log.d(TAG, "OkHttp forecast response failed ");
//                            alertUserAboutError();
                    } catch (IOException e) {
                        Log.e(TAG, "Exception Caught", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception Caught", e);
                    }
                }
            });
            getCityState();
        } else {
            Toast.makeText(this, R.string.network_unavailable_message,
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    //Returns city state details using Google maps API
    private void getCityState() {
        String locationUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng="
                + latitude + "," + longitude + "&sensor=false";
        Log.d(TAG, "Generated URL = " + locationUrl);

        if (isnetworkavailable()) {
            toggleRefresh();
            OkHttpClient okHttpClient_location = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(locationUrl)
                    .build();
            Call call = okHttpClient_location.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                            Log.d(TAG, "OkHttp google maps connection failed");
                            alertUserAboutError();
                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleRefresh();
                            }
                        });

                        String jsonData = response.body().string();
//                        Log.v(TAG, jsonData);
                        Log.v(TAG, "Response reply : " + response.isSuccessful());
                        if (response.isSuccessful()) {
                            mCurrentLocation = getCurrentLocationDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateLocationDisplay();
                                }
                            });
                        } else
//                            Log.d(TAG, "OkHttp google maps response failed");
                            alertUserAboutError();

                    } catch (IOException e) {
                        Log.e(TAG, "Exception Caught", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception Caught", e);
                    }
                }
            });

        }else {
            Toast.makeText(this, R.string.network_unavailable_message,
                    Toast.LENGTH_LONG).show();
        }
    }

    //Updates Location name in UI
    private void updateLocationDisplay() {
        mLocationLabel.setText(mCurrentLocation.getCity() + ", " + mCurrentLocation.getState());
    }

    //Returns object for current location variables
    private CurrentLocation getCurrentLocationDetails(String LocationJsonData) throws JSONException {
        JSONObject currentLocationJSONObject = new JSONObject(LocationJsonData);
        JSONArray result = currentLocationJSONObject.getJSONArray("results");
        JSONObject jsonObject = result.getJSONObject(0);
        JSONArray addressarray = jsonObject.getJSONArray("address_components");
        JSONObject city = addressarray.getJSONObject(2);
        JSONObject state = addressarray.getJSONObject(4);

        CurrentLocation currentLocation = new CurrentLocation();
        currentLocation.setCity(city.getString("long_name"));
        currentLocation.setState(state.getString("short_name"));
        Log.d("TAG", "current location:" + city.getString("long_name") + "," + state.getString("short_name"));
        return currentLocation;
    }

    //Checks for availability of play services
    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status != ConnectionResult.SUCCESS) {
            return false;
        }else
            return true;
    }

    //Refresh icon visibility updater
    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE){
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }else{
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    //Updates views in UI
    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mTimeLable.setText("At " + mCurrentWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    //Returns an object for current weather variables
    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "JSON Timezone: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        return currentWeather;
    }

    //checks for network availability
    private boolean isnetworkavailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected())
            isAvailable = true;
        return isAvailable;
    }

    //On error alert
    private void alertUserAboutError() {
        AlertDialogueFragment dialog = new AlertDialogueFragment();
        dialog.show(getFragmentManager(),"error_dialogue");
    }
}