package org.schabi.ocbookmarks.REST;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.nextcloud.android.sso.QueryParam;
import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.api.Response;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.ocbookmarks.REST.model.Bookmark;
import org.schabi.ocbookmarks.REST.model.Folder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by the-scrabi on 14.05.17.
 * Modified by @dasbiswajit on 14.04.2019
 */


public class OCBookmarksRestConnector {
    private String apiRootUrl = "";
    private final NextcloudAPI nextcloudAPI;
    String TAG = this.getClass().toString();


    /**
     * @param nextcloudAPI will be used if not null instead of traditional user / password authentication
     */
    public OCBookmarksRestConnector(NextcloudAPI nextcloudAPI) {
        this.nextcloudAPI = nextcloudAPI;
        // host is defined by SingleSignOnAccount

        apiRootUrl = "/index.php/apps/bookmarks/public/rest/v2";
        Log.e(TAG,"API Root-Url: "+apiRootUrl);
    }

    /**
     * Sending SSO fancy way
     */
    public JSONObject sendWithSSO(@NonNull String methode, @NonNull String relativeUrl, @NonNull Collection<QueryParam> parameter) throws RequestException {
        if (this.nextcloudAPI == null) {
            Log.e(TAG,"API not set up.");
            throw new RequestException("Trying to send request via SSO, but API is null.");
        }

        Log.i(TAG,"API is already set up");
        NextcloudRequest request = new NextcloudRequest
                .Builder()
                .setMethod(methode)
                .setUrl(apiRootUrl + relativeUrl)
                .setParameter(parameter)
                .build();
        try {
            Response response = nextcloudAPI.performNetworkRequestV2(request);

            final StringBuilder result = new StringBuilder();
            final BufferedReader rd = new BufferedReader(new InputStreamReader(response.getBody()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            response.getBody().close();

            return parseJson(methode, apiRootUrl + relativeUrl, result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RequestException(e);
        }
    }

    private JSONObject parseJson(String methode, String url, String response) throws RequestException {

        JSONObject data = null;
        if("GET".equals(methode) && url.endsWith("/folder")) {
            JSONObject array = null;
            try {
                data = new JSONObject(response);
//                data = new JSONObject();
//                data.put("data", array);
            } catch (JSONException je) {
                throw new RequestException("Parsing error, maybe owncloud does not support bookmark api", je);
            }
            return data;
        }
        if ("GET".equals(methode) && url.endsWith("/tag")) {
            // we have to handle GET /tag different:
            // https://github.com/nextcloud/bookmarks#list-all-tags
            JSONArray array = null;
            try {
                array = new JSONArray(response);
                data = new JSONObject();
                data.put("data", array);
            } catch (JSONException je) {
                throw new RequestException("Parsing error, maybe owncloud does not support bookmark api", je);
            }
            return data;
        } else if ("PUT".equals(methode)) {
            try {
                data = new JSONObject(response);
                return data.getJSONObject("item");
            } catch (JSONException je) {
                throw new RequestException("Parsing error, maybe owncloud does not support bookmark api", je);
            }
        } else {

            try {
                data = new JSONObject(response);
            } catch (JSONException je) {
                throw new RequestException("Parsing error, maybe owncloud does not support bookmark api", je);
            }

            try {
                if (!data.getString("status").equals("success")) {
                    throw new RequestException("Error bad request: " + url);
                }
            } catch (JSONException e) {
                throw new RequestException("Error bad request: " + url, e);
            }
            return data;
        }
    }

    // +++++++++++++++++
    // +   bookmarks   +
    // +++++++++++++++++

    public JSONArray getRawBookmarks() throws RequestException {
        try {
            JSONArray bookmarks = new JSONArray();
            int pageSize = 300;
            int resultLength = pageSize;
            int page = 0;

            while (resultLength == pageSize) {
                Collection<QueryParam> parameter = new ArrayList<>();
                parameter.add(new QueryParam("page", String.valueOf(page++)));
                parameter.add(new QueryParam("limit", "300"));
                JSONObject now = sendWithSSO("GET", "/bookmark", parameter);
                JSONArray data = now.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject bm = (JSONObject) data.get(i);
                    bookmarks.put(bm);
                }
                resultLength = bookmarks.length();
            }
            return bookmarks;
        } catch (JSONException e) {
            throw new RequestException("Could not parse data", e);
        }
    }

    public Bookmark[] getFromRawJson(JSONArray data) throws RequestException {
        try {
            Bookmark[] bookmarks = new Bookmark[data.length()];
            for (int i = 0; i < data.length(); i++) {
                JSONObject bookmark = data.getJSONObject(i);
                bookmarks[i] = getBookmarkFromJsonO(bookmark);
            }
            return bookmarks;
        } catch (JSONException e) {
            throw new RequestException("Could not parse data", e);
        }
    }

    public Bookmark[] getBookmarks() throws RequestException {
        JSONArray data = getRawBookmarks();
        return getFromRawJson(data);
    }


    private Bookmark getBookmarkFromJsonO(JSONObject jBookmark) throws RequestException {

        ArrayList<String> tags;
        try {
            JSONArray jTags = jBookmark.getJSONArray("tags");
            tags = new ArrayList<>();
            for (int j = 0; j < jTags.length(); j++) {
                tags.add(jTags.getString(j));
            }
        } catch (JSONException je) {
            throw new RequestException("Could not parse array", je);
        }

        List<Integer> folders;
        try {
            JSONArray jfolders = jBookmark.getJSONArray("folders");
            folders = new ArrayList<>(jfolders.length());
            for (int j = 0; j < jfolders.length(); j++) {
                folders.add(jfolders.getInt(j));
            }
        } catch (JSONException je) {
            throw new RequestException("Could not parse folder array", je);
        }


        // Todo: another api error we need to fix
        if (tags.size() == 1 && tags.get(0).isEmpty()) {
            tags = new ArrayList<>();
        }

        try {
            return Bookmark.emptyInstance()
                    .setId(jBookmark.getInt("id"))
                    .setUrl(jBookmark.getString("url"))
                    .setTitle(jBookmark.getString("title"))
                    .setUserId(jBookmark.getString("userId"))
                    .setDescription(jBookmark.getString("description"))
                    //.setPublic(false) //dummy to false for version 2 to 3 upgrade.
//                    .setAdded(new Date(jBookmark.getLong("added") * 1000))
                    .setLastModified(new Date(jBookmark.getLong("lastmodified") * 1000))
                    .setClickcount(jBookmark.getInt("clickcount"))
                    .setTags(tags)
                    .setFolders(folders);
        } catch (JSONException je) {
            throw new RequestException("Could not gather all data", je);
        }
    }

    @Deprecated
    private String createBookmarkParameterString(Bookmark bookmark) {
        if (!bookmark.getTitle().isEmpty() && !bookmark.getUrl().startsWith("http")) {
            //tittle can only be set if the sheme is given
            //this is a bug we need to fix
            bookmark.setUrl("http://" + bookmark.getUrl());
        }

        String url = "?url=" + URLEncoder.encode(bookmark.getUrl());

        if (!bookmark.getTitle().isEmpty()) {
            url += "&title=" + URLEncoder.encode(bookmark.getTitle());
        }
        if (!bookmark.getDescription().isEmpty()) {
            url += "&description=" + URLEncoder.encode(bookmark.getDescription());
        }
//        if(bookmark.isPublic()) {
//            url += "&is_public=1";
//        }

        for (String tag : bookmark.getTags()) {
            url += "&" + URLEncoder.encode("tags[]") + "=" + URLEncoder.encode(tag);
        }

        return url;
    }

    private Collection<QueryParam> createBookmarkParameter(Bookmark bookmark) {
        final Collection<QueryParam> parameter = new ArrayList<>(3 + bookmark.getTags().size());
        if (!bookmark.getTitle().isEmpty() && !bookmark.getUrl().startsWith("http")) {
            // Title can only be set if the sheme is given
            // This is a bug we need to fix
            bookmark.setUrl("http://" + bookmark.getUrl());
        }

        parameter.add(new QueryParam("url", bookmark.getUrl()));
        parameter.add(new QueryParam("title", bookmark.getTitle()));
        parameter.add(new QueryParam("description", bookmark.getDescription()));

        for (String tag : bookmark.getTags()) {
            parameter.add(new QueryParam("tags[]", tag));
        }

        for (Integer folder : bookmark.getFolders()) {
            parameter.add(new QueryParam("folders[]", folder.toString()));
        }


        return parameter;
    }

    public Bookmark addBookmark(Bookmark bookmark) throws RequestException {
        try {
            if (bookmark.getId() == -1) {
                JSONObject reply;
                reply = sendWithSSO("POST", "/bookmark", createBookmarkParameter(bookmark));

                Log.e(TAG, "Bookmark Creation Reply: "+reply);
                return getBookmarkFromJsonO(reply.getJSONObject("item"));
            } else {
                throw new RequestException("Bookmark id is set. Maybe this bookmark already exist: id=" + bookmark.getId());
            }
        } catch (JSONException je) {
            throw new RequestException("Could not parse reply", je);
        }
    }

    public void deleteBookmark(Bookmark bookmark) throws RequestException {
        if (bookmark.getId() < 0) {
            return;
        }
        sendWithSSO("DELETE", "/bookmark/" + bookmark.getId(), Collections.emptyList());
    }

    public Bookmark editBookmark(Bookmark bookmark) throws RequestException {
        return editBookmark(bookmark, bookmark.getId());
    }

    public Bookmark editBookmark(Bookmark bookmark, int newRecordId) throws RequestException {
        if (bookmark.getId() < 0) {
            throw new RequestException("Bookmark has no valid id. Maybe you want to add a bookmark? id="
                    + bookmark.getId());
        }
        if (bookmark.getUrl().isEmpty()) {
            throw new RequestException("Bookmark has no url. Maybe you want to add a bookmark?");
        }

        Collection<QueryParam> parameter = createBookmarkParameter(bookmark);
        parameter.add(new QueryParam("record_id", Integer.toString(newRecordId)));
        return getBookmarkFromJsonO(sendWithSSO("PUT", "/bookmark/" + bookmark.getId(), parameter));
    }

    // ++++++++++++++++++
    // +      folders      +
    // ++++++++++++++++++

    public Folder getFolders() throws RequestException {
        try {
            JSONArray data = sendWithSSO("GET", "/folder", Collections.emptyList()).getJSONArray("data");
            Folder root = Folder.createEmptyRootFolder();
            fillChildren(root, data);
            return root;
        } catch (JSONException je) {
            throw new RequestException("Could not get all folders", je);
        }
    }

    private void fillChildren(Folder rootFolder, JSONArray children) {
        if (children == null || children.length() < 1) {
            return;
        }
        List<Folder> childFolderList = new ArrayList<>();
        for (int i = 0; i < children.length(); i++) {
            try {
                JSONObject folderJson = children.getJSONObject(i);
                Folder folder = new Folder();
                folder.setId(folderJson.getInt("id"));
                folder.setParentFolderId(folderJson.getInt("parent_folder"));
                folder.setTitle(folderJson.getString("title"));
                if (folderJson.has("children")) {
                    fillChildren(folder, folderJson.getJSONArray("children"));
                }
                childFolderList.add(folder);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        rootFolder.setChildren(childFolderList);
    }


    // ++++++++++++++++++
    // +      tags      +
    // ++++++++++++++++++

    public String[] getTags() throws RequestException {
        try {
            JSONArray data;
            data = sendWithSSO("GET", "/tag", Collections.emptyList()).getJSONArray("data");

            String[] tags = new String[data.length()];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = data.getString(i);
            }

            return tags;
        } catch (JSONException je) {
            throw new RequestException("Could not get all tags", je);
        }
    }

    public void deleteTag(String tag) throws RequestException {
        sendWithSSO("DELETE", "/tag", Collections.singletonList(new QueryParam("old_name", tag)));
    }

    public void renameTag(String oldName, String newName) throws RequestException {
        final Collection<QueryParam> parameter = new ArrayList<>(2);
        parameter.add(new QueryParam("old_name", oldName));
        parameter.add(new QueryParam("new_name", newName));
        sendWithSSO("POST", "/tag", parameter);
    }
}
