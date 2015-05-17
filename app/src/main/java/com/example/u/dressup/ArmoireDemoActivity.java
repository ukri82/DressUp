package com.example.u.dressup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.example.u.dressup.DressView.Dress;

import java.util.ArrayList;
import java.util.List;

interface DressListLoadedCB
{
	public void dressListLoaded();
}

public class ArmoireDemoActivity extends Activity implements DressListLoadedCB, ArmoireView.DressSelectedCB
{
	@Override
	public void dressSelected(String aDressURL_in)
	{
		Log.i("dressSelected", "Selected Dress URL is = " + aDressURL_in);
		Intent resultIntent = new Intent();
		resultIntent.putExtra("DRESS_URL", aDressURL_in);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	private ArmoireView myArmoireView;
	ServerInterface.DressListGetter myDressListGetter = new ServerInterface.DressListGetter(this);

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		myDressListGetter.execute();

	}

	private void initializeCloud()
	{
		final Display display = getWindowManager().getDefaultDisplay();
		final int width = display.getWidth();
		final int height = display.getHeight();

		final List<DressView> aDressList = createAllDresses();

		myArmoireView = new ArmoireView(this, width, height, aDressList);
		myArmoireView.registerDressSelectedCB(this);

		setContentView(myArmoireView);
		myArmoireView.requestFocus();
		myArmoireView.setFocusableInTouchMode(true);
	}


	@Override
	public void dressListLoaded()
	{
		new Handler(Looper.getMainLooper()).post(new Runnable()
		{

			@Override
			public void run()
			{
				initializeCloud();
			}
		});

	}
	private List<DressView> createAllDresses()
	{
		final List<DressView> aDressList = new ArrayList<DressView>();


		ArrayList<String> aDressURLList = myDressListGetter.getAllDressURLs();

		for (int index = 0; index < aDressURLList.size(); index++)
		{
			aDressList.add(createDress(aDressURLList.get(index), aDressURLList.size() - index));
		}

		return aDressList;
	}


	private DressView createDress(final String url, final int popularity)
	{
		final Dress aDress = new Dress(url, popularity);
		return new DressView(this, aDress);
	}


}
