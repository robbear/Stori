package com.stori.stori;

import java.util.Comparator;
import java.util.Date;

public class StoriListItemComparator implements Comparator<StoriListItem> {
    @Override
    public int compare(StoriListItem lhs, StoriListItem rhs) {
        String lhsDate = lhs.getModifiedDate();
        String rhsDate = rhs.getModifiedDate();

        // Descending sort. If we want ascending, use lhsData.compareTo(rhsDate)
        return rhsDate.compareTo(lhsDate);
    }
}
