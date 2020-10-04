package org.schabi.ocbookmarks.REST;

import java.util.Date;
import java.util.Vector;

//{"id":"1","title":"London","parent_folder":"-1","children":[{"id":"2","title":"Crawley","parent_folder":"1","children":[]}]}

public class BookmarkFolder {
    private int id = -1;
    private String title = "";
    private String parent_folder = "";
    private String[] children = new String[0];


    public static BookmarkFolder emptyInstance() {
        return new BookmarkFolder();
    }

    private BookmarkFolder() {

    }

    // ++++++++++++++++++++
    // +  factory setter  +
    // ++++++++++++++++++++

    public BookmarkFolder setId(int id) {
        this.id = id;
        return this;
    }
    public BookmarkFolder setTitle(String title){
        this.title=title;
        return this;
    }
    public BookmarkFolder setParent_folder(String parent_folder){
        this.parent_folder=parent_folder;
        return this;
    }
    public BookmarkFolder setChildren(String[] children){
        this.children=children;
        return this;
    }

    // +++++++++++++++++++++++++
    // +   getter functions    +
    // +++++++++++++++++++++++++
    public int getId(){return id;}
    public String getTitle(){return title;}
    public String getParent_folder(){return parent_folder;}
    public String[] getChildren(){
        return children;
    }

    @Override
    public String toString() {
        String tagsString = "[";
        for(String tag : children) {
            tagsString += tag + ",";
        }
        tagsString += "]";
        return "id:" + Integer.toString(id) + "\n" +
                "title:" + title + "\n" +
                "parent_folder:" +parent_folder+"\n" +
                "children:" + children ;
    }


    public static String[] getFoldersFromBookmarks(BookmarkFolder[] bookmarks) {
        Vector<String> folderList = new Vector<>();
        for(BookmarkFolder b : bookmarks) {
            for(String folder : b.getChildren()) {
                if(!folderList.contains(folder)) {
                    folderList.add(folder);
                }
            }
        }

        String[] returnFolderList = new String[folderList.size()];
        for(int i = 0; i < returnFolderList.length; i++) {
            returnFolderList[i] = folderList.get(i);
        }
        return returnFolderList;
    }
}
