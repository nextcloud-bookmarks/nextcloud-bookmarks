package org.schabi.ocbookmarks.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.ocbookmarks.EditBookmarkDialog;
import org.schabi.ocbookmarks.R;
import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.model.BookmarkListElement;
import org.schabi.ocbookmarks.REST.model.Folder;
import org.schabi.ocbookmarks.listener.BookmarkListener;
import org.schabi.ocbookmarks.listener.FolderListener;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class BookmarksRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<BookmarkListElement> mListElements;
    Context mContext;
    LayoutInflater mInflater;
    FolderListener mFolderCallback;
    BookmarkListener mBookmarkCallback;

    private static final int FOLDER_TYPE = 0;
    private static final int BOOKMARK_TYPE = 1;

    public BookmarksRecyclerViewAdapter(ArrayList<BookmarkListElement> listElements, Context context) {
        this.mListElements = listElements;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
    }


    public void updateBookmarklist(ArrayList<BookmarkListElement> listElements) {
        this.mListElements.clear();
        this.mListElements.addAll(listElements);
        notifyDataSetChanged();
    }

    public void setBookmarkListener(BookmarkListener listener) {
        this.mBookmarkCallback = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case BOOKMARK_TYPE:
                return new BookmarkHolder(mInflater.inflate(R.layout.bookmark_list_item, parent, false));
            case FOLDER_TYPE:
                return new FolderViewHolder(mInflater.inflate(R.layout.bookmark_list_item_folder, parent, false));
            default:
                return null;
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof BookmarkHolder) {
            BookmarkHolder bookmarkView = (BookmarkHolder) holder;
            bookmarkView.relatedBookmarkId = holder.getAdapterPosition();
            Bookmark b = mListElements.get(holder.getAdapterPosition()).getBookmark();
            bookmarkView.titleView.setText(b.getTitle());
            if (!b.getDescription().isEmpty()) {
                bookmarkView.urlDescriptionView.setText(b.getDescription());
            } else {
                bookmarkView.urlDescriptionView.setText(b.getUrl());
            }
            IconHandler ih = new IconHandler(mContext);
            ih.loadIcon(bookmarkView.iconView, b);


        } else if (holder instanceof  FolderViewHolder) {
            FolderViewHolder folderView = (FolderViewHolder) holder;
            folderView.relatedBookmarkId = holder.getAdapterPosition();
            Folder f = mListElements.get(holder.getAdapterPosition()).getFolder();
            folderView.folderTitle.setText(f.getTitle());
        }



    }

    public void setBookmarkFolderListener(FolderListener fl){
        mFolderCallback = fl;
    }

    @Override
    public int getItemViewType(int position) {
        if (mListElements.get(position).isFolder()) {
            return FOLDER_TYPE;
        } else {
            return BOOKMARK_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return mListElements.size();
    }

    public class BookmarkHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        final PopupMenu popup;
        final TextView titleView;
        final TextView urlDescriptionView;
        final ImageView iconView;
        int relatedBookmarkId;

        public BookmarkHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            titleView = (TextView) view.findViewById(R.id.bookmark_title);
            urlDescriptionView = (TextView) view.findViewById(R.id.bookmark_url_description);
            iconView = (ImageView) view.findViewById(R.id.site_icon);

            popup = new PopupMenu(mContext, view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.edit_bookmark_item_menu, popup.getMenu());


            // try setting force show icons via reflections
            Object menuHelper;
            Class[] argTypes;
            try {
                Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
                fMenuHelper.setAccessible(true);
                menuHelper = fMenuHelper.get(popup);
                argTypes = new Class[]{boolean.class};
                menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                Bookmark bookmark = mListElements.get(relatedBookmarkId).getBookmark();

                switch (id) {
                    case R.id.share:
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, bookmark.getTitle());
                        intent.putExtra(Intent.EXTRA_TEXT, bookmark.getUrl());
                        mContext.startActivity(intent);
                        return true;
                    case R.id.edit_menu:
                        EditBookmarkDialog bookmarkDialog = new EditBookmarkDialog();
                        bookmarkDialog.getDialog((Activity) mContext,
                                bookmark,
                                mBookmarkCallback
                        ).show();
                        return true;
                    case R.id.delete_menu:
                        showDeleteDialog();
                        return true;
                }

                return false;
            });
        }

        private void showDeleteDialog() {
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.sure_to_delete_bookmark)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(mBookmarkCallback != null) {
                                mBookmarkCallback.deleteBookmark(mListElements.get(relatedBookmarkId).getBookmark());
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Bookmark b = mListElements.get(relatedBookmarkId).getBookmark();
            intent.setData(Uri.parse(b.getUrl()));
            mContext.startActivity(intent);
        }

        @Override
        public boolean onLongClick(View view) {
            popup.show();
            return true;
        }
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {

        final TextView folderTitle;
        int relatedBookmarkId;

        FolderViewHolder(View view) {
            super(view);
            folderTitle = (TextView) view.findViewById(R.id.folder_title);

            ((RelativeLayout) view.findViewById(R.id.layout)).setOnClickListener(view1 -> {
                Folder f = mListElements.get(relatedBookmarkId).getFolder();
                mFolderCallback.changeFolderCallback(f);
            });
        }
    }
}
