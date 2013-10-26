package com.hyperfine.slideshare.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ViewSwitcher;

import com.hyperfine.slideshare.R;
import com.hyperfine.slideshare.Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class ImagePickerFragment extends Fragment {
    public final static String TAG = "ImagePickerFragment";
    public final static int REQUEST_IMAGE = 1;

    private Activity m_activityParent;
    private Button m_pickButton;
    private ImageSwitcher m_imageSwitcher;
    private Drawable m_drawableImage = null;
    private String m_slideShareName = null;

    public static ImagePickerFragment newInstance(String slideShareName) {
        if(D)Log.d(TAG, "ImagePickerFragment.newInstance");

        ImagePickerFragment f = new ImagePickerFragment();

        f.setSlideShareName(slideShareName);

        return f;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("ImagePickerFragment.setSlideShareName: %s", name));

        m_slideShareName = name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D) Log.d(TAG, "ImagePickerFragment.onCreate");

        super.onCreate(savedInstanceState);

        Bundle argsBundle = getArguments();
        if (argsBundle != null) {
            // Set instance state
        }
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "ImagePickerFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "ImagePickerFragment.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "ImagePickerFragment.onResume");

        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "ImagePickerFragment.onAttach");

        super.onAttach(activity);

        m_activityParent = activity;

        // if (activity instanceof SomeActivityInterface) {
        // }
        // else {
        //     throw new ClassCastException(activity.toString() + " must implement SomeActivityInterface");
    }

    @Override
    public void onDetach() {
        if(D)Log.d(TAG, "ImagePickerFragment.onDetach");

        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D)Log.d(TAG, "ImagePickerFragment.onCreateView");

        View view = inflater.inflate(R.layout.fragment_imagepicker, container, false);

        m_pickButton = (Button)view.findViewById(R.id.control_imagepicker);
        m_pickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "ImagePickerFragment.onPickButtonClicked");

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });

        m_imageSwitcher = (ImageSwitcher)view.findViewById(R.id.picked_image);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "ImagePickerFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        m_imageSwitcher.setFactory((ViewSwitcher.ViewFactory)m_activityParent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(D)Log.d(TAG, String.format("ImagePickerFragment.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                if(D)Log.d(TAG, String.format("ImagePickerFragment.onActivityResult: intent data = %s", intent.getData().toString()));

                String fileName = UUID.randomUUID().toString() + ".jpg";
                boolean success = Utilities.copyGalleryImageToJPG(m_activityParent, m_slideShareName, fileName, intent);
                if(D)Log.d(TAG, String.format("ImagePickerFragment.onActivityResult - call to copyGalleryImageToJPG: success=%b, fileName=%s", success, fileName));

                InputStream stream = m_activityParent.getContentResolver().openInputStream(intent.getData());
                m_drawableImage = new BitmapDrawable(m_activityParent.getResources(), stream);
                m_imageSwitcher.setImageDrawable(m_drawableImage);
            }
            catch (IOException e) {
                if(E)Log.e(TAG, "ImagePickerFragment.onActivityResult", e);
                e.printStackTrace();
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "ImagePickerFragment.onActivityResult", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "ImagePickerFragment.onActivityResult", e);
                e.printStackTrace();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }
}
