package com.example.u.dressup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class ArmoireView extends RelativeLayout implements GestureDetector.OnGestureListener
{

	public interface DressSelectedCB
	{
		public void dressSelected(String aDressURL_in);
	}

	public ArmoireView(final Context mContext, final int width,
					   final int height, final List<DressView> dressList) {
		this(mContext, width, height, dressList, 7); // default for min/max
															// text size
	}


	public ArmoireView(final Context mContext, final int width,
					   final int height, final List<DressView> dressList, final int scrollSpeed)
	{

		super(mContext);
		this.myConetxt = mContext;
		setBackgroundColor(Color.WHITE);
		Drawable aBgImage = getResources().getDrawable( R.drawable.armoire_background );
		setBackground(aBgImage);

		myScrollSpeed = scrollSpeed;

		myCenterX = width / 2;
		myCenterY = height / 2;
		myRadius = Math.min(myCenterX * 0.75f, myCenterY * 0.75f);
		myShiftLeft = (int) (Math.min(myCenterX * 0.15f, myCenterY * 0.15f));

		myArmoire = new Armoire(dressList, (int) myRadius);

		for (final DressView aDressView : myArmoire.getDressCloud())
		{

			float aSizeFactor = height / ((float)aDressView.getImageHeight() * 8);
			aDressView.setImageSizeFactor(aSizeFactor);
			addView(aDressView);

			aDressView.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View arg0)
				{
					if (myCB != null)
					{
						myCB.dressSelected(((DressView) arg0).getURL());
					}

				}
			});

			aDressView.setOnTouchListener(new OnTouchListener()
			{

				Rect rect = null;

				@Override
				public boolean onTouch(View v, MotionEvent event)
				{

					if (event.getAction() == MotionEvent.ACTION_DOWN)
					{
						aDressView.setColorFilter(Color.argb(100, 0, 0, 0));
						rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
					}
					if (event.getAction() == MotionEvent.ACTION_UP)
					{
						aDressView.setColorFilter(Color.argb(0, 0, 0, 0));
					}
					if (event.getAction() == MotionEvent.ACTION_MOVE)
					{
						if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY()))
						{
							aDressView.setColorFilter(Color.argb(0, 0, 0, 0));
						}
					}
					return false;

				}
			});
		}

		myArmoire.setRadius((int) myRadius);
		myArmoire.create(true, myCenterX, myCenterY, myShiftLeft);

		myAngleX = 0.5f;
		myAngleY = 0.5f;

		myArmoire.setAngleX(myAngleX);
		myArmoire.setAngleY(myAngleY);
		myArmoire.update(myCenterX, myCenterX, myShiftLeft);

		enableFling();
		startAnimation();
	}

	private GestureDetectorCompat myDetector;

	private void enableFling()
	{
		myDetector = new GestureDetectorCompat(this.myConetxt,this);
	}

	DressSelectedCB myCB;
	public void registerDressSelectedCB(DressSelectedCB aCB_in)
	{
		myCB = aCB_in;
	}

	public void reset()
	{
		myArmoire.reset(myCenterX, myCenterY, myShiftLeft);
	}



	private void simulateRotation()
	{
		float aVelocityDiff = myLastVelocityX - myLastVeclocityXSign * MIN_VELOCITY;
		if(Math.abs(aVelocityDiff) > 0)
		{
			myLastVelocityX = Math.max(Math.abs(myLastVelocityX) - myDecelerationX, MIN_VELOCITY);
		}
		else
		{
			myLastVelocityX = MIN_VELOCITY;
		}

		myLastVelocityX = myLastVeclocityXSign * myLastVelocityX;

		myDecelerationX = Math.max(myDecelerationX - DECELERATION_CHANGE_RATE, MIN_DECELERATION);

		aVelocityDiff = myLastVelocityY - myLastVeclocityYSign * MIN_VELOCITY;
		if(Math.abs(aVelocityDiff) > 0)
		{
			myLastVelocityY = Math.max(Math.abs(myLastVelocityY) - myDecelerationY, MIN_VELOCITY);
		}
		else
		{
			myLastVelocityY = MIN_VELOCITY;
		}

		myLastVelocityY = myLastVeclocityYSign * myLastVelocityY;

		myDecelerationY = Math.max(myDecelerationY - DECELERATION_CHANGE_RATE, MIN_DECELERATION);

		myStartX = 0;
		myStartY = 0;

		//Log.i("simulateRotation", "(myDecelerationX, myDecelerationY) = (" + myDecelerationX + ", " + myDecelerationY + ")");
		//Log.i("simulateRotation", "(myLastVelocityX, myLastVelocityY) = (" + myLastVelocityX + ", " + myLastVelocityY + ")");


		rotateView(myLastVelocityX, myLastVelocityY);

	}

	private void TimerMethod()
	{
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				simulateRotation();
			}
		});
	}

	private void startAnimation()
	{
		myTimer = new Timer();
		myTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				TimerMethod();
			}

		}, 0, 100);
	}

	private void stopAnimation()
	{
		myTimer.cancel();
	}


	private void rotateView(float x, float y)
	{
		// rotate elements depending on how far the selection point is from
		// center of cloud
		final float dx = x - myStartX;
		final float dy = y - myStartY;

		myAngleX = (dy / myRadius) * myScrollSpeed * TOUCH_SCALE_FACTOR;
		myAngleY = (-dx / myRadius) * myScrollSpeed * TOUCH_SCALE_FACTOR;


		myArmoire.setAngleX(myAngleX);
		myArmoire.setAngleY(myAngleY);
		myArmoire.update(myCenterX, myCenterY, myShiftLeft);


	}
	@Override
	public boolean onTouchEvent(final MotionEvent e)
	{
		this.myDetector.onTouchEvent(e);

		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		stopAnimation();

		myStartX = e1.getX();
		myStartY = e1.getY();
		myLastVelocityX = velocityX;
		myLastVelocityY = velocityY;
		myLastVeclocityXSign = (int) Math.signum(velocityX);
		myLastVeclocityYSign = (int) Math.signum(velocityY);
		myDecelerationX = Math.max(Math.abs(velocityX / 15), MIN_DECELERATION);
		myDecelerationY = Math.max(Math.abs(velocityY / 15), MIN_DECELERATION);

		final float x = myStartX + velocityX / 2;
		final float y = myStartY + velocityY / 2;

		Log.i("onFling", "(myStartX, myStartY) = (" + myStartX + ", " + myStartY + ")");
		Log.i("onFling", "(x, y) = (" + x + ", " + y + ")");
		Log.i("onFling", "(velocityX, velocityY) = (" + velocityX + ", " + velocityY + ")");


		rotateView(x, y);

		startAnimation();

		return false;
	}

	public void onSingleTapConfirmed(MotionEvent e)
	{

	}

	public boolean	onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		return true;
	}

	public void	onShowPress(MotionEvent e)
	{

	}

	public boolean	onSingleTapUp(MotionEvent e)
	{
		return true;
	}

	public void	onLongPress(MotionEvent e)
	{

	}

	public boolean onDown(MotionEvent e)
	{
		stopAnimation();
		return false;
	}

	private final float TOUCH_SCALE_FACTOR = .8f;
	private final float TRACKBALL_SCALE_FACTOR = 10;
	private final float myScrollSpeed;
	private final Armoire myArmoire;
	private float myAngleX = 0;
	private float myAngleY = 0;
	private final float myCenterX, myCenterY;
	private final float myRadius;
	private final Context myConetxt;
	private final int myShiftLeft;

	private float myStartX = 0;
	private float myStartY = 0;

	private final float MIN_VELOCITY = 100;

	private final float MIN_DECELERATION = 50;

	private float myDecelerationX = MIN_DECELERATION;
	private float myDecelerationY = MIN_DECELERATION;

	private final float DECELERATION_CHANGE_RATE = 5;

	private float myLastVelocityX = MIN_VELOCITY;
	private float myLastVelocityY = MIN_VELOCITY;

	private int myLastVeclocityXSign = 1;
	private int myLastVeclocityYSign = 1;
	private Timer myTimer;

}
