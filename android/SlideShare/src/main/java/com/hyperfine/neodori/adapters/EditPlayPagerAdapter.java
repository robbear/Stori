package com.hyperfine.neodori.adapters;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.hyperfine.neodori.SlideJSON;
import com.hyperfine.neodori.SlideShareJSON;
import com.hyperfine.neodori.fragments.PlaySlidesFragment;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditPlayPagerAdapter extends FragmentStatePagerAdapter {
    public final static String TAG = "EditPlayPagerAdapter";

    private SlideShareJSON m_ssj;
    private String m_slideShareName;
    private Activity m_activityParent;

    public EditPlayPagerAdapter(FragmentManager fm) {
        super(fm);

        if(D) Log.d(TAG, "EditPlayPagerAdapter constructor");
    }

    public void setSlideShareJSON(SlideShareJSON ssj) {
        if(D)Log.d(TAG, "EditPlayPagerAdapter.setSlideShareJSON");

        m_ssj = ssj;
    }

    public void setSlideShareName(String slideShareName) {
        if(D)Log.d(TAG, String.format("EditPlayPagerAdapter.setSlideShareName: %s", slideShareName));

        m_slideShareName = slideShareName;
    }

    public void setActivityParent(Activity activityParent) {
        if(D)Log.d(TAG, "EditPlayPagerAdapter.setContext");

        m_activityParent = activityParent;
    }

    @Override
    public Fragment getItem(int i) {
        if(D)Log.d(TAG, String.format("EditPlayPagerAdapter.getItem(%d)", i));

        SlideJSON sj = null;
        try {
            sj = m_ssj.getSlide(i);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getItem", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getItem", e);
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
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getCount", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getCount", e);
            e.printStackTrace();
        }

        return count;
    }
}
