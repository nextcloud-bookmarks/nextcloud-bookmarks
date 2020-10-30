package org.schabi.ocbookmarks.REST;

import android.util.Log;

import androidx.annotation.Nullable;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.api.Response;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Created by the-scrabi on 14.05.17.
 * Modified by @dasbiswajit on 14.04.2019
 */


public class OCBookmarksRestConnector {
    private final String apiRootUrl;
    private final String usr;
    private final String pwd;
    @Nullable
    private final NextcloudAPI nextcloudAPI;

    private static final int TIME_OUT = 10000; // in milliseconds

    private static final String TAG = "ocbookmarks";

    /**
     * @param rootUrl      root url without trailing slash
     * @param user         username of the account
     * @param password     password
     * @param nextcloudAPI will be used if not null instead of traditional user / password authentication
     */
    public OCBookmarksRestConnector(String rootUrl, String user, String password, @Nullable NextcloudAPI nextcloudAPI) {
        usr = user;
        pwd = password;
        this.nextcloudAPI = nextcloudAPI;
        if (this.nextcloudAPI == null) {
            apiRootUrl = rootUrl + "/index.php/apps/bookmarks/public/rest/v2";
        } else {
            apiRootUrl = "/index.php/apps/bookmarks/public/rest/v2";
        }
    }

    public JSONObject send(String methode, String relativeUrl) throws RequestException {
        Log.e(TAG, "Connection String for Debug!" + apiRootUrl); //For Debug purpose
        if (nextcloudAPI != null) {
            final NextcloudRequest request = new NextcloudRequest
                    .Builder()
                    .setMethod(methode)
                    .setUrl(apiRootUrl + relativeUrl)
                    .build();
            final Response response;
            try {
                response = this.nextcloudAPI.performNetworkRequestV2(request);

                final StringBuilder result = new StringBuilder();
                final BufferedReader rd = new BufferedReader(new InputStreamReader(response.getBody()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                response.getBody().close();
                return parseJson(methode, apiRootUrl + relativeUrl, result.toString());
            } catch (Exception e) {
                throw new RequestException(e);
            }
        } else {
            final URL url;
            if (apiRootUrl.startsWith("https")) {
                Log.e(TAG, "apiRootUrl value is https:" + apiRootUrl); //#TODO: add functionality for http and https.
            }
            try {
                url = new URL(apiRootUrl + relativeUrl);
                BufferedReader in = null;
                StringBuilder response = new StringBuilder();
                HttpURLConnection connection = null;
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(methode);
                connection.setConnectTimeout(TIME_OUT);
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64((usr + ":" + pwd).getBytes())));
                Log.e(TAG, "Connection success!!");
                try {
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                } catch (Exception e) {
                    if (e.getMessage().contains("500")) {
                        throw new PermissionException(e);
                    }
                    throw new RequestException(e);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        connection.disconnect();
                    } catch (Exception e) {
                        throw new RequestException("Could not close connection", e);
                    }
                }
                return parseJson(methode, url.toString(), response.toString());
            } catch (IOException e) {
                throw new RequestException(e);
            }
        }
    }

    private JSONObject parseJson(String methode, String url, String response) throws RequestException {

        JSONObject data = null;
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
            return send("GET", "/bookmark?page=-1")
                    .getJSONArray("data");
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

        String[] tags;
        try {
            JSONArray jTags = jBookmark.getJSONArray("tags");
            tags = new String[jTags.length()];
            for (int j = 0; j < tags.length; j++) {
                tags[j] = jTags.getString(j);
            }
        } catch (JSONException je) {
            throw new RequestException("Could not parse array", je);
        }

        //another api error we need to fix
        if (tags.length == 1 && tags[0].isEmpty()) {
            tags = new String[0];
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
                    .setTags(tags);
        } catch (JSONException je) {
            throw new RequestException("Could not gather all data", je);
        }
    }

    private String createBookmarkParameter(Bookmark bookmark) {
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

    public Bookmark addBookmark(Bookmark bookmark) throws RequestException {
        try {
            if (bookmark.getId() == -1) {
                String url = "/bookmark" + createBookmarkParameter(bookmark);

                Log.e(TAG, "url String" + url);

                JSONObject replay = send("POST", url);
                return getBookmarkFromJsonO(replay.getJSONObject("item"));
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
        send("DELETE", "/bookmark/" + Integer.toString(bookmark.getId()));
    }

    public Bookmark editBookmark(Bookmark bookmark) throws RequestException {
        return editBookmark(bookmark, bookmark.getId());
    }

    public Bookmark editBookmark(Bookmark bookmark, int newRecordId) throws RequestException {
        if (bookmark.getId() < 0) {
            throw new RequestException("Bookmark has no valid id. Maybe you want to add a bookmark? id="
                    + Integer.toString((bookmark.getId())));
        }
        if (bookmark.getUrl().isEmpty()) {
            throw new RequestException("Bookmark has no url. Maybe you want to add a bookmark?");
        }
        String url = "/bookmark/" + Integer.toString(bookmark.getId()) + createBookmarkParameter(bookmark);
        url += "&record_id=" + Integer.toString(newRecordId);

        return getBookmarkFromJsonO(send("PUT", url));
    }

    // ++++++++++++++++++
    // +      tags      +
    // ++++++++++++++++++

    public String[] getTags() throws RequestException {
        try {
            JSONArray data = send("GET", "/tag").getJSONArray("data");

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
        send("DELETE", "/tag?old_name=" + URLEncoder.encode(tag));
    }

    public void renameTag(String oldName, String newName) throws RequestException {
        send("POST", "/tag?old_name=" + URLEncoder.encode(oldName)
                + "&new_name=" + URLEncoder.encode(newName));
    }
}
