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

public class MainActivity extends AppCompatActivity {

    private TabHost tabHost;
    private final String CHANNEL_ID = "conversion_result";

    // Matrices para nombres de unidades y tasas de conversión
    private final String[][] unitNames = {
            {"Dólar", "Euro", "Libra Esterlina", "Yen Japonés", "Yuan Chino", "Peso Mexicano", "Peso Argentino", "Real Brasileño", "Rublo Ruso", "Franco Suizo"}, // Monedas
            {"Kilogramo", "Gramo", "Miligramo", "Tonelada", "Libra", "Onza", "Stone", "Microgramo", "Quintal", "Slug"}, // Masa
            {"Litro", "Mililitro", "Galón", "Pinta", "Taza", "Onza líquida", "Barril", "Metro cúbico", "Decilitro", "Cuarto"}, // Volumen
            {"Metro", "Centímetro", "Milímetro", "Kilómetro", "Milla", "Yarda", "Pie", "Pulgada", "Nanómetro", "Micrómetro"}, // Longitud
            {"Byte", "Kilobyte", "Megabyte", "Gigabyte", "Terabyte", "Petabyte", "Exabyte", "Zettabyte", "Yottabyte", "Bit"}, // Almacenamiento
            {"Segundo", "Minuto", "Hora", "Día", "Semana", "Mes", "Año", "Milisegundo", "Microsegundo", "Nanosegundo"}, // Tiempo
            {"Bits por segundo", "Kilobits por segundo", "Megabits por segundo", "Gigabits por segundo", "Terabits por segundo", "Petabits por segundo", "Exabits por segundo", "Zettabits por segundo", "Yottabits por segundo", "Transmisión de símbolos por segundos"} // Transferencia de datos
    };

    private final double[][] conversionRates = {
            {1, 0.85, 0.75, 110.53, 6.45, 74.39, 1.25, 1.34, 0.91, 5.26}, // Monedas
            {1, 1000, 1000000, 0.001, 2.20462, 35.274, 0.15747, 1000000000, 0.01, 0.06852}, // Masa
            {1, 1000, 0.264172, 2.11338, 4.22675, 33.814, 0.008648, 0.001, 10, 1.05669}, // Volumen
            {1, 100, 1000, 0.001, 0.000621, 1.09361, 3.28084, 39.3701, 1000000000, 1000000}, // Longitud
            {1, 0.001, 0.000001, 0.000000001, 0.000000000001, 0.000000000000001, 0.000000000000000001, 0.000000000000000000001, 0.000000000000000000000001, 8}, // Almacenamiento
            {1, 0.01667, 0.0002778, 0.00001157, 0.00000165, 0.00000038, 0.0000000317, 1000, 1000000, 1000000000}, // Tiempo
            {1, 0.001, 0.000001, 0.000000001, 0.000000000001, 0.000000000000001, 0.000000000000000001, 0.000000000000000000001, 0.000000000000000000000001, 1} // Transferencia de datos
    };

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[0]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeMonedas.setAdapter(adapter);
        spnAMonedas.setAdapter(adapter);

        btnConvertirMonedas.setOnClickListener(v -> calcularConversion(
                txtCantidadMonedas, spnDeMonedas, spnAMonedas, lblRespuestaMonedas, 0));
    }

    private void inicializarPestanaMasa() {
        Spinner spnDeMasa = findViewById(R.id.spnDeMasa);
        Spinner spnAMasa = findViewById(R.id.spnAMasa);
        Button btnConvertirMasa = findViewById(R.id.btnConvertirMasa);
        EditText txtCantidadMasa = findViewById(R.id.txtCantidadMasa);
        TextView lblRespuestaMasa = findViewById(R.id.lblRespuestaMasa);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[1]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeMasa.setAdapter(adapter);
        spnAMasa.setAdapter(adapter);

        btnConvertirMasa.setOnClickListener(v -> calcularConversion(
                txtCantidadMasa, spnDeMasa, spnAMasa, lblRespuestaMasa, 1));
    }

    private void inicializarPestanaVolumen() {
        Spinner spnDeVolumen = findViewById(R.id.spnDeVolumen);
        Spinner spnAVolumen = findViewById(R.id.spnAVolumen);
        Button btnConvertirVolumen = findViewById(R.id.btnConvertirVolumen);
        EditText txtCantidadVolumen = findViewById(R.id.txtCantidadVolumen);
        TextView lblRespuestaVolumen = findViewById(R.id.lblRespuestaVolumen);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[2]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeVolumen.setAdapter(adapter);
        spnAVolumen.setAdapter(adapter);

        btnConvertirVolumen.setOnClickListener(v -> calcularConversion(
                txtCantidadVolumen, spnDeVolumen, spnAVolumen, lblRespuestaVolumen, 2));
    }

    private void inicializarPestanaLongitud() {
        Spinner spnDeLongitud = findViewById(R.id.spnDeLongitud);
        Spinner spnALongitud = findViewById(R.id.spnALongitud);
        Button btnConvertirLongitud = findViewById(R.id.btnConvertirLongitud);
        EditText txtCantidadLongitud = findViewById(R.id.txtCantidadLongitud);
        TextView lblRespuestaLongitud = findViewById(R.id.lblRespuestaLongitud);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[3]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeLongitud.setAdapter(adapter);
        spnALongitud.setAdapter(adapter);

        btnConvertirLongitud.setOnClickListener(v -> calcularConversion(
                txtCantidadLongitud, spnDeLongitud, spnALongitud, lblRespuestaLongitud, 3));
    }

    private void inicializarPestanaAlmacenamiento() {
        Spinner spnDeAlmacenamiento = findViewById(R.id.spnDeAlmacenamiento);
        Spinner spnAAlmacenamiento = findViewById(R.id.spnAAlmacenamiento);
        Button btnConvertirAlmacenamiento = findViewById(R.id.btnConvertirAlmacenamiento);
        EditText txtCantidadAlmacenamiento = findViewById(R.id.txtCantidadAlmacenamiento);
        TextView lblRespuestaAlmacenamiento = findViewById(R.id.lblRespuestaAlmacenamiento);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[4]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeAlmacenamiento.setAdapter(adapter);
        spnAAlmacenamiento.setAdapter(adapter);

        btnConvertirAlmacenamiento.setOnClickListener(v -> calcularConversion(
                txtCantidadAlmacenamiento, spnDeAlmacenamiento, spnAAlmacenamiento, lblRespuestaAlmacenamiento, 4));
    }

    private void inicializarPestanaTiempo() {
        Spinner spnDeTiempo = findViewById(R.id.spnDeTiempo);
        Spinner spnATiempo = findViewById(R.id.spnATiempo);
        Button btnConvertirTiempo = findViewById(R.id.btnConvertirTiempo);
        EditText txtCantidadTiempo = findViewById(R.id.txtCantidadTiempo);
        TextView lblRespuestaTiempo = findViewById(R.id.lblRespuestaTiempo);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[5]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeTiempo.setAdapter(adapter);
        spnATiempo.setAdapter(adapter);

        btnConvertirTiempo.setOnClickListener(v -> calcularConversion(
                txtCantidadTiempo, spnDeTiempo, spnATiempo, lblRespuestaTiempo, 5));
    }

    private void inicializarPestanaTransferencia() {
        Spinner spnDeTransferencia = findViewById(R.id.spnDeTransferencia);
        Spinner spnATransferencia = findViewById(R.id.spnATransferencia);
        Button btnConvertirTransferencia = findViewById(R.id.btnConvertirTransferencia);
        EditText txtCantidadTransferencia = findViewById(R.id.txtCantidadTransferencia);
        TextView lblRespuestaTransferencia = findViewById(R.id.lblRespuestaTransferencia);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitNames[6]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnDeTransferencia.setAdapter(adapter);
        spnATransferencia.setAdapter(adapter);

        btnConvertirTransferencia.setOnClickListener(v -> calcularConversion(
                txtCantidadTransferencia, spnDeTransferencia, spnATransferencia, lblRespuestaTransferencia, 6));
    }

    public void calcularConversion(EditText txtCantidad, Spinner spnDe, Spinner spnA, TextView lblRespuesta, int categoria) {
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

        if (de.equals(a)) {
            Toast.makeText(this, "No puede convertir la misma unidad.", Toast.LENGTH_SHORT).show();
            return;
        }

        int indiceDe = obtenerIndiceUnidad(de, categoria);
        int indiceA = obtenerIndiceUnidad(a, categoria);

        if (indiceDe == -1 || indiceA == -1) {
            Toast.makeText(this, R.string.conversion_no_soportada, Toast.LENGTH_SHORT).show();
            return;
        }

        double tasa = conversionRates[categoria][indiceA] / conversionRates[categoria][indiceDe];
        double resultado = cantidad * tasa;

        String mensaje = de + " a " + a + ": " + cantidad + " → " + resultado;
        lblRespuesta.setText(getString(R.string.lbl_resultado) + " " + resultado);

        mostrarNotificacion(mensaje);
    }

    private int obtenerIndiceUnidad(String unidad, int categoria) {
        for (int i = 0; i < unitNames[categoria].length; i++) {
            if (unitNames[categoria][i].equals(unidad)) {
                return i;
            }
        }
        return -1;
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
                .setContentTitle(getString(R.string.conversion_completada))
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }
}