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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
            RegionGrower aRegionGrower = new RegionGrower(myImageMatrix, myMinVal, myMaxVal, true);
            aRegionGrower.grow(0, 0);

            myVisitedMatrix = Arrays.copyOf(aRegionGrower.getVisitedMatrix(), aRegionGrower.getVisitedMatrix().length);
        }

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

    private class RegionGrower
    {
        private Mat myImageMatrix;
        private byte[] myImageBuf;
        private byte[] myVisitedMatrix;

        private byte myMinVal = (byte)255;
        private byte myMaxVal = 0;

        private boolean my8ConnectionNeeded = true;

        public RegionGrower(Mat anImageBuffer_in, byte aMinVal_in, byte aMaxVal_in, boolean a8connNeeded_in)
        {
            myImageMatrix = anImageBuffer_in;
            myImageBuf = new byte[(int) myImageMatrix.total()];
            myImageMatrix.get(0, 0, myImageBuf);

            myVisitedMatrix = new byte[(int) myImageMatrix.total()];

            myMinVal = aMinVal_in;
            myMaxVal = aMaxVal_in;

            my8ConnectionNeeded = a8connNeeded_in;
        }

        public void grow(int x, int y)
        {
            visitPixel(x, y);
        }

        public byte[] getVisitedMatrix()
        {
            return myVisitedMatrix;

        }

        private void visitPixel(int anXIndex_in, int aYIndex_in)
        {
            int anIndex = anXIndex_in * myImageMatrix.cols() + aYIndex_in;
            myVisitedMatrix[anIndex] = (byte)255;

            for (int i = anXIndex_in - 1; i <= anXIndex_in + 1; i++)
            {
                for (int j = aYIndex_in - 1; j <= aYIndex_in + 1; j++)
                {

                    if (i >= 0 && j >= 0 && i < myImageMatrix.rows() && j < myImageMatrix.cols())
                    {
                        if(!my8ConnectionNeeded)
                        {
                            double aDist = Math.pow(i - anXIndex_in, 2) + Math.pow(j - aYIndex_in, 2);
                            if(aDist > 1)
                                continue;
                        }

                        int anCurrentIndex = i * myImageMatrix.cols() + j;
                        if (myVisitedMatrix[anCurrentIndex] == 0 && myImageBuf[anCurrentIndex] <= myMaxVal && myImageBuf[anCurrentIndex] >= myMinVal)
                        {
                            visitPixel(i, j);
                        }
                    }
                }
            }


        }
    }

    private class ContourProcessor
    {
        private  byte[] myBinImage;
        private Size myImageSize;
        boolean myVisitedArray[];

        List<MatOfPoint> myBigContours = new ArrayList<MatOfPoint>();
        List<MatOfPoint> mySingleContours = new ArrayList<MatOfPoint>();

        int myNearestContourIndex = -1;
        boolean myNearestContourDirection = false;



        Mat myHiearchy = null;

        private Mat myContourImage;


        public ContourProcessor(Size anImageSize_in, byte[] aBinaryImage_in)
        {
            myBinImage = aBinaryImage_in;
            myImageSize = anImageSize_in;


        }

        public Point getSeedPoint()
        {
            Point aSeedPoint = new Point();
            aSeedPoint.x = myContourImage.rows() / 2;
            aSeedPoint.y = myContourImage.cols() / 2;

            byte[] aContBuff = new byte[(int) myContourImage.total()];
            myContourImage.get(0, 0, aContBuff);

            for(int i = 0; i < myContourImage.rows(); i++)
            {
                for(int j = 0; j < myContourImage.cols(); j++)
                {
                    int anIndex = (int)(i * myContourImage.cols() + j);
                    if(aContBuff[anIndex] < 0 && i < myContourImage.rows() - 1 && j < myContourImage.cols() - 1)
                    {
                        aSeedPoint.x = i + 1;
                        aSeedPoint.y = j + 1;
                        return aSeedPoint;
                    }
                }
            }
            return aSeedPoint;
        }

        private void selectBigContours(List<MatOfPoint> aContours)
        {
            int index = 0;

            int[] iBuff = new int[ (int) (myHiearchy.total() * myHiearchy.channels())];
            myHiearchy.get(0, 0, iBuff);

            for (int idx = 0; idx < aContours.size(); idx++)
            {
                if(iBuff[idx * 4 + 3] == -1)
                {
                    MatOfPoint contour = aContours.get(idx);

                    //printContour(contour);

                    Rect rect = Imgproc.boundingRect(new MatOfPoint(contour));

                    double aRectArea = rect.area();


                    if (aRectArea > 50)
                    {
                        MatOfPoint aSrcContour = aContours.get(idx);

                        if (index >= 0)
                            myBigContours.add(aSrcContour);

                        if (myBigContours.size() == 30)
                            return;

                        index++;
                    }
                }
            }

            Log.i("selectBigContours", "Number of big contours :" + myBigContours.size());
        }

        private void printContour(MatOfPoint aContour_in)
        {
            Log.i("aContour_in", "aContour_in size :" + aContour_in.size().height);
            for(int jdx = 0; jdx < aContour_in.size().height; jdx++)
            {
                double[] aPoint = aContour_in.get(jdx, 0);
                //Log.i("aContour_in", "aPoint :[" + aPoint[0] + ", " + aPoint[1] + "]");

                int aPrevIndex = jdx - 1;
                if(aPrevIndex < 0)
                {
                    aPrevIndex = (int)aContour_in.size().height - 1;
                }
                double[] aPrevPoint = aContour_in.get(aPrevIndex, 0);
                double aLength = Math.pow(aPoint[0] - aPrevPoint[0], 2) + Math.pow(aPoint[1] - aPrevPoint[1], 2);
                if(aLength > 2)
                {
                    Log.i("Big gap : ", "aPoint :[" + aPoint[0] + ", " + aPoint[1] + "]" + "aPrevPoint :[" + aPrevPoint[0] + ", " + aPrevPoint[1] + "]" + "Index = " + jdx);
                }
            }
        }
        private int getNextUnvisited()
        {
            for(int i = 0; i < myVisitedArray.length; i++)
            {
                if(myVisitedArray[i] == false)
                {
                    return i;
                }
            }
            return -1;
        }

        private List<Point> getLinePoints(int x1, int y1, int x2, int y2)   //  Bresenhaums algo
        {
            List<Point> aPointArray = new ArrayList<Point>();
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);

            int sx = (x1 < x2) ? 1 : -1;
            int sy = (y1 < y2) ? 1 : -1;

            int err = dx - dy;

            while (true)
            {
                aPointArray.add(new Point(x1, y1));

                if (x1 == x2 && y1 == y2) {
                    break;
                }

                int e2 = 2 * err;

                if (e2 > -dy) {
                    err = err - dy;
                    x1 = x1 + sx;
                }

                if (e2 < dx) {
                    err = err + dx;
                    y1 = y1 + sy;
                }
            }
            return aPointArray;
        }

        private void fillMissingPoints(double[] aLastPoint, double[] aPoint, MatOfPoint aSingleContour)
        {
            List<Point> aNewPoints = getLinePoints((int)aLastPoint[0], (int)aLastPoint[1], (int)aPoint[0], (int)aPoint[1]);
            for(int i = 1; i < aNewPoints.size() - 1; i++)  //  First and last points are already in the contour
            {
                double[] anInterMedPoint = new double[2];
                anInterMedPoint[0] = aNewPoints.get(i).x;
                anInterMedPoint[1] = aNewPoints.get(i).y;
                Mat aRows = Mat.zeros(1, 1, aSingleContour.type());
                aRows.put(0, 0, anInterMedPoint);
                aSingleContour.push_back(aRows);
            }
        }
        private void addNextContour()
        {
            if(myNearestContourIndex < 0)
                return;

            myVisitedArray[myNearestContourIndex] = true;

            MatOfPoint aSingleContour = mySingleContours.get(0);
            int aSingleContourSize = (int)aSingleContour.size().height;


            Mat aNextContour = myBigContours.get(myNearestContourIndex);

            Log.i("addNextContour", "aNextContour size :[" + aNextContour.size().height);
            Mat aNewContour = new Mat();
            for(int jdx = 0; jdx < aNextContour.size().height; jdx++)
            {
                double[] aPoint = null;

                if (myNearestContourDirection == true)
                {
                    aPoint = aNextContour.get(jdx, 0);
                } else
                {
                    aPoint = aNextContour.get((int) aNextContour.size().height - 1 - jdx, 0);
                }

                if(jdx == 0 && aSingleContourSize > 0)
                {
                    double[] aLastPoint = aSingleContour.get(aSingleContourSize - 1, 0);

                    /*List<Point> aNewPoints = getLinePoints((int)aLastPoint[0], (int)aLastPoint[1], (int)aPoint[0], (int)aPoint[1]);
                    for(int i = 1; i < aNewPoints.size() - 1; i++)  //  First and last points are already in the contour
                    {
                        double[] anInterMedPoint = new double[2];
                        anInterMedPoint[0] = aNewPoints.get(i).x;
                        anInterMedPoint[1] = aNewPoints.get(i).y;
                        Mat aRows = Mat.zeros(1, 1, aNextContour.type());
                        aRows.put(0, 0, anInterMedPoint);
                        aSingleContour.push_back(aRows);
                    }*/
                    fillMissingPoints(aLastPoint, aPoint, aSingleContour);
                }
               //Log.i("addNextContour", "aPoint :[" + aPoint[0] + ", " + aPoint[1]);
                Mat aRows = Mat.zeros(1, 1, aNextContour.type());
                aRows.put(0, 0, aPoint);

                aSingleContour.push_back(aRows);

            }

        }

        private void getNextContour()
        {
            myNearestContourIndex = -1;
            myNearestContourDirection = false;

            Mat aSingleContour = mySingleContours.get(0);

            if(aSingleContour.size().height == 0)
                return;

            double[] anEnd1 = aSingleContour.get((int)(aSingleContour.size().height - 1),0);


            double aNearestContourDistance = Double.MAX_VALUE;

            for(int i = 0; i < myBigContours.size(); i++)
            {
                if(myVisitedArray[i] == false)
                {
                    Mat aNextPossibleContour = myBigContours.get(i);

                    double[] aStart2 = aNextPossibleContour.get(0, 0);
                    double[] anEnd2 = aNextPossibleContour.get((int) (aNextPossibleContour.size().height - 1), 0);

                    double aDist1 = (aStart2[0] - anEnd1[0]) * (aStart2[0] - anEnd1[0]) + (aStart2[1] - anEnd1[1]) * (aStart2[1] - anEnd1[1]);
                    double aDist2 = (anEnd2[0] - anEnd1[0]) * (anEnd2[0] - anEnd1[0]) + (anEnd2[1] - anEnd1[1]) * (anEnd2[1] - anEnd1[1]);

                    if(aDist1 < aNearestContourDistance)
                    {
                        aNearestContourDistance = aDist1;
                        myNearestContourDirection = true;
                        myNearestContourIndex = i;
                    }
                    if(aDist2 < aNearestContourDistance)
                    {
                        aNearestContourDistance = aDist2;
                        myNearestContourDirection = false;
                        myNearestContourIndex = i;
                    }
                }
            }

        }

        private int getRealNumberOfPoints(Mat aContour_in)
        {
            int[] aVisitedMatrix = new int[(int)(myImageSize.height * myImageSize.width)];
            Arrays.fill(aVisitedMatrix, -1);

            for(int jdx = 0; jdx < aContour_in.size().height - 1; jdx++)
            {
                double[] aPoint = aContour_in.get(jdx, 0);
                int anIndex = (int)(aPoint[1] * myImageSize.width + aPoint[0]);
                if(aVisitedMatrix[anIndex] == -1)
                {
                    aVisitedMatrix[anIndex] = jdx;
                }
                else
                {
                    int aPrevPointIndex = aVisitedMatrix[anIndex] - 1;
                    if (aPrevPointIndex < 0)
                    {
                        aPrevPointIndex = (int)aContour_in.size().height - 1;
                    }
                    double[] aPrevPoint = aContour_in.get(aPrevPointIndex, 0);
                    double[] aNextPoint = aContour_in.get(jdx + 1, 0);
                    if((int)aPrevPoint[0] == (int)aNextPoint[0] && (int)aPrevPoint[1] == (int)aNextPoint[1])
                    {
                        return jdx + 1;
                    }
                }
            }
            return (int)aContour_in.size().height;
        }
        private void removeDuplicatePoints()
        {
            for(int i = 0; i < myBigContours.size(); i++)
            {
                Mat aNextContour = myBigContours.get(i);

                int aRealNumberOfPoints = getRealNumberOfPoints(aNextContour);

                Mat aNewContour = new MatOfPoint(aNextContour.rowRange(0, aRealNumberOfPoints - 1));
                aNextContour.release();
                aNextContour.push_back(aNewContour);
            }
        }
        private void joinContours()
        {
            mySingleContours.add(new MatOfPoint());

            myVisitedArray = new boolean[myBigContours.size()];

            myNearestContourIndex = 0;
            myNearestContourDirection = true;

            while(getNextUnvisited() != -1)
            {
                addNextContour();

                getNextContour();

            }

            MatOfPoint aSingleContour = mySingleContours.get(0);
            int aNumPoints = (int)aSingleContour.size().height;
            if(aNumPoints > 2)
            {
                double[] aLastPoint = aSingleContour.get(aNumPoints - 1, 0);
                double[] aFirstPoint = aSingleContour.get(0, 0);
                fillMissingPoints(aLastPoint, aFirstPoint, aSingleContour);
            }

        }

        private void smoothContours()
        {
            int aKernelSize = 15;
            Mat aNewContour = new Mat();
            int aNumPoints = (int)mySingleContours.get(0).size().height;
            for(int jdx = 0; jdx < aNumPoints; jdx++)
            {
                double[] aNewPoint = new double[2];
                aNewPoint[0] = 0;
                aNewPoint[1] = 0;

                for(int j = -aKernelSize/2; j <= aKernelSize/2; j++)
                {
                    int anIndex = jdx + j;
                    if(anIndex < 0)
                    {
                        anIndex = aNumPoints + anIndex;
                    }
                    if(anIndex >= aNumPoints)
                    {
                        anIndex = anIndex - aNumPoints;
                    }
                    double[] aPoint = null;
                    aPoint = mySingleContours.get(0).get(anIndex, 0);

                    aNewPoint[0] += aPoint[0];
                    aNewPoint[1] += aPoint[1];

                    //Log.i("joinContours", "mySingleContours aPoint :[" + aPoint[0] + ", " + aPoint[1] + "]");
                }
                aNewPoint[0] = aNewPoint[0] / aKernelSize;
                aNewPoint[1] = aNewPoint[1] / aKernelSize;

                //Log.i("joinContours", "mySingleContours aNewPoint :[" + aNewPoint[0] + ", " + aNewPoint[1] + "]");


                Mat aRows = Mat.zeros(1, 1, mySingleContours.get(0).type());
                aRows.put(0, 0, aNewPoint);

                aNewContour.push_back(aRows);
            }

            mySingleContours.get(0).release();
            mySingleContours.get(0).push_back(aNewContour);
        }

        public Mat getContourImage()
        {
            return myContourImage;
        }

        private void adjustContours()
        {
            Mat aDressOnlyMatGrayScale = Mat.zeros(myImageSize, CvType.CV_8U);
            aDressOnlyMatGrayScale.put(0, 0, myBinImage);

            Imgproc.erode(aDressOnlyMatGrayScale, aDressOnlyMatGrayScale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));
            Imgproc.dilate(aDressOnlyMatGrayScale, aDressOnlyMatGrayScale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

            Mat anEdgeImage = new Mat();
            Imgproc.Canny(aDressOnlyMatGrayScale, anEdgeImage, 0, 255);

            List<MatOfPoint> aContours = new ArrayList<MatOfPoint>();
            myHiearchy = new Mat();
            Imgproc.findContours(anEdgeImage, aContours, myHiearchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE, new Point(0, 0));

            selectBigContours(aContours);

            removeDuplicatePoints();

            joinContours();

            printContour(mySingleContours.get(0));

            smoothContours();

            myContourImage = Mat.zeros(myImageMatrix.size(), CvType.CV_8U);
            Imgproc.drawContours(myContourImage, mySingleContours, -1, new Scalar(255));
            //Imgproc.drawContours(myContourImage, myBigContours, -1, new Scalar(255, 0, 0));


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
