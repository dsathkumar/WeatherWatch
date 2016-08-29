package org.example.android.weatherwatch;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class GetCurrentLocation extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    public LocationListener mLocationListener;
    private Double latitude;
    private Double longitude;
    public static final String TAG = MainActivity.class.getSimpleName();
    private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setUpGoogleApiClient();
        createLocationRequest();
    }

    private void setUpGoogleApiClient() {
        Log.d(TAG, "New Activity Context : " + this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected  void onStart(){
        super.onStart();
        if(mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Log.i(TAG, "GoogleAPIClient : "+mGoogleApiClient+" ;isconnected : "+mGoogleApiClient.isConnected());

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d(TAG,"mlastlocation lat= "+latitude+", long= "+longitude);

            Intent returnIntent = new Intent();
            returnIntent.putExtra("latitude", latitude);
            returnIntent.putExtra("longitude", longitude);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();

        }else {
            Log.i(TAG, "mLastLocation is null");
            Toast.makeText(this,"Please enable GPS to get your current location !",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Location services failed.");
    }
}
