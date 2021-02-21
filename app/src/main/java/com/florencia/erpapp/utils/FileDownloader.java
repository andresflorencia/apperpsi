package com.florencia.erpapp.utils;

import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownloader {
    private static final int  MEGABYTE = 2048 * 2048;

    public static boolean downloadFile(String fileUrl, File directory, ProgressBar progressBar){
        try {

            URL url = new URL(fileUrl);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(directory);
            int totalSize = urlConnection.getContentLength();
            Log.d("TAG", "tamaio: " + totalSize);
            byte[] buffer = new byte[MEGABYTE];
            int bufferLength = 0;
            //progressBar.setProgress(0);
            //progressBar.setMax(totalSize);
            int i = 1;
            while((bufferLength = inputStream.read(buffer))>0 ){
                fileOutputStream.write(buffer, 0, bufferLength);
                Log.d("TAG", "Progreso: " + (i+1));
                i++;
            }
            fileOutputStream.close();
            Log.d("TAGDOWN", "Archivo descargado");
            return true;
        } catch (FileNotFoundException e) {
            Log.d("TAGDOWN", "NotFound(): "+ e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            Log.d("TAGDOWN", "Malformed(): "+ e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("TAGDOWN", "IO(): "+ e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}