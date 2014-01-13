package com.hyperfine.neodori.adapters;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.hyperfine.neodori.EditPlayActivity;
import com.hyperfine.neodori.SlideJSON;
import com.hyperfine.neodori.SlideShareJSON;
import com.hyperfine.neodori.fragments.EditPlayFragment;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditPlayPagerAdapter extends FragmentStatePagerAdapter {
    public final static String TAG = "EditPlayPagerAdapter";

    private SlideShareJSON m_ssj;
    private String m_slideShareName;
    private EditPlayActivity m_editPlayActivity;

    public EditPlayPagerAdapter(FragmentManager fm) {
        super(fm);

        if(D)Log.d(TAG, "EditPlayPagerAdapter constructor");
    }

    public void setSlideShareJSON(SlideShareJSON ssj) {
        if(D)Log.d(TAG, "EditPlayPagerAdapter.setSlideShareJSON");

        m_ssj = ssj;
    }

    public void setSlideShareName(String slideShareName) {
        if(D)Log.d(TAG, String.format("EditPlayPagerAdapter.setSlideShareName: %s", slideShareName));

        m_slideShareName = slideShareName;
    }

    public void setEditPlayActivity(EditPlayActivity editPlayActivity) {
        if(D)Log.d(TAG, "EditPlayPagerAdapter.setContext");

        m_editPlayActivity = editPlayActivity;
    }

    @Override
    public Fragment getItem(int i) {
        if(D)Log.d(TAG, String.format("EditPlayPagerAdapter.getItem(%d)", i));

        SlideJSON sj = null;
        String slideUuid = null;
        try {
            sj = m_ssj.getSlide(i);
            slideUuid = m_ssj.getSlideUuidByOrderIndex(i);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getItem", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayPagerAdapter.getItem", e);
            e.printStackTrace();
        }

        return EditPlayFragment.newInstance(m_editPlayActivity, i, m_slideShareName, slideUuid, sj);
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
