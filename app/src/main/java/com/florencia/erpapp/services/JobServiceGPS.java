package com.florencia.erpapp.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.florencia.erpapp.models.Ubicacion;
import com.florencia.erpapp.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JobServiceGPS extends JobService {

    private boolean jobCancelled = false;
    SharedPreferences sPreferences;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("TAG", "onStartJob");
        tomarPosicion(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("TAG", "onStopJob");
        jobCancelled = true;
        return false;
    }

    private void tomarPosicion(final JobParameters params) {
        Log.d("TAG", "tomarPosicion");
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //for(int i = 0; i < 10; i++){
                    if (jobCancelled)
                        return;
                    //Log.d("TAG", "tarea: " + (i+1));
                    try {
                        sPreferences = getSharedPreferences("DatosSesion", MODE_PRIVATE);
                        int idUser = sPreferences.getInt("idUser", 0);
                        String rucempresa = sPreferences.getString("rucempresa", "");
                        if (idUser > 0 && !rucempresa.equals("") && permitirJob()) {
                            Context co = getApplication().getApplicationContext();
                            GPSTracker gpsTracker = new GPSTracker(co);
                            Location miLocation = gpsTracker.getLastKnownLocation();
                            SQLite.sqlDB = new SQLite(getApplication().getApplicationContext(), true);
                            if (miLocation != null) {
                                Ubicacion.Save(idUser, rucempresa, miLocation.getLatitude(), miLocation.getLongitude());
                                //Log.d("TAG", "Lat: " + miLocation.getLatitude() + " - Lon: " + miLocation.getLongitude());
                            }
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    //}
                    //Log.d("TAG", "Job Terminado");
                    jobFinished(params, true);
                }
            }).start();
        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
        }
    }

    private boolean permitirJob() {
        boolean retorno = false;
        try {
            //Lo primero que tienes que hacer es establecer el formato que tiene tu fecha para que puedas obtener un objeto de tipo Date el cual es el que se utiliza para obtener la diferencia.
            Locale idioma = new Locale("es", "ES");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", idioma);

            //Parceas tus fechas en string a variables de tipo date se agrega un try catch porque si el formato declarado anteriormente no es igual a tu fecha obtendrás una excepción
            Date dateIniciaDia = dateFormat.parse(Utils.getDateFormat("yyyy/MM/dd") + " 07:00:00");
            Date dateFinalDia = dateFormat.parse(Utils.getDateFormat("yyyy/MM/dd") + " 18:00:00");
            Date dateNow = dateFormat.parse(Utils.getDateFormat("yyyy/MM/dd HH:mm:ss"));

            //obtienes la diferencia de las fechas
            long millInicio = dateIniciaDia.getTime();
            long millFinal = dateFinalDia.getTime();
            long millNow = dateNow.getTime();

            //Obtener diferencia en milisegundos
            //long horas = TimeUnit.MINUTES.convert(difference, TimeUnit.MILLISECONDS);
            retorno = (millNow >= millInicio && millNow <= millFinal);
            //Log.d("TAG",retorno?"SI ESTÀ EN EL RANGO DE HORAS": "NO ESTÀ EN EL RANGO DE HORAS");
            //Toast.makeText(getApplication().getApplicationContext(), retorno?"Si está en el rango": "No está en el rango",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            retorno = false;
            Log.d("TAG", e.getMessage());
        }
        return retorno;
    }

}

