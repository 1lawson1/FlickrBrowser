package com.bassettbrigade.flickrbrowser;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class GetFlickrJsonData extends AsyncTask<String, Void, List<Photo>> implements GetRawData.OnDownloadComplete {
    private static final String TAG = "GetFlickrJsonData";
    private List<Photo> photoList = null;
    private String url;
    private String language;
    private boolean matchAll;
    private boolean runningOnSameThread = false;

    private final OnDataAvailable callback;

    interface OnDataAvailable {
        void onDataAvailable(List<Photo> data, DownloadStatus status);
    }

    public GetFlickrJsonData(String url, String language, boolean matchAll, OnDataAvailable callback) {
        Log.d(TAG, "GetFlickrJsonData: called");
        this.url = url;
        this.language = language;
        this.matchAll = matchAll;
        this.callback = callback;
    }

    void executeOnSameThread(String searchCriteria){
        Log.d(TAG, "executeOnSameThread:  starts");
        runningOnSameThread = true;
        String destinationURI = createUri(searchCriteria, language, matchAll);

        GetRawData getRawData = new GetRawData(this);
        getRawData.execute(destinationURI);
        Log.d(TAG, "executeOnSameThread: ends");
    }

    @Override
    protected void onPostExecute(List<Photo> photos) {
        Log.d(TAG, "onPostExecute: start");
        if(callback != null){
            callback.onDataAvailable(photoList, DownloadStatus.OK);
        }
        Log.d(TAG, "onPostExecute: ends");
    }

    @Override
    protected List<Photo> doInBackground(String... params) {
        Log.d(TAG, "doInBackground: starts");
        String destinationUri = createUri(params[0],language, matchAll);

        GetRawData getRawData = new GetRawData(this);
        getRawData.runInSameThread(destinationUri);
        Log.d(TAG, "doInBackground: ends");
        return photoList;
    }

    private String createUri(String searchCriteria, String language, boolean matchAll){
        Log.d(TAG, "createUri: starts");

//        Uri uri = Uri.parse(url);
//        Uri.Builder builder = uri.buildUpon();
//        builder = builder.appendQueryParameter("tags", searchCriteria);
//        builder = builder.appendQueryParameter("tagmode", matchAll ? "ALL":"ANY");
//        builder = builder.appendQueryParameter("lang", language);
//        builder = builder.appendQueryParameter("format", "json");
//        builder = builder.appendQueryParameter("nojsoncallback", "1");
//        uri = builder.build();

        return Uri.parse(url).buildUpon()
                .appendQueryParameter("tags", searchCriteria)
                .appendQueryParameter("tagmode", matchAll ? "All": "ANY")
                .appendQueryParameter("lang", language)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback","1")
                .build().toString();
    }
    @Override
    public void onDownloadComplete(String data, DownloadStatus status) {
        Log.d(TAG, "onDownloadComplete: starts status = " + status);

        if(status == DownloadStatus.OK){
            photoList = new ArrayList<>();
            try{
                JSONObject jsonObject = new JSONObject(data);
                JSONArray jsonArray = jsonObject.getJSONArray("items");

                for(int i = 0; i<jsonArray.length();i++){
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    String title = jsonObject1.getString("title");
                    String author = jsonObject1.getString("author");
                    String authorId = jsonObject1.getString("author_id");
                    String tags = jsonObject1.getString("tags");

                    JSONObject jsonObject2 = jsonObject1.getJSONObject("media");
                    String photoUrl = jsonObject2.getString("m");

                    String link = photoUrl.replaceFirst("_m.","_b.");

                    Photo photo = new Photo(title,author,authorId,link,tags,photoUrl);
                    photoList.add(photo);

                    Log.d(TAG, "onDownloadComplete: " + photo.toString());
                }
            } catch(JSONException e){
                e.printStackTrace();
                Log.e(TAG, "onDownloadComplete: Error occurred - " + e.getMessage());
                status = DownloadStatus.FAILED_OR_EMPTY;
            }
        }
        if(runningOnSameThread && callback != null){
            callback.onDataAvailable(photoList, status);
        }
        Log.d(TAG, "onDownloadComplete: ends");
    }
}
