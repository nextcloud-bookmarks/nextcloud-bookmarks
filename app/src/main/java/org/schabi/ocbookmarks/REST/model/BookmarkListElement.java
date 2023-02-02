package org.schabi.ocbookmarks.REST.model;

public class BookmarkListElement {


    private Folder mFolder;
    private Bookmark mBookmark;

    private boolean isFolder = false;


    public BookmarkListElement(Folder folder) {
        isFolder = true;
        mFolder = folder;
    }

    public BookmarkListElement(Bookmark bookmark) {
        isFolder = false;
        mBookmark = bookmark;
    }


    public boolean isFolder() {
        return isFolder;
    }

    public Folder getFolder() {
        return mFolder;
    }

    public Bookmark getBookmark() {
        return mBookmark;
    }
}
