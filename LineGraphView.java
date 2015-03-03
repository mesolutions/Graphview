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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

/**
 * Line Graph View. This draws a line chart.
 */
public class LineGraphView extends GraphView {
	private final Paint paintBackground;
	private boolean drawBackground;
	private boolean drawDataPoints;
	private float dataPointsRadius = 7f;

	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		paintBackground = new Paint();
		paintBackground.setColor(Color.rgb(20, 40, 60));
		paintBackground.setStrokeWidth(4);
		paintBackground.setAlpha(128);
		
	}

	public LineGraphView(Context context, String title) {
		super(context, title);
		paintBackground = new Paint();
		paintBackground.setColor(Color.rgb(20, 40, 60));
		paintBackground.setStrokeWidth(4);
		paintBackground.setAlpha(128);
	}


	@Override
	public void drawSeries(Canvas canvas, GraphViewDataInterface[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style) {
		double lastEndY = 0;
		double lastEndX = 0;
		boolean startPath=true;
		int[] colors={0xFF034672,0xfff4f4f4};
		int[] colorsTransparent={0x00000000,0x00000000};
		Paint p = null;
		p = new Paint();
		// draw data
		paint.setStrokeWidth(5);
		
		if(style.color!=Color.RED)
		{
		paint.setStyle(Style.STROKE);
		p.setShader(new LinearGradient(0,0,0,graphheight,colors,null,Shader.TileMode.CLAMP));
		}
		else
		{
			p.setShader(new LinearGradient(0, 0, 0, graphheight, colorsTransparent, null, TileMode.CLAMP));
		}
		paint.setAntiAlias(true);
		
		Path bgPath = new Path();
		if (drawBackground) {
			bgPath = new Path();
		}

		lastEndY = 0;
		lastEndX = 0;
		float firstX = 0;
		paint.setColor(style.color);
		paint.setStrokeWidth(dpToPx(1.5f));
		int t=0;
		for (int i = 0; i < values.length; i++) {
			if(values[i].getY()<0)
			{
				if((lastEndX!=0&&firstX!=0))
				{
				bgPath.lineTo((float) lastEndX, graphheight + border);
				bgPath.lineTo(firstX, graphheight + border);
				bgPath.close();
				}
				canvas.drawPath(bgPath, p);
				startPath=false;
				t=0;
				//i++;
				continue;
			}
			double valY = values[i].getY() - minY;
			double ratY = valY / diffY;
			double y = graphheight * ratY;
			p.setPathEffect(new PathEffect());
			double valX = values[i].getX() - minX;
			double ratX = valX / diffX;
			double x = graphwidth * ratX;
			if (t > 0) {
				float startX = (float) lastEndX + (horstart + 1);	  	
				float startY = (float) (border - lastEndY) + graphheight;
				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight;
				Paint tp=new Paint();
				tp.setColor(Color.RED);
				// draw data point
				if (drawDataPoints) {
					canvas.drawCircle(startX, startY, dataPointsRadius,tp );
				}
				canvas.drawLine(startX, startY, endX, endY, paint);
				if (bgPath != null) {
					if (t==1) {
						firstX = startX;
						bgPath=new Path();
						//paint.setColor(style.color);
						startPath=true;
						bgPath.moveTo(startX, startY);
					}
					//bgPath.setFillType(ft);
					bgPath.lineTo(endX, endY);
				}
			}
			lastEndY = y;
			lastEndX = x;
			t++;
		}
		
		if (bgPath != null&&startPath==true&&bgPath.isEmpty()==false) {
			// end / close path
			if(lastEndX!=0&&firstX!=0)
			{
			bgPath.lineTo((float) lastEndX, graphheight + border-dpToPx(1));
			bgPath.lineTo(firstX, graphheight + border-dpToPx(1));
			}
			
			bgPath.close();
			canvas.drawPath(bgPath, p);
		}
	}

	public int getBackgroundColor() {
		return paintBackground.getColor();
	}

	public float getDataPointsRadius() {
		return dataPointsRadius;
	}

	public boolean getDrawBackground() {
		return drawBackground;
	}

	public boolean getDrawDataPoints() {
		return true;
	}

	/**
	 * sets the background color for the series.
	 * This is not the background color of the whole graph.
	 * @see #setDrawBackground(boolean)
	 */
	@Override
	public void setBackgroundColor(int color) {
		paintBackground.setColor(color);
	}

	/**
	 * sets the radius of the circles at the data points.
	 * @see #setDrawDataPoints(boolean)
	 * @param dataPointsRadius
	 */
	public void setDataPointsRadius(float dataPointsRadius) {
		//this.dataPointsRadius = dataPointsRadius;
	}

	/**
	 * @param drawBackground true for a light blue background under the graph line
	 * @see #setBackgroundColor(int)
	 */
	public void setDrawBackground(boolean drawBackground) {
		this.drawBackground = drawBackground;
	}

	/**
	 * You can set the flag to let the GraphView draw circles at the data points
	 * @see #setDataPointsRadius(float)
	 * @param drawDataPoints
	 */
	public void setDrawDataPoints(boolean drawDataPoints) {
		this.drawDataPoints = drawDataPoints;
	}

}
