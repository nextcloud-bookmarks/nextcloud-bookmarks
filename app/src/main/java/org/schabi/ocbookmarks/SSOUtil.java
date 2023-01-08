package org.schabi.ocbookmarks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.gson.GsonBuilder;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.TokenMismatchException;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

/**
 * This class keeps {@link NextcloudAPI} instance
 */
@WorkerThread
public class SSOUtil {

    private static final String TAG = SSOUtil.class.getSimpleName();

    private static NextcloudAPI mNextcloudAPI;

    public static NextcloudAPI getNextcloudAPI(@NonNull Context appContext, @NonNull SingleSignOnAccount ssoAccount) {
        if (mNextcloudAPI != null) {
            return mNextcloudAPI;
        }
        Log.v(TAG, "NextcloudRequest account: " + ssoAccount.name);
        final NextcloudAPI nextcloudAPI = new NextcloudAPI(appContext, ssoAccount, new GsonBuilder().create(), new NextcloudAPI.ApiConnectedListener() {
            @Override
            public void onConnected() {
                Log.i(TAG, "SSO API connected for " + ssoAccount);
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        });
        mNextcloudAPI = nextcloudAPI;
        return nextcloudAPI;
    }

    /**
     * Invalidates the cached {@link NextcloudAPI}
     * Should be called in case a {@link TokenMismatchException} occurs.
     */
    public static void invalidateAPICache() {
        Log.v(TAG, "Invalidating API cache");
        if (mNextcloudAPI != null) {
            mNextcloudAPI.stop();
        }
        mNextcloudAPI = null;
    }
}
