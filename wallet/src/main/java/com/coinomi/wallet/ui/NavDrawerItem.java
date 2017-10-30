package com.coinomi.wallet.ui;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;

import java.util.List;

/**
 * @author John L. Jegutanis
 */
public class NavDrawerItem {
    NavDrawerItemType itemType;
    String title;
    int iconRes;
    Object itemData;
    CoinType subType;
    String icon;

    public NavDrawerItem(NavDrawerItemType itemType, String title, int iconRes, Object itemData) {
        this.itemType = itemType;
        this.title = title;
        this.iconRes = iconRes;
        this.itemData = itemData;
    }
    public NavDrawerItem(NavDrawerItemType itemType, String title, String icon, Object itemData, CoinType subType) {
        this.itemType = itemType;
        this.title = title;
        this.icon = icon;
        this.itemData = itemData;
        Preconditions.checkState(subType.isSubType(), "The provided type is not a sub type");
        this.subType = subType;
    }
    public static void addItem(List<NavDrawerItem> items, NavDrawerItemType type) {
        NavDrawerItem.addItem(items, type, null, -1, null);
    }

    public static void addItem(List<NavDrawerItem> items, NavDrawerItemType type, String title) {
        NavDrawerItem.addItem(items, type, title, -1, null);
    }

    public static void addItem(List<NavDrawerItem> items, NavDrawerItemType type, String title, Integer iconRes, Object data) {
        items.add(new NavDrawerItem(type, title, iconRes, data));
    }

    public static void addItem(List<NavDrawerItem> items, NavDrawerItemType type, String title, String icon, Object data, CoinType subType) {
        items.add(new NavDrawerItem(type, title, icon, data, subType));
    }
}
