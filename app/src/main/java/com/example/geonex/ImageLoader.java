package com.example.geonex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private final Context context;
    private final PerformanceOptimizer optimizer;
    private final ExecutorService executorService;

    public ImageLoader(Context context) {
        this.context = context;
        this.optimizer = ((GeonexApplication) context.getApplicationContext()).getPerformanceOptimizer();
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * Load image from resource with caching
     */
    public void loadImage(int resId, ImageView imageView, int reqWidth, int reqHeight) {
        String cacheKey = "res_" + resId;

        // Check cache first
        Bitmap cachedBitmap = optimizer.getCachedBitmap(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        // Load in background
        executorService.execute(() -> {
            // Decode bitmap with optimal size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resId, options);

            options.inSampleSize = optimizer.getOptimalSampleSize(
                    options.outWidth, options.outHeight, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = optimizer.shouldLoadHighRes() ?
                    Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);

            if (bitmap != null) {
                optimizer.cacheBitmap(cacheKey, bitmap);

                imageView.post(() -> imageView.setImageBitmap(bitmap));
            }
        });
    }

    /**
     * Load image from URL
     */
    public void loadImageFromUrl(String urlString, ImageView imageView, int reqWidth, int reqHeight) {
        String cacheKey = "url_" + urlString;

        Bitmap cachedBitmap = optimizer.getCachedBitmap(cacheKey);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();

                // First decode with inJustDecodeBounds=true
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                input.close();

                // Re-open stream for actual decoding
                input = connection.getInputStream();

                options.inSampleSize = optimizer.getOptimalSampleSize(
                        options.outWidth, options.outHeight, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = optimizer.shouldLoadHighRes() ?
                        Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

                Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                input.close();
                connection.disconnect();

                if (bitmap != null) {
                    optimizer.cacheBitmap(cacheKey, bitmap);

                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }

            } catch (IOException e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
            }
        });
    }

    /**
     * Convert drawable to bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        Bitmap bitmap = Bitmap.createBitmap(width > 0 ? width : 100,
                height > 0 ? height : 100,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        executorService.shutdown();
    }
}