package org.schabi.ocbookmarks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.schabi.ocbookmarks.REST.Bookmark;
import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;


public class MainActivity extends AppCompatActivity {

    private static final String DATA_FILE_NAME = "data.json";
    private static final String DATA_BACKUP_FILE_NAME = "data-backup.json";

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Toolbar mToolbar;

    private NextcloudAPI mNextcloudAPI = null;

    private static final String BOOKMARK_FRAGMENT = "bookmark_fragment";
    private BookmarkFragment mBookmarkFragment = null;
    private static final String TAGS_FRAGMENT = "tags_fragment";
    private TagsFragment mTagsFragment = null;
    private ProgressBar mainProgressBar;

    private SharedPreferences sharedPreferences;
    private static LoginData loginData;

    private DrawerLayout drawerLayout;
    private NavigationView navigationview;
    SharedPreferences sharedPrefs;

    private static final String TAG = MainActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Get Navigationview and do the action
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navigationview = (NavigationView)findViewById(R.id.nvView);
        navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.email:
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.support_email));
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));
                    startActivity(Intent.createChooser(intent, getString(R.string.send_email)));
                    //Toast.makeText(MainActivity.this, "Report issues to Developer",Toast.LENGTH_SHORT).show();break;
                    default:
                        return true;
                }


            }
        });
        View headerView = navigationview.getHeaderView(0);
        TextView userTextView= (TextView)headerView.findViewById(R.id.userTextView);
        TextView urlTextView= (TextView)headerView.findViewById(R.id.urlTextView);
        sharedPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        urlTextView.setText(sharedPrefs.getString(getString(R.string.login_url), ""));
        userTextView.setText(sharedPrefs.getString(getString(R.string.login_user), ""));


        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //setup sliding tabs
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditBookmarkDialog bookmarkDialog = new EditBookmarkDialog();
                AlertDialog dialog = bookmarkDialog.getDialog(MainActivity.this, null, new EditBookmarkDialog.OnBookmarkChangedListener() {
                    @Override
                    public void bookmarkChanged(Bookmark bookmark) {
                        addEditBookmark(bookmark);
                    }
                });
                dialog.show();
            }
        });

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position != 1) {
                    mBookmarkFragment.releaseTag();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);


        if(savedInstanceState == null) {
            mBookmarkFragment = new BookmarkFragment();
            setupBookmarkFragmentListener();
            mTagsFragment = new TagsFragment();
            setupTagFragmentListener();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        FragmentManager fm = getSupportFragmentManager();
        mBookmarkFragment = (BookmarkFragment) fm.getFragment(inState, BOOKMARK_FRAGMENT);
        mTagsFragment = (TagsFragment) fm.getFragment(inState, TAGS_FRAGMENT);
        setupBookmarkFragmentListener();
        setupTagFragmentListener();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        FragmentManager fm = getSupportFragmentManager();
        fm.putFragment(outState, BOOKMARK_FRAGMENT, mBookmarkFragment);
        fm.putFragment(outState, TAGS_FRAGMENT, mTagsFragment);
    }

    private void setupBookmarkFragmentListener() {
        mBookmarkFragment.setOnRequestReloadListener(new BookmarkFragment.OnRequestReloadListener() {
            @Override
            public void requestReload() {
                reloadData();
            }
        });

        mBookmarkFragment.setOnBookmarkChangedListener(new EditBookmarkDialog.OnBookmarkChangedListener() {
            @Override
            public void bookmarkChanged(Bookmark bookmark) {
                addEditBookmark(bookmark);
            }
        });

        mBookmarkFragment.setOnBookmarkDeleteListener(new BookmarkFragment.OnBookmarkDeleteListener() {
            @Override
            public void deleteBookmark(final Bookmark bookmark) {
                setRefreshing(true);
                AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        OCBookmarksRestConnector connector = new OCBookmarksRestConnector(
                                loginData.url,
                                loginData.user,
                                loginData.password,
                                mNextcloudAPI);
                        try {
                            connector.deleteBookmark(bookmark);
                        } catch (Exception e) {
                            return getString(R.string.could_not_delete_bookmark);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if(result != null) {
                            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG);
                        }
                        reloadData();
                    }
                }.execute();
            }
        });
    }

    private void addEditBookmark(final Bookmark bookmark) {
        setRefreshing(true);
        AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                OCBookmarksRestConnector connector = new OCBookmarksRestConnector(
                        loginData.url,
                        loginData.user,
                        loginData.password,
                        mNextcloudAPI);
                if(bookmark.getId() < 0) {
                    // add new bookmark
                    try {
                        connector.addBookmark(bookmark);
                    } catch (Exception e) {
                        if(BuildConfig.DEBUG) e.printStackTrace();
                        return getString(R.string.could_not_add_bookmark);
                    }
                } else {
                    try {
                        connector.editBookmark(bookmark);
                    } catch (Exception e) {
                        if(BuildConfig.DEBUG) e.printStackTrace();
                        return getString(R.string.could_not_change_bookmark);
                    }
                }
                return null;
            }

            @Override
            protected  void onPostExecute(String result) {
                if(result != null) {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                }
                reloadData();
            }
        }.execute();
    }

    private void setupTagFragmentListener() {
        mTagsFragment.setOnTagTapedListener(new TagsFragment.OnTagTapedListener() {
            @Override
            public void onTagTaped(String tag) {
                mBookmarkFragment.showByTag(tag);
                mViewPager.setCurrentItem(1);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        });

        mTagsFragment.setOnRequestReloadListener(new TagsFragment.OnRequestReloadListener() {
            @Override
            public void requestReload() {
                reloadData();
            }
        });

        mTagsFragment.setOnTagEditedListener(new TagsRecyclerViewAdapter.OnTagEditedListener() {
            @Override
            public void onTagEdited(final String oldTag, final String newTag) {
                setRefreshing(true);
                AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        OCBookmarksRestConnector connector =
                                new OCBookmarksRestConnector(
                                        loginData.url,
                                        loginData.user,
                                        loginData.password,
                                        mNextcloudAPI);
                        try {
                            connector.renameTag(oldTag, newTag);
                        } catch (Exception e) {
                            return getString(R.string.could_not_update_tag);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if(result != null) {
                            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                        }
                        reloadData();
                    }
                };
                updateTask.execute();
            }
        });

        mTagsFragment.setOnTagDeletedListener(new TagsRecyclerViewAdapter.OnTagDeletedListener() {
            @Override
            public void onTagDeleted(final String tag) {
                setRefreshing(true);
                AsyncTask<Void, Void, String> updateTask = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        OCBookmarksRestConnector connector = new OCBookmarksRestConnector(
                                loginData.url,
                                loginData.user,
                                loginData.password,
                                mNextcloudAPI);
                        try {
                            connector.deleteTag(tag);
                        } catch (Exception e) {
                            return getString(R.string.could_not_delete_tag);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if(result != null) {
                            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG);
                        }
                        reloadData();
                    }
                };
                updateTask.execute();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //todo: only reload if no data is stored so fare
        // start login activity when nececary:
        sharedPreferences =
                getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        loginData = new LoginData();
        loginData.url = sharedPreferences.getString(getString(R.string.login_url), "");
        loginData.user = sharedPreferences.getString(getString(R.string.login_user), "");
        loginData.password = sharedPreferences.getString(getString(R.string.login_pwd), "");
        loginData.token=sharedPreferences.getString(getString(R.string.login_token), "");
        loginData.ssologin=sharedPreferences.getBoolean(String.valueOf(R.string.ssologin), false);

        if (loginData.ssologin) {
            try {
                mNextcloudAPI = SSOUtil.getNextcloudAPI(this, SingleAccountHelper.getCurrentSingleSignOnAccount(this));
            } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                e.printStackTrace();
                SSOUtil.invalidateAPICache();
            }
        }

        if(loginData.url.isEmpty()) {
            Intent intent = new Intent(this, LoginAcitivty.class);
            startActivity(intent);
        } else {
            reloadData();
            loadFromFile();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem backupDataItem = menu.findItem(R.id.action_backup_data);
        if (backupDataItem != null) {
            backupDataItem.setVisible(getDataFileIfExists() != null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_change_login:
                Intent intent = new Intent(this, LoginAcitivty.class);
                startActivity(intent);
                return true;
            case R.id.action_reload_icons:
                IconHandler iconHandler = new IconHandler(MainActivity.this);
                iconHandler.deleteAll();
                reloadData();
                return true;
            case R.id.action_backup_data:
                new BackupDataTask(this).execute();
                return true;
            case android.R.id.home:
                mBookmarkFragment.releaseTag();
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                mViewPager.setCurrentItem(0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            // return PlaceholderFragment.newInstance(position + 1);
            switch (position) {
                case 0:
                    return mTagsFragment;
                case 1:
                    return mBookmarkFragment;
                default:
                    Log.e(TAG, "Fragment not found");
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tags);
                case 1:
                    return getString(R.string.bookmarks);
            }
            return null;
        }
    }

    private void reloadData() {
        RelodDataTask relodDataTask = new RelodDataTask();
        relodDataTask.execute();
    }

    private void setRefreshing(boolean refresh) {
        mBookmarkFragment.setRefreshing(refresh);
        mTagsFragment.setRefreshing(refresh);
    }

    private class RelodDataTask extends AsyncTask<Void, Void, Bookmark[]> {
        protected Bookmark[] doInBackground(Void... bla) {
            try {
                OCBookmarksRestConnector connector =
                        new OCBookmarksRestConnector(loginData.url, loginData.user, loginData.password, mNextcloudAPI);
                JSONArray data = connector.getRawBookmarks();
                storeToFile(data);
                return connector.getFromRawJson(data);
            } catch (Exception e) {
                if(BuildConfig.DEBUG) e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Bookmark[] bookmarks) {
            if(bookmarks == null) {
                Toast.makeText(MainActivity.this, R.string.connectino_failed, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mainProgressBar.setVisibility(View.GONE);
                mTagsFragment.updateData(Bookmark.getTagsFromBookmarks(bookmarks));
                mBookmarkFragment.updateData(bookmarks);
                setRefreshing(false);
            }
        }
    }

    private static class BackupDataTask extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;

        BackupDataTask(MainActivity mainActivity) {
            this.activityReference = new WeakReference<>(mainActivity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            final MainActivity mainActivity = activityReference.get();
            if (mainActivity == null || mainActivity.isFinishing()) {
                return null;
            }

            final File dataFile = mainActivity.getDataFileIfExists();
            if (dataFile == null) {
                Log.e(this.getClass().getName(), DATA_FILE_NAME + " does not exist");
                return null;
            }

            final File backupDir = mainActivity.getExternalFilesDir(null);
            if (backupDir == null) {
                Log.e(this.getClass().getName(), "External storage not available");
                return null;
            }

            final File backupFile = new File(backupDir, DATA_BACKUP_FILE_NAME);
            if (backupFile.exists() && !backupFile.delete()) {
                Log.e(this.getClass().getName(), "Existing backup file could not be deleted");
                return null;
            }

            try {
                doCopy(dataFile, backupFile);
                return backupFile.getAbsolutePath();
            } catch (Exception e) {
                Log.e(this.getClass().getName(), "Error creating backup of " + dataFile, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String backupFilePath) {
            final MainActivity mainActivity = activityReference.get();
            if (mainActivity == null || mainActivity.isFinishing()) {
                return;
            }
            if (backupFilePath != null) {
                mainActivity.mainProgressBar.setVisibility(View.GONE);
                mainActivity.setRefreshing(false);
                Toast.makeText(
                        mainActivity,
                        mainActivity.getApplicationContext().getString(
                                R.string.backup_successful,
                                backupFilePath),
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(
                        mainActivity,
                        R.string.backup_failed,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }

        private void doCopy(final File dataFile, final File backupFile) throws Exception {
            try (final InputStream fis = new FileInputStream(dataFile);
                 final OutputStream fos = new FileOutputStream(backupFile)) {
                final byte[] buffer = new byte[1024];

                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.flush();
            }
        }
    }

    private File getDataFileIfExists() {
        final File dataFile = new File(getFilesDir() + File.pathSeparator + DATA_FILE_NAME);
        return dataFile.exists() ? dataFile : null;
    }

    private void loadFromFile() {
        File jsonFile = getDataFileIfExists();
        if (jsonFile != null) {
            StringBuilder text = new StringBuilder();
            mainProgressBar.setVisibility(View.GONE);
            try {
                BufferedReader br = new BufferedReader(new FileReader(jsonFile));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append("\n");
                }
                br.close();
                OCBookmarksRestConnector connector =
                        new OCBookmarksRestConnector(loginData.url,
                                loginData.user,
                                loginData.password,
                                mNextcloudAPI);
                Bookmark[] bookmarks = connector.getFromRawJson(new JSONArray(text.toString()));
                mTagsFragment.updateData(Bookmark.getTagsFromBookmarks(bookmarks));
                mBookmarkFragment.updateData(bookmarks);
            } catch (JSONException je) {
                if (BuildConfig.DEBUG) je.printStackTrace();
            } catch (Exception e) {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        }
    }

    private void storeToFile(JSONArray data) {
        try {
            FileOutputStream jsonFile =
                    new FileOutputStream(getFilesDir() + File.pathSeparator + DATA_FILE_NAME);
            jsonFile.write(data.toString().getBytes());
            jsonFile.flush();
            jsonFile.close();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
    }
}
