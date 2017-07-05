package com.tmuguet.viewpointer;

import android.location.Location;

/**
 * Created by tmuguet on 07/08/2014.
 */
public class Node extends Location {
    long id;
    String name;
    String wikipedia;

    public Node() {
        super("poi");
    }

    public Node(Location n) {
        super(n);
    }

    public Node(Node n) {
        super(n);
        id = n.id;
        name = n.name;
        wikipedia = n.wikipedia;
    }

    @Override
    public void reset() {
        super.reset();
        id = -1;
        name = "";
        wikipedia = "";
    }

    public boolean isValid() {
        return id > -1 && !name.isEmpty();
    }

    @Override
    public String toString() {
        return "{" + name + ": lat=" + getLatitude() + ", lon=" + getLongitude() + ", elevation=" + getAltitude() + ", id=" + id + ", wp=" + wikipedia + ")";
    }
}
