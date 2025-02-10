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

    // Mapa para almacenar las tasas de conversión
    private static final Map<String, Double> conversionRates = new HashMap<>();

    static {
        // Monedas
        conversionRates.put("Dólar-Euro", 0.92);
        conversionRates.put("Euro-Dólar", 1.09);
        conversionRates.put("Colón CR-Dólar", 0.0016);
        conversionRates.put("Dólar-Colón CR", 624.50);
        conversionRates.put("Quetzal-Dólar", 0.13);
        conversionRates.put("Lempira-Dólar", 0.041);

        // Masa
        conversionRates.put("Kilogramos-Libras", 2.20462);
        conversionRates.put("Libras-Kilogramos", 0.453592);
        conversionRates.put("Onzas-Gramos", 28.3495);
        conversionRates.put("Gramos-Onzas", 0.035274);

        // Volumen
        conversionRates.put("Litros-Mililitros", 1000.0);
        conversionRates.put("Mililitros-Litros", 0.001);
        conversionRates.put("Galones-Litros", 3.78541);
        conversionRates.put("Litros-Galones", 0.264172);

        // Longitud
        conversionRates.put("Kilómetros-Millas", 0.621371);
        conversionRates.put("Millas-Kilómetros", 1.60934);
        conversionRates.put("Metros-Pies", 3.28084);
        conversionRates.put("Pies-Metros", 0.3048);

        // Almacenamiento
        conversionRates.put("Bits-Bytes", 0.125);
        conversionRates.put("Bytes-Kilobytes", 0.0009765625);
        conversionRates.put("Kilobytes-Megabytes", 0.0009765625);
        conversionRates.put("Megabytes-Gigabytes", 0.0009765625);
        conversionRates.put("Gigabytes-Terabytes", 0.0009765625);

        // Tiempo
        conversionRates.put("Segundos-Minutos", 0.0166667);
        conversionRates.put("Minutos-Horas", 0.0166667);
        conversionRates.put("Horas-Días", 0.0416667);
        conversionRates.put("Días-Semanas", 0.142857);
        conversionRates.put("Semanas-Meses", 0.230137);
        conversionRates.put("Meses-Años", 0.0833333);

        // Transferencia de Datos
        conversionRates.put("Bits por segundo-Kilobits por segundo", 0.001);
        conversionRates.put("Kilobits por segundo-Megabits por segundo", 0.001);
        conversionRates.put("Megabits por segundo-Gigabits por segundo", 0.001);
        conversionRates.put("Gigabits por segundo-Terabits por segundo", 0.001);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuración de pestañas
        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        agregarPestana(getString(R.string.tab_monedas), R.id.tab1);
        agregarPestana(getString(R.string.tab_masa), R.id.tab2);
        agregarPestana(getString(R.string.tab_volumen), R.id.tab3);
        agregarPestana(getString(R.string.tab_longitud), R.id.tab4);
        agregarPestana(getString(R.string.tab_almacenamiento), R.id.tab5);
        agregarPestana(getString(R.string.tab_tiempo), R.id.tab6);
        agregarPestana(getString(R.string.tab_transferencia_datos), R.id.tab7);

        // Inicialización de Spinners y botones para cada pestaña
        inicializarPestanaMonedas();
        inicializarPestanaMasa();
        inicializarPestanaVolumen();
        inicializarPestanaLongitud();
        inicializarPestanaAlmacenamiento();
        inicializarPestanaTiempo();
        inicializarPestanaTransferencia();

        createNotificationChannel();
    }

    private void agregarPestana(String nombre, int id) {
        TabHost.TabSpec spec = tabHost.newTabSpec(nombre);
        spec.setContent(id);
        spec.setIndicator(nombre);
        tabHost.addTab(spec);
    }

    private void inicializarPestanaMonedas() {
        Spinner spnDeMonedas = findViewById(R.id.spnDeMonedas);
        Spinner spnAMonedas = findViewById(R.id.spnAMonedas);
        Button btnConvertirMonedas = findViewById(R.id.btnConvertirMonedas);
        EditText txtCantidadMonedas = findViewById(R.id.txtCantidadMonedas);
        TextView lblRespuestaMonedas = findViewById(R.id.lblRespuestaMonedas);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.monedas_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeMonedas.setAdapter(adapter);
        spnAMonedas.setAdapter(adapter);

        btnConvertirMonedas.setOnClickListener(v -> calcularConversion(
                txtCantidadMonedas, spnDeMonedas, spnAMonedas, lblRespuestaMonedas));
    }

    private void inicializarPestanaMasa() {
        Spinner spnDeMasa = findViewById(R.id.spnDeMasa);
        Spinner spnAMasa = findViewById(R.id.spnAMasa);
        Button btnConvertirMasa = findViewById(R.id.btnConvertirMasa);
        EditText txtCantidadMasa = findViewById(R.id.txtCantidadMasa);
        TextView lblRespuestaMasa = findViewById(R.id.lblRespuestaMasa);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.masa_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeMasa.setAdapter(adapter);
        spnAMasa.setAdapter(adapter);

        btnConvertirMasa.setOnClickListener(v -> calcularConversion(
                txtCantidadMasa, spnDeMasa, spnAMasa, lblRespuestaMasa));
    }

    private void inicializarPestanaVolumen() {
        Spinner spnDeVolumen = findViewById(R.id.spnDeVolumen);
        Spinner spnAVolumen = findViewById(R.id.spnAVolumen);
        Button btnConvertirVolumen = findViewById(R.id.btnConvertirVolumen);
        EditText txtCantidadVolumen = findViewById(R.id.txtCantidadVolumen);
        TextView lblRespuestaVolumen = findViewById(R.id.lblRespuestaVolumen);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.volumen_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeVolumen.setAdapter(adapter);
        spnAVolumen.setAdapter(adapter);

        btnConvertirVolumen.setOnClickListener(v -> calcularConversion(
                txtCantidadVolumen, spnDeVolumen, spnAVolumen, lblRespuestaVolumen));
    }

    private void inicializarPestanaLongitud() {
        Spinner spnDeLongitud = findViewById(R.id.spnDeLongitud);
        Spinner spnALongitud = findViewById(R.id.spnALongitud);
        Button btnConvertirLongitud = findViewById(R.id.btnConvertirLongitud);
        EditText txtCantidadLongitud = findViewById(R.id.txtCantidadLongitud);
        TextView lblRespuestaLongitud = findViewById(R.id.lblRespuestaLongitud);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.longitud_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeLongitud.setAdapter(adapter);
        spnALongitud.setAdapter(adapter);

        btnConvertirLongitud.setOnClickListener(v -> calcularConversion(
                txtCantidadLongitud, spnDeLongitud, spnALongitud, lblRespuestaLongitud));
    }

    private void inicializarPestanaAlmacenamiento() {
        Spinner spnDeAlmacenamiento = findViewById(R.id.spnDeAlmacenamiento);
        Spinner spnAAlmacenamiento = findViewById(R.id.spnAAlmacenamiento);
        Button btnConvertirAlmacenamiento = findViewById(R.id.btnConvertirAlmacenamiento);
        EditText txtCantidadAlmacenamiento = findViewById(R.id.txtCantidadAlmacenamiento);
        TextView lblRespuestaAlmacenamiento = findViewById(R.id.lblRespuestaAlmacenamiento);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.almacenamiento_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeAlmacenamiento.setAdapter(adapter);
        spnAAlmacenamiento.setAdapter(adapter);

        btnConvertirAlmacenamiento.setOnClickListener(v -> calcularConversion(
                txtCantidadAlmacenamiento, spnDeAlmacenamiento, spnAAlmacenamiento, lblRespuestaAlmacenamiento));
    }

    private void inicializarPestanaTiempo() {
        Spinner spnDeTiempo = findViewById(R.id.spnDeTiempo);
        Spinner spnATiempo = findViewById(R.id.spnATiempo);
        Button btnConvertirTiempo = findViewById(R.id.btnConvertirTiempo);
        EditText txtCantidadTiempo = findViewById(R.id.txtCantidadTiempo);
        TextView lblRespuestaTiempo = findViewById(R.id.lblRespuestaTiempo);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.tiempo_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeTiempo.setAdapter(adapter);
        spnATiempo.setAdapter(adapter);

        btnConvertirTiempo.setOnClickListener(v -> calcularConversion(
                txtCantidadTiempo, spnDeTiempo, spnATiempo, lblRespuestaTiempo));
    }

    private void inicializarPestanaTransferencia() {
        Spinner spnDeTransferencia = findViewById(R.id.spnDeTransferencia);
        Spinner spnATransferencia = findViewById(R.id.spnATransferencia);
        Button btnConvertirTransferencia = findViewById(R.id.btnConvertirTransferencia);
        EditText txtCantidadTransferencia = findViewById(R.id.txtCantidadTransferencia);
        TextView lblRespuestaTransferencia = findViewById(R.id.lblRespuestaTransferencia);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.transferencia_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeTransferencia.setAdapter(adapter);
        spnATransferencia.setAdapter(adapter);

        btnConvertirTransferencia.setOnClickListener(v -> calcularConversion(
                txtCantidadTransferencia, spnDeTransferencia, spnATransferencia, lblRespuestaTransferencia));
    }

    public void calcularConversion(EditText txtCantidad, Spinner spnDe, Spinner spnA, TextView lblRespuesta) {
        String cantidadStr = txtCantidad.getText().toString();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(this, R.string.ingrese_valor_valido, Toast.LENGTH_SHORT).show();
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.entrada_invalida, Toast.LENGTH_SHORT).show();
            return;
        }

        String de = spnDe.getSelectedItem().toString();
        String a = spnA.getSelectedItem().toString();
        String claveConversion = de + "-" + a;

        if (!conversionRates.containsKey(claveConversion)) {
            Toast.makeText(this, R.string.conversion_no_soportada, Toast.LENGTH_SHORT).show();
            return;
        }

        double resultado = cantidad * conversionRates.get(claveConversion);
        String mensaje = de + " a " + a + ": " + cantidad + " → " + resultado;
        lblRespuesta.setText(getString(R.string.lbl_resultado) + " " + resultado);

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
                .setContentTitle(getString(R.string.conversion_completada)) // Corregido aquí
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }
    }