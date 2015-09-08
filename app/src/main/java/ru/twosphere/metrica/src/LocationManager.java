package ru.twosphere.metrica.src;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Александр on 27.08.2015.
 */
public class LocationManager
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final String TAG = "location-updates-sample";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 60 * 1000 * 2;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    protected OnLocationReceive mLocationReceiver;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Single instance for LocationManager
     */
    protected static LocationManager mInstance;

    public static LocationManager getInstance() {
        if (mInstance == null) {
            mInstance = new LocationManager();
        }
        return mInstance;
    }

    private Context mContext;
    
    public void init(Context context) {
        mContext = context;
        mRequestingLocationUpdates = false;
        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        try {
            Log.i(TAG, "Building GoogleApiClient");
            mGoogleApiClient = new GoogleApiClient.Builder(this.mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            createLocationRequest();
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdates() {
        try {
            if (!mRequestingLocationUpdates) {
                mRequestingLocationUpdates = true;
                if (mGoogleApiClient.isConnected()) {
                    startLocationUpdates();
                }
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdates() {
        try {
            if (mRequestingLocationUpdates) {
                mRequestingLocationUpdates = false;
                stopLocationUpdates();
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    public void setOnLocationListener(OnLocationReceive locationReceiver) {
        mLocationReceiver = locationReceiver;
    }

    public interface OnLocationReceive {

        void onReceive(String latitude, String longitude);
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateReciever() {
        if (mCurrentLocation != null && mLocationReceiver != null) {
            mLocationReceiver.onReceive(
                String.valueOf(mCurrentLocation.getLatitude()),
                String.valueOf(mCurrentLocation.getLongitude())
            );
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    public void start() {
        try {
            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    public void onResume() {
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        try {
            if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    public void onPause() {
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        try {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    public void stop() {
        try {
            mGoogleApiClient.disconnect();
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        try {
            mCurrentLocation = location;
            updateReciever();
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");

        try {
            if (mCurrentLocation == null) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                updateReciever();
            }
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        try {
            mGoogleApiClient.connect();
        } catch (Exception err) {
            Log.d(TAG, "Error with location manager");
            err.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
}
