package org.commcare.dalvik.activities;

import org.commcare.dalvik.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

public class FullScreenImage extends Activity
{
  protected void onCreate(Bundle savedInstanceState) {
   setContentView(R.layout.full_image);
   Intent intent = getIntent();
   int imageId = intent.getIntExtra("id", 0);
   ImageView imageView = (ImageView)findViewById(R.id.full_image);

   imageView.setLayoutParams( new ViewGroup.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));

   imageView.setImageResource(imageId);
   imageView.setScaleType(ImageView.ScaleType.FIT_XY);

   }
 }
