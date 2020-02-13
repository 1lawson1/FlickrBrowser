package com.bassettbrigade.flickrbrowser;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

enum DownloadStatus { IDLE,PROCESSING, NOT_INITIALISED, FAILED_OR_EMPTY,OK}

class GetRawData extends AsyncTask<String,Void,String> {
    private static final String TAG = "GetRawData";
    private final OnDownloadComplete callback;
    private DownloadStatus downloadStatus;

    interface OnDownloadComplete{
        void onDownloadComplete(String data, DownloadStatus status);
    }

    public GetRawData(OnDownloadComplete callback) {
        downloadStatus = DownloadStatus.IDLE;
        this.callback = callback;
    }

    void runInSameThread(String s){
        onPostExecute(doInBackground(s));
    }

    @Override
    protected void onPostExecute(String s) {
        if(callback != null){
            callback.onDownloadComplete(s, downloadStatus);
        }
    }

    @Override
    protected String doInBackground(String... strings) {
        HttpURLConnection  connection = null;
        BufferedReader reader = null;

        if(strings == null){
            downloadStatus = DownloadStatus.NOT_INITIALISED;
            return null;
        }

        try {
            downloadStatus = DownloadStatus.PROCESSING;
            URL url = new URL(strings[0]);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int response = connection.getResponseCode();
            Log.d(TAG, "doInBackground: The response code was " + response);

            StringBuilder result = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while(null != (line = reader.readLine())){
                result.append(line).append("\n");
            }

            downloadStatus = DownloadStatus.OK;
            return result.toString();

        }catch (MalformedURLException e){
            Log.e(TAG, "doInBackground: Invalid URL." + e.getMessage());
        }catch (IOException e) {
            Log.e(TAG, "doInBackground: Exception reading data." + e.getMessage());
        }catch (SecurityException e) {
            Log.e(TAG, "doInBackground: Security Exception. Need permission?" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null){
                try{
                    reader.close();
                } catch (IOException e){
                    Log.e(TAG, "doInBackground: Error closing stream " + e.getMessage());
                }
            }
        }

        downloadStatus = DownloadStatus.FAILED_OR_EMPTY;
        return null;
    }

}
