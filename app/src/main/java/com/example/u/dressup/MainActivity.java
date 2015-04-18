package com.example.u.dressup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import java.io.ByteArrayOutputStream;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class MainActivity extends ActionBarActivity implements LongPressCB {

    private static final String TAG = "Dressup::MainActivity";

    private static ImageView mySelfyView, myDressView;
    private static DressedupView myMergedView;
    private static final int SELECT_PHOTO = 100;
    private static final int TAKE_PICTURE = 101;

    private static int mySelectionImageNo = 0;

    private static Bitmap mySelfieBmp, myDressBmp;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySelfyView = (ImageView) MainActivity.this.findViewById(R.id.imageSelfie);
        myDressView = (ImageView) MainActivity.this.findViewById(R.id.imageDress);
        myMergedView = (DressedupView) MainActivity.this.findViewById(R.id.imageMerged);

        run();
    }

    @Override
    public void press()
    {
        Intent anIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        anIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        startActivityForResult(anIntent, TAKE_PICTURE);
    }
    private void run() {


        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        myMergedView.setCurrentActivityDims(screenSize);

        myMergedView.registerPressCB(this);

        mySelfyView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                mySelectionImageNo = 1;

            }
        });
        myDressView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                mySelectionImageNo = 2;
            }
        });





                /*
                if (mySelfieBmp != null && myDressBmp != null)
                //if (myDressBmp != null)
                {


					Mat img1 = new Mat();
					Utils.bitmapToMat(mySelfieBmp, img1);
                    Mat img2 = new Mat();
                    Utils.bitmapToMat(myDressBmp, img2);

                    Mat aSelfieMat = new Mat (img1.cols(), img1.rows(), CvType.CV_8U);
                    Imgproc.cvtColor(img1, aSelfieMat, Imgproc.COLOR_BGR2GRAY);

                    Mat aDressMat = new Mat (img2.cols(), img2.rows(), CvType.CV_8U);
                    Imgproc.cvtColor(img2, aDressMat, Imgproc.COLOR_BGR2GRAY);


                    ImageProcessor anImageProc = new ImageProcessor(aDressMat);
                    anImageProc.regionGrow();

                    aDressMat.put(0, 0, anImageProc.getVisitedMatrix());
                    Mat anInvertedDressMat= new Mat(aDressMat.rows(),aDressMat.cols(), aDressMat.type(), new Scalar(255,255,255));

                    Core.subtract(anInvertedDressMat, aDressMat, anInvertedDressMat);

                    Mat aPartialDressImage = new Mat(anInvertedDressMat.cols(), anInvertedDressMat.rows(), CvType.CV_8U);
                    img2.copyTo(aPartialDressImage, anInvertedDressMat);

                    Mat aPartialSelfieImage = new Mat(anInvertedDressMat.cols(), anInvertedDressMat.rows(), CvType.CV_8U);
                    img1.copyTo(aPartialSelfieImage, aDressMat);

                    Mat aBlendedImage = new Mat(anInvertedDressMat.cols(), anInvertedDressMat.rows(), img1.type());
                    Core.addWeighted(aPartialSelfieImage, 1, aPartialDressImage, 1, 0, aBlendedImage);

                    Bitmap bm = Bitmap.createBitmap(aBlendedImage.cols(), aBlendedImage.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(aBlendedImage, bm);



                    myMergedView.setImageBitmap(bm);
                    myMergedView.invalidate();

			        /*
			        Scalar lowerThreshold = new Scalar ( 120, 100, 100 ); // Blue color – lower hsv values
			        Scalar upperThreshold = new Scalar ( 179, 255, 255 ); // Blue color – higher hsv values
			        Core.inRange ( aDressMat, lowerThreshold , upperThreshold, aDressMat );

			        Mat aDilatedDressMat = new Mat (img2.cols(), img2.rows(), CvType.CV_8U);
			        Imgproc.dilate ( aDressMat, aDilatedDressMat, new Mat() );*/

                    /*Mat imageHSV = new Mat(img2.size(), CvType.CV_8U);
                    Mat imageBlurr = new Mat(img2.size(), CvType.CV_8U);
                    Mat imageA = new Mat(img2.size(), CvType.CV_8U);
                    Imgproc.cvtColor(img2, imageHSV, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(7,7), 0);
                    Imgproc.adaptiveThreshold(imageBlurr, imageA, 255,Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,7, 5);


                    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

                    Mat aContourMat = new Mat (img2.cols(), img2.rows(), CvType.CV_8U);

                    //Imgproc.findContours ( imageA, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE );
                    Imgproc.findContours ( imageA, contours, new Mat(), Imgproc.RETR_LIST, 4 );

                    Log.i(TAG, "contours.size() = " + contours.size());*/

			        /*Scalar color = new Scalar( 0, 0, 255 );
			        for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ )
			        {
			           if(Imgproc.contourArea(contours.get(contourIdx)) > 1)  // Minimum size allowed for consideration
			           {
			               Imgproc.drawContours ( aContourMat, contours, contourIdx, color);
			           }
			        }*/

                   /* MatOfPoint anAllPointSet = new MatOfPoint();

                    for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ )
                    {
                        anAllPointSet.push_back(contours.get(contourIdx));
                    }

                    Log.i(TAG, "anAllPointSet.size() = " + anAllPointSet.size());*/
			        /*for ( int i = 0; i < anAllPointSet.size().height; i++ )
			        {
			        	Log.i(TAG, "anAllPointSet = (" + anAllPointSet.get(i, 0)[0] + ", " + anAllPointSet.get(i, 0)[1] + ")");
			        }

			        MatOfInt aContourHull = new MatOfInt();
			        Imgproc.convexHull (anAllPointSet, aContourHull);

			        Log.i(TAG, "aContourHull.size() = " + aContourHull.size());

			        List<MatOfPoint> aConvexContours = new ArrayList<MatOfPoint>();
			        MatOfPoint aConvexContour = new MatOfPoint();
			        for ( int i = 0; i < aContourHull.size().height; i++ )
			        {
			        	int index = (int)aContourHull.get(i, 0)[0];
			        	double[] point = new double[] {anAllPointSet.get(index, 0)[0], anAllPointSet.get(index, 0)[1]};
			        	aConvexContour.put(i, 0, point);
			        	Log.i(TAG, "point = (" + point[0] + ", " + point[1] + ")");
			        }*/

                    /*TreeMap<Integer, Integer> aLeftPoints = new TreeMap<Integer, Integer>();
                    TreeMap<Integer, Integer> aRightPoints = new TreeMap<Integer, Integer>();
                    for ( int i = 0; i < anAllPointSet.size().height; i++ )
                    {
                        int anX = (int)anAllPointSet.get(i, 0)[0];
                        int aY = (int)anAllPointSet.get(i, 0)[1];
                        if(aLeftPoints.containsKey(aY))
                        {
                            int aCurrLeft = (int)aLeftPoints.get(aY);
                            if(anX < aCurrLeft)
                            {
                                aLeftPoints.put(aY, anX);
                            }
                        }
                        else
                        {
                            aLeftPoints.put(aY, anX);
                        }

                        if(aRightPoints.containsKey(aY))
                        {
                            int aCurrRight = (int)aRightPoints.get(aY);
                            if(anX > aCurrRight)
                            {
                                aRightPoints.put(aY, anX);
                            }
                        }
                        else
                        {
                            aRightPoints.put(aY, anX);
                        }
                    }

                    List<MatOfPoint> aConvexContours = new ArrayList<MatOfPoint>();
                    MatOfPoint aConvexContour = new MatOfPoint();

                    int i = 0;
                    for (Map.Entry<Integer, Integer> entry : aLeftPoints.entrySet())
                    {
                        Log.i(TAG, "point = (" + entry.getKey() + " = " + entry.getValue() + ")");
                        double[] point = new double[] {entry.getKey(), entry.getValue()};
                        aConvexContour.put(i++, 0, point);
                    }
                    aConvexContours.add(aConvexContour);

                    aConvexContour = new MatOfPoint();
                    i = 0;
                    for (Map.Entry<Integer, Integer> entry : aRightPoints.entrySet())
                    {
                        Log.i(TAG, "point = (" + entry.getKey() + " = " + entry.getValue() + ")");
                        double[] point = new double[] {entry.getKey(), entry.getValue()};
                        aConvexContour.put(i++, 0, point);
                    }
                    aConvexContours.add(aConvexContour);*/


			        /*aConvexContours.add(aConvexContour);*/
                    /*Scalar color = new Scalar( 255, 255, 255 );
                    Imgproc.drawContours ( aContourMat, aConvexContours, 0, color);
                    Imgproc.drawContours ( aContourMat, aConvexContours, 1, color);

                    Bitmap bm = Bitmap.createBitmap(aContourMat.cols(), aContourMat.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(aContourMat, bm);

                    myMergedView.setImageBitmap(bm);
                    myMergedView.invalidate();*/
            /*    }
            }
        });*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent call = new Intent(MainActivity.this, null);
            call.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            call.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(call);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {

        super.onNewIntent(newIntent);

        run();
    }


    void setSelfie(Bitmap aBMP_in)
    {
        mySelfyView.setImageBitmap(aBMP_in);
        //mySelfieBmp = aBMP_in;
        mySelfyView.invalidate();

        myMergedView.setImageBitmap(aBMP_in);
        myMergedView.invalidate();
        myMergedView.setSelfieBMP(aBMP_in);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode)
        {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK)
                {
                    Uri selectedImage = imageReturnedIntent.getData();

                    InputStream imageStream = null;
                    try
                    {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    }
                    catch (FileNotFoundException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                    yourSelectedImage = Bitmap.createScaledBitmap(yourSelectedImage, 200, 300, true);

                    if (mySelectionImageNo == 1)
                    {
                        setSelfie(yourSelectedImage);

                    }
                    else if (mySelectionImageNo == 2)
                    {
                        myDressView.setImageBitmap(yourSelectedImage);
                        myDressBmp = yourSelectedImage;
                        myDressView.invalidate();
                        myMergedView.setDressBMP(myDressBmp);
                    }
                }
                break;
            case TAKE_PICTURE:
                if (resultCode == RESULT_OK)
                {
                    Bitmap yourSelectedImage = (Bitmap)imageReturnedIntent.getExtras().get("data");
                    yourSelectedImage = Bitmap.createScaledBitmap(yourSelectedImage, 200, 300, true);

                    setSelfie(yourSelectedImage);
                }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
    }
}
