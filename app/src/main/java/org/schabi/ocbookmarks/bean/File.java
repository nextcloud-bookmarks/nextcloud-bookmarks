package org.schabi.ocbookmarks.bean;

import org.schabi.ocbookmarks.R;
import tellh.com.recyclertreeview_lib.LayoutItemType;

/**
 * Created by tlh on 2016/10/1 :)
 */

public class File implements LayoutItemType {
    public String fileName;

    public File(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public int getLayoutId() {
        //return R.layout.item_file;
        return R.layout.bookmark_list_item;
    }
}
