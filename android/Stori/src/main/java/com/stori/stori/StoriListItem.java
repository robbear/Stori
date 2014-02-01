package com.stori.stori;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import static com.stori.stori.Config.D;

public class StoriListItem implements Parcelable {
    public final static String TAG = "StoriListItem";

    private String m_slideShareName;
    private String m_title;
    private String m_modifiedDate;
    private int m_countSlides;

    public String getSlideShareName() {
        return m_slideShareName;
    }

    public String getTitle() {
        return m_title;
    }

    public String getModifiedDate() {
        return m_modifiedDate;
    }

    public int getSlideCount() {
        return m_countSlides;
    }

    public StoriListItem(Parcel in) {
        // Remember: order matters for Parcelable
        m_slideShareName = in.readString();
        m_title = in.readString();
        m_modifiedDate = in.readString();
        m_countSlides = in.readInt();

        if(D)Log.d(TAG, String.format("StoriListItem constructor with parcel: slideShareName=%s, title=%s, date=%s, countSlides=%d", m_slideShareName, m_title, m_modifiedDate, m_countSlides));
    }

    public StoriListItem(String slideShareName, String title, String modifiedDate, int countSlides) {
        if(D)Log.d(TAG, String.format("StoriListItem constructor: slideShareName=%s, title=%s, modifiedDate=%s, countSlides=%d", slideShareName, title, modifiedDate, countSlides));

        m_slideShareName = slideShareName;
        m_title = title;
        m_modifiedDate = modifiedDate;
        m_countSlides = countSlides;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if(D)Log.d(TAG, "StoriListItem.writeToParcel");

        // Remember: order matters for Parcelable
        dest.writeString(m_slideShareName);
        dest.writeString(m_title);
        dest.writeString(m_modifiedDate);
        dest.writeInt(m_countSlides);
    }

    @Override
    public int describeContents() {
        if(D)Log.d(TAG, "StoriListItem.describeContents");

        return 0;
    }

    public static final Parcelable.Creator<StoriListItem> CREATOR = new Parcelable.Creator<StoriListItem>() {
        public StoriListItem createFromParcel(Parcel in) {
            if(D)Log.d(TAG, "StoriListItem.createFromParcel");

            return new StoriListItem(in);
        }

        public StoriListItem[] newArray(int size) {
            if(D)Log.d(TAG, String.format("StoriListItem.newArray: size=%d", size));

            return new StoriListItem[size];
        }
    };
}

