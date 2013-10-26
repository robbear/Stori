package com.hyperfine.slideshare.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.hyperfine.slideshare.SlideJSON;
import com.hyperfine.slideshare.SlideShareJSON;
import com.hyperfine.slideshare.fragments.PlaySlidesFragment;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class PlaySlidesPagerAdapter extends FragmentStatePagerAdapter {

    public final static String TAG = "PlaySlidesPagerAdapter";

    private SlideShareJSON m_ssj;
    private String m_slideShareName;
    private Activity m_activityParent;

    public PlaySlidesPagerAdapter(FragmentManager fm) {
        super(fm);

        if(D) Log.d(TAG, "PlaySlidesPagerAdapter constructor");
    }

    public void setSlideShareJSON(SlideShareJSON ssj) {
        if(D)Log.d(TAG, "PlaySlidesPagerAdapter.setSlideShareJSON");

        m_ssj = ssj;
    }

    public void setSlideShareName(String slideShareName) {
        if(D)Log.d(TAG, "PlaySlidesPagerAdapter.setSlideShareName");

        m_slideShareName = slideShareName;
    }

    public void setActivityParent(Activity activityParent) {
        if(D)Log.d(TAG, "PlaySlidesPagerAdapter.setContext");

        m_activityParent = activityParent;
    }

    @Override
    public Fragment getItem(int i) {
        if(D)Log.d(TAG, String.format("PlaySlidesPagerAdapter.getItem(%d)", i));

        SlideJSON sj = null;
        try {
            sj = m_ssj.getSlide(i);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getItem", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getItem", e);
            e.printStackTrace();
        }

        return PlaySlidesFragment.newInstance(m_activityParent, i, m_slideShareName, sj);
    }

    @Override
    public int getCount() {
        int count = 0;

        try {
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getCount", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getCount", e);
            e.printStackTrace();
        }

        return count;
    }
}
