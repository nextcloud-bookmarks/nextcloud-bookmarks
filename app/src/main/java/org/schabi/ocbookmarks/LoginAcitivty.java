package org.schabi.ocbookmarks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.BuildConfig;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;
import org.schabi.ocbookmarks.REST.RequestException;
import org.schabi.ocbookmarks.api.LoginData;
import org.schabi.ocbookmarks.api.SSOUtil;

import java.io.File;

public class LoginAcitivty extends AppCompatActivity {

    // reply info
    private static final int OK = 0;
    private static final int CONNECTION_FAIL = 1;
    private static final int HOST_NOT_FOUND = 2;
    private static final int FILE_NOT_FOUND = 3;
    private static final int TIME_OUT = 4;
    private static final int SSO_FAILED = 5;
    private boolean mPasswordVisible = false;

    LoginData loginData = new LoginData();

    Button ssoButton;
    TextView errorView;

    SharedPreferences sharedPrefs;

    TestLoginTask testLoginTask;

    String TAG = this.getClass().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_acitivty);

        ssoButton = findViewById(R.id.ssoButton);
        errorView = findViewById(R.id.loginErrorView);
        errorView.setVisibility(View.VISIBLE);

        sharedPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        ssoButton.setOnClickListener(v -> {
            try {
                AccountImporter.pickNewAccount(LoginAcitivty.this);
            } catch (NextcloudFilesAppNotInstalledException | AndroidGetAccountsPermissionNotGranted e) {
                UiExceptionManager.showDialogForException(LoginAcitivty.this, e);
            }
        });
        checkIfSSOIsDone();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfSSOIsDone();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this, (account) -> {

                Log.d(TAG, "Login Attempt: "+account.name);
                SingleAccountHelper.setCurrentAccount(this.getApplicationContext(), account.name);
                loginData.ssologin = true;
                checkIfSSOIsDone();
            });
        } catch (AccountImportCancelledException e) {
            Log.i("log", "Account import has been canceled.");
        }
    }


    private void checkIfSSOIsDone() {
        try {
            SingleAccountHelper.getCurrentSingleSignOnAccount(this.getApplicationContext());
            // If we pass here, we do have an account set and can continue.
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("ResourceType")
    private void storeLogin(LoginData loginData) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(getString(R.string.ssologin), loginData.ssologin);
        editor.apply();
    }

    private void deleteFiles() {
        // delete files from a previous login
        File homeDir = getApplicationContext().getFilesDir();
        for (File file : homeDir.listFiles()) {
            if (file.toString().contains(".png") ||
                    file.toString().contains(".noicon") ||
                    file.toString().contains(".json")) {
                file.delete();
            }
        }
    }

    private class TestLoginTask extends AsyncTask<LoginData, Void, Integer> {
        protected Integer doInBackground(LoginData... loginDatas) {
            NextcloudAPI nextcloudAPI = null;
            if (loginData.ssologin) {
                try {
                    nextcloudAPI = SSOUtil.getNextcloudAPI(LoginAcitivty.this, SingleAccountHelper.getCurrentSingleSignOnAccount(LoginAcitivty.this));
                } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                    e.printStackTrace();
                    return SSO_FAILED;
                }
            }

            OCBookmarksRestConnector connector = new OCBookmarksRestConnector(nextcloudAPI);
            try {
                connector.getBookmarks();
                return OK;
            } catch (RequestException re) {
                if (BuildConfig.DEBUG) {
                    re.printStackTrace();
                }

                if (re.getMessage().contains("FileNotFound")) {
                    return FILE_NOT_FOUND;
                }
                if (re.getMessage().contains("UnknownHost")) {
                    return HOST_NOT_FOUND;
                }
                if (re.getMessage().contains("SocketTimeout")) {
                    return TIME_OUT;
                }
                return CONNECTION_FAIL;
            } catch (Exception e) {
                return CONNECTION_FAIL;
            }
        }

        protected void onPostExecute(Integer result) {
            ssoButton.setVisibility(View.VISIBLE);

            if (result == OK) {
                storeLogin(loginData);
                deleteFiles();
                finish();
            } else {
                SingleAccountHelper.setCurrentAccount(LoginAcitivty.this, null);
                errorView.setVisibility(View.VISIBLE);
                switch (result) {
                    case CONNECTION_FAIL:
                        errorView.setText(getString(R.string.connection_failed_login));
                        break;
                    case HOST_NOT_FOUND:
                        errorView.setText(getString(R.string.login_host_not_found));
                        break;
                    case FILE_NOT_FOUND:
                        errorView.setText(getString(R.string.login_failed));
                        break;
                    case TIME_OUT:
                        errorView.setText(getString(R.string.login_timeout));
                        break;
                    case SSO_FAILED:
                        errorView.setText(getString(R.string.sso_failed));
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
