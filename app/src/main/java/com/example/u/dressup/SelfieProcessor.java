package com.example.u.dressup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by u on 03.05.2015.
 */
public class SelfieProcessor
{
    public SelfieProcessor(Mat aSelfieImage_in, Mat aDressImage_in, Context aContext_in)
    {
        mySelfieMatrix = aSelfieImage_in;
        myDressMatrix = aDressImage_in;
        myContext = aContext_in;
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
        Point aZoomFactor = new Point(1, 1);
        if (mySelfieBoundingRect != null)
        {
            aZoomFactor.x = (double)mySelfieBoundingRect.width / myDressBoundingRect.width;
            aZoomFactor.y = (double)mySelfieBoundingRect.height / myDressBoundingRect.height;
        }
        return aZoomFactor;
    }

    public Point getPanFactor()
    {
        Point aPanFactor = new Point(0, 0);
        if (mySelfieBoundingRect != null)
        {
            Point aZoom = getZoomFactor();
            Point aSelfieCenter = new Point(mySelfieBoundingRect.x + mySelfieBoundingRect.width/2, mySelfieBoundingRect.y + mySelfieBoundingRect.height / 2);
            Point aDressCenter = new Point(myDressBoundingRect.x + myDressBoundingRect.width/2, myDressBoundingRect.y + myDressBoundingRect.height / 2);

            aPanFactor.x = (aSelfieCenter.x - aDressCenter.x) / aZoom.x;
            aPanFactor.y = (aSelfieCenter.y - aDressCenter.y) / aZoom.y;
        }
        return aPanFactor;
    }

    public Rect getBodyRect()
    {
        return mySelfieBoundingRect;
    }
    public Rect getDressRect()
    {
        return myDressBoundingRect;
    }

    private void calculateSelfieBoundingRect()
    {
        Rect anUpperBodyRect = getBodyPartRect(R.raw.haarcascade_mcs_upperbody);
        if(anUpperBodyRect != null)
        {
            Log.i("getBoundingBox", "anUpperBodyRect is identified = [" + anUpperBodyRect.toString() + "]");
            mySelfieBoundingRect = new Rect();

            int aHeadHeight = (int)(anUpperBodyRect.height * 0.6);

            mySelfieBoundingRect.x = anUpperBodyRect.x;
            mySelfieBoundingRect.y = anUpperBodyRect.y + aHeadHeight;
            mySelfieBoundingRect.width = anUpperBodyRect.width;
            //mySelfieBoundingRect.height = Math.min((int)(aHeadHeight * 6), mySelfieMatrix.rows() - mySelfieBoundingRect.y);
            mySelfieBoundingRect.height = (int)(aHeadHeight * 5);
        }
        else
        {
            Rect aFaceRect = getBodyPartRect(R.raw.haarcascade_frontalface_alt);
            if(aFaceRect != null)
            {
                Log.i("getBoundingBox", "aFaceRect is identified = [" + aFaceRect.toString() + "]");

                mySelfieBoundingRect = new Rect();

                int aHeadHeight = aFaceRect.height;

                mySelfieBoundingRect.x = aFaceRect.x + aFaceRect.width / 2 - aFaceRect.width * 2 / 3;
                mySelfieBoundingRect.y = aFaceRect.y + (int)(aFaceRect.height * 1.2);
                mySelfieBoundingRect.width = aFaceRect.width * 3;
                //mySelfieBoundingRect.height = Math.min((int)(aHeadHeight * 6), mySelfieMatrix.rows() - mySelfieBoundingRect.y);
                mySelfieBoundingRect.height = (int)(aHeadHeight * 5);
            }
        }

        if(mySelfieBoundingRect != null)
        {
            Log.i("getBoundingBox", "mySelfieBoundingRect = [" + mySelfieBoundingRect.toString() + "]");
        }
        else
        {
            Log.i("getBoundingBox", "The body height could not be estimated from the selfie");
        }
    }

    private void calculateDressBoundingRect1()
    {
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        Mat descriptors = new Mat();
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        detector.detect(myDressMatrix, keypoints);

        descriptor.compute(myDressMatrix, keypoints, descriptors);

        //keypoints = extractStrongResponses(keypoints);

        myDressBoundingRect = getBoundingBox(keypoints);
        Log.i("getBoundingBox", "myDressBoundingRect = [" + myDressBoundingRect.toString() + "]");
    }

    private void calculateDressBoundingRect()
    {
        int aLeft = myDressMatrix.cols();
        int aTop = myDressMatrix.rows();
        int aRight = 0;
        int aBottom = 0;

        byte[] aDressBuff = new byte[(int) myDressMatrix.total()];
        myDressMatrix.get(0, 0, aDressBuff);

        for (int ii = 0; ii < myDressMatrix.rows(); ii++)
        {
            for (int jj = 0; jj < myDressMatrix.cols(); jj++)
            {
                int anIndex = (int) (ii * myDressMatrix.cols() + jj);
                int aPixelVal = aDressBuff[anIndex] & 0xFF;

                if(aPixelVal == 0)
                {
                    aLeft = Math.min(aLeft, jj);
                    aRight = Math.max(aRight, jj);
                    aTop = Math.min(aTop, ii);
                    aBottom = Math.max(aBottom, ii);
                }
            }
        }

        aDressBuff = null;

        myDressBoundingRect = new Rect();
        myDressBoundingRect.x = aLeft;
        myDressBoundingRect.y = aTop;
        myDressBoundingRect.width = aRight - aLeft;
        myDressBoundingRect.height = aBottom - aTop;

        Log.i("getBoundingBox", "myDressBoundingRect = [" + myDressBoundingRect.toString() + "]");
    }

    public void register()
    {
        calculateSelfieBoundingRect();

        calculateDressBoundingRect();

        System.gc();
    }

    private double crossCorrelate(Mat aParentMat, byte[] aParentBuf, Mat aTemplateMat, byte[] aTemplateBuf, int aStartX, int aStartY)
    {
        double aCorrelationValue = 0;
        for (int ii = 0; ii < aTemplateMat.rows(); ii++)
        {
            for (int jj = 0; jj < aTemplateMat.cols(); jj++)
            {
                int aParentIndex = (int) ((aStartX + ii) * aParentMat.cols() + aStartY + jj);
                double aParentPixel = aParentBuf[aParentIndex] & 0xFF;

                int aTemplateIndex = (int) (ii * aTemplateMat.cols() + jj);
                double aTemplatePixel = aTemplateBuf[aTemplateIndex] & 0xFF;

                aCorrelationValue += (aParentPixel * aTemplatePixel);
            }
        }
        return aCorrelationValue;
    }
    public Mat readHumanMat()
    {
        String imageInSD = Environment.getExternalStorageDirectory().getAbsolutePath() +"/Pictures/Head.png";
        Bitmap bitmap = BitmapFactory.decodeFile(imageInSD);
        //bitmap = Bitmap.createScaledBitmap(bitmap, 200, 300, true);

        Mat aSelfieThresh = Mat.zeros(mySelfieMatrix.size(), mySelfieMatrix.type());
        //Imgproc.adaptiveThreshold(mySelfieMatrix, aSelfieThresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 3, 4);
        //Imgproc.Canny(aSelfieThresh, aSelfieThresh, 254, 255);

        Rect aFaceRect = null;

        try
        {
            InputStream is = myContext.getResources().openRawResource(R.raw.haarcascade_mcs_upperbody);
            File cascadeDir = myContext.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_mcs_face.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1)
            {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            CascadeClassifier cc = new CascadeClassifier(mCascadeFile.getAbsolutePath());

            //InputStream is = SelfieProcessor.class.getResourceAsStream("haarcascade_frontalface_alt.xml");
            //CascadeClassifier cc = new CascadeClassifier(is);
            MatOfRect rec = new MatOfRect();
            //cc.detectMultiScale(mySelfieMatrix,rec,1.1,2,2,new Size(400,400),new Size(400,400));
            cc.detectMultiScale(mySelfieMatrix, rec);
            Rect[] outrec = rec.toArray();
            Log.i("readHumanMat", "Number of identified rects :" + outrec.length);

            aFaceRect = outrec[0];

        } catch (IOException e)
        {
            e.printStackTrace();
            Log.e("readHumanMat", "Failed to load cascade. Exception thrown: " + e);
        }


        /*

        Mat aHumanMat = new Mat();
        Utils.bitmapToMat(bitmap, aHumanMat);
        Log.i("readHumanMat", "Face bitmap size :[" + aHumanMat.rows() + ", " + aHumanMat.cols() + "]");
        Imgproc.cvtColor(aHumanMat, aHumanMat, Imgproc.COLOR_BGR2GRAY);

        byte[] aSelfieBuf = new byte[(int) aSelfieThresh.total()];
        aSelfieThresh.get(0, 0, aSelfieBuf);

        byte[] aFaceBuf = new byte[(int) aHumanMat.total()];
        aHumanMat.get(0, 0, aFaceBuf);

        double aMaxCorrelationValue = 0;
        Point aMaxPoint = new Point();
        for (int i = 0; i < aSelfieThresh.rows() / 2 - aHumanMat.rows(); i++)
        {
            int aStart = aSelfieThresh.cols()/4;
            int anEnd = aStart + aStart * 3 - aHumanMat.cols();
            for (int j = aStart; j < anEnd; j++)
            {
                double aCorrelationValue = crossCorrelate(aSelfieThresh, aSelfieBuf, aHumanMat, aFaceBuf, i ,j);

                //Log.i("readHumanMat", "aCorrelationValue value at :[" + i + ", " + j + "] is = " + aCorrelationValue);
                if(aCorrelationValue > aMaxCorrelationValue )
                {
                    aMaxCorrelationValue = aCorrelationValue;
                    aMaxPoint.x = i;
                    aMaxPoint.y = j;
                }
            }
        }

        Log.i("readHumanMat", "aMaxCorrelationValue value is at :[" + aMaxPoint.x + ", " + aMaxPoint.y + "] and value is = " + aMaxCorrelationValue);
        int aStartX = (int)aMaxPoint.x;
        int aStartY = (int)aMaxPoint.y;

        aSelfieThresh.get(0, 0, aSelfieBuf);

        for (int ii = 0; ii < aHumanMat.rows(); ii++)
        {
            for (int jj = 0; jj < aHumanMat.cols(); jj++)
            {
                int aSelfieIndex = (int) ((aStartX + ii) * aSelfieThresh.cols() + aStartY + jj);
                double aSelfiePixel = aSelfieBuf[aSelfieIndex] & 0xFF;

                int aFaceIndex = (int) (ii * aHumanMat.cols() + jj);
                double aHumanPixel = aFaceBuf[aFaceIndex] & 0xFF;

                double aBlendedVal = (aSelfiePixel  + aHumanPixel );

                aSelfieBuf[aSelfieIndex] = (byte)aBlendedVal;
            }
        }*/

        byte[] aSelfieBuf = new byte[(int) mySelfieMatrix.total()];
        Core.rectangle(mySelfieMatrix, new Point(aFaceRect.x, aFaceRect.y), new Point(aFaceRect.x + aFaceRect.width , aFaceRect.y + aFaceRect.height), new Scalar(255));
        mySelfieMatrix.get(0, 0, aSelfieBuf);
        aSelfieThresh.put(0, 0, aSelfieBuf);
        return aSelfieThresh;
    }

    private String getCascadeFile(int aResourceId_in)
    {
        String aFilePath = null;
        try
        {
            InputStream is = myContext.getResources().openRawResource(aResourceId_in);
            File cascadeDir = myContext.getDir("cascade", Context.MODE_PRIVATE);
            String aCascadeFileName = "haarcascade_mcs_" + aResourceId_in + ".xml";
            File mCascadeFile = new File(cascadeDir, aCascadeFileName);
            if(!mCascadeFile.exists())
            {
                Log.i("getCascadeFile", aCascadeFileName + " is not existing. Copying from resource");

                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1)
                {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();
            }
            else
            {
                Log.i("getCascadeFile", aCascadeFileName + " is already existing");
            }
            is.close();
            aFilePath = mCascadeFile.getAbsolutePath();

            Log.i("getCascadeFile", "aFilePath : " + aFilePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e("getBodyPartRect", "Failed to load cascade. Exception thrown: " + e);
        }

        return aFilePath;
    }
    private Rect getBodyPartRect(int aResourceId_in)
    {
        CascadeClassifier cc = new CascadeClassifier(getCascadeFile(aResourceId_in));

        MatOfRect rec = new MatOfRect();

        cc.detectMultiScale(mySelfieMatrix, rec);

        Rect[] outrec = rec.toArray();
        Log.i("getBodyPartRect", "Number of identified parts :" + outrec.length);

        Rect aFaceRect = null;

        double aMaxArea = 0;
        int aMaxAreaIndex = -1;

        for(int i = 0; i < outrec.length; i++)
        {
            if(outrec[i].area() > aMaxArea)
            {
                aMaxArea = outrec[i].area();
                aMaxAreaIndex = i;
            }
        }
        if(aMaxAreaIndex >= 0)
        {
            double aSelfieArea = mySelfieMatrix.width() * mySelfieMatrix.height();
            if(aMaxArea / aSelfieArea > 0.01 )
                aFaceRect = outrec[aMaxAreaIndex];
            else
                Log.i("getBodyPartRect", "Body part found. But the area is very small and suspected to be wrongly identified. Area is :" + outrec[aMaxAreaIndex].area());
        }

        return aFaceRect;
    }

    private Mat mySelfieMatrix;
    private Mat myDressMatrix;
    private Context myContext;
}