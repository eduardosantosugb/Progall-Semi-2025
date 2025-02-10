package com.ugb.miprimeraaplicacion;


import android.Manifest;
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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TabHost tabHost;
    private final String CHANNEL_ID = "conversion_result";

    private static final Map<String, Double> conversionRates = new HashMap<>();

    static {
        // Monedas
        conversionRates.put("Dólar a Euro", 0.92);
        conversionRates.put("Euro a Dólar", 1.09);
        conversionRates.put("Colón CR a Dólar", 0.0016);
        conversionRates.put("Dólar a Colón CR", 624.50);
        conversionRates.put("Quetzal a Dólar", 0.13);
        conversionRates.put("Lempira a Dólar", 0.041);

        // Masa
        conversionRates.put("Kilogramos a Libras", 2.20462);
        conversionRates.put("Libras a Kilogramos", 0.453592);
        conversionRates.put("Onzas a Gramos", 28.3495);
        conversionRates.put("Gramos a Onzas", 0.035274);

        // Volumen
        conversionRates.put("Litros a Mililitros", 1000.0);
        conversionRates.put("Mililitros a Litros", 0.001);
        conversionRates.put("Galones a Litros", 3.78541);
        conversionRates.put("Litros a Galones", 0.264172);

        // Longitud
        conversionRates.put("Kilómetros a Millas", 0.621371);
        conversionRates.put("Millas a Kilómetros", 1.60934);
        conversionRates.put("Metros a Pies", 3.28084);
        conversionRates.put("Pies a Metros", 0.3048);

        // Almacenamiento
        conversionRates.put("Bits a Bytes", 0.125);
        conversionRates.put("Bytes a Kilobytes", 0.0009765625);
        conversionRates.put("Kilobytes a Megabytes", 0.0009765625);
        conversionRates.put("Megabytes a Gigabytes", 0.0009765625);
        conversionRates.put("Gigabytes a Terabytes", 0.0009765625);

        // Tiempo
        conversionRates.put("Segundos a Minutos", 0.0166667);
        conversionRates.put("Minutos a Horas", 0.0166667);
        conversionRates.put("Horas a Días", 0.0416667);
        conversionRates.put("Días a Semanas", 0.142857);
        conversionRates.put("Semanas a Meses", 0.230137);
        conversionRates.put("Meses a Años", 0.0833333);

        // Transferencia de Datos
        conversionRates.put("Bits por segundo a Kilobits por segundo", 0.001);
        conversionRates.put("Kilobits por segundo a Megabits por segundo", 0.001);
        conversionRates.put("Megabits por segundo a Gigabits por segundo", 0.001);
        conversionRates.put("Gigabits por segundo a Terabits por segundo", 0.001);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuración de pestañas
        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        agregarPestana("Monedas", R.id.tab1);
        agregarPestana("Masa", R.id.tab2);
        agregarPestana("Volumen", R.id.tab3);
        agregarPestana("Longitud", R.id.tab4);
        agregarPestana("Almacenamiento", R.id.tab5);
        agregarPestana("Tiempo", R.id.tab6);
        agregarPestana("Transferencia de Datos", R.id.tab7);

        // Inicialización de Spinners
        Spinner spinnerMonedas = findViewById(R.id.spnMonedas);
        Spinner spinnerMasa = findViewById(R.id.spnMasa);
        Spinner spinnerVolumen = findViewById(R.id.spnVolumen);
        Spinner spinnerLongitud = findViewById(R.id.spnLongitud);
        Spinner spinnerAlmacenamiento = findViewById(R.id.spnAlmacenamiento);
        Spinner spinnerTiempo = findViewById(R.id.spnTiempo);
        Spinner spinnerTransferencia = findViewById(R.id.spnTransferencia);

        // Crear ArrayAdapter específicos para cada Spinner
        ArrayAdapter<CharSequence> adapterMonedas = ArrayAdapter.createFromResource(
                this, R.array.monedas_options, android.R.layout.simple_spinner_item);
        adapterMonedas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterMasa = ArrayAdapter.createFromResource(
                this, R.array.masa_options, android.R.layout.simple_spinner_item);
        adapterMasa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterVolumen = ArrayAdapter.createFromResource(
                this, R.array.volumen_options, android.R.layout.simple_spinner_item);
        adapterVolumen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterLongitud = ArrayAdapter.createFromResource(
                this, R.array.longitud_options, android.R.layout.simple_spinner_item);
        adapterLongitud.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterAlmacenamiento = ArrayAdapter.createFromResource(
                this, R.array.almacenamiento_options, android.R.layout.simple_spinner_item);
        adapterAlmacenamiento.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterTiempo = ArrayAdapter.createFromResource(
                this, R.array.tiempo_options, android.R.layout.simple_spinner_item);
        adapterTiempo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> adapterTransferencia = ArrayAdapter.createFromResource(
                this, R.array.transferencia_options, android.R.layout.simple_spinner_item);
        adapterTransferencia.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asignar adaptadores a los Spinners
        spinnerMonedas.setAdapter(adapterMonedas);
        spinnerMasa.setAdapter(adapterMasa);
        spinnerVolumen.setAdapter(adapterVolumen);
        spinnerLongitud.setAdapter(adapterLongitud);
        spinnerAlmacenamiento.setAdapter(adapterAlmacenamiento);
        spinnerTiempo.setAdapter(adapterTiempo);
        spinnerTransferencia.setAdapter(adapterTransferencia);

        // Configurar botones para calcular conversiones
        Button btnConvertirMonedas = findViewById(R.id.btnConvertirMonedas);
        Button btnConvertirMasa = findViewById(R.id.btnConvertirMasa);
        Button btnConvertirVolumen = findViewById(R.id.btnConvertirVolumen);
        Button btnConvertirLongitud = findViewById(R.id.btnConvertirLongitud);
        Button btnConvertirAlmacenamiento = findViewById(R.id.btnConvertirAlmacenamiento);
        Button btnConvertirTiempo = findViewById(R.id.btnConvertirTiempo);
        Button btnConvertirTransferencia = findViewById(R.id.btnConvertirTransferencia);

        btnConvertirMonedas.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadMonedas);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaMonedas);
            calcularConversion(txtCantidad, spinnerMonedas, lblRespuesta);
        });

        btnConvertirMasa.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadMasa);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaMasa);
            calcularConversion(txtCantidad, spinnerMasa, lblRespuesta);
        });

        btnConvertirVolumen.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadVolumen);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaVolumen);
            calcularConversion(txtCantidad, spinnerVolumen, lblRespuesta);
        });

        btnConvertirLongitud.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadLongitud);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaLongitud);
            calcularConversion(txtCantidad, spinnerLongitud, lblRespuesta);
        });

        btnConvertirAlmacenamiento.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadAlmacenamiento);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaAlmacenamiento);
            calcularConversion(txtCantidad, spinnerAlmacenamiento, lblRespuesta);
        });

        btnConvertirTiempo.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadTiempo);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaTiempo);
            calcularConversion(txtCantidad, spinnerTiempo, lblRespuesta);
        });

        btnConvertirTransferencia.setOnClickListener(v -> {
            EditText txtCantidad = findViewById(R.id.txtCantidadTransferencia);
            TextView lblRespuesta = findViewById(R.id.lblRespuestaTransferencia);
            calcularConversion(txtCantidad, spinnerTransferencia, lblRespuesta);
        });

        createNotificationChannel();
    }

    private void agregarPestana(String nombre, int id) {
        TabHost.TabSpec spec = tabHost.newTabSpec(nombre);
        spec.setContent(id);
        spec.setIndicator(nombre);
        tabHost.addTab(spec);
    }

    public void calcularConversion(EditText txtCantidad, Spinner spnOpciones, TextView lblRespuesta) {
        String cantidadStr = txtCantidad.getText().toString();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un valor válido", Toast.LENGTH_SHORT).show();
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Entrada inválida. Ingrese un número.", Toast.LENGTH_SHORT).show();
            return;
        }

        String opcion = spnOpciones.getSelectedItem().toString();
        if (!conversionRates.containsKey(opcion)) {
            Toast.makeText(this, "Conversión no soportada", Toast.LENGTH_SHORT).show();
            return;
        }

        double resultado = cantidad * conversionRates.get(opcion);
        String mensaje = opcion + ": " + cantidad + " → " + resultado;
        lblRespuesta.setText("Resultado: " + resultado);

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }
}