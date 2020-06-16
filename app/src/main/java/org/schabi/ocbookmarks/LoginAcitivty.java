package org.schabi.ocbookmarks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;
import org.schabi.ocbookmarks.REST.RequestException;

import java.io.File;

public class LoginAcitivty extends AppCompatActivity {

    // reply info
    private static final int OK = 0;
    private static final int CONNECTION_FAIL = 1;
    private static final int HOST_NOT_FOUND= 2;
    private static final int FILE_NOT_FOUND = 3;
    private static final int TIME_OUT = 4;
    private boolean mPasswordVisible = false;

    LoginData loginData = new LoginData();

    EditText urlInput;
    EditText userInput;
    EditText passwordInput;
    Button connectButton;
    Button ssoButton;
    ProgressBar progressBar;
    TextView errorView;
    ImageView mImageViewShowPwd;

    SharedPreferences sharedPrefs;

    TestLoginTask testLoginTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_acitivty);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setTitle(getString(R.string.oc_bookmark_login));
        urlInput = (EditText) findViewById(R.id.urlInput);
        userInput = (EditText) findViewById(R.id.userInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);
        connectButton = (Button) findViewById(R.id.connectButton);
        ssoButton= (Button) findViewById(R.id.ssoButton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorView = (TextView) findViewById(R.id.loginErrorView);
        mImageViewShowPwd= (ImageView) findViewById(R.id.imgView_ShowPassword);

        errorView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        mImageViewShowPwd.setOnClickListener(ImgViewShowPasswordListener);
        sharedPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        urlInput.setText(sharedPrefs.getString(getString(R.string.login_url), ""));
        userInput.setText(sharedPrefs.getString(getString(R.string.login_user), ""));
        passwordInput.setText(sharedPrefs.getString(getString(R.string.login_pwd), ""));

        if(!passwordInput.getText().toString().isEmpty()) {
            mImageViewShowPwd.setVisibility(View.GONE);
        }
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginData.url = fixUrl(urlInput.getText().toString());
                loginData.user = userInput.getText().toString();
                loginData.password = passwordInput.getText().toString();
                loginData.ssologin = false;
                loginData.token = "";
                urlInput.setText(loginData.url);

                testLoginTask = new TestLoginTask();
                testLoginTask.execute(loginData);
                progressBar.setVisibility(View.VISIBLE);
                connectButton.setVisibility(View.INVISIBLE);
            }
        });
        ssoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AccountImporter.pickNewAccount(LoginAcitivty.this);
                }
                catch (NextcloudFilesAppNotInstalledException e)
                {
                    UiExceptionManager.showDialogForException(LoginAcitivty.this, e);
                } catch (AndroidGetAccountsPermissionNotGranted e)
                { UiExceptionManager.showDialogForException(LoginAcitivty.this, e); }
            }
        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this, (account) -> {
                SingleAccountHelper.setCurrentAccount(this,account.name);
                loginData.url=account.url;
                loginData.user=account.userId;
                loginData.ssologin = true;
                loginData.token = account.token;;
                loginData.password="";
//                storeLogin(loginData);
                finish();
                testLoginTask = new TestLoginTask();
                testLoginTask.execute(loginData);
                progressBar.setVisibility(View.VISIBLE);
                connectButton.setVisibility(View.INVISIBLE);
                ssoButton.setVisibility(View.INVISIBLE);
            });
        } catch (AccountImportCancelledException e) {
            Log.i("log", "Account import has been canceled.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        AccountImporter.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    private View.OnClickListener ImgViewShowPasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPasswordVisible = !mPasswordVisible;

            if(mPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        }
    };
    private String fixUrl(String rawUrl) {
        if(!rawUrl.startsWith("http")) {
            rawUrl = "https://" + rawUrl;
        }
        if(rawUrl.endsWith("/")) {
            rawUrl = rawUrl.substring(0, rawUrl.length()-1);
        }
        return rawUrl;
    }

    @SuppressLint("ResourceType")
    private void storeLogin(LoginData loginData) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getString(R.string.login_url), loginData.url);
        editor.putString(getString(R.string.login_user), loginData.user);
        editor.putBoolean(getString(R.string.ssologin), loginData.ssologin);
        if (loginData.ssologin){
            editor.putString(getString(R.string.login_token), loginData.token);
        }
        else
        {
            editor.putString(getString(R.string.login_pwd), loginData.password);
        }

        editor.apply();
    }

    private void deleteFiles() {
        // delete files from a previous login
        File homeDir = getApplicationContext().getFilesDir();
        for(File file : homeDir.listFiles()) {
            if(file.toString().contains(".png") ||
                    file.toString().contains(".noicon") ||
                    file.toString().contains(".json")) {
                file.delete();
            }
        }
    }

    private class TestLoginTask extends AsyncTask<LoginData, Void, Integer> {
        protected Integer doInBackground(LoginData... loginDatas) {
            LoginData loginData = loginDatas[0];
            OCBookmarksRestConnector connector =
                    new OCBookmarksRestConnector(loginData.url, loginData.user, loginData.password,loginData.token, loginData.ssologin);
            try {
                connector.getBookmarks();
                return OK;
            } catch (RequestException re) {
                if(BuildConfig.DEBUG) {
                    re.printStackTrace();
                }

                if(re.getMessage().contains("FileNotFound")) {
                    return FILE_NOT_FOUND;
                }
                if(re.getMessage().contains("UnknownHost")) {
                    return HOST_NOT_FOUND;
                }
                if(re.getMessage().contains("SocketTimeout")) {
                    return TIME_OUT;
                }
                return CONNECTION_FAIL;
            } catch (Exception e) {
                return CONNECTION_FAIL;
            }
        }

        protected void onPostExecute(Integer result) {
            connectButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            switch (result) {
                case OK:
                    storeLogin(loginData);
                    deleteFiles();
                    finish();
                    break;
                case CONNECTION_FAIL:
                    errorView.setText(getString(R.string.connection_failed_login));
                    errorView.setVisibility(View.VISIBLE);
                    break;
                case HOST_NOT_FOUND:
                    errorView.setText(getString(R.string.login_host_not_found));
                    errorView.setVisibility(View.VISIBLE);
                    break;
                case FILE_NOT_FOUND:
                    errorView.setText(getString(R.string.login_failed));
                    errorView.setVisibility(View.VISIBLE);
                    break;
                case TIME_OUT:
                    errorView.setText(getString(R.string.login_timeout));
                    errorView.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }
}
