package net.mootoh.messtin_android.app;

import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

/**
 * Created by mootoh on 5/12/14.
 */
public class GDriveHelper {
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    static GDriveHelper s_instance;

    final private GoogleApiClient client_;

    static public GDriveHelper getInstance() {
        return s_instance;
    }

    static public GDriveHelper createInstance(final Context context, GoogleApiClient.ConnectionCallbacks callbacks) {
        s_instance = new GDriveHelper(context, callbacks);
        return s_instance;
    }

    private GDriveHelper(final Context context, GoogleApiClient.ConnectionCallbacks callbacks) {
        client_ = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(callbacks)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("@@@", "onConnetionFailed: " + connectionResult.toString());
                        if (connectionResult.hasResolution()) {
                            try {
                                connectionResult.startResolutionForResult((android.app.Activity)context, RESOLVE_CONNECTION_REQUEST_CODE);
                            } catch (IntentSender.SendIntentException e) {
                                // Unable to resolve, message user appopriately
                            }
                        } else {
                            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), (android.app.Activity)context, 0).show();
                        }
                    }
                })
                .build();
    }

    public void connect() {
        client_.connect();
    }

    public void disconnect() {
        client_.disconnect();
    }

    public GoogleApiClient getClient() {
        return client_;
    }
}
