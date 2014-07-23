package org.commcare.android.view;

import java.io.File;

import org.commcare.dalvik.R;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.views.media.AudioButton;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This layout for the GenericMenuFormAdapter allows you to load an image, audio, and text
 * to menus.
 * 
 * @author wspride
 */

public class TextImageAudioView extends RelativeLayout {
    private static final String t = "AVTLayout";

    private TextView mTextView;
    private AudioButton mAudioButton;
    private ImageView mImageView;
    private TextView mMissingImage;
    private final int imageDimension = 100;
    private final int fontSize = 20;


    public TextImageAudioView(Context c) {
        super(c);
        mTextView = null;
        mAudioButton = null;
        mImageView = null;
        mMissingImage = null;
    }
    
    //accepts a string to display and URI links to the audio and image, builds the proper TextImageAudio view
    public void setAVT(String displayText, String audioURI, String imageURI) {
    	this.removeAllViews();
    	
        String mDisplayText = displayText;
        
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mTextView = (TextView)inflater.inflate(R.layout.entity_item_text, null);
        
        mTextView.setText(displayText);
        
        // Layout configurations for our elements in the relative layout
        RelativeLayout.LayoutParams textParams =
            new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams audioParams =
            new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams imageParams =
            new RelativeLayout.LayoutParams(imageDimension,imageDimension);
        
        String audioFilename = "";
        if(audioURI != null && !audioURI.equals("")) {
		    try {
		        audioFilename = ReferenceManager._().DeriveReference(audioURI).getLocalURI();
		    } catch (InvalidReferenceException e) {
		        Log.e(t, "Invalid reference exception");
		        e.printStackTrace();
		    }
        }

        File audioFile = new File(audioFilename);

        // First set up the audio button
        if (audioFilename != "" && audioFile.exists()) {
            // An audio file is specified
            mAudioButton = new AudioButton(getContext(), audioURI, true);
            mAudioButton.setId(3245345); // random ID to be used by the relative layout.
            // Set not focusable so that list onclick will work
            mAudioButton.setFocusable(false);
            mAudioButton.setFocusableInTouchMode(false);
            audioParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            audioParams.addRule(CENTER_VERTICAL);
            addView(mAudioButton, audioParams);
        }

        // Now set up the image view
        String errorMsg = null;
        if (imageURI != null && !imageURI.equals("")) {
            try {
                String imageFilename = ReferenceManager._().DeriveReference(imageURI).getLocalURI();
                final File imageFile = new File(imageFilename);
                if (imageFile.exists()) {
                    Bitmap b = null;
                    try {
                        Display display =
                            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                                    .getDefaultDisplay();
                        int screenWidth = display.getWidth();
                        int screenHeight = display.getHeight();
                        b =
                            FileUtils
                                    .getBitmapScaledToDisplay(imageFile, screenHeight, screenWidth);
                    } catch (OutOfMemoryError e) {
                        errorMsg = "ERROR: " + e.getMessage();
                    }

                    if (b != null) {
                        mImageView = new ImageView(getContext());
                        mImageView.setPadding(10, 10, 10, 10);
                        mImageView.setAdjustViewBounds(true);
                        mImageView.setImageBitmap(b);
                        mImageView.setId(23422634);
                        imageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        addView(mImageView, imageParams);
                    } else if (errorMsg == null) {
                        // An error hasn't been logged and loading the image failed, so it's likely
                        // a bad file.
                        errorMsg = getContext().getString(R.string.file_invalid, imageFile);

                    }
                } else if (errorMsg == null) {
                    // An error hasn't been logged. We should have an image, but the file doesn't
                    // exist.
                    errorMsg = getContext().getString(R.string.file_missing, imageFile);
                }

            } catch (InvalidReferenceException e) {
                Log.e(t, "image invalid reference exception");
                e.printStackTrace();
            }
        }
        
        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        if(imageURI != null && !imageURI.equals("") && mImageView != null){
        	textParams.addRule(RelativeLayout.RIGHT_OF,mImageView.getId());
        }
        else{
        	textParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }
        	
        if(mAudioButton != null) {
        	textParams.addRule(RelativeLayout.LEFT_OF, mAudioButton.getId());
        }
        addView(mTextView, textParams);
    }


    /**
     * This adds a divider at the bottom of this layout. Used to separate fields in lists.
     * 
     * @param v
     */
    public void addDivider(ImageView v) {
        RelativeLayout.LayoutParams dividerParams =
            new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        if (mImageView != null) {
            dividerParams.addRule(RelativeLayout.BELOW, mImageView.getId());
        } else if (mMissingImage != null) {
            dividerParams.addRule(RelativeLayout.BELOW, mMissingImage.getId());
        }
        else if (mAudioButton != null) {
            dividerParams.addRule(RelativeLayout.BELOW, mAudioButton.getId());
        } else if (mTextView != null) {
            // No picture
            dividerParams.addRule(RelativeLayout.BELOW, mTextView.getId());
        } else {
            Log.e(t, "Tried to add divider to uninitialized ATVWidget");
            return;
        }
        addView(v, dividerParams);
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            if (mAudioButton != null) {
                mAudioButton.endPlaying();
            }
        }
    }

}
