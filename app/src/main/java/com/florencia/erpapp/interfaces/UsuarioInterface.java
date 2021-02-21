package com.florencia.erpapp.interfaces;

import com.florencia.erpapp.models.Usuario;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface UsuarioInterface {

    @FormUrlEncoded
    @POST("registramovil")
    Call<JsonObject> IniciarSesion(@Field("usuario") String user, @Field("clave") String clave,
                                    @Field("phone") String phone);

    @POST("getlastversion")
    Call<JsonObject> getLastVersion();

    @POST("verificaconexion")
    Call<JsonObject> verificaconexion();

    @GET
    Call<ResponseBody> downloadApk(@Url String url);
}

