package com.jwetherell.augmented_reality.activity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.jwetherell.augmented_reality.camera.CameraSurface;
import com.jwetherell.augmented_reality.data.ARData;
import com.jwetherell.augmented_reality.ui.Marker;
import com.jwetherell.augmented_reality.ui.Radar;
import com.jwetherell.augmented_reality.widget.VerticalSeekBar;

import java.text.DecimalFormat;

/**
 * This class extends the SensorsActivity and is designed tie the AugmentedView
 * and zoom bar together.
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class AugmentedReality extends SensorsActivity implements OnTouchListener {

    private static final String TAG = "AugmentedReality";
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private static final CharSequence START_TEXT = "0 m";
    private static final int ZOOMBAR_BACKGROUND_COLOR = Color.argb(125, 55, 55, 55);
    private static String END_TEXT = FORMAT.format(AugmentedReality.MAX_ZOOM) + " km";
    private static final int END_TEXT_COLOR = Color.WHITE;

    protected static CameraSurface camScreen = null;
    protected static VerticalSeekBar myZoomBar = null;
    protected static TextView endLabel = null;
    protected static LinearLayout zoomLayout = null;
    protected static AugmentedView augmentedView = null;

    public static float MAX_ZOOM = 10; // in KM
    public static float ONE_PERCENT = MAX_ZOOM / 100f;
    public static float TEN_PERCENT = 10f * ONE_PERCENT;
    public static float TWENTY_PERCENT = 2f * TEN_PERCENT;
    public static float EIGHTY_PERCENTY = 4f * TWENTY_PERCENT;

    public static boolean landscape = false;
    public static boolean useCollisionDetection = false;
    public static boolean showRadar = true;
    public static boolean showZoomBar = true;
    public static final boolean alternateLayout = true;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (alternateLayout)
            Radar.radarOnBottomRight = true;

        FrameLayout frameLayout = new FrameLayout(getActivity());

        camScreen = new CameraSurface(getActivity());
        frameLayout.addView(camScreen);

        augmentedView = new AugmentedView(getActivity());
        augmentedView.setOnTouchListener(this);
        LayoutParams augLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        frameLayout.addView(augmentedView, augLayout);

        zoomLayout = new LinearLayout(getActivity());
        zoomLayout.setVisibility((showZoomBar) ? LinearLayout.VISIBLE : LinearLayout.GONE);
        zoomLayout.setOrientation(LinearLayout.VERTICAL);
        zoomLayout.setPadding(5, 5, 5, 5);
        zoomLayout.setBackgroundColor(ZOOMBAR_BACKGROUND_COLOR);

        endLabel = new TextView(getActivity());
        endLabel.setText(END_TEXT);
        endLabel.setTextColor(END_TEXT_COLOR);
        LinearLayout.LayoutParams zoomTextParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        zoomTextParams.gravity = Gravity.CENTER;
        zoomLayout.addView(endLabel, zoomTextParams);

        myZoomBar = new VerticalSeekBar(getActivity());
        myZoomBar.setMax(100);
        myZoomBar.setProgress(50);
        myZoomBar.setOnSeekBarChangeListener(myZoomBarOnSeekBarChangeListener);
        LinearLayout.LayoutParams zoomBarParams;
        if (alternateLayout) {
            zoomBarParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0);
            zoomBarParams.weight = 1;
            zoomBarParams.gravity = Gravity.CENTER_HORIZONTAL;
//            zoomBarParams.gravity = Gravity.LEFT;
        } else {
            zoomBarParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            zoomBarParams.gravity = Gravity.CENTER_HORIZONTAL;
        }
        zoomLayout.addView(myZoomBar, zoomBarParams);

        if (alternateLayout) {
            TextView startLabel = new TextView(getActivity());
            startLabel.setText(START_TEXT);
            startLabel.setTextColor(END_TEXT_COLOR);
            zoomTextParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            zoomTextParams.gravity = Gravity.CENTER_HORIZONTAL;
            zoomLayout.addView(startLabel, zoomTextParams);
        }
        frameLayout.addView(zoomLayout, getZoomUILayoutParams());

        updateDataOnZoom();

        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);

        return frameLayout;
    }

    private FrameLayout.LayoutParams getZoomUILayoutParams() {
        if (alternateLayout) {
            FrameLayout.LayoutParams zoomBarParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                                  LayoutParams.MATCH_PARENT,
                                                                                  Gravity.LEFT);
            zoomBarParams.bottomMargin = (int) (2 * Radar.RADIUS);
            return zoomBarParams;
        } else {
            return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        }
    }

    public float getMaxZoom() {
        return MAX_ZOOM;
    }

    public void setMaxZoom(float maxZoomInKm) {
        MAX_ZOOM = maxZoomInKm;
        ONE_PERCENT = MAX_ZOOM / 100f;
        TEN_PERCENT = 10f * ONE_PERCENT;
        TWENTY_PERCENT = 2f * TEN_PERCENT;
        EIGHTY_PERCENTY = 4f * TWENTY_PERCENT;
        if (myZoomBar != null) {
            updateDataOnZoom();
            camScreen.invalidate();
        }
        END_TEXT = FORMAT.format(AugmentedReality.MAX_ZOOM) + " km";
        if (endLabel != null) {
            endLabel.setText(END_TEXT);
        }
    }

    public void setZoomRatio(float ratio) {
        if (myZoomBar != null) {
            myZoomBar.setProgress((int) (getMaxZoom() * ratio));
            updateDataOnZoom();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent evt) {
        super.onSensorChanged(evt);

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER || evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            augmentedView.postInvalidate();
        }
    }

    private OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new OnSeekBarChangeListener() {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateDataOnZoom();
            camScreen.invalidate();
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // Ignore
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            updateDataOnZoom();
            camScreen.invalidate();
        }
    };

    private static float calcZoomLevel() {
        int myZoomLevel = myZoomBar.getProgress();
        float myout = 0;

        float percent = 0;
        if (myZoomLevel <= 25) {
            percent = myZoomLevel / 25f;
            myout = ONE_PERCENT * percent;
        } else if (myZoomLevel > 25 && myZoomLevel <= 50) {
            percent = (myZoomLevel - 25f) / 25f;
            myout = ONE_PERCENT + (TEN_PERCENT * percent);
        } else if (myZoomLevel > 50 && myZoomLevel <= 75) {
            percent = (myZoomLevel - 50f) / 25f;
            myout = TEN_PERCENT + (TWENTY_PERCENT * percent);
        } else {
            percent = (myZoomLevel - 75f) / 25f;
            myout = TWENTY_PERCENT + (EIGHTY_PERCENTY * percent);
        }

        return myout;
    }

    /**
     * Called when the zoom bar has changed.
     */
    protected void updateDataOnZoom() {
        float zoomLevel = calcZoomLevel();
        ARData.setRadius(zoomLevel);
        ARData.setZoomLevel(FORMAT.format(zoomLevel));
        ARData.setZoomProgress(myZoomBar.getProgress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View view, MotionEvent me) {
        // See if the motion event is on a Marker
        for (Marker marker : ARData.getMarkers()) {
            if (marker.handleClick(me.getX(), me.getY())) {
                if (me.getAction() == MotionEvent.ACTION_UP) markerTouched(marker);
                return true;
            }
        }

        return getActivity().onTouchEvent(me);
    };

    protected void markerTouched(Marker marker) {
        Log.w(TAG, "markerTouched() not implemented.");
    }
}