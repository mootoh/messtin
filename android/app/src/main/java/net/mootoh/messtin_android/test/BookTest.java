package net.mootoh.messtin_android.test;

import android.os.Bundle;
import android.test.AndroidTestCase;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by mootoh on 7/7/14.
 */
public class BookTest extends AndroidTestCase {
    // to wait for a callack from Google Api
    boolean connected = false;

    public void testGoogleApiClient() {
        final CountDownLatch signal = new CountDownLatch(1);
        assertNotNull(getContext());

        GoogleApiClient client = new GoogleApiClient.Builder(getContext())
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_FILE)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    connected = true;
                    signal.countDown();
                }

                @Override
                public void onConnectionSuspended(int i) {
                    fail("should not reach here");
                    signal.countDown();
                }
            })
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    fail("should not reach here");
                    signal.countDown();
                }
            })
            .build();
        try {
            signal.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("should not reach here");
            e.printStackTrace();
        }
        assertTrue(connected);
    }
}