package com.tmuguet.viewpointer;

import android.content.Context;

/**
 * Created by tmuguet on 21/08/2014.
 */
public class ViewpointerApp {
    private static Locater locater;
    private static Orientation orientation;

    public static void setContext(Context context) {
        locater = new Locater(context);
        orientation = new Orientation(context);
    }

    public static Locater getLocater() {
        return locater;
    }

    public static Orientation getOrientation() {
        return orientation;
    }
}
