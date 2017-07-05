package com.tmuguet.viewpointer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;
import com.tmuguet.viewpointer.util.SystemUiHider;

import java.util.List;


/**
 * Created by tmuguet on 09/08/2014.
 */
public class MainActivity extends Activity implements Locater.LocaterListener, Orientation.OrientationListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 10000;
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private static final int REQUEST_LOAD = 1;
    private static final int REQUEST_SAVE = 2;
    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;
    /**
     * The flags to pass to {@link com.tmuguet.viewpointer.util.SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };
    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private OverlayView overlayView;
    private EditText latitude;
    private EditText longitude;
    private EditText altitude;
    private String locationInfo = "";
    private String orientationInfo = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ViewpointerApp.setContext(getApplicationContext());

        FrameLayout content = (FrameLayout) findViewById(R.id.fullscreen_content);

        overlayView = new OverlayView(this);
        CameraView cameraView = new CameraView(getApplicationContext(), this);


        content.addView(cameraView);
        content.addView(overlayView);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, content, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.set_location).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.set_location).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                updateLocation();
            }
        });

        findViewById(R.id.load_viewpoints).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.load_viewpoints).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                loadFile();
            }
        });

        findViewById(R.id.save_viewpoints).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.save_viewpoints).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                saveFile();
            }
        });
    }

    @Override
    protected void onPause() {
        overlayView.onPause();
        ViewpointerApp.getLocater().removeListener(this).onPause();
        ViewpointerApp.getOrientation().removeListener(this).onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlayView.onResume();
        ViewpointerApp.getLocater().addListener(this).onResume();
        ViewpointerApp.getOrientation().addListener(this).onResume();
    }

    @Override
    protected void onDestroy() {
        overlayView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onDataChanged(final Location newLocation, final List<Node> newNodes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Location updated", Toast.LENGTH_SHORT).show();
                locationInfo = String.format("Location: lat=%.3f; lon=%.3f; alt=%.0f\n%d viewpoints found", newLocation.getLatitude(), newLocation.getLongitude(), newLocation.getAltitude(), newNodes.size());

                final TextView infoView = (TextView) findViewById(R.id.content_info);
                infoView.setText(locationInfo + "\n" + orientationInfo);
            }
        });
    }

    @Override
    public void onOrientationChanged(final float[] newOrientation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                orientationInfo = String.format("Heading: %.0f / %.0f / %.0f°", Math.toDegrees(newOrientation[0]), Math.toDegrees(newOrientation[1]), Math.toDegrees(newOrientation[2]));

                final TextView infoView = (TextView) findViewById(R.id.content_info);
                infoView.setText(locationInfo + "\n" + orientationInfo);
            }
        });
    }

    public void updateLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_setlocation, null));

        builder.setTitle(R.string.set_location);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    double lat = Double.valueOf(latitude.getText().toString());
                    double lng = Double.valueOf(longitude.getText().toString());
                    double alt = Double.valueOf(altitude.getText().toString());

                    Location manualLocation = new Location("manual");
                    manualLocation.setLatitude(lat);
                    manualLocation.setLongitude(lng);
                    manualLocation.setAltitude(alt);
                    ViewpointerApp.getLocater().setManualLocation(manualLocation);
                } catch (NumberFormatException e) {
                    ViewpointerApp.getLocater().setManualLocation(null);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // do nothing
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        latitude = (EditText) dialog.findViewById(R.id.latitude);
        longitude = (EditText) dialog.findViewById(R.id.longitude);
        altitude = (EditText) dialog.findViewById(R.id.altitude);
    }

    public void loadFile() {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);

        startActivityForResult(intent, REQUEST_LOAD);
    }

    public void saveFile() {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);

        startActivityForResult(intent, REQUEST_SAVE);
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
            if (requestCode == REQUEST_LOAD) {
                ViewpointerApp.getLocater().setNodes(filePath);
            } else {
                ViewpointerApp.getLocater().exportNodes(filePath);
            }
        }
    }
}