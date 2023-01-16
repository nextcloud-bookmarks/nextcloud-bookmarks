package org.schabi.ocbookmarks;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private ArrayList<Bookmark> bookmarkList = new ArrayList<>();
    private ArrayList<BookmarkListElement> mFilteredBookmarks = new ArrayList<>();
    private BookmarksRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout refreshLayout;

    private Folder mRootFolder;
    private Folder mCurrentFolder;

    private ArrayList<String> tagFilter = new ArrayList<>();


    private OnRequestReloadListener onRequestReloadListener = null;
    private BookmarkListener bookmarkListener = null;



    @Override
    public void changeFolderCallback(@NonNull Folder f) {
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
        for (Folder f: currentFolder.getChildren()) {
            mFilteredBookmarks.add(new BookmarkListElement(f));
        }

        for (Bookmark b : bookmarkList) {
            if (b.getFolders().contains(currentFolder.getId())){
                mFilteredBookmarks.add(new BookmarkListElement(b));
            }
        }

        mAdapter.updateBookmarklist(mFilteredBookmarks);
    }

    public void showByTag(String tag) {
        mFilteredBookmarks.clear();
        for(Bookmark b : bookmarkList) {
            for(String bTag : b.getTags()) {
                if(bTag.equals(tag)) {
                    //mFilteredBookmarks.add(b);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void releaseTag() {
        mFilteredBookmarks.clear();
        for(Bookmark b : bookmarkList) {
           // mFilteredBookmarks.add(b);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void updateData(Folder hierarchy, Bookmark[] bookmarks) {
        mRootFolder = hierarchy;
        bookmarkList.clear();
        mFilteredBookmarks.clear();
        bookmarkList.addAll(Arrays.asList(bookmarks));
        buildCurrentView(mRootFolder);
    }

    public void setRefreshing(boolean refresh) {
        refreshLayout.setRefreshing(refresh);
    }
}
