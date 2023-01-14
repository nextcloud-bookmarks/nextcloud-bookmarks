package org.schabi.ocbookmarks.REST.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Folder implements Serializable {
    private static final long serialVersionUID = 1L;

    // https://nextcloud-bookmarks.readthedocs.io/en/latest/folder.html#folder-model
    // This is how it looks like in JSON:
    /*
    {
  "status": "success", "data": [
    {"id": 1, "title": "work", "parent_folder": -1},
    {"id": 2, "title": "personal", "parent_folder": -1, "children": [
          {"id": 3, "title": "garden", "parent_folder": 2},
          {"id": 4, "title": "music", "parent_folder": 2}
        ]},
      ]
    }
     */

    private int id;
    private String title;
    private int parentFolderId;
    private List<Folder> children = new ArrayList<>();


    public static Folder createEmptyRootFolder() {
        Folder root = new Folder();
        root.setId(-1);
        root.setTitle("All Bookmarks");
        root.setParentFolderId(-1);
        return root;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public List<Folder> getChildren() {
        return children;
    }

    public void setChildren(List<Folder> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder folder = (Folder) o;

        if (id != folder.id) return false;
        if (parentFolderId != folder.parentFolderId) return false;
        if (title != null ? !title.equals(folder.title) : folder.title != null) return false;
        return children != null ? children.equals(folder.children) : folder.children == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + parentFolderId;
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }
}
