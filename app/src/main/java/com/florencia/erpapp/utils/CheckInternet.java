package com.florencia.erpapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class CheckInternet extends AsyncTask<String, String, String> {
    onlinelistener listener;
    private boolean corriendo = true;

    public interface onlinelistener {
        void isConnect(boolean connect);
    }

    public void setListener(onlinelistener listener) {
        this.listener = listener;
    }

    public void stop() {
        corriendo = false;
    }

    @Override
    protected String doInBackground(String... strings) {

        while (corriendo) { //System.out.println("Hilo internetbucle");
            if (isOnline())
                publishProgress("true");
            else publishProgress("false");
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        System.out.println("Hilo internetcheck terminado");
        super.onPostExecute(s);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values[0].equalsIgnoreCase("true"))
            listener.isConnect(true);
        else
            listener.isConnect(false);

        super.onProgressUpdate(values);
    }

    private boolean isOnline()//verifica conexion real
    {
        try {
            int timeoutMs = 1500;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
            sock.connect(sockaddr, timeoutMs);
            sock.close();
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
    }

    public boolean isNetworkRedconnect(Context context)//solo verifica conexion de red
    {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable()) {
                System.out.println("Conexion de Red Activa");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Conexion de Red No activa");
            e.printStackTrace();
        }
        return false;
    }


}
