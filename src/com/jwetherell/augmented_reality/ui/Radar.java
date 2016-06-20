package com.jwetherell.augmented_reality.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.jwetherell.augmented_reality.activity.AugmentedReality;
import com.jwetherell.augmented_reality.camera.CameraModel;
import com.jwetherell.augmented_reality.common.Calculator;
import com.jwetherell.augmented_reality.data.ARData;
import com.jwetherell.augmented_reality.data.ScreenPosition;
import com.jwetherell.augmented_reality.ui.objects.PaintableCircle;
import com.jwetherell.augmented_reality.ui.objects.PaintableIcon;
import com.jwetherell.augmented_reality.ui.objects.PaintableLine;
import com.jwetherell.augmented_reality.ui.objects.PaintableObject;
import com.jwetherell.augmented_reality.ui.objects.PaintablePosition;
import com.jwetherell.augmented_reality.ui.objects.PaintableRadarPoints;
import com.jwetherell.augmented_reality.ui.objects.PaintableText;

import ar.com.carlosefonseca.common.utils.UnitUtils;

/**
 * This class will visually represent a radar screen with a radar radius and
 * blips on the screen in their appropriate locations.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class Radar {

    public static float Density = 1;

    public static float RADIUS_DP = 80;
    public static float RADIUS;

    private static final int LINE_COLOR = Color.argb(150, 0, 0, 220);
    private static float PAD_X = 0;
    private static float PAD_Y = 0;
    public static float paddingY = 10;
    private static final int RADAR_COLOR = Color.argb(100, 0, 0, 200);
    private static final int TEXT_COLOR = Color.rgb(255, 255, 255);
    private static final int TEXT_SIZE = 12;

    private static ScreenPosition leftRadarLine = null;
    private static ScreenPosition rightRadarLine = null;
    private static PaintablePosition leftLineContainer = null;
    private static PaintablePosition rightLineContainer = null;
    private static PaintablePosition circleContainer = null;

    private static PaintableRadarPoints radarPoints = null;
    private static PaintablePosition pointsContainer = null;

    private static PaintableText paintableText = null;
    private static PaintablePosition paintedContainer = null;

    public static Bitmap icon;
    public static Bitmap circleImage;
    @Deprecated
    public static boolean radarOnBottomRight = false;
    public static Position radarPosition = Position.TOP_LEFT;

    public enum Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public Radar() {
        if (leftRadarLine == null) leftRadarLine = new ScreenPosition();
        if (rightRadarLine == null) rightRadarLine = new ScreenPosition();
    }

    public static void setDensity(float d) {
        Radar.Density = d;
        RADIUS = d * RADIUS_DP;
        PAD_X = 10 * d;
        PAD_Y = paddingY * d;
    }

    public static void setPaddingY(float paddingY) {
        Radar.paddingY = paddingY;
        PAD_Y = paddingY * Radar.Density;
    }

    /**
     * Draw the radar on the given Canvas.
     *
     * @param canvas Canvas to draw on.
     * @throws NullPointerException if Canvas is NULL.
     */
    public void draw(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        // Update the pitch and bearing using the phone's rotation matrix
        Calculator.calcPitchBearing(ARData.getRotationMatrix());
        ARData.setAzimuth(Calculator.getAzimuth());

        if (AugmentedReality.landscape) {
            canvas.save();
            canvas.translate(5, canvas.getHeight() - 5);
            canvas.rotate(-90);
        } else if (radarPosition != Position.TOP_LEFT) {
            canvas.save();
            switch (radarPosition) {

                case TOP_RIGHT:
                    canvas.translate(canvas.getClipBounds().width() - 2 * (RADIUS + PAD_X), 0);
                    break;
                case BOTTOM_LEFT:
                    canvas.translate(0, canvas.getClipBounds().height() - 2 * (RADIUS + PAD_Y));
                    break;
                case BOTTOM_RIGHT:
                    canvas.translate(canvas.getClipBounds().width() - 2 * (RADIUS + PAD_X),
                                     canvas.getClipBounds().height() - 2 * (RADIUS + PAD_Y));
                    break;
            }
        }

        // Update the radar graphics and text based upon the new pitch and
        // bearing
        drawRadarCircle(canvas);
        drawRadarPoints(canvas);
        if (circleImage == null) drawRadarLines(canvas);
        drawRadarText(canvas);

        if (AugmentedReality.landscape) {
            canvas.restore();
        } else if (radarPosition != Position.TOP_LEFT) {
            canvas.restore();
        }
    }

    private void drawRadarCircle(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        if (circleContainer == null) {
            PaintableObject paintableObject;
            if (circleImage != null) {
                paintableObject = new PaintableIcon(circleImage, (int) (RADIUS * 2), ((int) (RADIUS * 2)));
            } else {
                paintableObject = new PaintableCircle(RADAR_COLOR, RADIUS, true);
            }
            circleContainer = new PaintablePosition(paintableObject, PAD_X + RADIUS, PAD_Y + RADIUS, 0, 1);
        }
        circleContainer.paint(canvas);
    }

    private void drawRadarPoints(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        if (radarPoints == null) radarPoints = new PaintableRadarPoints();

        if (pointsContainer == null) {
            pointsContainer = new PaintablePosition(radarPoints, PAD_X, PAD_Y, -ARData.getAzimuth(), 1);
        } else { pointsContainer.set(radarPoints, PAD_X, PAD_Y, -ARData.getAzimuth(), 1); }

        pointsContainer.paint(canvas);
    }

    private void drawRadarLines(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        // Left line
        if (leftLineContainer == null) {
            leftRadarLine.set(0, -RADIUS);
            leftRadarLine.rotate(-CameraModel.DEFAULT_VIEW_ANGLE / 2);
            leftRadarLine.add(PAD_X + RADIUS, PAD_Y + RADIUS);

            float leftX = leftRadarLine.getX() - (PAD_X + RADIUS);
            float leftY = leftRadarLine.getY() - (PAD_Y + RADIUS);
            PaintableLine leftLine = new PaintableLine(LINE_COLOR, leftX, leftY);
            leftLineContainer = new PaintablePosition(leftLine, PAD_X + RADIUS, PAD_Y + RADIUS, 0, 1);
        }
        leftLineContainer.paint(canvas);

        // Right line
        if (rightLineContainer == null) {
            rightRadarLine.set(0, -RADIUS);
            rightRadarLine.rotate(CameraModel.DEFAULT_VIEW_ANGLE / 2);
            leftRadarLine.add(PAD_X + RADIUS, PAD_Y + RADIUS);

            float rightX = rightRadarLine.getX() - (PAD_X + RADIUS);
            float rightY = rightRadarLine.getY() - (PAD_Y + RADIUS);
            PaintableLine rightLine = new PaintableLine(LINE_COLOR, rightX, rightY);
            rightLineContainer = new PaintablePosition(rightLine, PAD_X + RADIUS, PAD_Y + RADIUS, 0, 1);
        }
        rightLineContainer.paint(canvas);
    }

    private void drawRadarText(Canvas canvas) {
        if (canvas == null) throw new NullPointerException();

        // Direction text
        int range = (int) (ARData.getAzimuth() / (360f / 16f));
        String dirTxt = "";
        if (range == 15 || range == 0) { dirTxt = "N"; } else if (range == 1 || range == 2) { dirTxt = "NE"; } else if (
                range == 3 || range == 4) {
            dirTxt = "E";
        } else if (range == 5 || range == 6) { dirTxt = "SE"; } else if (range == 7 || range == 8) {
            dirTxt = "S";
        } else if (range == 9 || range == 10) { dirTxt = "SW"; } else if (range == 11 || range == 12) { dirTxt = "W"; } else if (
                range == 13 || range == 14) { dirTxt = "NW"; }
        int bearing = (int) ARData.getAzimuth();
        radarText(canvas, "" + bearing + ((char) 176) + " " + dirTxt, (PAD_X + RADIUS), PAD_Y, true);

        // Zoom text
        radarText(canvas, formatDist(ARData.getRadius() * 1000), (PAD_X + RADIUS), (PAD_Y + RADIUS * 2 - 10), true);
    }

    private void radarText(Canvas canvas, String txt, float x, float y, boolean bg) {
        if (canvas == null || txt == null) throw new NullPointerException();

        if (paintableText == null) { paintableText = new PaintableText(txt, TEXT_COLOR, (int) (TEXT_SIZE * Density), bg); } else {
            paintableText.set(txt, TEXT_COLOR, (int) (TEXT_SIZE * Density), bg);
        }

        if (paintedContainer == null) { paintedContainer = new PaintablePosition(paintableText, x, y, 0, 1); } else {
            paintedContainer.set(paintableText, x, y, 0, 1);
        }

        paintedContainer.paint(canvas);
    }

    private static String formatDist(float meters) {
        return UnitUtils.stringForDistance((int) meters);
//        if (meters < 1000) {
//            return ((int) meters) + "m";
//        } else if (meters < 10000) {
//            return formatDec(meters / 1000f, 1) + "km";
//        } else {
//            return ((int) (meters / 1000f)) + "km";
//        }
    }

    private static String formatDec(float val, int dec) {
        int factor = (int) Math.pow(10, dec);

        int front = (int) (val);
        int back = (int) Math.abs(val * (factor)) % factor;

        return front + "." + back;
    }
}
