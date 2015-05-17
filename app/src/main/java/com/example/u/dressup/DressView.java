package com.example.u.dressup;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class DressView extends ImageView implements Comparable<DressView>, ServerInterface.ImageLoadedCB
{

	private static final Dress DEFAULT_BUNDLE = new Dress("Shirt", 1);

	private final int myPopularity;

	private float myCenterX, myCenterY, myCenterZ;

	private int myImageWidth = 80, myImageHeight = 120;


	private String myURL;


	@Deprecated
	public DressView(final Context context) {
		this(context, DEFAULT_BUNDLE);
	}

	public DressView(final Context context, final Dress dress)
	{
		super(context);

		new ServerInterface.ImageLoadTask(dress.myURL, this).execute();

		myURL = dress.myURL;
		myPopularity = dress.myPopularity;

		init(0f, 0f, 0f);
	}

	public void imageLoaded(Bitmap result)
	{
		setImageBitmap(Bitmap.createScaledBitmap(result, myImageWidth, myImageHeight, false));
	}

	@Override
	public int compareTo(final DressView another)
	{
		int diff = -1;

		if (another != null) {
			diff = (int)(another.getCenterZ() - getCenterZ());
		}

		return diff;
	}


	private void init(final float centerX, final float centerY, final float centerZ)
	{

		this.myCenterX = centerX;
		this.myCenterY = centerX;
		this.myCenterZ = centerZ;
	}

	public float getCenterX() {
		return myCenterX;
	}


	public void setCenterX(final float myCenterX) {
		this.myCenterX = myCenterX;
	}

	public float getCenterY() {
		return myCenterY;
	}

	public void setCenterY(final float myCenterY) {
		this.myCenterY = myCenterY;
	}

	public String getURL()
	{
		return myURL;
	}

	public float getCenterZ() {
		return myCenterZ;
	}

	public void setCenterZ(final float myCenterZ)
	{
		this.myCenterZ = myCenterZ;
	}

	public void setImageSizeFactor(float aFactor_in)
	{
		this.myImageWidth = (int)(this.myImageWidth * aFactor_in);
		this.myImageHeight = (int)(this.myImageHeight * aFactor_in);
	}

	public int getImageWidth() {
		return this.myImageWidth;
	}

	public int getImageHeight() {
		return this.myImageHeight;
	}

	public static class Dress
	{
		private final String myURL;
		private final int myPopularity;


		public Dress(final String url, final int popularity)
		{
			super();
			this.myURL = url;
			this.myPopularity = popularity;
		}
	}
}
