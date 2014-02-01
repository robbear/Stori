package com.stori.stori.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.stori.stori.R;
import com.stori.stori.StoriListItem;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListAdapter extends BaseAdapter {
    public final static String TAG = "StoriListAdapter";

    private Context m_context;
    private LayoutInflater m_inflater;
    private ArrayList<StoriListItem> m_storiListItems;

    public StoriListAdapter(Context context) {
        if(D)Log.d(TAG, "StoriListAdapter constructor");

        m_context = context;
        m_inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setStoriListItems(ArrayList<StoriListItem> items) {
        if(D)Log.d(TAG, "StoriListAdapter.setStoriListItems");

        m_storiListItems = items;
    }

    @Override
    public int getCount() {
        if (m_storiListItems != null) {
            return m_storiListItems.size();
        }
        else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (m_storiListItems != null && position < m_storiListItems.size()) {
            return m_storiListItems.get(position);
        }
        else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = m_inflater.inflate(R.layout.item_storiitem, parent, false);
        }

        StoriListItem sli = (StoriListItem)getItem(position);
        if (sli == null) {
            if(D)Log.d(TAG, "StoriListAdapter.getView - got null for child item, so bailing");
            return null;
        }

        TextView titleView = (TextView)convertView.findViewById(R.id.text_title);
        titleView.setText(sli.getTitle());

        TextView dateView = (TextView)convertView.findViewById(R.id.text_date);
        dateView.setText(sli.getModifiedDate());

        int count = sli.getSlideCount();
        TextView countView = (TextView)convertView.findViewById(R.id.text_count);
        countView.setText(count == 1 ?
                m_context.getString(R.string.storilistadapter_count_text_single) :
                String.format(m_context.getString(R.string.storilistadapter_count_text_format, count)));

        return convertView;
    }
}
