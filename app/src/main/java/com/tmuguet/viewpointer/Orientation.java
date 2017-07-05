package com.tmuguet.viewpointer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by tmuguet on 17/08/2014.
 */
public class Orientation implements SensorEventListener {

    private Context context;
    private Set<OrientationListener> listeners = new HashSet<OrientationListener>();
    private SensorManager sensorManager = null;
    private Sensor rotationVectorSensor;
    private float[] orientation;

    public Orientation(Context context) {
        this.context = context;

        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void onResume() {
        sensorManager.registerListener(this, rotationVectorSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onPause() {
        sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        float orientation[] = event.values;
        for (OrientationListener listener : listeners) {
            listener.onOrientationChanged(orientation);
        }
    }

    public Orientation addListener(OrientationListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
        return this;
    }

    public Orientation removeListener(OrientationListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
        return this;
    }


    public interface OrientationListener {
        void onOrientationChanged(float[] newOrientation);
    }
}
