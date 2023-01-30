package org.schabi.ocbookmarks;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.model.BookmarkListElement;
import org.schabi.ocbookmarks.REST.model.Folder;
import org.schabi.ocbookmarks.listener.BookmarkListener;
import org.schabi.ocbookmarks.listener.FolderListener;
import org.schabi.ocbookmarks.listener.OnRequestReloadListener;
import org.schabi.ocbookmarks.ui.BookmarksRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by the-scrabi on 15.05.17.
 */

public class BookmarkFragment extends Fragment implements FolderListener {

    private ArrayList<Bookmark> mBookmarkList = new ArrayList<>();
    private ArrayList<BookmarkListElement> mFilteredBookmarks = new ArrayList<>();
    private BookmarksRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout refreshLayout;

    private Folder mRootFolder;
    private Folder mCurrentFolder;

    private String mTagFilter = "";
    private String mSearchTerm = "";


    private OnRequestReloadListener onRequestReloadListener = null;
    private BookmarkListener bookmarkListener = null;



    @Override
    public void changeFolderCallback(@NonNull Folder f) {
        if(f.getId() == Folder.UP_ID) {
            f = getFolderFromID(mCurrentFolder.getParentFolderId());
        }
        buildCurrentView(f);
    }


    public void setBookmarkListener(BookmarkListener listener) {
        bookmarkListener = listener;
    }

    public void setOnRequestReloadListener(OnRequestReloadListener listener) {
        onRequestReloadListener = listener;
    }

    public boolean onBackHandled() {
        if(mCurrentFolder ==  null) {
            return false;
        }
        if(mRootFolder.getId() == mCurrentFolder.getId()) {
            return false;
        }

        Folder f = getFolderFromID(mCurrentFolder.getParentFolderId());
        buildCurrentView(f);

        return true;
    }

    private Folder getFolderFromID(int id) {
        if(id == mRootFolder.getId()) {
            return mRootFolder;
        }
        return getFolderFromID(id, mRootFolder);
    }

    private Folder getFolderFromID(int id, Folder level) {
        for(Folder f: level.getChildren()){
            if(f.getId() == id) {
                return f;
            }
            Folder m = getFolderFromID(id, f);
            if(m != null) {
                return m;
            }
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fagment_bookmarks, container, false);

        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_bookmarks);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.rv);
        mAdapter = new BookmarksRecyclerViewAdapter(mFilteredBookmarks, getContext());
        mAdapter.setBookmarkListener(bookmarkListener);
        mAdapter.setBookmarkFolderListener(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        refreshLayout.setOnRefreshListener(() -> {
            if(onRequestReloadListener != null) {
                onRequestReloadListener.requestReload();
            }
        });

        return rootView;
    }

    public void buildCurrentView(Folder currentFolder) {
        mCurrentFolder = currentFolder;
        mFilteredBookmarks = new ArrayList<>();

        if(!isCurrentFolderRoot()) {
            Folder up = new Folder();
            up.setTitle("..");
            up.setId(Folder.UP_ID);
            mFilteredBookmarks.add(new BookmarkListElement(up));
        }

        for (Folder f: currentFolder.getChildren()) {
            mFilteredBookmarks.add(new BookmarkListElement(f));
        }

        for (Bookmark b : mBookmarkList) {
            boolean shouldAdd = true;

            if(!mSearchTerm.equals("") &&
                    !b.getTitle().contains(mSearchTerm) &&
                    !b.getDescription().contains(mSearchTerm) &&
                    !b.getUrl().contains(mSearchTerm)
            ) {
                shouldAdd = false;
            }

            if(!mTagFilter.equals("") && !b.getTags().contains(mTagFilter)) {
                shouldAdd = false;
            }

            if (b.getFolders().contains(currentFolder.getId()) && shouldAdd){
                mFilteredBookmarks.add(new BookmarkListElement(b));
            }
        }

        mAdapter.updateBookmarklist(mFilteredBookmarks);
    }

    public void showByTag(String tag) {
        mTagFilter = tag;
        buildCurrentView(mCurrentFolder);
    }

    public void releaseTag() {
        mTagFilter = "";
        buildCurrentView(mCurrentFolder);
    }

    public void search(String term) {
        mSearchTerm = term;
        buildCurrentView(mCurrentFolder);
    }

    public void clearSearch() {
        mSearchTerm = "";
        buildCurrentView(mCurrentFolder);
    }

    public void updateData(Folder hierarchy, Bookmark[] bookmarks) {
        mRootFolder = hierarchy;
        mBookmarkList.clear();
        mFilteredBookmarks.clear();
        mSearchTerm = "";
        mTagFilter = "";
        mBookmarkList.addAll(Arrays.asList(bookmarks));
        buildCurrentView(mRootFolder);
    }

    public boolean isCurrentFolderRoot() {
        return mRootFolder.equals(mCurrentFolder);
    }

    public void setRefreshing(boolean refresh) {
        refreshLayout.setRefreshing(refresh);
    }
}
