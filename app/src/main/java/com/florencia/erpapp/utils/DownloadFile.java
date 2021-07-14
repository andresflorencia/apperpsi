package com.florencia.erpapp.utils;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadFile extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... strings) {
        String fileUrl = strings[0];   // -> http://maven.apache.org/maven-1.x/maven.pdf
        String fileName = strings[1];  // -> maven.pdf
        String extStorageDirectory = strings[2];//Environment.getExternalStorageDirectory().toString();
        File folder = new File(extStorageDirectory, "erpapk");
        folder.mkdir();
        File pdfFile = new File(folder, fileName);

        try {
            pdfFile.createNewFile();
        } catch (IOException e) {
            Log.d("TAG", e.getMessage());
            e.printStackTrace();
        }
        FileDownloader.downloadFile(fileUrl, pdfFile, null);
        return null;
    }


}