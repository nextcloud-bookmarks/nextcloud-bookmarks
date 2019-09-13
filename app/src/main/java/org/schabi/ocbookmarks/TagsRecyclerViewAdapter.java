package org.schabi.ocbookmarks;

import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Activity;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by the-scrabi on 25.05.17.
 */

class TagsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<String> tagList = new ArrayList<>();
    Activity context;
    LayoutInflater inflater;
    boolean addTagMode = false;

    public interface OnTagTapedListener {
        void onTagTaped(String tag);
    }
    private OnTagTapedListener onTagTapedListener = null;
    public void setOnTagTapedListener(OnTagTapedListener listener) {
        onTagTapedListener = listener;
    }

    public interface OnTagEditedListener {
        void onTagEdited(String oldTag, String newTag);
    }
    private OnTagEditedListener onTagEditedListener = null;
    public void setOnTagEditedListener(OnTagEditedListener listener) {
        onTagEditedListener = listener;
    }

    public interface OnTagDeletedListener {
        void onTagDeleted(String tag);
    }
    private OnTagDeletedListener onTagDeletedListener = null;
    public void setOnTagDeletedListener(OnTagDeletedListener listener) {
        onTagDeletedListener = listener;
    }

    public TagsRecyclerViewAdapter(Activity acitivty, boolean addTagMode, ArrayList<String> list) {
        this.addTagMode = addTagMode;
        this.context = acitivty;
        inflater = LayoutInflater.from(context);
        tagList = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0:
                return new TagHolder(inflater.inflate(R.layout.tag_list_item, parent, false));
            case 1:
                return new AddTagHolder(inflater.inflate(R.layout.add_tag_list_item, parent, false));
            case 2:
                return new FooderTagHolder(inflater.inflate(R.layout.fooder_tag_list_item, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(position < tagList.size()) {
            TagHolder tagHolder = (TagHolder) holder;
            tagHolder.setTag(position, tagList.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position < tagList.size()) {
            return 0;
        } else {
            if(addTagMode) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    @Override
    public int getItemCount() {
        return tagList.size() + 1;
    }

    public void addTag(String tagName) {
        tagList.add(tagName);
        notifyDataSetChanged();
    }

    class TagHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener{
        private final TextView textView;
        private final PopupMenu popup;
        private final CardView cardView;
        private String tagName;
        private int tagId;

        public TagHolder(View view) {
            super(view);

            textView = (TextView) view.findViewById(R.id.tag_text);
            cardView = (CardView) view.findViewById(R.id.card_view);

            cardView.setOnClickListener(this);
            cardView.setOnLongClickListener(this);

            popup = new PopupMenu(context, view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.edit_tag_item_menu, popup.getMenu());

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
                    switch (id) {
                        case R.id.edit_menu:
                            showEditDialog();
                            return true;
                        case R.id.delete_menu:
                            if(!addTagMode) {
                                showDeleteDialog();
                            } else {
                                notifyDataSetChanged();
                                onTagDeletedListener.onTagDeleted(tagName);
                            }
                            return true;
                    }

                    return false;
                }
            });
        }

        public void setTag(int id, String tag) {
            tagName = tag;
            tagId = id;
            textView.setText(tagName);
        }

        @Override
        public void onClick(View view) {
            if(addTagMode) {
                showEditDialog();
            } else {
                if (onTagTapedListener != null) {
                    onTagTapedListener.onTagTaped(tagName);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            popup.show();
            return true;
        }

        private void showDeleteDialog() {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.sure_to_delete_tag)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(onTagDeletedListener != null) {
                                for(int i = 0; i < tagList.size(); i++) {
                                    if(tagList.get(i).equals(tagName)) {
                                        tagList.remove(i);
                                    }
                                }
                                notifyDataSetChanged();
                                onTagDeletedListener.onTagDeleted(tagName);
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

        private void showEditDialog() {
            final EditText editText = new EditText(context);
            editText.setText(tagName);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.edit_tag)
                    .setView(editText)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //setTagName(editText.getText().toString());
                            // tagList.set(tagId, editText.getText().toString());

                            if(onTagEditedListener != null) {
                                onTagEditedListener.onTagEdited(tagName, editText.getText().toString());
                            }
                            notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
    }

    class AddTagHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public AddTagHolder(View view) {
            super(view);
            CardView cardView = (CardView) view.findViewById(R.id.card_view);
            cardView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final EditText editText = new EditText(context);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.edit_tag)
                    .setView(editText)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            addTag(editText.getText().toString());
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
    }

    class FooderTagHolder extends RecyclerView.ViewHolder {
        public FooderTagHolder(View view) {
            super(view);
        }
    }
}