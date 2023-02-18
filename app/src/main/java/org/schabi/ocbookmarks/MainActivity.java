package org.schabi.ocbookmarks;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.nextcloud.android.sso.BuildConfig;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.exceptions.SSOException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.OCBookmarksRestConnector;
import org.schabi.ocbookmarks.REST.model.Folder;
import org.schabi.ocbookmarks.api.SSOUtil;
import org.schabi.ocbookmarks.listener.BookmarkListener;
import org.schabi.ocbookmarks.listener.OnRequestReloadListener;
import org.schabi.ocbookmarks.ui.IconHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private static final String DATA_FILE_NAME = "data.json";
    private static final String DATA_BACKUP_FILE_NAME = "data-backup.json";
    private static final int TAGLIST_MIN_ID = 10;

    private NextcloudAPI mNextcloudAPI = null;

    private static final String BOOKMARK_FRAGMENT = "bookmark_fragment";
    private BookmarkFragment mBookmarkFragment = null;
    private ProgressBar mainProgressBar;

    private NavigationView navigationview;
    private DrawerLayout drawerLayout;

    private LinearLayout normalToolbar;
    private LinearLayout searchToolbar;
    private TextView searchButton;
    private ImageButton backButton;
    private SearchView searchBar;
    private ImageButton menuButton;


    private static final String TAG = MainActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Get Navigationview and do the action
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationview = findViewById(R.id.nvView);
        navigationview.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if(id >= TAGLIST_MIN_ID) {
                String tag = item.getTitle().toString();
                mBookmarkFragment.showByTag(tag);
            } else {
                mBookmarkFragment.releaseTag();
            }
            drawerLayout.closeDrawer(this.navigationview);
            return true;
        });

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        normalToolbar =  findViewById(R.id.normalToolbar);
        searchToolbar =  findViewById(R.id.searchToolbar);
        searchButton = findViewById(R.id.search_text);
        backButton = findViewById(R.id.backButton);
        searchBar = findViewById(R.id.searchbar);
        menuButton = findViewById(R.id.menu_button);

        searchButton.setOnClickListener(v -> {
            searchToolbar.setVisibility(View.VISIBLE);
            normalToolbar.setVisibility(View.GONE);
            searchBar.requestFocus();
        });

        backButton.setOnClickListener(v->{
            searchToolbar.setVisibility(View.GONE);
            normalToolbar.setVisibility(View.VISIBLE);
        });

        menuButton.setOnClickListener(v->{
            drawerLayout.openDrawer(GravityCompat.START);
        });


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            EditBookmarkDialog bookmarkDialog = new EditBookmarkDialog();
            AlertDialog dialog = bookmarkDialog.getDialog(MainActivity.this, null, new BookmarkListener() {
                @Override
                public void bookmarkChanged(Bookmark bookmark) {
                    bookmark.setFolders(Arrays.asList(mBookmarkFragment.getCurrentFolder().getId()));
                    addEditBookmark(bookmark);
                }

                @Override
                public void deleteBookmark(Bookmark bookmark) {}
            });
            dialog.show();
        });

        mainProgressBar = findViewById(R.id.mainProgressBar);


        if(savedInstanceState == null) {
            mBookmarkFragment = new BookmarkFragment();
            setupBookmarkFragmentListener();
        }

        prepareSSO();
    }

    @Override
    public void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        FragmentManager fm = getSupportFragmentManager();
        mBookmarkFragment = (BookmarkFragment) fm.getFragment(inState, BOOKMARK_FRAGMENT);
        setupBookmarkFragmentListener();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        FragmentManager fm = getSupportFragmentManager();
        fm.putFragment(outState, BOOKMARK_FRAGMENT, mBookmarkFragment);
    }

    private void setupBookmarkFragmentListener() {

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.container, mBookmarkFragment)
                .commit();

        mBookmarkFragment.setOnRequestReloadListener(new OnRequestReloadListener() {
            @Override
            public void requestReload() {
                reloadData();
            }
        });

        mBookmarkFragment.setBookmarkListener(new BookmarkListener() {
            @Override
            public void bookmarkChanged(Bookmark bookmark) {
                addEditBookmark(bookmark);
            }

            @Override
            public void deleteBookmark(final Bookmark bookmark) {
                setRefreshing(true);
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        OCBookmarksRestConnector connector = new OCBookmarksRestConnector(mNextcloudAPI);
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
                OCBookmarksRestConnector connector = new OCBookmarksRestConnector(mNextcloudAPI);
//                        loginData.token,
//                        loginData.ssologin);
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

    @Override
    public void onResume() {
        super.onResume();
        prepareSSO();
    }

    @Override
    public void onBackPressed() {
        mBookmarkFragment.onBackHandled();
    }


    private void prepareSSO() {
        if(mNextcloudAPI != null) {
            Log.e(TAG, "API is already set up, we can continue...");
            return;
        }

        try {
            Log.e(TAG, "Prepare the API");
            SingleSignOnAccount ssoa = SingleAccountHelper.getCurrentSingleSignOnAccount(this.getApplicationContext());
            Log.e(TAG, "Found user: "+ssoa.name);
            mNextcloudAPI = SSOUtil.getNextcloudAPI(this, ssoa);
            Log.e(TAG, "Done!");

            View headerView = navigationview.getHeaderView(0);
            TextView userTextView= (TextView)headerView.findViewById(R.id.userTextView);
            TextView urlTextView= (TextView)headerView.findViewById(R.id.urlTextView);
            urlTextView.setText(ssoa.url);
            userTextView.setText(ssoa.name);
            reloadData();

        } catch (NextcloudFilesAppAccountNotFoundException e) {
            e.printStackTrace();
            SSOUtil.invalidateAPICache();
        } catch (NoCurrentAccountSelectedException e) {
            Log.e(TAG, "Exception: No Account set up, log in again!");
            Log.e(TAG, e.toString());
            Intent intent = new Intent(this, LoginAcitivty.class);
            startActivity(intent);
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
                try {
                    SSOUtil.invalidateAPICache();
                    SingleAccountHelper.setCurrentAccount(this, null);
                    SingleAccountHelper.reauthenticateCurrentAccount(this);
                } catch (SSOException e) {
                    UiExceptionManager.showDialogForException(this, e);
                }
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
                if (drawerLayout.isDrawerOpen(this.navigationview)) {
                    drawerLayout.closeDrawer(this.navigationview);
                } else {
                    drawerLayout.openDrawer(this.navigationview);
                }

                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadData() {
        RelodDataTask relodDataTask = new RelodDataTask();
        relodDataTask.execute();
    }

    private void setRefreshing(boolean refresh) {
        mBookmarkFragment.setRefreshing(refresh);
    }

    private class RelodDataTask extends AsyncTask<Void, Void, Bookmark[]> {
        Folder root = null;
        protected Bookmark[] doInBackground(Void... bla) {
            try {
                prepareSSO();
                OCBookmarksRestConnector connector =
                        new OCBookmarksRestConnector(mNextcloudAPI);
                        //new OCBookmarksRestConnector(loginData.url, loginData.user, loginData.password,loginData.token,loginData.ssologin);
                root = connector.getFolders();

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
                mBookmarkFragment.updateData(root, bookmarks);


                Menu menu = navigationview.getMenu();
                menu.removeGroup(R.id.tag_group);
                SubMenu subMenu = menu.addSubMenu(R.id.tag_group, 1, Menu.NONE, R.string.nav_drawer_tags_header);

                int i = TAGLIST_MIN_ID;
                for (String tag: Bookmark.getTagsFromBookmarks(bookmarks)) {
                    MenuItem menuItem = subMenu.add(i, i++, Menu.NONE, tag);
                    menuItem.setIcon(R.drawable.ic_tag);
                }
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
                OCBookmarksRestConnector connector = new OCBookmarksRestConnector(mNextcloudAPI);
                Bookmark[] bookmarks = connector.getFromRawJson(new JSONArray(text.toString()));
                mBookmarkFragment.updateData(connector.getFolders(), bookmarks);
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
