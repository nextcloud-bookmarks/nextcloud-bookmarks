package org.schabi.ocbookmarks;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nextcloud.android.sso.BuildConfig;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;
import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.api.LoginData;
import org.schabi.ocbookmarks.api.SSOUtil;
import org.schabi.ocbookmarks.listener.BookmarkListener;

public class AddBookmarkActivity extends AppCompatActivity implements BookmarkListener {

    LoginData loginData = new LoginData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bookmark_activity);
        setTitle("");

        Intent intent = getIntent();
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        EditBookmarkDialog bookmarkDialog = new EditBookmarkDialog();
        bookmarkDialog.newBookmark(title, url);
        AlertDialog dialog = bookmarkDialog.getDialog(this, null, this);
        dialog.show();

        dialog.setOnDismissListener(dialog1 -> AddBookmarkActivity.this.finish());
    }


    private void update(Bookmark bookmark) {
        AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                NextcloudAPI nextcloudAPI = null;
                try {
                    SingleSignOnAccount ssoa =  SingleAccountHelper.getCurrentSingleSignOnAccount(AddBookmarkActivity.this.getApplicationContext());
                    nextcloudAPI = SSOUtil.getNextcloudAPI(AddBookmarkActivity.this, ssoa);
                } catch (NextcloudFilesAppAccountNotFoundException e) {
                    Toast.makeText(AddBookmarkActivity.this,
                            R.string.nextcloud_files_app_account_not_found_message,
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return null;
                } catch ( NoCurrentAccountSelectedException e) {
                    Toast.makeText(AddBookmarkActivity.this,
                            R.string.no_current_account_selected_exception_message,
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return null;
                }

                OCBookmarksRestConnector connector = new OCBookmarksRestConnector(nextcloudAPI);
                try {
                    connector.addBookmark(bookmark);
                } catch (Exception e) {
                    if(BuildConfig.DEBUG) e.printStackTrace();
                    return getString(R.string.could_not_add_bookmark);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if(result != null) {
                    Toast.makeText(AddBookmarkActivity.this,
                            result,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AddBookmarkActivity.this,
                            R.string.bookmark_saved,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    @Override
    public void bookmarkChanged(Bookmark bookmark) {
        update(bookmark);
    }

    @Override
    public void deleteBookmark(Bookmark bookmark) {}
}
