package com.ugb.tienda;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.widget.ImageView;

public class ImageUtils {

    // Carga la imagen desde la ruta y la aplica como imagen circular en el ImageView
    public static void loadImage(Context context, String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }

        Bitmap circularBitmap = getCircularBitmap(bitmap);
        imageView.setImageBitmap(circularBitmap);
    }

    // Método para obtener una versión circular de un bitmap
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minEdge = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(minEdge, minEdge, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        float radius = minEdge / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect srcRect = new Rect((width - minEdge) / 2, (height - minEdge) / 2, (width + minEdge) / 2, (height + minEdge) / 2);
        Rect destRect = new Rect(0, 0, minEdge, minEdge);
        canvas.drawBitmap(bitmap, srcRect, destRect, paint);

        return output;
    }
}
