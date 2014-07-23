package org.commcare.android.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;
/**
 * Class used for managing the LoadImageTasks that load images into a list. 
 * Ensures that proper caching occurs and attempts to limit overflows
 * 
 * @author wspride
 *
 */
@SuppressLint("NewApi")
public class CachingAsyncImageLoader implements ComponentCallbacks2 {
	private TCLruCache cache;
	public static final int RETRY_LIMIT=5;		// how many times we should retry loading the image before giving up
	private int scaleFactor;					// how much to degrade the quality of the image to ensure no heap overflow
	private final int CACHE_DIVISOR =2 ;

	public CachingAsyncImageLoader(Context context, int mScaleFactor) {
		ActivityManager am = (ActivityManager) context.getSystemService(
				Context.ACTIVITY_SERVICE);
		int memoryClass = (am.getMemoryClass() * 1024 * 1024)/CACHE_DIVISOR;		//basically, set the heap to be everything we can get
		cache = new TCLruCache(memoryClass);
		scaleFactor = mScaleFactor;
	}

	public void display(String url, ImageView imageview, int defaultresource) {
		imageview.setImageResource(defaultresource);
		Bitmap image = cache.get(url);
		if (image != null) {
			imageview.setImageBitmap(image);
		}
		else {
			new SetImageTask(imageview).execute(url);
		}
	}

	@SuppressLint("NewApi")
	private class TCLruCache extends LruCache<String, Bitmap> {

		public TCLruCache(int maxSize) {
			super(maxSize);
		}
	}

	/**
	 * Simple member class used for asyncronously loading and setting ImageView bitmaps
	 * @author wspride
	 *
	 */
	private class SetImageTask extends AsyncTask<String, Void, Bitmap> {
		private ImageView mImageView = null;

		public SetImageTask(ImageView imageView) {
			mImageView = imageView;
		}

		protected Bitmap doInBackground(String... file) { 
			return getImageBitmap(file[0], 0);
		}

		protected void onPostExecute(Bitmap result) {

			if (result != null && mImageView != null) {
				mImageView.setImageBitmap(result);
			}
		}

		public Bitmap getImageBitmap(String file, int counter){
			Bitmap bitmap = null;
			bitmap = MediaUtil.getScaledImageFromReference(file, scaleFactor);
			return bitmap;
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

	@Override
	public void onLowMemory() {
	}

	@Override
	public void onTrimMemory(int level) {
		if (level >= TRIM_MEMORY_MODERATE) {
			cache.evictAll();
		}
	}
}