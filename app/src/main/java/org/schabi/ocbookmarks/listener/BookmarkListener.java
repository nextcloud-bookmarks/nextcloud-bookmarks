package org.schabi.ocbookmarks.listener;

import org.schabi.ocbookmarks.REST.model.Bookmark;

public interface BookmarkListener {
    void bookmarkChanged(Bookmark bookmark);
    void deleteBookmark(Bookmark bookmark);
}
