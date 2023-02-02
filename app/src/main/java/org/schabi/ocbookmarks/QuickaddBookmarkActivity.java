package org.schabi.ocbookmarks;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nextcloud.android.sso.BuildConfig;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;
import org.schabi.ocbookmarks.api.SSOUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuickaddBookmarkActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bookmark_activity);
        setTitle("");


        TextView stateView = findViewById(R.id.adding_text_view);
        ProgressBar progressView = findViewById(R.id.progressView);
        ImageView successView = findViewById(R.id.successView);

        Intent intent = getIntent();
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        Bookmark bookmark = Bookmark.emptyInstance();
        bookmark.setTitle(title);
        bookmark.setUrl(url);
//        bookmark.setTags(new String[] {this.getString(R.string.share_target_quick)});
        ArrayList<String> list;
        list = new ArrayList<String>();
        list.add(String.valueOf(R.string.share_target_quick));
        bookmark.setTags(list);



        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            String result = getString(R.string.bookmark_saved);
            NextcloudAPI nextcloudAPI = null;
            try {
                SingleSignOnAccount ssoa =  SingleAccountHelper.getCurrentSingleSignOnAccount(QuickaddBookmarkActivity.this.getApplicationContext());
                nextcloudAPI = SSOUtil.getNextcloudAPI(QuickaddBookmarkActivity.this, ssoa);
            } catch (NextcloudFilesAppAccountNotFoundException e) {
                Toast.makeText(QuickaddBookmarkActivity.this,
                        R.string.nextcloud_files_app_account_not_found_message,
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch ( NoCurrentAccountSelectedException e) {
                Toast.makeText(QuickaddBookmarkActivity.this,
                        R.string.no_current_account_selected_exception_message,
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            OCBookmarksRestConnector connector = new OCBookmarksRestConnector(nextcloudAPI);
            try {
                connector.addBookmark(bookmark);
            } catch (Exception e) {
                if(BuildConfig.DEBUG) e.printStackTrace();
                result = getString(R.string.could_not_add_bookmark);
            }
            String finalResult = result;
            handler.post(() -> {
                progressView.setVisibility(View.GONE);
                successView.setVisibility(View.VISIBLE);
                stateView.setText(finalResult);

                Toast.makeText(QuickaddBookmarkActivity.this,
                        finalResult,
                        Toast.LENGTH_LONG).show();
                finish();
            });
        });




    }
}
