package com.stori.stori.adapters;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.stori.stori.PlaySlidesActivity;
import com.stori.stori.SlideJSON;
import com.stori.stori.SlideShareJSON;
import com.stori.stori.fragments.PlaySlidesFragment;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class PlaySlidesPagerAdapter extends FragmentStatePagerAdapter {

    public final static String TAG = "PlaySlidesPagerAdapter";

    private SlideShareJSON m_ssj;
    private String m_slideShareName;
    private PlaySlidesActivity m_playSlidesActivity;

    public PlaySlidesPagerAdapter(FragmentManager fm) {
        super(fm);

        if(D) Log.d(TAG, "PlaySlidesPagerAdapter constructor");
    }

    public void setSlideShareJSON(SlideShareJSON ssj) {
        if(D)Log.d(TAG, "PlaySlidesPagerAdapter.setSlideShareJSON");

        m_ssj = ssj;
    }

    public void setSlideShareName(String slideShareName) {
        if(D)Log.d(TAG, String.format("PlaySlidesPagerAdapter.setSlideShareName: %s", slideShareName));

        m_slideShareName = slideShareName;
    }

    public void setPlaySlidesActivity(PlaySlidesActivity playSlidesActivity) {
        if(D)Log.d(TAG, "PlaySlidesPagerAdapter.setPlaySlidesActivity");

        m_playSlidesActivity = playSlidesActivity;
    }

    @Override
    public Fragment getItem(int i) {
        if(D)Log.d(TAG, String.format("PlaySlidesPagerAdapter.getItem(%d)", i));

        SlideJSON sj = null;
        String slideUuid = null;
        try {
            sj = m_ssj.getSlide(i);
            slideUuid = m_ssj.getSlideUuidByOrderIndex(i);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getItem", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesPagerAdapter.getItem", e);
            e.printStackTrace();
        }

        return PlaySlidesFragment.newInstance(m_playSlidesActivity, i, m_slideShareName, slideUuid, sj);
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
