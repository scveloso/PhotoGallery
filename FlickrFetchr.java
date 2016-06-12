package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;
import android.widget.Gallery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Veloso on 6/11/2016.
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "308d2387069589c13e9c84d01ab0792b";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    private List<GalleryItem> items = new ArrayList<>();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        // Create a URL from given String url
        URL url = new URL(urlSpec);

        // Create a connection to the URL made
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Need to call getInputStream to actually connect to endpoint
            // Also needed to get response code
            InputStream in = connection.getInputStream();

            // If response from made connection is not ok, throw exception
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            // Call read repeatedly until your connection runs out of data
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            // When done, close and spit out byte array
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    // Converts bytes fetched by getUrlBytes(String) into a String
    public String getURLString (String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        try {
            String jsonString = getURLString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        return uriBuilder.build().toString();
    }

    private void parseItems (List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photosJsonArray.length(); i++) {
            JSONObject photoJsonObject = photosJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            // Ignore images that do not have an image url
            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}
