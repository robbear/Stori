package com.stori.stori.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.stori.stori.R;
import com.stori.stori.SSPreferences;
import com.stori.stori.StoriListActivity;
import com.stori.stori.StoriListItem;
import com.stori.stori.Utilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListAdapter extends BaseAdapter {
    public final static String TAG = "StoriListAdapter";

    private StoriListActivity m_storiListActivity;
    private LayoutInflater m_inflater;
    private ArrayList<StoriListItem> m_storiListItems;
    private String m_userUuid;
    private SharedPreferences m_prefs;

    public StoriListAdapter(StoriListActivity storiListActivity, String userUuid) {
        if(D)Log.d(TAG, "StoriListAdapter constructor");

        m_storiListActivity = storiListActivity;
        m_userUuid = userUuid;
        m_inflater = (LayoutInflater)storiListActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        m_prefs = storiListActivity.getSharedPreferences(SSPreferences.PREFS(storiListActivity), Context.MODE_PRIVATE);
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

        final StoriListItem sli = (StoriListItem)getItem(position);
        if (sli == null) {
            if(D)Log.d(TAG, "StoriListAdapter.getView - got null for child item, so bailing");
            return null;
        }

        TextView titleView = (TextView)convertView.findViewById(R.id.text_title);
        titleView.setText(sli.getTitle());

        TextView dateView = (TextView)convertView.findViewById(R.id.text_date);

        //
        // Convert date string to local date
        //
        String dateString = null;
        try {
            Calendar calendar = Utilities.toCalendarFromISO8601String(sli.getModifiedDate());
            DateFormat sdf = SimpleDateFormat.getDateTimeInstance();
            dateString = sdf.format(calendar.getTime());
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriListAdapter.getView", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriListAdapter.getView", e);
            e.printStackTrace();
        }

        // Set the dateView text
        dateView.setText(dateString);

        int count = sli.getSlideCount();
        TextView countView = (TextView)convertView.findViewById(R.id.text_count);
        countView.setText(count == 1 ?
                m_storiListActivity.getString(R.string.storilistadapter_count_text_single) :
                String.format(m_storiListActivity.getString(R.string.storilistadapter_count_text_format, count)));

        View menuTrigger = convertView.findViewById(R.id.menu_trigger);
        menuTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu pm = new PopupMenu(m_storiListActivity, view);
                pm.inflate(R.menu.menu_storilistitem);
                Menu menu = pm.getMenu();

                // Address issue #52. Do not allow download and play of current
                // edit item.
                String currentEditSlideshareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(m_storiListActivity), SSPreferences.DEFAULT_EDITPROJECTNAME(m_storiListActivity));

                if (currentEditSlideshareName != null && currentEditSlideshareName.equals(sli.getSlideShareName())) {
                    if(D)Log.d(TAG, String.format("StoriListAdapter.getView: blocking Play menu for slideShareName=%s", currentEditSlideshareName));
                    menu.removeItem(R.id.menu_storilistitem_play);
                }
                else {
                    MenuItem play = menu.findItem(R.id.menu_storilistitem_play);
                    play.setTitle(m_storiListActivity.getString(R.string.menu_storilistitem_play));
                    play.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            m_storiListActivity.downloadForPlay(sli);
                            return true;
                        }
                    });
                }

                MenuItem edit = menu.findItem(R.id.menu_storilistitem_edit);
                edit.setTitle(m_storiListActivity.getString(R.string.menu_storilistitem_edit));
                edit.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_storiListActivity.downloadForEdit(sli);
                        return true;
                    }
                });

                MenuItem share = menu.findItem(R.id.menu_storilistitem_share);
                share.setTitle(m_storiListActivity.getString(R.string.menu_storilistitem_share));
                share.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Utilities.shareShow(m_storiListActivity, m_userUuid, sli.getSlideShareName(), sli.getTitle());
                        return true;
                    }
                });

                MenuItem delete = menu.findItem(R.id.menu_storilistitem_delete);
                delete.setTitle(m_storiListActivity.getString(R.string.menu_storilistitem_delete));
                delete.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ArrayList<StoriListItem> items = new ArrayList<StoriListItem>();
                        items.add(sli);
                        m_storiListActivity.deleteStoris(items);
                        return true;
                    }
                });

                pm.show();
            }
        });

        return convertView;
    }
}
