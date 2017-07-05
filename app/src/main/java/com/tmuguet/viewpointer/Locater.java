package com.tmuguet.viewpointer;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by tmuguet on 09/08/2014.
 */
public class Locater implements LocationListener {

    private static long minTimeLocationUpdate = 60000;  // 1 minute
    private static float minDistanceLocationUpdate = 10;    // 10 meters
    private static double range = 10;   // °
    private Context context;
    private Set<LocaterListener> listeners = new HashSet<LocaterListener>();
    private LocationManager locationManager;
    private String provider;
    private List<Node> nodes = new ArrayList<Node>();
    private Location manualLocation;
    private Location location;
    private String xmlNodes;

    public Locater(Context context) {
        this.context = context;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
    }

    public void onResume() {
        locationManager.requestLocationUpdates(provider, minTimeLocationUpdate, minDistanceLocationUpdate, this);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            onLocationChanged(location);
        }
    }

    protected void onPause() {
        locationManager.removeUpdates(this);
    }

    public void setManualLocation(Location manualLocation) {
        if (manualLocation != null) {
            this.manualLocation = manualLocation;
            onLocationChanged(manualLocation);
        } else {
            this.manualLocation = null;
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                onLocationChanged(location);
            }
        }
    }

    public void setNodes(String path) {
        File f = new File(path);
        StringBuilder str = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                str.append(line);
            }
            br.close();

            xmlNodes = str.toString();
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null || manualLocation != null) {
                onLocationChanged(location);
            }
        } catch (IOException e) {
            android.util.Log.e("Locater", "Could not read file " + f.getAbsolutePath(), e);
        }
    }

    public void exportNodes(String path) {
        File f = new File(path);
        try {
            FileWriter fstream = new FileWriter(f);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(xmlNodes);
            out.close();
            fstream.close();
        } catch (IOException e) {
            android.util.Log.e("Locater", "Could not write to file " + f.getAbsolutePath(), e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (manualLocation != null) {
            location = manualLocation;
        }

        new LoadNodesTask().execute(location);
    }

    public Locater addListener(LocaterListener listener) {
        synchronized (this) {
            listeners.add(listener);
            if (location != null) {
                listener.onDataChanged(location, new ArrayList<Node>(nodes));
            }
        }
        return this;
    }

    public Locater removeListener(LocaterListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
        return this;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(context, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(context, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    public interface LocaterListener {
        void onDataChanged(Location newLocation, List<Node> newNodes);
    }

    public class LoadNodesTask extends AsyncTask<Location, Integer, List<Node>> {
        @Override
        protected List<Node> doInBackground(Location... params) {
            try {
                QueryParser parser = null;
                if (xmlNodes == null) {
                    XmlSerializer query = Xml.newSerializer();
                    StringWriter queryWriter = new StringWriter();
                    query.setOutput(queryWriter);

                    query.startDocument("UTF-8", true);
                    query.startTag("", "osm-script");
                    query.startTag("", "query");
                    query.attribute("", "type", "node");
                    query.startTag("", "around");
                    query.attribute("", "lat", String.valueOf(params[0].getLatitude()));
                    query.attribute("", "lon", String.valueOf(params[0].getLongitude()));
                    query.attribute("", "radius", "10000.0");
                    query.endTag("", "around");
                    query.startTag("", "has-kv");
                    query.attribute("", "k", "natural");
                    query.attribute("", "v", "peak");
                    query.endTag("", "has-kv");
                    query.endTag("", "query");
                    query.startTag("", "print");
                    query.endTag("", "print");
                    query.endTag("", "osm-script");
                    query.endDocument();

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("http://overpass-api.de/api/interpreter");
                    List<NameValuePair> d = new ArrayList<NameValuePair>();
                    d.add(new BasicNameValuePair("data", queryWriter.toString()));
                    httpPost.setEntity(new UrlEncodedFormEntity(d));

                    HttpResponse response = httpClient.execute(httpPost);
                    Scanner s = new Scanner(response.getEntity().getContent()).useDelimiter("\\A");
                    if (s.hasNext()) {
                        xmlNodes = s.next();
                    }
                }

                if (xmlNodes != null && !xmlNodes.isEmpty()) {
                    parser = new QueryParser(xmlNodes);
                }

                synchronized (this) {
                    if (parser != null) {
                        nodes = parser.nodes;
                    }
                    location = params[0];
                    List<Node> nodesCopy = new ArrayList<Node>(nodes);
                    for (LocaterListener listener : listeners) {
                        listener.onDataChanged(location, nodesCopy);
                    }
                }
                return nodes;
            } catch (IOException e) {
                android.util.Log.e("Error", "Error", e);
                return null;
            }
        }
    }
}
