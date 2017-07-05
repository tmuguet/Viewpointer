package com.tmuguet.viewpointer;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.hardware.Camera;
import android.location.Location;
import android.os.Handler;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tmuguet on 17/08/2014.
 */
public class OverlayView extends View implements Locater.LocaterListener, Orientation.OrientationListener {

    private static final int labelWidth = 200;

    private final Activity context;
    private final Handler handler;
    private final float verticalFOV;
    private final float horizontalFOV;
    private final TextPaint textPaint;
    private final Paint objectPaint;
    private Location lastLocation;
    private List<Node> lastNodes;
    private float[] lastOrientation;
    private boolean toast = false;

    public OverlayView(Activity context) {
        super(context);
        this.context = context;
        this.handler = new Handler();

        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        verticalFOV = params.getVerticalViewAngle();
        horizontalFOV = params.getHorizontalViewAngle();
        camera.release();

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Align.LEFT);
        textPaint.setTextSize(14);
        textPaint.setColor(Color.WHITE);

        objectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        objectPaint.setColor(Color.WHITE);

        ViewpointerApp.getLocater().addListener(this);
        ViewpointerApp.getOrientation().addListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float curBearingToNode;
        double curAltToNode;
        float curDistToNode;
        double curAngleToNode;

        if (lastLocation != null && lastOrientation != null) {
            toast = false;
            canvas.save();

            new StaticLayout(String.format("%.0f m - (%.0f / %.0f / %.0f)°", lastLocation.getAltitude(), Math.toDegrees(lastOrientation[0]), Math.toDegrees(lastOrientation[1]), Math.toDegrees(lastOrientation[2])), textPaint,
                    200, Alignment.ALIGN_CENTER, 0, 0, true).draw(canvas);

/*            float dy0 = (float) ((canvas.getHeight() / verticalFOV) * Math.toDegrees(lastOrientation[1]));
            canvas.rotate((float) (-90.0f - Math.toDegrees(lastOrientation[2])));

            canvas.translate(0, -dy0);

            canvas.drawLine(-canvas.getHeight(), canvas.getHeight() / 2, canvas.getWidth() + canvas.getHeight(), canvas.getHeight() / 2, objectPaint);
            canvas.translate(15f, canvas.getHeight() / 2);

            new StaticLayout(String.format("%.0f m - %.0f°", lastLocation.getAltitude(), Math.toDegrees(lastOrientation[0])), textPaint,
                    200, Alignment.ALIGN_CENTER, 0, 0, true).draw(canvas);
            canvas.restore();


            ArrayList<Pair<Float, Float>> positions = new ArrayList<Pair<Float, Float>>(lastNodes.size());

            for (Node n : lastNodes) {
                canvas.save();

                curBearingToNode = lastLocation.bearingTo(n);
                curDistToNode = lastLocation.distanceTo(n);
                curAltToNode = (n.hasAltitude() && lastLocation.hasAltitude()) ? n.getAltitude() - lastLocation.getAltitude() : lastLocation.getAltitude();
                curAngleToNode = -Math.toDegrees(Math.asin(curAltToNode / curDistToNode));

                canvas.rotate((float) (-90.0f - Math.toDegrees(lastOrientation[2])));

                float dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(lastOrientation[0]) - curBearingToNode));
                //float dy = (float) ((canvas.getHeight() / verticalFOV) * (Math.toDegrees(lastOrientation[1]) - curAngleToNode));
                float dy = 0;

                canvas.translate(-dx, -dy);

                canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 5.0f, objectPaint);

                canvas.restore();
                canvas.save();

                float width = textPaint.measureText(n.name) + 10;
                float labeldy = 15;
                for (Pair<Float, Float> f : positions) {
                    if ((dx > f.first && dx < f.second) || (dx + width > f.first && dx + width < f.second)) {
                        labeldy += 15;
                    }
                }

                canvas.rotate((float) (-90.0f - Math.toDegrees(lastOrientation[2])));
                canvas.translate(-dx + canvas.getWidth() / 2, labeldy);
                canvas.drawLine(0, 0, 0, canvas.getHeight() / 2 - labeldy - dy, objectPaint);
                new StaticLayout(n.name, textPaint,
                        (int) width, Alignment.ALIGN_NORMAL, 0, 0, true).draw(canvas);
                positions.add(new Pair(dx, dx + width));

                canvas.restore();
            }*/

        } else if (!toast) {
            Toast.makeText(context, "No known location", Toast.LENGTH_SHORT).show();
            toast = true;
        }
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void onDestroy() {
        ViewpointerApp.getLocater().removeListener(this);
        ViewpointerApp.getOrientation().removeListener(this);
    }

    @Override
    public void onDataChanged(Location newLocation, List<Node> newNodes) {
        lastLocation = newLocation;
        lastNodes = newNodes;
        context.runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      OverlayView.this.invalidate();
                                  }
                              }
        );
    }

    @Override
    public void onOrientationChanged(float[] newOrientation) {
        lastOrientation = newOrientation;
        context.runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      OverlayView.this.invalidate();
                                  }
                              }
        );
    }
}
