package com.ugb.tiendacouchdb;

import android.os.AsyncTask;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebServiceClient {

    // URL base de CouchDB (ejemplo; debe ajustarse a tu servidor)
    private static final String BASE_URL = "http://127.0.0.1:5984/_utils/#/database/tienda/_design/productos/_view/by_codigo";


    // Método para enviar (insertar o actualizar) un producto
    public static void sendProduct(final Product product, final WebServiceCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            new AsyncTask<Void, Void, String>() {
                Exception exception;
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        URL url = new URL(BASE_URL + "/producto");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);

                        // Construir JSON del producto
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("codigo", product.getCodigo());
                        jsonParam.put("descripcion", product.getDescripcion());
                        jsonParam.put("marca", product.getMarca());
                        jsonParam.put("presentacion", product.getPresentacion());
                        jsonParam.put("precio", product.getPrecio());
                        jsonParam.put("costo", product.getCosto());
                        jsonParam.put("ganancia", product.getGanancia());
                        jsonParam.put("imagen", product.getImagen());

                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        os.writeBytes(jsonParam.toString());
                        os.flush();
                        os.close();

                        // Leer respuesta
                        int responseCode = conn.getResponseCode();
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return response.toString();
                    } catch (Exception e) {
                        exception = e;
                        return null;
                    }
                }
                @Override
                protected void onPostExecute(String s) {
                    if (exception != null) {
                        callback.onError(exception);
                    } else {
                        callback.onSuccess(s);
                    }
                }
            }.execute();
        }
    }


    public interface WebServiceCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }
}
