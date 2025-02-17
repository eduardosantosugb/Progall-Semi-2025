package com.ugb.cuadrasmart;


import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

public class ReportesActivity extends AppCompatActivity {

    private TextView tvResumenReportes;
    private Button btnGenerarPDF;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        tvResumenReportes = findViewById(R.id.tvResumenReportes);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);
        dbHelper = new DatabaseHelper(this);

        // Generar y mostrar el resumen del reporte
        generarResumenReporte();

        // Configurar el botón para generar PDF de forma funcional
        btnGenerarPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generarPDF();
            }
        });
    }

    // Método para generar un resumen simple a partir de los registros en la base de datos
    private void generarResumenReporte() {
        Cursor cursor = dbHelper.obtenerRegistros();
        int totalRegistros = 0;
        double sumaDiscrepancias = 0.0;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                totalRegistros++;
                double discrepancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
                sumaDiscrepancias += discrepancia;
            } while (cursor.moveToNext());
            cursor.close();
        }

        double promedioDiscrepancia = totalRegistros > 0 ? sumaDiscrepancias / totalRegistros : 0;
        DecimalFormat df = new DecimalFormat("#.##");
        String resumen = "Total de Registros: " + totalRegistros + "\n" +
                "Suma de Discrepancias: " + df.format(sumaDiscrepancias) + "\n" +
                "Promedio de Discrepancia: " + df.format(promedioDiscrepancia);
        tvResumenReportes.setText(resumen);
    }

    // Método para generar un PDF con el resumen del reporte
    private void generarPDF() {
        // Configurar un PdfDocument y la información de la página
        PdfDocument pdfDocument = new PdfDocument();
        // Definimos un tamaño de página personalizado (por ejemplo, 595 x 842 puntos, tamaño A4 en puntos)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);

        // Dibujar el título
        canvas.drawText("Reporte de Eficiencia - CuadraSmart", 40, 40, paint);

        // Dibujar una línea debajo del título
        paint.setStrokeWidth(2);
        canvas.drawLine(40, 50, 555, 50, paint);

        // Dibujar el resumen del reporte (obtenido desde tvResumenReportes)
        paint.setTextSize(14);
        String resumen = tvResumenReportes.getText().toString();
        // Dividir el texto en líneas si es necesario (aquí de forma simple se separa por "\n")
        String[] lineas = resumen.split("\n");
        int posY = 80;
        for (String linea : lineas) {
            canvas.drawText(linea, 40, posY, paint);
            posY += 20;
        }

        pdfDocument.finishPage(page);

        // Guardar el PDF en el directorio privado de la aplicación
        try {
            // getExternalFilesDir(null) genera un directorio privado para la app en almacenamiento externo.
            File pdfDir = getExternalFilesDir(null);
            if (pdfDir == null) {
                Toast.makeText(this, "No se pudo acceder al almacenamiento", Toast.LENGTH_SHORT).show();
                return;
            }
            File pdfFile = new File(pdfDir, "reporte_cuadrasmart.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            Toast.makeText(this, "PDF generado en: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }
}