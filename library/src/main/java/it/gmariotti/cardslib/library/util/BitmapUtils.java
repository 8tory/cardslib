
package it.gmariotti.cardslib.library.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import it.gmariotti.cardslib.library.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    public static Bitmap createVideoThumbnail(Context context, String filePath) {
        final Bitmap bitmap = createVideoThumbnail(filePath);
        if (bitmap == null)
            return null;

        Bitmap play = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_control_play);

        final int x = bitmap.getWidth() / 2;
        final int y = bitmap.getHeight() / 2;
        final int w = bitmap.getWidth() / 5;
        final int h = play.getHeight() * w / play.getWidth();
        final Rect rect = new Rect(x - w / 2, y - h / 2, x + w / 2, y + h / 2);

        final Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(play, null, rect, null);

        play.recycle();
        play = null;

        return bitmap;
    }

    private static Bitmap createVideoThumbnail(String filePath) {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        try {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();

            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);

            // The method name changes between API Level 9 and 10.
            if (Build.VERSION.SDK_INT <= 9) {
                return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
            } else {
                byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
                if (data != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null)
                        return bitmap;
                }
                return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } catch (InstantiationException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } finally {
            try {
                if (instance != null) {
                    clazz.getMethod("release").invoke(instance);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
