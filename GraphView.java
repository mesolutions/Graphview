/**
 * This file is part of GraphView.
 *
 * GraphView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphView.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 *
 * Copyright Jonas Gehring
 */

package com.jjoe64.graphview;

import java.io.Serializable;
import java.sql.Date;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.compatible.ScaleGestureDetector;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs. This
 * 
 * is the abstract base class for all graphs. Extend this class and implement
 * {@link #drawSeries(Canvas, GraphViewDataInterface[], float, float, float, double, double, double, double, float)}
 * to display a custom graph. Use {@link LineGraphView} for creating a line
 * chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser
 *         General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout {
	static final private class GraphViewConfig {
		static final float BORDER = 20;
	}

	public class GraphViewContentView extends View // implements
	// View.OnClickListener
	{
		private float lastTouchEventX;
		private float graphwidth;
		private boolean scrollingStarted;
		private Paint paintInd;
		double viewPStart, viewPEnd;
		boolean clickedOnce = false;
		private double viewWidth;
		boolean redrawGraph = true;
		private GestureDetector mDetector;
		boolean isShowPressCalled = false;
		final int axisColor=0xFFCCCCCC;
		Canvas canvas;


		class EventData {
			public float x;
			public float y;
			public float pressure;
		}

		Map<Integer, EventData> eventDataMap;

		/**
		 * @param context
		 */

		public GraphViewContentView(Context context) {
			super(context);

			paintInd = new Paint();
			eventDataMap = new HashMap<Integer, EventData>();
			final SimpleDateFormat dhFormat = new SimpleDateFormat(
					"MMM-dd \n HH:mm"); 	//NO I18N  
			mDetector = new GestureDetector(GraphView.this.getContext(),
					new mListener());
			this.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (clickedOnce) {
						eventDataMap.clear();
						onMoveGesture(initialStart, initialEnd);
					}

					clickedOnce = true;
					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							clickedOnce = false;
						}
					}, 300);

				}
			});
			mDetector = new GestureDetector(GraphView.this.getContext(),
					new mListener());
		}

		public Map<Integer, Float> getEventData() {
			Map<Integer, Float> newMap = new HashMap<Integer, Float>();
			newMap.put(0, eventDataMap.get(0).x);
			newMap.put(1, eventDataMap.get(1).x);
			return newMap;
		}

		public void clearEventData() {
			eventDataMap.clear();
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {

		
			this.canvas = canvas;
			paint.setAntiAlias(true);
			paint.setStrokeWidth(0);

			paintInd.setStyle(Paint.Style.FILL);
			paintInd.setColor(Color.LTGRAY);
			// canvas.drawColor(Color.BLACK);

			float border = GraphViewConfig.BORDER;
			float horstart = 0;
			float height = getHeight();
			float width = getWidth() - 1;

			// measure bottom text
			// if(redrawGraph)
			{

				if (labelTextHeight == null || horLabelTextWidth == null) {
					paint.setTextSize(getGraphViewStyle().getTextSize());
					double testX = ((getMaxX(true) - getMinX(true)) * 0.783)
							+ getMinX(true);
					String testLabel = formatLabel(testX, true);
					paint.getTextBounds(testLabel, 0, testLabel.length(),
							textBounds);
					
					labelTextHeight = (int) (dpToPx(10));
					horLabelTextWidth = (textBounds.width());
				}
				border += labelTextHeight;

				float graphheight = height - (2 * border);
				graphwidth = width;
				if (horlabels == null) {
					horlabels = generateHorlabels(graphwidth);
				}
				if (verlabels == null) {
					verlabels = generateVerlabels(graphheight);
				}

				// vertical lines
				paint.setTextAlign(Align.LEFT);
				int vers = verlabels.length - 1;
				for (int i = 0; i < verlabels.length; i++) {
					paint.setColor(graphViewStyle.getGridColor());
					float y = ((graphheight / vers) * i) + border;
					canvas.drawLine(horstart, y, width, y, paint);

				}
				
				
				Paint axisPaint=new Paint();
				axisPaint.setColor(axisColor);
				axisPaint.setStrokeWidth(5);
				Paint paintYAxis=new Paint();
				paintYAxis.setStrokeWidth(dpToPx(1));
				paintYAxis.setColor(0xFFCCCCCC);
				canvas.drawLine(0, graphheight+border, 0, 0, paintYAxis);
				canvas.drawLine(0, graphheight+border+dpToPx(1), graphwidth, graphheight+border+dpToPx(1), paintYAxis);
				
				drawHorizontalLabels(canvas, border, horstart, height,
						horlabels, graphwidth);
				paint.setTextAlign(Align.CENTER);
				double maxY = getMaxY();
				double minY = getMinY();
				double maxX = viewportStart + viewportSize;
				//
				//
				double minX = viewportStart;
				// getMinX(false);
				//
				double diffX = maxX - minX;

				if (maxY == minY) {
					// if min/max is the same, fake it so that we can render a
					// line
					if (maxY == 0) {
						// if both are zero, change the values to prevent
						// division
						// by zero
						maxY = 1.0d;
						minY = 0.0d;
					} else {
						maxY = maxY * 1.05d;
						minY = minY * 0.95d;
					}
				}
				double diffY = maxY - minY;
				paint.setStrokeCap(Paint.Cap.ROUND);
				for (int i = 0; i < graphSeries.size(); i++) {
					drawSeries(canvas, _values(i), graphwidth, graphheight,
							border, minX, minY, diffX, diffY, horstart,
							graphSeries.get(i).style);
				}

			}

			paintInd.setStyle(Paint.Style.FILL);
			paintInd.setColor(0xFF398eb5);
			for (EventData event : eventDataMap.values()) {

				canvas.drawRect(event.x - dpToPx(0.5f), 0, event.x + dpToPx(0.5f),
						canvas.getHeight(), paintInd);
			}

		}

		public void onMoveGesture(double vpStart, double vpEnd) {
			// view port update
			if (viewportSize != 0) {

				viewportStart = vpStart;
				viewportSize = vpEnd - vpStart;

				setViewPort(viewportStart, viewportSize);
				if (!staticHorizontalLabels)
					horlabels = null;
				if (!staticVerticalLabels)
					verlabels = null;

				viewVerLabels.invalidate();
			}
			invalidate();
		}

		/**
		 * @param event
		 */

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
			this.dispatchDraw(canvas);

			viewWidth = this.getRight() - this.getLeft();
			int pointerIndex = event.getActionIndex();
			int pointerId = event.getPointerId(pointerIndex);
			int eventType = event.getAction();
			switch (eventType & MotionEvent.ACTION_MASK) {

//			case MotionEvent.ACTION_HOVER_EXIT:
//			case MotionEvent.ACTION_CANCEL:

			case MotionEvent.ACTION_DOWN:
				EventData eventData = new EventData();
				eventData.x = event.getX(pointerIndex);
				eventData.y = event.getY(pointerIndex);
				if (eventDataMap.size() < 2) {
					eventDataMap.put(Integer.valueOf(pointerId), eventData);
				}
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				getParent().requestDisallowInterceptTouchEvent(true);
				eventData = new EventData();
				eventData.x = event.getX(pointerIndex);
				eventData.y = event.getY(pointerIndex);
				if (eventDataMap.size() < 2) {
					eventDataMap.put(Integer.valueOf(pointerId), eventData);
				}
				invalidate();
				return true;

			case MotionEvent.ACTION_MOVE:
				getParent().requestDisallowInterceptTouchEvent(true);
				for (int i = 0; i < event.getPointerCount(); i++) {
					int curPointerId = event.getPointerId(i);
					if (eventDataMap.containsKey(Integer.valueOf(curPointerId))) {
						EventData moveEventData = eventDataMap.get(new Integer(
								curPointerId));
						moveEventData.x = event.getX(i);
						moveEventData.y = event.getY(i);
						//
					}
				}
				invalidate();
				return true;

			case MotionEvent.ACTION_POINTER_UP:

				double tempViewPEnd;
				double tempViewPStart;
				double vpDiff;

				if (eventDataMap.size() == 2) {

					tempViewPEnd = viewportSize + viewportStart;
					tempViewPStart = viewportStart;
					if (eventDataMap.get(0).x < eventDataMap.get(1).x) {
						viewPStart = (eventDataMap.get(0).x);
						viewPEnd = eventDataMap.get(1).x;
					} else {
						viewPEnd = (eventDataMap.get(0).x);
						viewPStart = eventDataMap.get(1).x;
					}
					viewPEnd /= viewWidth;
					viewPStart /= viewWidth;
					vpDiff = tempViewPEnd - tempViewPStart;
					viewPEnd = tempViewPStart + (viewPEnd * vpDiff);

					viewPStart = tempViewPStart + (viewPStart * vpDiff);
					setViewPort(viewPStart, (viewPEnd - viewPStart));
					if (_values(0).length < 8) {
						setViewPort(tempViewPStart, tempViewPEnd
								- tempViewPStart);
						// redrawGraph=false;
						viewPStart = tempViewPStart;
						viewPEnd = tempViewPEnd;
					}
					onMoveGesture(viewPStart, viewPEnd);
				}

				eventDataMap.clear();
				invalidate();
				return true;

			case MotionEvent.ACTION_UP:
				eventDataMap.clear();
				invalidate();
				return true;
			}
			return false;

		}

		@Override
		public boolean performClick() {
			// TODO Auto-generated method stub
			return super.performClick();
		}

		private class mListener extends GestureDetector.SimpleOnGestureListener {

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				// TODO Auto-generated method stub
				return true;
			}	

			@Override
			public void onShowPress(MotionEvent e) {
				// TODO Auto-generated method stub
				isShowPressCalled = true;
				super.onShowPress(e);
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				// TODO Auto-generated method stub
				// return super.onSingleTapUp(e);
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				// TODO Auto-generated method stub
				super.onLongPress(e);
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent event,
					float distanceX, float distanceY) {
				// TODO Auto-generated method stub
				if (isShowPressCalled)
				{
					return false;
				}
				else
				{
					return true;
				}
				// return super.onScroll(e1, event, distanceX, distanceY);
			}
		}

	}

	/**
	 * one data set for a graph series
	 */
	static public class GraphViewData implements GraphViewDataInterface,
			Serializable {
		public final double valueX;
		public final double valueY;

		public GraphViewData(long valueX, double valueY) {
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}

		@Override
		public double getX() {
			return valueX;
		}

		@Override
		public double getY() {
			return valueY;
		}

	}

	public enum LegendAlign {
		TOP, MIDDLE, BOTTOM
	}

	private class VerLabelsView extends View {
		/**
		 * @param context
		 */
		public VerLabelsView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(getGraphViewStyle()
					.getVerticalLabelsWidth() == 0 ? 100 : getGraphViewStyle()
					.getVerticalLabelsWidth(), LayoutParams.FILL_PARENT));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			// normal
			paint.setStrokeWidth(0);

			if (labelTextHeight == null || verLabelTextWidth == null) {
				paint.setTextSize(getGraphViewStyle().getTextSize());
				double testY = ((getMaxY() - getMinY()) * 0.783) + getMinY();
				String testLabel = formatLabel(testY, false);
				paint.getTextBounds(testLabel, 0, testLabel.length(),
						textBounds);
				labelTextHeight = (textBounds.height());
				verLabelTextWidth = (textBounds.width());
			}
			if (getGraphViewStyle().getVerticalLabelsWidth() == 0
					&& getLayoutParams().width != verLabelTextWidth
							+ GraphViewConfig.BORDER) {
				setLayoutParams(new LayoutParams(
						(int) (verLabelTextWidth + GraphViewConfig.BORDER),
						LayoutParams.FILL_PARENT));
			} else if (getGraphViewStyle().getVerticalLabelsWidth() != 0
					&& getGraphViewStyle().getVerticalLabelsWidth() != getLayoutParams().width) {
				setLayoutParams(new LayoutParams(getGraphViewStyle()
						.getVerticalLabelsWidth(), LayoutParams.FILL_PARENT));
			}

			float border = GraphViewConfig.BORDER;
			border += labelTextHeight;
			float height = getHeight();
			float graphheight = height - (2 * border);

			if (verlabels == null) {
				verlabels = generateVerlabels(graphheight);
			}

			// vertical labels
			paint.setTextAlign(getGraphViewStyle().getVerticalLabelsAlign());
			int labelsWidth = getWidth();
			int labelsOffset = 0;
			if (getGraphViewStyle().getVerticalLabelsAlign() == Align.RIGHT) {
				labelsOffset = labelsWidth;
			} else if (getGraphViewStyle().getVerticalLabelsAlign() == Align.CENTER) {
				labelsOffset = labelsWidth / 2;
			}
			int vers = verlabels.length - 1;
			paint.setColor(graphViewStyle.getVerticalLabelsColor());
			for (int i = 0; i < verlabels.length; i++) {
				float y = ((graphheight / vers) * i) + border;
				
				canvas.drawText(verlabels[i], labelsOffset, y, paint);
			}

			// reset
			paint.setTextAlign(Align.LEFT);
		}

	}

	protected final Paint paint;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean scrollable;
	private boolean disableTouch;
	private double viewportStart;
	private double viewportSize;
	private final View viewVerLabels;
	private ScaleGestureDetector scaleDetector;
	private boolean scalable;
	private final NumberFormat[] numberformatter = new NumberFormat[2];
	private final List<GraphViewSeries> graphSeries;
	private boolean showLegend = false;
	private LegendAlign legendAlign = LegendAlign.MIDDLE;
	private boolean manualYAxis;
	private double manualMaxYValue;
	private double manualMinYValue;
	protected GraphViewStyle graphViewStyle;
	private final GraphViewContentView graphViewContentView;
	private CustomLabelFormatter customLabelFormatter;
	private Integer labelTextHeight;
	private Integer horLabelTextWidth;
	private Integer verLabelTextWidth;
	private final Rect textBounds = new Rect();
	private boolean staticHorizontalLabels;
	private boolean staticVerticalLabels;
	public double initialStart, initialEnd;
	private  float screenDensity;

	public GraphView(Context context, AttributeSet attrs) {
		this(context, attrs.getAttributeValue(null, "title"));

		int width = attrs.getAttributeIntValue("android", "layout_width",
				LayoutParams.MATCH_PARENT);
		int height = attrs.getAttributeIntValue("android", "layout_height",
				LayoutParams.MATCH_PARENT);
		setLayoutParams(new LayoutParams(width, height));
	}

	public GraphViewContentView getContentView() {
		return this.graphViewContentView;
	}

	/**
	 * @param context
	 * @param title
	 *            [optional]
	 */
	public GraphView(Context context, String title) {
		super(context);
		
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		if (title == null)
		{
			this.title = "";
		}
		this.setScreenDensity(context.getResources().getDisplayMetrics().density);
		graphViewStyle = new GraphViewStyle();
		graphViewStyle.useTextColorFromTheme(context);

		paint = new Paint();
		graphSeries = new ArrayList<GraphViewSeries>();

		viewVerLabels = new VerLabelsView(context);

		graphViewContentView = new GraphViewContentView(context);
		addView(viewVerLabels);
		addView(graphViewContentView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
		graphViewContentView.bringToFront();

	}

	public double getViewportStart() {
		return this.viewportStart;
	}

	public double getViewportSize() {
		return this.viewportSize;
	}

	public void setViewportStart(double viewportStart) {
		this.viewportStart = viewportStart;
	}

	public void setViewportSize(double viewportSize) {
		this.viewportSize = viewportSize;
	}

	private GraphViewDataInterface[] _values(int idxSeries) {
		GraphViewDataInterface[] values = graphSeries.get(idxSeries).values;
		synchronized (values) {
			if (viewportStart == 0 && viewportSize == 0) {
				// all data
				return values;
			} else {
				// viewport
				List<GraphViewDataInterface> listData = new ArrayList<GraphViewDataInterface>();
				for (int i = 0; i < values.length; i++) {
					if (values[i].getX() >= viewportStart) {
						if (values[i].getX() > viewportStart + viewportSize) {
							listData.add(values[i]); // one more for nice
														// scrolling
							break;
						} else {
							listData.add(values[i]);
						}
					} else {
						if (listData.isEmpty()) {
							listData.add(values[i]);
						}
						listData.set(0, values[i]); // one before, for nice
													// scrolling
					}
				}

				return listData.toArray(new GraphViewDataInterface[listData
						.size()]);
			}
		}
	}

	/**
	 * add a series of data to the graph
	 * 
	 * @param series
	 */
	public void addSeries(GraphViewSeries series) {
		series.addGraphView(this);
		graphSeries.add(series);
		redrawAll();
	}

	public void setInitialXBounds(double start, double end) {
		this.initialStart = start;
		this.initialEnd = end;
	}

	protected void drawHorizontalLabels(Canvas canvas, float border,
			float horstart, float height, String[] horlabels, float graphwidth) {
		// horizontal labels + lines
		Calendar cal1, cal2;
		cal1 = new GregorianCalendar();
		cal2 = new GregorianCalendar();
		String prevDate = "";
		
		//canvas.translate(0, 15);
		int hors = horlabels.length - 1;
		for (int i = 0; i < horlabels.length; i++) {
			
			float x = ((graphwidth / hors) * i) + horstart;
			
			paint.setTextAlign(Align.CENTER);
			paint.setColor(Color.BLACK);
			if (i == horlabels.length - 1)
			{
				paint.setTextAlign(Align.RIGHT);
			}
			if (i == 0)
			{
				paint.setTextAlign(Align.LEFT);
			}
			paint.setColor(graphViewStyle.getHorizontalLabelsColor());
			String[] Text = horlabels[i].split("\\n");

			if (Text.length > 1) {

				if (i > 0 && (Text[0].equals(prevDate) == false)) {
					canvas.drawText(Text[1], x, height - dpToPx(7), paint);
					canvas.drawText(Text[0], x, height, paint);
				} else if (i == 0) {
					canvas.drawText(Text[1], x, height - dpToPx(7), paint);
					canvas.drawText(Text[0], x, height , paint);
				} else {
					canvas.drawText(Text[1], x, height- dpToPx(7), paint);
				}
				prevDate = Text[0];
			} else
			{
				canvas.drawText(horlabels[i], x, height- dpToPx(5), paint);
			}
		}
	}

	protected void drawLegend(Canvas canvas, float height, float width) {
		float textSize = paint.getTextSize();
		int spacing = getGraphViewStyle().getLegendSpacing();
		int border = getGraphViewStyle().getLegendBorder();
		int legendWidth = getGraphViewStyle().getLegendWidth();

		int shapeSize = (int) (textSize * 0.8d);

		// rect
		paint.setARGB(180, 100, 100, 100);
		float legendHeight = (shapeSize + spacing) * graphSeries.size() + 2
				* border - spacing;
		float lLeft = width - legendWidth - border * 2;
		float lTop;
		switch (legendAlign) {
		case TOP:
			lTop = 0;
			break;
		case MIDDLE:
			lTop = height / 2 - legendHeight / 2;
			break;
		default:
			lTop = height - GraphViewConfig.BORDER - legendHeight
					- getGraphViewStyle().getLegendMarginBottom();
		}
		float lRight = lLeft + legendWidth;
		float lBottom = lTop + legendHeight;
		canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8,
				paint);

		for (int i = 0; i < graphSeries.size(); i++) {
			paint.setColor(graphSeries.get(i).style.color);
			canvas.drawRect(new RectF(lLeft + border, lTop + border
					+ (i * (shapeSize + spacing)), lLeft + border + shapeSize,
					lTop + border + (i * (shapeSize + spacing)) + shapeSize),
					paint);
			if (graphSeries.get(i).description != null) {
				paint.setColor(Color.WHITE);
				paint.setTextAlign(Align.LEFT);
				canvas.drawText(graphSeries.get(i).description, lLeft + border
						+ shapeSize + spacing, lTop + border + shapeSize
						+ (i * (shapeSize + spacing)), paint);
			}
		}
	}

	abstract protected void drawSeries(Canvas canvas,
			GraphViewDataInterface[] values, float graphwidth,
			float graphheight, float border, double minX, double minY,
			double diffX, double diffY, float horstart,
			GraphViewSeriesStyle style);

	/**
	 * formats the label use #setCustomLabelFormatter or static labels if you
	 * want custom labels
	 * 
	 * @param value
	 *            x and y values
	 * @param isValueX
	 *            if false, value y wants to be formatted
	 * @deprecated use {@link #setCustomLabelFormatter(CustomLabelFormatter)}
	 * @return value to display
	 */
	@Deprecated
	protected String formatLabel(double value, boolean isValueX) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd");
		SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm");
		SimpleDateFormat dhFormat = new SimpleDateFormat("MMM-dd \n HH:mm");
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd  HH:mm:ss");
		if (customLabelFormatter != null) {
			String label = customLabelFormatter.formatLabel(value, isValueX);
			if (label != null) {
				return label;
			}
		}
		double highestvalue = isValueX ? viewportStart + viewportSize
				: getMaxY();
		double lowestvalue = isValueX ? viewportStart : getMinY();

		if (isValueX) {
			if ((highestvalue - lowestvalue) >= 1000 * 60 * 60 * 24 * 3) {
				return dateFormat.format(new Date((long) value));
			} else if ((highestvalue - lowestvalue >= 1000 * 60 * 60 * 24 * 1)
					&& (highestvalue - lowestvalue <= 1000 * 60 * 60 * 24 * 3))
			{
				return dhFormat.format(new Date((long) value));
			}
			else
			{
				return hourFormat.format(new Date((long) value));
			}
		} else
		{
			return "" + (int) value;
		}

	}

	private int calculateNumLabels(long m, long n) {
		long diff = n - m;
		for (int l = 8; l >= 3; l--) {
			if (diff % (l - 1) == 0)
			{
				return l;
			}
		}
		return 0;
	}

	private String[] generateHorlabels(float graphwidth) {
		GraphView.this.formatHorLabels(graphSeries.size());
		int numLabels = getGraphViewStyle().getNumHorizontalLabels() - 1;

		String[] labels = new String[numLabels + 1];
		double min = viewportStart;
		double max = viewportStart + viewportSize;

		for (int i = 0; i <= numLabels; i++) {
			labels[i] = formatLabel(min + ((max - min) * i / numLabels), true);
		}

		return labels;
	}

	synchronized private String[] generateVerlabels(float graphheight) {
		int numLabels = getGraphViewStyle().getNumVerticalLabels() - 1;
		if (numLabels < 0) {
			numLabels = (int) (graphheight / (labelTextHeight * 3));
		}
		String[] labels = new String[numLabels + 1];
		double min = getMinY();
		double max = getMaxY();
		if (max == min) {
			// if min/max is the same, fake it so that we can render a line
			if (max == 0) {
				// if both are zero, change the values to prevent division by
				// zero
				max = 1.0d;
				min = 0.0d;
			} else {
				max = max * 1.05d;
				min = min * 0.95d;
			}
		}

		for (int i = 0; i <= numLabels; i++) {
			labels[numLabels - i] = formatLabel(min
					+ ((max - min) * i / numLabels), false);
		}
		return labels;
	}

	/**
	 * @return the custom label formatter, if there is one. otherwise null
	 */
	public CustomLabelFormatter getCustomLabelFormatter() {
		return customLabelFormatter;
	}

	/**
	 * @return the graphview style. it will never be null.
	 */
	public GraphViewStyle getGraphViewStyle() {
		return graphViewStyle;
	}

	/**
	 * get the position of the legend
	 * 
	 * @return
	 */
	public LegendAlign getLegendAlign() {
		return legendAlign;
	}

	/**
	 * @return legend width
	 * @deprecated use {@link GraphViewStyle#getLegendWidth()}
	 */
	@Deprecated
	public float getLegendWidth() {
		return getGraphViewStyle().getLegendWidth();
	}

	/**
	 * returns the maximal X value of the current viewport (if viewport is set)
	 * otherwise maximal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're
	 *            doing!
	 */

	public void formatHorLabels(int formatLabels) {
		double curStartDate = viewportStart;
		double curEndDate = viewportStart + viewportSize;

		int calcNumLabels = this.getGraphViewStyle().getNumHorizontalLabels();
		Calendar tcal1 = new GregorianCalendar();
		Calendar tcal2 = new GregorianCalendar();

		tcal1.setTimeInMillis((long) curEndDate);
		if ((curEndDate - curStartDate) >= 13 * 24 * 60 * 60 * 1000) {
			tcal2.setTimeInMillis((long) curStartDate);
			this.getGraphViewStyle().setNumHorizontalLabels(7);
			int day = tcal1.get(Calendar.DAY_OF_MONTH);
			if (tcal1.get(Calendar.HOUR_OF_DAY) > 12)
			{
				day++;
			}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH), day,
					0, 0, 0);
			day = tcal2.get(Calendar.DAY_OF_MONTH);
			if (tcal2.get(Calendar.HOUR_OF_DAY) > 12)
			{
				day++;
			}
			tcal2.set(tcal2.get(Calendar.YEAR), tcal2.get(Calendar.MONTH), day,
					0, 0, 0);
			calcNumLabels = calculateNumLabels(tcal2.get(Calendar.DAY_OF_YEAR),
					tcal1.get(Calendar.DAY_OF_YEAR));
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
		}

		else if ((curEndDate - curStartDate) >= 3 * 24 * 60 * 60 * 1000) {
			tcal2.setTimeInMillis((long) curStartDate);
			int day = tcal1.get(Calendar.DAY_OF_MONTH);
			if (tcal1.get(Calendar.HOUR_OF_DAY) > 12)
			{	day++;}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH), day,
					0, 0, 0);
			day = tcal2.get(Calendar.DAY_OF_MONTH);
			if (tcal2.get(Calendar.HOUR_OF_DAY) > 12)
				{day++;}
			tcal2.set(tcal2.get(Calendar.YEAR), tcal2.get(Calendar.MONTH), day,
					0, 0, 0);
			calcNumLabels = calculateNumLabels(tcal2.get(Calendar.DAY_OF_YEAR),
					tcal1.get(Calendar.DAY_OF_YEAR));
			while (calcNumLabels == 0) {
				tcal2.set(Calendar.DAY_OF_MONTH,
						tcal2.get(Calendar.DAY_OF_MONTH) + 1);
				calcNumLabels = calculateNumLabels(
						tcal2.get(Calendar.DAY_OF_YEAR),
						tcal1.get(Calendar.DAY_OF_YEAR));
			}
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());
		}

		else if ((curEndDate - curStartDate) >= 140 * 60 * 1000) {
			tcal2.setTimeInMillis((long) curStartDate);
			int hour = tcal1.get(Calendar.HOUR_OF_DAY);
			if (tcal1.get(Calendar.MINUTE) > 20)
				{hour++;}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH),
					tcal1.get(Calendar.DAY_OF_MONTH), hour, 0, 0);
			hour = tcal2.get(Calendar.HOUR_OF_DAY);
			if (tcal2.get(Calendar.MINUTE) > 20)
				{hour++;}
			tcal2.set(tcal2.get(Calendar.YEAR), tcal2.get(Calendar.MONTH),
					tcal2.get(Calendar.DAY_OF_MONTH), hour, 0, 0);
			calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
					/ (1000 * 60 * 60), tcal1.getTimeInMillis()
					/ (1000 * 60 * 60));
			while (calcNumLabels == 0) {
				tcal2.set(Calendar.HOUR, tcal2.get(Calendar.HOUR) + 1);
				calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
						/ (1000 * 60 * 60), tcal1.getTimeInMillis()
						/ (1000 * 60 * 60));
			}
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());
		}


		else if ((curEndDate - curStartDate) >= 1000 * 60 * 60 * 2) {
			tcal2.setTimeInMillis((long) curStartDate);
			int roundoff1 = tcal1.get(Calendar.MINUTE) % 15;
			int roundoff2 = tcal2.get(Calendar.MINUTE) % 15;
			if (roundoff1 < 7)
				{roundoff1 = -roundoff1;}
			else
				{roundoff1 = 15 - roundoff1;}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH),
					tcal1.get(Calendar.DAY_OF_MONTH),
					tcal1.get(Calendar.HOUR_OF_DAY), tcal1.get(Calendar.MINUTE)
							+ (roundoff1), 0);
			if (roundoff2 < 7)
				{roundoff2 = -roundoff2;}
			else
				{roundoff2 = 15 - roundoff2;}
			tcal2.set(Calendar.MINUTE, tcal2.get(Calendar.MINUTE) + (roundoff2));
			tcal2.set(Calendar.SECOND, 0);
			calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
					/ (1000 * 60 * 15), tcal1.getTimeInMillis()
					/ (1000 * 60 * 15));
			while (calcNumLabels == 0) {
				tcal2.setTimeInMillis(tcal2.getTimeInMillis() + 1000 * 60 * 15);
				calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
						/ (1000 * 60 * 15), tcal1.getTimeInMillis()
						/ (1000 * 60 * 15));
			}
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());

		} else if ((curEndDate - curStartDate) >= 1000 * 60 * 45) {
			tcal2.setTimeInMillis((long) curStartDate);
			int roundoff1 = tcal1.get(Calendar.MINUTE) % 15;
			int roundoff2 = tcal2.get(Calendar.MINUTE) % 15;
			if (roundoff1 < 7)
				{roundoff1 = -roundoff1;}
			else
				{roundoff1 = 15 - roundoff1;}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH),
					tcal1.get(Calendar.DAY_OF_MONTH),
					tcal1.get(Calendar.HOUR_OF_DAY), tcal1.get(Calendar.MINUTE)
							+ (roundoff1), 0);
			if (roundoff2 < 7)
				{roundoff2 = -roundoff2;}
			else
				{roundoff2 = 15 - roundoff2;}
			tcal2.set(Calendar.MINUTE, tcal2.get(Calendar.MINUTE) + (roundoff2));
			tcal2.set(Calendar.SECOND, 0);
			calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
					/ (1000 * 60 * 15), tcal1.getTimeInMillis()
					/ (1000 * 60 * 15));
			while (calcNumLabels == 0) {
				tcal2.setTimeInMillis(tcal2.getTimeInMillis() + 1000 * 60 * 5);
				calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
						/ (1000 * 60 * 15), tcal1.getTimeInMillis()
						/ (1000 * 60 * 15));
			}
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());
		}

		else if ((curEndDate - curStartDate) >= 1000 * 60 * 10) {
			tcal2.setTimeInMillis((long) curStartDate);
			int roundoff1 = tcal1.get(Calendar.MINUTE) % 5;
			int roundoff2 = tcal2.get(Calendar.MINUTE) % 5;
			if (roundoff1 < 3)
				{roundoff1 = -roundoff1;}
			else
				{roundoff1 = 5 - roundoff1;}
			tcal1.set(tcal1.get(Calendar.YEAR), tcal1.get(Calendar.MONTH),
					tcal1.get(Calendar.DAY_OF_MONTH),
					tcal1.get(Calendar.HOUR_OF_DAY), tcal1.get(Calendar.MINUTE)
							+ (roundoff1), 0);
			if (roundoff2 < 3)
				{roundoff2 = -roundoff2;}
			else
				{roundoff2 = 5 - roundoff2;}
			tcal2.set(Calendar.MINUTE, tcal2.get(Calendar.MINUTE) + (roundoff2));
			tcal2.set(Calendar.SECOND, 0);
			calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
					/ (1000 * 60 * 5), tcal1.getTimeInMillis()
					/ (1000 * 60 * 5));
			while (calcNumLabels == 0) {
				tcal2.setTimeInMillis(tcal2.getTimeInMillis() + 1000 * 60 * 5);
				calcNumLabels = calculateNumLabels(tcal2.getTimeInMillis()
						/ (1000 * 60 * 5), tcal1.getTimeInMillis()
						/ (1000 * 60 * 5));
			}
			this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
			this.setViewPort(tcal2.getTimeInMillis(), tcal1.getTimeInMillis()
					- tcal2.getTimeInMillis());
		}


		this.getGraphViewStyle().setNumHorizontalLabels(calcNumLabels);
		this.getGraphViewStyle().setNumVerticalLabels(6);
	}

	protected double getMaxX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart + viewportSize;
		} else {
			double highest = 0;
			if (graphSeries.size() > 0) {
				GraphViewDataInterface[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					highest = 0;
				} else {
					highest = values[values.length - 1].getX();
				}
				for (int i = 1; i < graphSeries.size(); i++) {
					values = graphSeries.get(i).values;
					if (values.length > 0) {
						highest = Math.max(highest,
								values[values.length - 1].getX());
					}
				}
			}
			return initialEnd;
		}
	}

	/**
	 * returns the maximal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMaxY() {

		double largest;
		if (manualYAxis) {
			largest = manualMaxYValue;
		} else {
			largest = Integer.MIN_VALUE;
			for (int i = 0; i < graphSeries.size(); i++) {
				GraphViewDataInterface[] values = _values(i);
				for (int ii = 0; ii < values.length; ii++)
				{
					if (values[ii].getY() > largest)
						{largest = values[ii].getY();}
				}
			}
		}

		int nofdig = 0;
		int tval = (int) largest;
		if (largest < 5)
			{largest = 5;}
		else if (largest < 10)
			{return largest;}
		else {
			int powToDiv;
			int tlarge;
			tlarge = (int) largest;
			nofdig = 1;
			while (tval > 10) {
				nofdig++;
				tval = (int) tval / 10;
			}
			if (tval < 4)
				{powToDiv = nofdig - 2;}
			else
				{powToDiv = nofdig - 1;}
			tlarge = (int) ((int) ((tlarge / Math.pow(10, powToDiv)) + 1) * Math
					.pow(10, powToDiv));
			largest = tlarge;
		}

		return largest;
	}

	/**
	 * returns the minimal X value of the current viewport (if viewport is set)
	 * otherwise minimal X value of all data.
	 * 
	 * @param ignoreViewport
	 * 
	 *            warning: only override this, if you really know want you're
	 *            doing!
	 */
	protected double getMinX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			{return viewportStart;}
		} else {
			// otherwise use the min x value
			// values must be sorted by x, so the first value has the smallest X
			// value
			double lowest = 0;
			if (graphSeries.size() > 0) {
				GraphViewDataInterface[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					lowest = 0;
				} else {
					lowest = values[0].getX();
				}
				for (int i = 1; i < graphSeries.size(); i++) {
					values = graphSeries.get(i).values;
					if (values.length > 0) {
						lowest = Math.min(lowest, values[0].getX());
					}
				}
			}
			return initialStart;
		}
	}

	/**
	 * returns the minimal Y value of all data.
	 * 
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMinY() {
		double smallest;
		if (manualYAxis) {
			smallest = manualMinYValue;
		} else {
			smallest = Integer.MAX_VALUE;
			for (int i = 0; i < graphSeries.size(); i++) {
				GraphViewDataInterface[] values = _values(i);
				for (int ii = 0; ii < values.length; ii++)
				{
					if (values[ii].getY() < smallest)
					{
						smallest = values[ii].getY();
					}
				}
			}
		}
		return 0;
	}

	public boolean isDisableTouch() {
		return disableTouch;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public boolean isShowLegend() {
		return showLegend;
	}

	/**
	 * forces graphview to invalide all views and caches. Normally there is no
	 * need to call this manually.
	 */
	public void redrawAll() {
		if (!staticVerticalLabels)
			{verlabels = null;}
		if (!staticHorizontalLabels)
			{horlabels = null;}
		numberformatter[0] = null;
		numberformatter[1] = null;
		labelTextHeight = null;
		horLabelTextWidth = null;
		verLabelTextWidth = null;

		invalidate();
		viewVerLabels.invalidate();
		graphViewContentView.invalidate();
	}

	/**
	 * removes all series
	 */
	public void removeAllSeries() {
		for (GraphViewSeries s : graphSeries) {
			s.removeGraphView(this);
		}
		while (!graphSeries.isEmpty()) {
			graphSeries.remove(0);
		}
		redrawAll();
	}

	/**
	 * removes a series
	 * 
	 * @param series
	 *            series to remove
	 */
	public void removeSeries(GraphViewSeries series) {
		series.removeGraphView(this);
		graphSeries.remove(series);
		redrawAll();
	}

	/**
	 * removes series
	 * 
	 * @param index
	 */
	public void removeSeries(int index) {
		if (index < 0 || index >= graphSeries.size()) {
			throw new IndexOutOfBoundsException("No series at index " + index);
		}

		removeSeries(graphSeries.get(index));
	}

	/**
	 * scrolls to the last x-value
	 * 
	 * @throws IllegalStateException
	 *             if scrollable == false
	 */
	public void scrollToEnd() {
		if (!scrollable)
			{throw new IllegalStateException("This GraphView is not scrollable.");}
		double max = getMaxX(true);
		viewportStart = max - viewportSize;

		// don't clear labels width/height cache
		// so that the display is not flickering
		if (!staticVerticalLabels)
		{
			verlabels = null;
		}
		if (!staticHorizontalLabels)
		{
			horlabels = null;
		}
		invalidate();
		viewVerLabels.invalidate();
		graphViewContentView.invalidate();
	}

	/**
	 * set a custom label formatter
	 * 
	 * @param customLabelFormatter2
	 */

	/**
	 * The user can disable any touch gestures, this is useful if you are using
	 * a real time graph, but don't want the user to interact
	 * 
	 * @param disableTouch
	 */
	public void setDisableTouch(boolean disableTouch) {
		this.disableTouch = disableTouch;
	}

	/**
	 * set custom graphview style
	 * 
	 * @param style
	 */
	public void setGraphViewStyle(GraphViewStyle style) {
		graphViewStyle = style;
		labelTextHeight = null;
	}

	/**
	 * set's static horizontal labels (from left to right)
	 * 
	 * @param horlabels
	 *            if null, labels were generated automatically
	 */
	public void setHorizontalLabels(String[] horlabels) {
		staticHorizontalLabels = horlabels != null;
		this.horlabels = horlabels;
	}

	/**
	 * legend position
	 * 
	 * @param legendAlign
	 */
	public void setLegendAlign(LegendAlign legendAlign) {
		this.legendAlign = legendAlign;
	}

	/**
	 * legend width
	 * 
	 * @param legendWidth
	 * @deprecated use {@link GraphViewStyle#setLegendWidth(int)}
	 */
	@Deprecated
	public void setLegendWidth(float legendWidth) {
		getGraphViewStyle().setLegendWidth((int) legendWidth);
	}

	/**
	 * you have to set the bounds {@link #setManualYAxisBounds(double, double)}.
	 * That automatically enables manualYAxis-flag. if you want to disable the
	 * menual y axis, call this method with false.
	 * 
	 * @param manualYAxis
	 */
	public void setManualYAxis(boolean manualYAxis) {
		this.manualYAxis = manualYAxis;
	}

	/**
	 * set manual Y axis limit
	 * 
	 * @param max
	 * @param min
	 */
	public void setManualYAxisBounds(double max, double min) {
		manualMaxYValue = max;
		manualMinYValue = min;
		manualYAxis = true;
	}

	/**
	 * this forces scrollable = true
	 * 
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable) {
		this.scalable = scalable;
		if (scalable == true && scaleDetector == null) {
			scrollable = true; // automatically forces this
			scaleDetector = new ScaleGestureDetector(getContext(),
					new ScaleGestureDetector.SimpleOnScaleGestureListener() {
						@Override
						public boolean onScale(ScaleGestureDetector detector) {
							double center = viewportStart + viewportSize / 2;
							viewportSize /= detector.getScaleFactor();
							viewportStart = center - viewportSize / 2;

							// viewportStart must not be < minX
							double minX = getMinX(true);
							if (viewportStart < minX) {
								viewportStart = minX;
							}

							// viewportStart + viewportSize must not be > maxX
							double maxX = getMaxX(true);
							if (viewportSize == 0) {
								viewportSize = maxX;
							}
							double overlap = viewportStart + viewportSize
									- maxX;
							if (overlap > 0) {
								// scroll left
								if (viewportStart - overlap > minX) {
									viewportStart -= overlap;
								} else {
									// maximal scale
									viewportStart = minX;
									viewportSize = maxX - viewportStart;
								}
							}
							redrawAll();
							return true;
						}
					});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you
	 * use a viewport {@link #setViewPort(double, double)} which doesn't
	 * displays all data.
	 * 
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}

	/**
	 * sets the title of graphview
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * set's static vertical labels (from top to bottom)
	 * 
	 * @param verlabels
	 *            if null, labels were generated automatically
	 */
	public void setVerticalLabels(String[] verlabels) {
		staticVerticalLabels = verlabels != null;
		this.verlabels = verlabels;
	}

	/**
	 * set's the viewport for the graph.
	 * 
	 * @see #setManualYAxisBounds(double, double) to limit the y-viewport
	 * @param start
	 *            x-value
	 * @param size
	 */
	public void setViewPort(double start, double size) {
		if (size < 0) {
			throw new IllegalArgumentException(
					"Viewport size must be greater than 0!");	//NO I18N  
		}
		viewportStart = start;
		viewportSize = size;
	}

	public float getScreenDensity() {
		return screenDensity;
	}

	public void setScreenDensity(float screenDensity) {
		this.screenDensity = screenDensity;
	}
	
	protected int dpToPx(float dp)
	{
	    return Math.round((float)dp * this.screenDensity);
	}

}
