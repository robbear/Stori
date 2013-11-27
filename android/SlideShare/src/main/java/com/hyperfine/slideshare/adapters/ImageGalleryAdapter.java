package com.hyperfine.slideshare.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.hyperfine.slideshare.R;
import com.hyperfine.slideshare.SlideJSON;
import com.hyperfine.slideshare.SlideShareJSON;
import com.hyperfine.slideshare.Utilities;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class ImageGalleryAdapter extends BaseAdapter {
    public final static String TAG = "ImageGalleryAdapter";

    private SlideShareJSON m_ssj;
    private String m_slideShareName;
    private Context m_context;
    private LayoutInflater m_inflater;

    public ImageGalleryAdapter() {
        super();

        if(D)Log.d(TAG, "ImageGalleryAdapter constructor");
    }

    public void setSlideShareJSON(SlideShareJSON ssj) {
        if(D)Log.d(TAG, "ImageGalleryAdapter.setSlideShareJSON");

        m_ssj = ssj;
    }

    public void setSlideShareName(String slideShareName) {
        if(D)Log.d(TAG, "ImageGalleryAdapter.setSlideShareName");

        m_slideShareName = slideShareName;
    }

    public void setContext(Context context) {
        if(D)Log.d(TAG, "ImageGalleryAdapter.setContext");

        m_context = context;

        m_inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        int count = 0;

        try {
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "ImageGalleryAdapter.getCount", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "ImageGalleryAdapter.getCount", e);
            e.printStackTrace();
        }

        return count;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SlideJSON sj = null;
        String imageFileName = null;

        View viewItem = m_inflater.inflate(R.layout.item_gallery, parent, false);
        ImageView imageView = (ImageView)viewItem.findViewById(R.id.gallery_image);

        try {
            sj = m_ssj.getSlide(position);
            imageFileName = sj.getImageFilename();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "ImageGalleryAdapter.getView", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "ImageGalleryAdapter.getView", e);
            e.printStackTrace();
        }

        if(D)Log.d(TAG, String.format("ImageGalleryAdapter.getView: position=%d, parent.width=%d, parent.height=%d", position, parent.getWidth(), parent.getHeight()));

        /* BUGBUG
        imageView.setId(position);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, String.format("ImageGalleryAdapter.image.onClick: position=%d", v.getId()));
            }
        });
        */
        renderImage(imageView, imageFileName, parent.getHeight());

        return viewItem;
    }

    private void renderImage(ImageView imageView, String imageFileName, int parentHeight) {
        if(D)Log.d(TAG, String.format("ImageGalleryAdapter.renderImage: %s", imageFileName));

        if (imageFileName == null) {
            imageView.setImageResource(R.drawable.ic_defaultslideimage);
        }
        else {
            try {
                int targetW = imageView.getWidth();
                int targetH = imageView.getHeight();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(Utilities.getAbsoluteFilePath(m_context, m_slideShareName, imageFileName), options);
                int photoW = options.outWidth;
                int photoH = options.outHeight;

                if (parentHeight != 0) {
                    targetH = parentHeight;
                    targetW = (photoW * parentHeight) / photoH;
                }

                if(D)Log.d(TAG, String.format("ImageGalleryAdapter.renderImage: targetW=%d, targetH=%d", targetW, targetH));
                if(D)Log.d(TAG, String.format("ImageGalleryAdapter.renderImage: photoW=%d, photoH=%d", photoW, photoH));

                if (targetW == 0) targetW = photoW;
                if (targetH == 0) targetH = photoH;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
                if(D)Log.d(TAG, String.format("ImageGalleryAdapter.renderImage: scaleFactor=%d", scaleFactor));

                // Decode the image file into a Bitmap sized to fill the View
                options.inJustDecodeBounds = false;
                options.inSampleSize = scaleFactor;
                options.inPurgeable = true;

                Bitmap bitmap = BitmapFactory.decodeFile(Utilities.getAbsoluteFilePath(m_context, m_slideShareName, imageFileName), options);
                Drawable drawableImage = new BitmapDrawable(m_context.getResources(), bitmap);
                imageView.setImageDrawable(drawableImage);
            }
            catch (Exception e) {
                if(D)Log.e(TAG, "ImageGalleryAdapter.renderImage", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(D)Log.e(TAG, "ImageGalleryAdapter.renderImage", e);
                e.printStackTrace();
            }
        }
    }
}
