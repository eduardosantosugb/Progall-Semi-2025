package com.ugb.miprimeraaplicacion;



import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText txtCantidad;
    private Spinner spnOpciones;
    private Button btnCalcular;
    private TextView lblRespuesta;
    private ListView lstHistorial;
    private TabHost tabHost;
    private ArrayAdapter<String> historialAdapter;
    private ArrayList<String> historialList;
    private final String CHANNEL_ID = "conversion_result";

    private static final Map<String, Double> conversionRates = new HashMap<>();

    static {
        // Conversión de Monedas
        conversionRates.put("Dólar a Euro", 0.92);
        conversionRates.put("Euro a Dólar", 1.09);
        conversionRates.put("Colón CR a Dólar", 0.0016);
        conversionRates.put("Dólar a Colón CR", 624.50);
        conversionRates.put("Quetzal a Dólar", 0.13);
        conversionRates.put("Lempira a Dólar", 0.041);

        // Conversión de Masa
        conversionRates.put("Kilogramos a Libras", 2.20462);
        conversionRates.put("Libras a Kilogramos", 0.453592);
        conversionRates.put("Onzas a Gramos", 28.3495);
        conversionRates.put("Gramos a Onzas", 0.035274);

        // Conversión de Volumen
        conversionRates.put("Litros a Mililitros", 1000.0);
        conversionRates.put("Mililitros a Litros", 0.001);
        conversionRates.put("Galones a Litros", 3.78541);
        conversionRates.put("Litros a Galones", 0.264172);

        // Conversión de Longitud
        conversionRates.put("Kilómetros a Millas", 0.621371);
        conversionRates.put("Millas a Kilómetros", 1.60934);
        conversionRates.put("Metros a Pies", 3.28084);
        conversionRates.put("Pies a Metros", 0.3048);

        // Conversión de Almacenamiento
        conversionRates.put("Bits a Bytes", 0.125);
        conversionRates.put("Bytes a Kilobytes", 0.0009765625);
        conversionRates.put("Kilobytes a Megabytes", 0.0009765625);
        conversionRates.put("Megabytes a Gigabytes", 0.0009765625);
        conversionRates.put("Gigabytes a Terabytes", 0.0009765625);

        // Conversión de Tiempo
        conversionRates.put("Segundos a Minutos", 0.0166667);
        conversionRates.put("Minutos a Horas", 0.0166667);
        conversionRates.put("Horas a Días", 0.0416667);
        conversionRates.put("Días a Semanas", 0.142857);
        conversionRates.put("Semanas a Meses", 0.230137);
        conversionRates.put("Meses a Años", 0.0833333);

        // Conversión de Transferencia de Datos
        conversionRates.put("Bits por segundo a Kilobits por segundo", 0.001);
        conversionRates.put("Kilobits por segundo a Megabits por segundo", 0.001);
        conversionRates.put("Megabits por segundo a Gigabits por segundo", 0.001);
        conversionRates.put("Gigabits por segundo a Terabits por segundo", 0.001);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar TabHost
        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("Conversion");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Conversión");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Historial");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Historial");
        tabHost.addTab(spec);

        // Inicializar elementos
        txtCantidad = findViewById(R.id.txtCantidad);
        spnOpciones = findViewById(R.id.spnOpciones);
        btnCalcular = findViewById(R.id.btnCalcular);
        lblRespuesta = findViewById(R.id.lblRespuesta);
        lstHistorial = findViewById(R.id.lstHistorial);

        // Configurar historial
        historialList = new ArrayList<>();
        historialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historialList);
        lstHistorial.setAdapter(historialAdapter);

        // Configurar Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.opciones_conversion, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnOpciones.setAdapter(adapter);

        // Acción del botón
        btnCalcular.setOnClickListener(v -> calcularConversion());

        createNotificationChannel();
    }

    private void calcularConversion() {
        String cantidadStr = txtCantidad.getText().toString();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un valor válido", Toast.LENGTH_SHORT).show();
            return;
        }

        double cantidad = Double.parseDouble(cantidadStr);
        String opcion = spnOpciones.getSelectedItem().toString();
        double resultado = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resultado = cantidad * conversionRates.getOrDefault(opcion, 1.0);
        }

        String mensaje = "Resultado: " + resultado;
        lblRespuesta.setText(mensaje);

        historialList.add(opcion + ": " + cantidad + " → " + resultado);
        historialAdapter.notifyDataSetChanged();

        mostrarNotificacion(mensaje);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Canal de Conversiones";
            String description = "Canal para notificaciones de conversiones";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void mostrarNotificacion(String mensaje) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Conversión Completada")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }
}