package com.example.u.dressup;

import android.util.Log;


import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by u on 03.05.2015.
 */
public class SelfieProcessor
{
    public SelfieProcessor(Mat aSelfieImage_in, Mat aDressImage_in)
    {
        mySelfieMatrix = aSelfieImage_in;
        myDressMatrix = aDressImage_in;
    }

    private void printKeyPoints(MatOfKeyPoint keypoints1)
    {
        for(int i = 0; i < keypoints1.size().height; i++)
        {
            double[] aPoint = null;
            aPoint = keypoints1.get(i, 0);
            Log.i("register", "aKeyPointArray[i].response = " + aPoint[5] + " aKeyPointArray[i].point = [" +  aPoint[2] + ", " + aPoint[3] + "]");
        }
    }

    private Rect getBoundingBox(MatOfKeyPoint keypoints)
    {
        int left = mySelfieMatrix.cols();
        int top = mySelfieMatrix.rows();
        int right = 0;
        int bottom = 0;

        KeyPoint[] aKeyPointArray = keypoints.toArray();

        for(int i = 0; i < aKeyPointArray.length; i++)
        {
            int x = (int)aKeyPointArray[i].pt.x;
            int y = (int)aKeyPointArray[i].pt.y;

            if(x > right)
            {
                right = x;
            }
            if(x < left)
            {
                left = x;
            }

            if(y > bottom)
            {
                bottom = y;
            }
            if(y < top)
            {
                top = y;
            }


        }

        Rect rect = new Rect(left, top, right - left, bottom - top);
        return rect;
    }

    private int myMaxNumPoints = 230;

    private MatOfKeyPoint extractStrongResponses(MatOfKeyPoint keypoints)
    {
        KeyPoint[] aKeyPointArray = keypoints.toArray();

        int aMinIndex = 0;
        if(aKeyPointArray.length > myMaxNumPoints)
            aMinIndex = aKeyPointArray.length - myMaxNumPoints;

        List<KeyPoint> aKeyPointArrayRes = new ArrayList<KeyPoint>();

        for(int i = aMinIndex; i < aKeyPointArray.length; i++)
            aKeyPointArrayRes.add(aKeyPointArray[i]);

        keypoints.fromList(aKeyPointArrayRes);

        return keypoints;
    }

    Rect mySelfieBoundingRect;
    Rect myDressBoundingRect;

    public Point getZoomFactor()
    {
        return new Point((double)mySelfieBoundingRect.width / myDressBoundingRect.width, (double)mySelfieBoundingRect.height / myDressBoundingRect.height);
    }

    public Point getPanFactor()
    {
        Point aZoom = getZoomFactor();
        Point aSelfieCenter = new Point(mySelfieBoundingRect.x + mySelfieBoundingRect.width/2, mySelfieBoundingRect.y + mySelfieBoundingRect.height / 2);
        Point aDressCenter = new Point(myDressBoundingRect.x + myDressBoundingRect.width/2, myDressBoundingRect.y + myDressBoundingRect.height / 2);

        return new Point((aSelfieCenter.x - aDressCenter.x) / aZoom.x, (aSelfieCenter.y - aDressCenter.y) / aZoom.y);
    }

    public void register()
    {
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB );
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE );

        Mat descriptors1 = new Mat();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();

        detector.detect(mySelfieMatrix, keypoints1);

        descriptor.compute(mySelfieMatrix, keypoints1, descriptors1);
        //printKeyPoints(keypoints1);

        //keypoints1 = extractStrongResponses(keypoints1);

        Mat descriptors2 = new Mat();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        detector.detect(myDressMatrix, keypoints2);

        descriptor.compute(myDressMatrix, keypoints2, descriptors2);

        //keypoints2 = extractStrongResponses(keypoints2);

        mySelfieBoundingRect = getBoundingBox(keypoints1);
        Log.i("getBoundingBox", "mySelfieBoundingRect = [" + mySelfieBoundingRect.toString() + "]");

        myDressBoundingRect = getBoundingBox(keypoints2);
        Log.i("getBoundingBox", "myDressBoundingRect = [" + myDressBoundingRect.toString() + "]");
    }

    private Mat mySelfieMatrix;
    private Mat myDressMatrix;
}