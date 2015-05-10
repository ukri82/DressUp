package com.example.u.dressup;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

import java.lang.Math;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

/**
 * Created by u on 17.03.2015.
 */


public class ImageProcessor
{
    public ImageProcessor(Mat anImageBuffer_in)
    {
        myImageMatrix = anImageBuffer_in;
        myImageBuf = new byte[(int) myImageMatrix.total()];
        myImageMatrix.get(0, 0, myImageBuf);

        //myVisitedMatrix = new byte[(int) myImageMatrix.total()];

    }
    public void regionGrow()
    {
        //  Perform initial region growing with an estimate of the background pixel values.
        {
            calculateInitialRange();
            RegionGrowerQBased aRegionGrower = new RegionGrowerQBased(myImageMatrix, myMinVal, myMaxVal, true);
            aRegionGrower.grow(0, 0);

            myVisitedMatrix = Arrays.copyOf(aRegionGrower.getVisitedMatrix(), aRegionGrower.getVisitedMatrix().length);
        }

        correctRegion();
    }

    public void correctRegion()
    {
        Mat aVisitedMatrix = Mat.zeros(myImageMatrix.size(), CvType.CV_8U);
        aVisitedMatrix.put(0, 0, myVisitedMatrix);

        Imgproc.dilate(aVisitedMatrix, aVisitedMatrix, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3)));

        aVisitedMatrix.get(0, 0, myVisitedMatrix);
    }
    public void correctRegion1()
    {
        for(int i = 0; i < myImageMatrix.rows(); i++)
        {
            for(int j = 0; j < myImageMatrix.cols(); j++)
            {
                int anIndex = (int)(i * myImageMatrix.cols() + j);
                byte aPixelVal = myVisitedMatrix[anIndex];
                if(aPixelVal == 0)
                {
                    int aNumVisitedPixels = 0;
                    for (int x = i - 1; x <= i + 1; x++)
                    {
                        for (int y = j - 1; y <= j + 1; y++)
                        {
                            if (x >= 0 && y >= 0 && x < myImageMatrix.rows() && y < myImageMatrix.cols())
                            {
                                int anCurrentIndex = x * myImageMatrix.cols() + y;
                                if (myVisitedMatrix[anCurrentIndex] > 0)
                                {
                                    aNumVisitedPixels++;
                                }
                            }
                        }
                    }
                    if(aNumVisitedPixels > 4)
                    {
                        myVisitedMatrix[anIndex] = (byte)255;
                    }
                }
            }
        }
    }
    public void correctRegionContourSmoothing()
    {
        //  Extract the contour boundary of the region and smooth it. Result is a binary image with only the boundary burned to it.
        Point aSeedPoint = null;
        {
            ContourProcessor aContourProc = new ContourProcessor(myImageMatrix.size(), myVisitedMatrix);
            aContourProc.adjustContours();

            //  Copy the bondary binary image out of the object.
            byte[] aBuffer = new byte[(int) aContourProc.getContourImage().total()];
            aContourProc.getContourImage().get(0, 0, aBuffer);
            myContourImage = Mat.zeros(aContourProc.getContourImage().size(), aContourProc.getContourImage().type());
            myContourImage.put(0, 0, aBuffer);

            aSeedPoint = aContourProc.getSeedPoint();   //  The seed point to denote the interior of the boundary
            Log.i("regionGrow", "aSeedPoint :[" + aSeedPoint.x + ", " + aSeedPoint.y + "]");
        }

        //  Now fill the interior of the above boundary with white pixels so that that dress area is marked clearly. This is the final dress matrix.
        {
            RegionGrower aRegionGrower1 = new RegionGrower(myContourImage, (byte) 0, (byte) 0, false);  //  The threshold is 0-0, anything outside this value (=255) is the boundary. Don't go out of it. Also only check for 4-connectivity.


            aRegionGrower1.grow((int) aSeedPoint.x, (int) aSeedPoint.y);

            //  The resulting binary image is inverted. i.e, required area is white filled and outside it black. We want the other way round. Invert it.
            Mat aDressOnlyMatGrayScaleInv = Mat.zeros(myImageMatrix.size(), myImageMatrix.type());
            aDressOnlyMatGrayScaleInv.setTo(new Scalar(255));

            Mat aVisitedMat = Mat.zeros(myImageMatrix.size(), myImageMatrix.type());
            aVisitedMat.put(0, 0, aRegionGrower1.getVisitedMatrix());

            Core.subtract(aDressOnlyMatGrayScaleInv, aVisitedMat, aVisitedMat);

            //  Perform one round of dilation of this area to smooth out.
            Imgproc.dilate(aVisitedMat, aVisitedMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

            Core.add(myContourImage, aVisitedMat, aVisitedMat);

            aVisitedMat.get(0, 0, myVisitedMatrix);
        }
    }


    public byte[] getVisitedMatrix()
    {
        return myVisitedMatrix;

    }

    public Mat getContourImage()
    {
        return myContourImage;
    }

    private void processPixel(int i, int j)
    {
        byte aVal = myImageBuf[i * myImageMatrix.cols() + j];
        boolean updateMinMax = true;
        if(myCurrAvg == -1)
        {
            myCurrAvg = aVal;
        }
        else
        {
            if(Math.abs(aVal - myCurrAvg) < 10) //  Ignore pixels with great variations. They won't contribute to 'background'
            {
                myCurrAvg = (byte) (myCurrAvg / 2 + aVal / 2);
            }
            else
            {
                updateMinMax = false;
            }
        }
        if(updateMinMax)
        {
            myMaxVal = (byte) Math.max(aVal, myMaxVal);
            myMinVal = (byte) Math.min(aVal, myMinVal);
        }
    }
    private void calculateInitialRange()
    {
        myCurrAvg = -1;
        for(int i = 0; i < myImageMatrix.rows(); i++)
        {
            processPixel(i, 0);
        }
        for(int j = 0; j < myImageMatrix.cols(); j++)
        {
            processPixel(0, j);
        }

        //myMinVal = (byte)(myMinVal - myMinVal * 0.1);
        myMaxVal = (byte)(myMaxVal + myMaxVal * 0.1);

        Log.i("calculateInitialRange", "myMinVal :" + myMinVal);
        Log.i("calculateInitialRange", "myMaxVal :" + myMaxVal);
    }



    private Mat myImageMatrix;
    private byte[] myImageBuf;

    private  byte[] myVisitedMatrix;

    private byte myMinVal = (byte)255;
    private byte myMaxVal = 0;

    private Mat myContourImage;

    byte myCurrAvg = -1;
}
