package com.tmuguet.viewpointer;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tmuguet on 07/08/2014.
 */
public class QueryParser {
    static final String OSM = "osm";
    static final String NOTE = "note";
    static final String META = "meta";
    static final String NODE = "node";
    static final String TAG = "tag";

    final List<Node> nodes = new ArrayList<Node>();
    String note;

    public QueryParser(String in) {

        final Node currentNode = new Node();
        RootElement root = new RootElement("osm");

        root.getChild(NOTE).setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                note = body;
            }
        });

        Element node = root.getChild(NODE);
        node.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                try {
                    currentNode.reset();
                    currentNode.id = Long.parseLong(attributes.getValue("id"));
                    currentNode.setLatitude(Double.parseDouble(attributes.getValue("lat")));
                    currentNode.setLongitude(Double.parseDouble(attributes.getValue("lon")));
                } catch (NumberFormatException e) {
                    currentNode.reset();
                }
            }
        });
        node.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
                if (currentNode.isValid()) {
                    nodes.add(new Node(currentNode));
                }
            }
        });
        node.getChild(TAG).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String key = attributes.getValue("k");
                String value = attributes.getValue("v");
                if (key.equals("ele")) {
                    try {
                        currentNode.setAltitude(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        currentNode.removeAltitude();
                    }
                } else if (key.equals("name")) {
                    currentNode.name = value;
                } else if (key.equals("wikipedia")) {
                    currentNode.wikipedia = value;
                }
            }
        });

        try {
            Xml.parse(in, root.getContentHandler());
        } catch (SAXException e) {
            android.util.Log.e("QueryParser", "Error", e);
        }
    }
}
