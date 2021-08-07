package org.schabi.ocbookmarks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;

import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.model.Folder;
import org.schabi.ocbookmarks.viewbinder.DirectoryNodeBinder;
import org.schabi.ocbookmarks.viewbinder.FileNodeBinder;

import org.schabi.ocbookmarks.bean.Dir;
import org.schabi.ocbookmarks.bean.File;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by the-scrabi on 15.05.17.
 */

public class BookmarkFragment extends Fragment {

    private ArrayList<Bookmark> bookmarkList = new ArrayList<>();
    private ArrayList<Bookmark> bookmarkToShowList = new ArrayList<>();
    private BookmarksRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout refreshLayout;

    //for treeview
    private RecyclerView rv;
    private TreeViewAdapter adapter;
    private Folder hierarchy;
    //END Treeview

    public interface OnRequestReloadListener {
        void requestReload();
    }
    private OnRequestReloadListener onRequestReloadListener = null;
    public void setOnRequestReloadListener(OnRequestReloadListener listener) {
        onRequestReloadListener = listener;
    }

    private EditBookmarkDialog.OnBookmarkChangedListener onBookmarkChangedListener = null;
    public void setOnBookmarkChangedListener(EditBookmarkDialog.OnBookmarkChangedListener listener) {
        onBookmarkChangedListener = listener;
    }

    public interface OnBookmarkDeleteListener {
        void deleteBookmark(Bookmark bookmark);
    }
    private OnBookmarkDeleteListener onBookmarkDeleteListener = null;
    public void setOnBookmarkDeleteListener(OnBookmarkDeleteListener listener) {
        onBookmarkDeleteListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fagment_bookmarks, container, false);



        refreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_bookmarks);



//        RecyclerView recyclerView =
//                (RecyclerView) rootView.findViewById(R.id.bookmark_recycler_view);
//        mAdapter = new BookmarksRecyclerViewAdapter(getActivity());
//        recyclerView.setAdapter(mAdapter);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //For tree view
        rv = (RecyclerView) rootView.findViewById(R.id.rv);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TreeViewAdapter(new ArrayList<>(), Arrays.asList(new FileNodeBinder(), new DirectoryNodeBinder()));
        buildTree();

        // whether collapse child nodes when their parent node was close.
//        adapter.ifCollapseChildWhileCollapseParent(true);
        adapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {
            @Override
            public boolean onClick(TreeNode node, RecyclerView.ViewHolder holder) {
                if (!node.isLeaf()) {
                    //Update and toggle the node.
                    onToggle(!node.isExpand(), holder);
//                    if (!node.isExpand())
//                        adapter.collapseBrotherNode(node);
                }
                return false;
            }

            @Override
            public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {
                DirectoryNodeBinder.ViewHolder dirViewHolder = (DirectoryNodeBinder.ViewHolder) holder;
                final ImageView ivArrow = dirViewHolder.getIvArrow();
                int rotateDegree = isExpand ? 90 : -90;
                ivArrow.animate().rotationBy(rotateDegree)
                        .start();
            }
        });
        rv.setAdapter(adapter);
        //End of treeview


        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(onRequestReloadListener != null) {
                    onRequestReloadListener.requestReload();
                }
            }
        });

        return rootView;
    }

    private void buildTree() {
        List<TreeNode> nodes = new ArrayList<>();
        TreeNode<Dir> top;
        if (hierarchy == null) {
            top = new TreeNode<>(new Dir("All Bookmarks"));
        } else {
            top = new TreeNode<>(new Dir(hierarchy.getTitle()));
            fillNode(top, hierarchy);
        }
        nodes.add(top); //The plan is to keep this top always as All Bookmarks.

        adapter.refresh(nodes);
    }

    private void fillNode(TreeNode<Dir> folderNode, Folder folder) {
        if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
            for (Folder childFolder : folder.getChildren()) {
                TreeNode<Dir> childNode = new TreeNode<>(new Dir(childFolder.getTitle()));
                folderNode.addChild(childNode);
                fillNode(childNode, childFolder);
            }
        }
        for (Bookmark b : bookmarkToShowList) {
            if (b.getFolders().contains(folder.getId())){
                folderNode.addChild(new TreeNode<>(new File(b.getTitle())));
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.id_action_close_all:
                adapter.collapseAll();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showByTag(String tag) {
        bookmarkToShowList.clear();
        for(Bookmark b : bookmarkList) {
            for(String bTag : b.getTags()) {
                if(bTag.equals(tag)) {
                    bookmarkToShowList.add(b);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void releaseTag() {
        bookmarkToShowList.clear();
        for(Bookmark b : bookmarkList) {
            bookmarkToShowList.add(b);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void updateData(Folder hierarchy, Bookmark[] bookmarks) {
        this.hierarchy = hierarchy;
        bookmarkList.clear();
        bookmarkToShowList.clear();
        for(Bookmark b : bookmarks) {
            bookmarkList.add(b);
            bookmarkToShowList.add(b);
        }
        buildTree();
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setRefreshing(boolean refresh) {
        refreshLayout.setRefreshing(refresh);
    }

    class BookmarksRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Context context;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        public BookmarksRecyclerViewAdapter(Context context) {
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case 0:
                    return new BookmarkHolder(inflater.inflate(R.layout.bookmark_list_item, parent, false));
                case 1:
                    return new FooderViewHolder(inflater.inflate(R.layout.bookmark_list_item_fooder, parent, false));
                default:
                    return null;
            }
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(holder instanceof BookmarkHolder) {
                BookmarkHolder bookmarkHolder = (BookmarkHolder) holder;
                bookmarkHolder.relatedBookmarkId = position;
                Bookmark b = bookmarkToShowList.get(position);
                bookmarkHolder.titleView.setText(b.getTitle());
                if(!b.getDescription().isEmpty()) {
                    bookmarkHolder.urlDescriptionView.setText(b.getDescription());
                } else {
                    bookmarkHolder.urlDescriptionView.setText(b.getUrl());
                }
                IconHandler ih = new IconHandler(getContext());
                ih.loadIcon(bookmarkHolder.iconView, b);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position < bookmarkToShowList.size()) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int getItemCount() {
            return bookmarkToShowList.size() + 1;
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

                popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.edit_bookmark_item_menu, popup.getMenu());


                // try setting force show icons via reflections (android is a peace of shit)
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

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        Bookmark bookmark = bookmarkToShowList.get(relatedBookmarkId);

                        switch (id) {
                            case R.id.share:
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_SUBJECT, bookmark.getTitle());
                                intent.putExtra(Intent.EXTRA_TEXT, bookmark.getUrl());
                                startActivity(intent);
                                return true;
                            case R.id.edit_menu:
                                EditBookmarkDialog bookmarkDialog = new EditBookmarkDialog();
                                bookmarkDialog.getDialog(getActivity(),
                                        bookmark,
                                        new EditBookmarkDialog.OnBookmarkChangedListener() {
                                            @Override
                                            public void bookmarkChanged(Bookmark bookmark) {
                                                onBookmarkChangedListener.bookmarkChanged(bookmark);
                                            }
                                        }).show();

                                return true;
                            case R.id.delete_menu:
                                showDeleteDialog();
                                return true;
                        }

                        return false;
                    }
                });
            }

            private void showDeleteDialog() {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.sure_to_delete_bookmark)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(onBookmarkChangedListener != null) {
                                    onBookmarkDeleteListener
                                            .deleteBookmark(bookmarkToShowList.get(relatedBookmarkId));
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
                intent.setData(Uri.parse(bookmarkToShowList.get(relatedBookmarkId).getUrl()));
                startActivity(intent);
            }

            @Override
            public boolean onLongClick(View view) {
                popup.show();
                return true;
            }
        }

        class FooderViewHolder extends RecyclerView.ViewHolder {
            FooderViewHolder(View view) {
                super(view);
            }
        }
    }
}
