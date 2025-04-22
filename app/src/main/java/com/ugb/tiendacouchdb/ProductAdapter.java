package com.ugb.tiendacouchdb;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> implements Filterable {

    private List<Product> productList;
    private List<Product> productListFull;
    private Context context;

    public void setProductList(List<Product> updatedList) {
        this.productList.clear();
        this.productList.addAll(updatedList);
        this.productListFull.clear();
        this.productListFull.addAll(updatedList);
        notifyDataSetChanged();
    }


    public ProductAdapter(List<Product> productList, Context context) {
        this.productList = productList;
        this.context = context;
        // Se crea una copia de la lista original para usar en el filtrado.
        productListFull = new ArrayList<>(productList);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        final Product product = productList.get(position);
        holder.tvProductName.setText(product.getDescripcion());
        holder.tvProductPrice.setText("$" + product.getPrecio());
        // Se asume que 'imagen' es una URI válida; de lo contrario, puedes usar Glide o Picasso para cargar la imagen.
        holder.imgProduct.setImageURI(Uri.parse(product.getImagen()));

        // Al tocar el item se abre la pantalla de detalles del producto.
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ProductDetailsActivity.class);
                intent.putExtra("product", product);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // Implementación del filtro
    @Override
    public Filter getFilter() {
        return productFilter;
    }

    private Filter productFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Product> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(productListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                // Filtra por descripción o código (puedes ampliar el criterio)
                for (Product item : productListFull) {
                    if (item.getDescripcion().toLowerCase().contains(filterPattern) ||
                            item.getCodigo().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            productList.clear();
            productList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imgProduct;
        TextView tvProductName, tvProductPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewProduct);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }
}
