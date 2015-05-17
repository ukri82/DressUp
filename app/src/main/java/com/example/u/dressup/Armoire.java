package com.example.u.dressup;

import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Armoire
{

	private final List<DressView> myDressCloud;
	private int myRadius;
	private float sin_myAngleX, cos_myAngleX, sin_myAngleY, cos_myAngleY, sin_myAngleZ, cos_myAngleZ;
	private final float myAngleZ = 0;
	private float myAngleX = 0;
	private float myAngleY = 0;

	private boolean myDistributeEven = true;


	public Armoire(final List<DressView> dressCloud, final int radius)
	{
		myDressCloud = new ArrayList<DressView>(dressCloud);
		this.myRadius = radius;
	}

	public void create(final boolean distrEven, final float lcenterX, final float lcenterY, final int shiftLeft)
	{
		this.myDistributeEven = distrEven;

		positionAll(distrEven);
		calculateSineCosine(myAngleX, myAngleY, myAngleZ);
		updateAll(lcenterX, lcenterY, shiftLeft);
	}

	public void reset(final float lcenterX, final float lcenterY, final int shiftLeft)
	{
		create(myDistributeEven, lcenterX, lcenterY, shiftLeft);
	}

	// updates the transparency/scale of all elements
	public void update(final float lcenterX, final float lcenterY, final int shiftLeft)
	{
		// if myAngleX and myAngleY under threshold, skip motion calculations for
		// performance
		if ((Math.abs(myAngleX) > .1) || (Math.abs(myAngleY) > .1))
		{
			calculateSineCosine(myAngleX, myAngleY, myAngleZ);
			updateAll(lcenterX, lcenterY, shiftLeft);
		}
	}


	private void positionAll(final boolean distrEven)
	{
		double phi = 0;
		double theta = 0;
		final int max = myDressCloud.size();
		// distribute: (disrtEven is used to specify whether distribute random
		// or even
		for (int i = 1; i < (max + 1); i++)
		{
			if (distrEven)
			{
				phi = Math.acos(-1.0 + (((2.0 * i) - 1.0) / max));
				theta = Math.sqrt(max * Math.PI) * phi;
			} else
			{
				phi = Math.random() * (Math.PI);
				theta = Math.random() * (2 * Math.PI);
			}

			myDressCloud.get(i - 1).setCenterX(
					(int) ((myRadius * Math.cos(theta) * Math.sin(phi))));
			myDressCloud.get(i - 1).setCenterY(
					(int) (myRadius * Math.sin(theta) * Math.sin(phi)));
			myDressCloud.get(i - 1).setCenterZ((int) (myRadius * Math.cos(phi)));
		}
	}

	private void updateAll(final float lcenterX, final float lcenterY, final int shiftLeft)
	{
		for (final DressView aDressView : myDressCloud)
		{
			// There exists two options for this part:
			// multiply positions by a x-rotation matrix
			final float rx1 = (aDressView.getCenterX());
			final float ry1 = ((aDressView.getCenterY()) * cos_myAngleX)
					+ (aDressView.getCenterZ() * -sin_myAngleX);
			final float rz1 = ((aDressView.getCenterY()) * sin_myAngleX)
					+ (aDressView.getCenterZ() * cos_myAngleX);
			// multiply new positions by a y-rotation matrix
			final float rx2 = (rx1 * cos_myAngleY) + (rz1 * sin_myAngleY);
			final float ry2 = ry1;
			final float rz2 = (rx1 * -sin_myAngleY) + (rz1 * cos_myAngleY);
			// multiply new positions by a z-rotation matrix
			final float rx3 = (rx2 * cos_myAngleZ) + (ry2 * -sin_myAngleZ);
			final float ry3 = (rx2 * sin_myAngleZ) + (ry2 * cos_myAngleZ);
			final float rz3 = rz2;
			// set arrays to new positions
			aDressView.setCenterX(rx3);
			aDressView.setCenterY(ry3);
			aDressView.setCenterZ(rz3);

			// add perspective
			final int diameter = 2 * myRadius;
			final float per = diameter / (diameter + rz3);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

			params.setMargins((int) ((lcenterX - shiftLeft) + (rx3 * per)),
					(int) (lcenterY + (ry3 * per)), 0, 0);

			aDressView.setLayoutParams(params);

			int aWidth = (int)(aDressView.getImageWidth() * per);
			int aHeight = (int)(aDressView.getImageHeight() * per);

			aDressView.getLayoutParams().height = aHeight;
			aDressView.getLayoutParams().width = aWidth;

			aDressView.bringToFront();
		}
		depthSort();
	}

	private void depthSort()
	{
		Collections.sort(myDressCloud);
	}

	private void calculateSineCosine(final float mAngleX, final float mAngleY,
									 final float mAngleZ)
	{
		final double degToRad = (Math.PI / 180);
		sin_myAngleX = (float) Math.sin(mAngleX * degToRad);
		cos_myAngleX = (float) Math.cos(mAngleX * degToRad);
		sin_myAngleY = (float) Math.sin(mAngleY * degToRad);
		cos_myAngleY = (float) Math.cos(mAngleY * degToRad);
		sin_myAngleZ = (float) Math.sin(mAngleZ * degToRad);
		cos_myAngleZ = (float) Math.cos(mAngleZ * degToRad);
	}

	public void setRadius(final int myRadius)
	{
		this.myRadius = myRadius;
	}

	public void setAngleX(final float mAngleX)
	{
		this.myAngleX = mAngleX;
	}

	public void setAngleY(final float mAngleY)
	{
		this.myAngleY = mAngleY;
	}

	public List<DressView> getDressCloud() {
		return myDressCloud;
	}
}
