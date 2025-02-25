package com.ugb.cuadrasmart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class TiendasAdapter extends BaseAdapter {
    private Context context;
    private List<Tienda> tiendaList;
    private LayoutInflater inflater;

    public TiendasAdapter(Context context, List<Tienda> tiendaList) {
        this.context = context;
        this.tiendaList = tiendaList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return tiendaList.size();
    }

    @Override
    public Object getItem(int position) {
        return tiendaList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.item_tienda, parent, false);
            holder = new ViewHolder();
            holder.ivTiendaIcon = convertView.findViewById(R.id.ivTiendaIcon);
            holder.tvTiendaNombre = convertView.findViewById(R.id.tvTiendaNombre);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Tienda tienda = tiendaList.get(position);
        holder.tvTiendaNombre.setText(tienda.getNombre());
        holder.ivTiendaIcon.setImageResource(tienda.getIconResId());
        return convertView;
    }

    private static class ViewHolder {
        ImageView ivTiendaIcon;
        TextView tvTiendaNombre;
    }
}