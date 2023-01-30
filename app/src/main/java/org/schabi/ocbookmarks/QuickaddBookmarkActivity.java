package org.schabi.ocbookmarks;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nextcloud.android.sso.BuildConfig;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.schabi.ocbookmarks.REST.Bookmark;
import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;

public class QuickaddBookmarkActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bookmark_activity);
        setTitle("");

        Intent intent = getIntent();
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        Bookmark bookmark = Bookmark.emptyInstance();
        bookmark.setTitle(title);
        bookmark.setUrl(url);
        bookmark.setTags(new String[] {this.getString(R.string.share_target_quick)});


        AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                NextcloudAPI nextcloudAPI = null;
                try {
                    SingleSignOnAccount ssoa =  SingleAccountHelper.getCurrentSingleSignOnAccount(QuickaddBookmarkActivity.this.getApplicationContext());
                    nextcloudAPI = SSOUtil.getNextcloudAPI(QuickaddBookmarkActivity.this, ssoa);
                } catch (NextcloudFilesAppAccountNotFoundException e) {
                    Toast.makeText(QuickaddBookmarkActivity.this,
                            R.string.nextcloud_files_app_account_not_found_message,
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return null;
                } catch ( NoCurrentAccountSelectedException e) {
                    Toast.makeText(QuickaddBookmarkActivity.this,
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
                    Toast.makeText(QuickaddBookmarkActivity.this,
                            result,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(QuickaddBookmarkActivity.this,
                            R.string.bookmark_saved,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();


    }
}
