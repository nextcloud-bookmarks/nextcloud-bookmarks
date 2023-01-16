package org.schabi.ocbookmarks.REST.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Created by the-scrabi on 14.05.17.
 * modified by bisasda
 */
public class Bookmark {
    private int id = -1;
    private String url = "";
    private String title = "";
    private String userId = "";
    private String description = "";
//    private Date added = null;
    private Date lastModified = null;
    private int clickcount = -1;
//    private boolean isPublic = false;
    private ArrayList<String> tags = new ArrayList<>();
    private List<Integer> folders = new ArrayList<>();

    private boolean isFolder = false;

    public static Bookmark emptyInstance() {
        return new Bookmark();
    }

    private Bookmark() {

    }

    // ++++++++++++++++++++
    // +  factory setter  +
    // ++++++++++++++++++++

    public Bookmark setId(int id) {
        this.id = id;
        return this;
    }

    public Bookmark setUrl(String url) {
        this.url = url;
        return this;
    }

    public Bookmark setTitle(String title) {
        this.title = title;
        return this;
    }

    public Bookmark setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Bookmark setDescription(String description) {
        this.description = description;
        return this;
    }

//    public Bookmark setAdded(Date added) {
//        this.added = added;
//        return this;
//    }

    public Bookmark setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public Bookmark setClickcount(int clickcount) {
        this.clickcount = clickcount;
        return this;
    }

    public Bookmark setTags(ArrayList<String> tags) {
        this.tags = tags;
        return this;
    }

    public Bookmark setFolders(List<Integer> folders) {
        this.folders = folders;
        return this;
    }
//    public Bookmark setPublic(boolean aPublic) {
//        isPublic = aPublic;
//        return this;
//    }

    // +++++++++++++++++++++++++
    // +   getter functions    +
    // +++++++++++++++++++++++++


    public int getId() {
        return id;
    }
    public String getUrl() {
        return url;
    }
    public String getTitle() {
        return title;
    }
    public String getUserId() {
        return userId;
    }
    public String getDescription() {
        return description;
    }
//    public Date getAdded() {
//        return added;
//    }
    public Date getLastModified() {
        return lastModified;
    }
    public int getClickcount() {
        return clickcount;
    }
    public ArrayList<String> getTags() {
        return tags;
    }
    public List<Integer> getFolders(){
        return folders;
    }
//    public boolean isPublic() {
//        return isPublic;
//    }

    @Override
    public String toString() {
        String tagsString = "[";
        for(String tag : tags) {
            tagsString += tag + ",";
        }
        tagsString += "]";

        String foldersString = "[";
        for(int folder : folders) {
            foldersString += folder + ",";
        }
        foldersString += "]";

        return "id:" + Integer.toString(id) + "\n" +
                "url:" + url + "\n" +
                "title:" + title + "\n" +
                "userId:" + userId + "\n" +
                "description:" + description + "\n" +
//                "added:" + added.toString() + "\n" +
                "lastModified:" + lastModified.toString() + "\n" +
                "clickount:" + clickcount + "\n" +
                "tags:" + tagsString+ "\n" +
                "folders:"+foldersString;
//                "isPublic:" + Boolean.toString(isPublic);
    }


    public static String[] getTagsFromBookmarks(Bookmark[] bookmarks) {
        Vector<String> tagList = new Vector<>();
        for(Bookmark b : bookmarks) {
            for(String tag : b.getTags()) {
                if(!tagList.contains(tag)) {
                    tagList.add(tag);
                }
            }
        }

        String[] returnTagList = new String[tagList.size()];
        for(int i = 0; i < returnTagList.length; i++) {
            returnTagList[i] = tagList.get(i);
        }
        return returnTagList;
    }

    public static int[] getFoldersFromBookmarks(Bookmark[] bookmarks) {
        Vector<Integer> folderList = new Vector<>();
        for(Bookmark b : bookmarks) {
            for(int folder : b.getFolders()) {
                if(!folderList.contains(folder)) {
                    folderList.add(folder);
                }
            }
        }

        int[] returnFolderList = new int[folderList.size()];
        for(int i = 0; i < returnFolderList.length; i++) {
            returnFolderList[i] = folderList.get(i);
        }
        return returnFolderList;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }
}
