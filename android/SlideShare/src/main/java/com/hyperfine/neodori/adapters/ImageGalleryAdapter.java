package com.hyperfine.neodori.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.hyperfine.neodori.R;
import com.hyperfine.neodori.SlideJSON;
import com.hyperfine.neodori.SlideShareJSON;
import com.hyperfine.neodori.Utilities;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

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

                if (parentHeight != 0) {
                    targetH = parentHeight;
                }

                String filePath = Utilities.getAbsoluteFilePath(m_context, m_slideShareName, imageFileName);
                Bitmap bitmap = Utilities.getConstrainedBitmap(filePath, targetW, targetH);

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
