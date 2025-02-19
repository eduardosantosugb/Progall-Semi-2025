package com.ugb.cuadrasmart;


import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class HistorialAdapter extends BaseAdapter {

    private Context context;
    private List<Registro> registros;
    private LayoutInflater inflater;

    public HistorialAdapter(Context context, List<Registro> registros) {
        this.context = context;
        this.registros = registros;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return registros.size();
    }

    @Override
    public Object getItem(int position) {
        return registros.get(position);
    }

    @Override
    public long getItemId(int position) {
        return registros.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = inflater.inflate(R.layout.item_historial, parent, false);
            holder = new ViewHolder();
            holder.tvFecha = convertView.findViewById(R.id.tvFecha);
            holder.tvHorarios = convertView.findViewById(R.id.tvHorarios);
            holder.tvCajaCajero = convertView.findViewById(R.id.tvCajaCajero);
            holder.tvDiscrepancia = convertView.findViewById(R.id.tvDiscrepancia);
            holder.tvEstado = convertView.findViewById(R.id.tvEstado);
            holder.tvJustificacion = convertView.findViewById(R.id.tvJustificacion);
            holder.ivEvidencia = convertView.findViewById(R.id.ivEvidencia);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Registro registro = registros.get(position);
        holder.tvFecha.setText("Fecha: " + registro.getFecha());
        holder.tvHorarios.setText("Entrada: " + registro.getHoraInicio() + " - Salida: " + registro.getHoraCierre());
        holder.tvCajaCajero.setText("Caja: " + registro.getNumeroCaja() + " - Cajero: " + registro.getCajero());
        holder.tvDiscrepancia.setText("Discrepancia: " + registro.getDiscrepancia());
        holder.tvEstado.setText("Estado: " + registro.getEstado());

        // Mostrar justificación si existe
        if (registro.getJustificacion() != null && !registro.getJustificacion().isEmpty() &&
                !registro.getJustificacion().equalsIgnoreCase("no hubo justificación")) {
            holder.tvJustificacion.setText("Justificación: " + registro.getJustificacion());
            holder.tvJustificacion.setVisibility(View.VISIBLE);
        } else {
            holder.tvJustificacion.setVisibility(View.GONE);
        }

        // Mostrar evidencia (foto) si existe
        if (registro.getEvidencia() != null && !registro.getEvidencia().isEmpty()) {
            holder.ivEvidencia.setImageURI(Uri.parse(registro.getEvidencia()));
            holder.ivEvidencia.setVisibility(View.VISIBLE);
        } else {
            holder.ivEvidencia.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvFecha;
        TextView tvHorarios;
        TextView tvCajaCajero;
        TextView tvDiscrepancia;
        TextView tvEstado;
        TextView tvJustificacion;
        ImageView ivEvidencia;
    }
}