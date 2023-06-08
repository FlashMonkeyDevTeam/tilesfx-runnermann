/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2016-2021 Gerrit Grunwald.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.hansolo.tilesfx.runnermann.skins;

import eu.hansolo.tilesfx.runnermann.Alarm;
import eu.hansolo.tilesfx.runnermann.Section;
import eu.hansolo.tilesfx.runnermann.Tile;
import eu.hansolo.tilesfx.runnermann.events.AlarmEvent;
import eu.hansolo.tilesfx.runnermann.events.TimeEvent.TimeEventType;
import eu.hansolo.tilesfx.runnermann.events.TimeEventListener;
import eu.hansolo.tilesfx.runnermann.fonts.Fonts;
import eu.hansolo.tilesfx.runnermann.tools.GradientLookup;
import eu.hansolo.tilesfx.runnermann.tools.Helper;
import javafx.beans.value.ChangeListener;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static eu.hansolo.tilesfx.runnermann.events.TileEvent.EventType.RECALC;
import static eu.hansolo.tilesfx.runnermann.events.TileEvent.EventType.TIME_PERIOD;
import static eu.hansolo.tilesfx.runnermann.events.TileEvent.EventType.VISIBILITY;
import static eu.hansolo.tilesfx.runnermann.tools.Helper.enableNode;


public class CountdownTimerTileSkin extends TileSkin {
    private static final double                  ANGLE_RANGE = 360;
    private static final DateTimeFormatter       DTF         = DateTimeFormatter.ofPattern("HH:mm:ss");
    private              double                  size;
    private              double                  chartSize;
    private              Arc                     barBackground;
    private              Arc                     bar;
    private              Line                    separator;
    private              Text                    titleText;
    private              Text                    text;
    private              Text                    durationText;
    private              TextFlow                durationFlow;
    private              Text                    timeText;
    private              TextFlow                timeFlow;
    private              long                    range;
    private              double                  angleStep;
    private              Duration                duration;
    private              ChangeListener<Boolean> runningListener;
    private              TimeEventListener       timeListener;

    // LS Added for dynamic bar color
    private              GradientLookup gradientLookup;
    private              boolean        colorGradientEnabled;
    private              int            noOfGradientStops;
    private              long            count;


    // ******************** Constructors **************************************
    public CountdownTimerTileSkin(Tile TILE) {
        super(TILE);
    }


    // ******************** Initialization ************************************
    @Override protected void initGraphics() {
        super.initGraphics();

        duration  = tile.getTimePeriod();
        minValue  = 0;
        maxValue  = duration.getSeconds();
        range     = duration.getSeconds();
        angleStep = ANGLE_RANGE / range;
        locale    = tile.getLocale();
        count     = 1;

        // LS added gradients
        gradientLookup       = new GradientLookup(tile.getGradientStops());
        noOfGradientStops    = tile.getGradientStops().size();
        sectionsVisible      = tile.getSectionsVisible();
        colorGradientEnabled = tile.isStrokeWithGradient();

        titleText = new Text();
        titleText.setFill(tile.getTitleColor());
        enableNode(titleText, !tile.getTitle().isEmpty());

        text = new Text(tile.getText());
        text.setFill(tile.getTextColor());
        enableNode(text, tile.isTextVisible());

        barBackground = new Arc(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.468, PREFERRED_HEIGHT * 0.468, 90, 360);
        barBackground.setType(ArcType.OPEN);
        barBackground.setStroke(tile.getBarBackgroundColor());
        barBackground.setStrokeWidth(PREFERRED_WIDTH * 0.125);
        barBackground.setStrokeLineCap(StrokeLineCap.BUTT);
        barBackground.setFill(null);

        bar = new Arc(PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.5, PREFERRED_WIDTH * 0.468, PREFERRED_HEIGHT * 0.468, 90, 0);
        bar.setType(ArcType.OPEN);
        bar.setStroke(tile.getBarColor());
        bar.setStrokeWidth(PREFERRED_WIDTH * 0.125);
        bar.setStrokeLineCap(StrokeLineCap.BUTT);
        bar.setFill(null);

        separator = new Line(PREFERRED_WIDTH * 0.5, 1, PREFERRED_WIDTH * 0.5, 0.16667 * PREFERRED_HEIGHT);
        separator.setStroke(tile.getBackgroundColor());
        separator.setFill(Color.TRANSPARENT);

        durationText = new Text();
        durationText.setFont(Fonts.latoRegular(PREFERRED_WIDTH * 0.27333));
        durationText.setFill(tile.getValueColor());
        durationText.setTextOrigin(VPos.CENTER);

        durationFlow = new TextFlow(durationText);
        durationFlow.setTextAlignment(TextAlignment.CENTER);

        // Current time
        timeText = new Text("");
        timeText.setFont(Fonts.latoRegular(PREFERRED_WIDTH * 0.27333));
        timeText.setFill(tile.getValueColor());
        timeText.setTextOrigin(VPos.CENTER);
        enableNode(timeText, true);

        timeFlow = new TextFlow(timeText);
        timeFlow.setTextAlignment(TextAlignment.CENTER);

        runningListener = (o, ov, nv) -> {
            if (nv) {
                timeText.setText("");
            }
        };
        timeListener = e -> {
            if (TimeEventType.SECOND == e.TYPE) {
                updateBar();
            }
        };

        getPane().getChildren().addAll(barBackground, bar, separator, titleText, text, durationFlow, timeFlow);
    }

    @Override protected void registerListeners() {
        super.registerListeners();
        tile.runningProperty().addListener(runningListener);
        tile.addTimeEventListener(timeListener);
    }


    // ******************** Methods *******************************************
    @Override protected void handleEvents(final String EVENT_TYPE) {
        super.handleEvents(EVENT_TYPE);

        if (RECALC.name().equals(EVENT_TYPE)) {
            redraw();
        } else if (VISIBILITY.name().equals(EVENT_TYPE)) {
            enableNode(titleText, !tile.getTitle().isEmpty());
            enableNode(text, tile.isTextVisible());
            enableNode(timeText, tile.isValueVisible());
        } else if (TIME_PERIOD.name().equals(EVENT_TYPE)) {
            duration  = tile.getTimePeriod();
            minValue  = 0;
            maxValue  = duration.getSeconds();
            range     = duration.getSeconds();
            angleStep = ANGLE_RANGE / range;
        }
    }



    private void updateBar() {

        long secs = duration.getSeconds();
        long value = secs;
        double colorStep = getColorStep(secs, range);

        //if (duration.getSeconds() > 0) {

        if (secs > 0) {
            bar.setLength(-value * angleStep);
            durationText.setText(DTF.format(LocalTime.ofSecondOfDay(value)) + "\nAHEAD");
        }
        else if ( secs == 0 ) {
            count = 1;
            durationText.setText(DTF.format(LocalTime.ofSecondOfDay(0)) + "\nAHEAD");
            tile.fireAlarmEvent(new AlarmEvent(new Alarm(ZonedDateTime.now(), tile.getAlarmColor())));
            bar.setLength(0);
        }
        else if (secs >= (range * -3)) {
            if (count > range) {
                count = 1;
            }
            bar.setLength(count++ * angleStep);
            durationText.setText(DTF.format(LocalTime.ofSecondOfDay(-value)) + "\nPAST");
        }
        else {
            //tile.setRunning(false);
            durationText.setText("");
            bar.setLength(0);
            durationText.setText(DTF.format(LocalTime.ofSecondOfDay(-value)) + "\nPAST");
        }
        setBarColor(colorStep);
        duration = duration.minusSeconds(1);
    }

    private double getColorStep(long secs, long range) {
        double nr = secs / (double) (range * 4);
        if ( secs > 0 ) {;
            return 1;
        } else if ( nr > -.251 ) {
            return 0.74;
        } else if ( nr > -.501 ) {
            return .4;
        } else if ( nr > -.761 ){
            return 0.0;
        } else {
            return 0.0;
        }
    }

    /**
     * Must be between 1 - 0
     * @param colorStep between 1 - 0
     */
    private void setBarColor(double colorStep) {
        //double colorStep = seconds / range;
        if (!sectionsVisible && !colorGradientEnabled) {
            //bar.setStroke(tile.getBarColor());
            bar.setStroke(gradientLookup.getColorAt(colorStep));
        } else if (colorGradientEnabled && noOfGradientStops > 1) {
            bar.setStroke(gradientLookup.getColorAt(colorStep));
        } else {
            bar.setStroke(tile.getBarColor());
            for (Section section : sections) {
                if (section.contains(colorStep)) {
                    bar.setStroke(section.getColor());
                    break;
                }
            }
        }
    }

    @Override public void dispose() {
        tile.runningProperty().removeListener(runningListener);
        tile.removeTimeEventListener(timeListener);
        super.dispose();
    }


    // ******************** Resizing ******************************************
    @Override protected void resizeStaticText() {
        double maxWidth = width - size * 0.1;
        double fontSize = size * textSize.factor;

        boolean customFontEnabled = tile.isCustomFontEnabled();
        Font    customFont        = tile.getCustomFont();
        Font    font              = (customFontEnabled && customFont != null) ? Font.font(customFont.getFamily(), fontSize) : Fonts.latoRegular(fontSize);

        titleText.setFont(font);
        if (titleText.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(titleText, maxWidth, fontSize); }
        switch(tile.getTitleAlignment()) {
            default    :
            case LEFT  : titleText.relocate(size * 0.05, size * 0.05); break;
            case CENTER: titleText.relocate((width - titleText.getLayoutBounds().getWidth()) * 0.5, size * 0.05); break;
            case RIGHT : titleText.relocate(width - (size * 0.05) - titleText.getLayoutBounds().getWidth(), size * 0.05); break;
        }

        text.setFont(font);
        if (text.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(text, maxWidth, fontSize); }
        switch(tile.getTextAlignment()) {
            default    :
            case LEFT  : text.setX(size * 0.05); break;
            case CENTER: text.setX((width - text.getLayoutBounds().getWidth()) * 0.5); break;
            case RIGHT : text.setX(width - (size * 0.05) - text.getLayoutBounds().getWidth()); break;
        }
        text.setY(height - size * 0.05);
    }
    @Override protected void resizeDynamicText() {
        double maxWidth = chartSize * 0.8;
        double fontSize = chartSize * 0.15;
        durationText.setFont(Fonts.latoRegular(fontSize));
        if (durationText.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(durationText, maxWidth, fontSize); }

        maxWidth = chartSize * 0.4;
        fontSize = chartSize * 0.1;
        timeText.setFont(Fonts.latoRegular(fontSize));
        if (timeText.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(timeText, maxWidth, fontSize); }
    }

    @Override protected void resize() {
        super.resize();
        width  = tile.getWidth() - tile.getInsets().getLeft() - tile.getInsets().getRight();
        height = tile.getHeight() - tile.getInsets().getTop() - tile.getInsets().getBottom();
        size   = width < height ? width : height;

        if (tile.isShowing() && width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);

            double chartWidth  = contentBounds.getWidth();
            double chartHeight = contentBounds.getHeight();
            chartSize          = chartWidth < chartHeight ? chartWidth : chartHeight;

            double radius = chartSize * 0.495 - contentBounds.getX();

            barBackground.setCenterX(contentCenterX);
            barBackground.setCenterY(contentCenterY);
            barBackground.setRadiusX(radius);
            barBackground.setRadiusY(radius);
            barBackground.setStrokeWidth(chartSize * 0.125);

            bar.setCenterX(contentCenterX);
            bar.setCenterY(contentCenterY);
            bar.setRadiusX(radius);
            bar.setRadiusY(radius);
            bar.setStrokeWidth(chartSize * 0.125);

            separator.setStartX(contentCenterX);
            separator.setStartY(contentCenterX - radius - chartSize * 0.05);
            separator.setEndX(contentCenterX);
            separator.setEndY(contentCenterX - radius + chartSize * 0.05);

            resizeStaticText();
            durationFlow.setPrefWidth(width * 0.9);
            durationFlow.relocate(width * 0.05, bar.getCenterY() - chartSize * 0.12);

            timeFlow.setPrefWidth(width * 0.9);
            timeFlow.relocate(width * 0.05, bar.getCenterY() + chartSize * 0.15);
        }
    }

    @Override protected void redraw() {
        super.redraw();

        //gradientLookup.setStops(tile.getGradientStops());
        //colorGradientEnabled = tile.isStrokeWithGradient();

        //barBackground.setStroke(tile.getBarBackgroundColor());
        //setBarColor(duration.getSeconds());

        durationText.setFill(tile.getValueColor());
        timeText.setFill(tile.getValueColor());
        titleText.setFill(tile.getTitleColor());
        text.setFill(tile.getTextColor());
        separator.setStroke(tile.getBackgroundColor());

        titleText.setText(tile.getTitle());
        text.setText(tile.getText());

        resizeStaticText();
        resizeDynamicText();
    }
}