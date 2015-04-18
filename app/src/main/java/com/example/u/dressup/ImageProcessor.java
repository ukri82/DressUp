package com.example.u.dressup;

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
    public ImageProcessor(Mat anImageBuffer_in) {
        myImageMatrix = anImageBuffer_in;
        myImageBuf = new byte[(int) myImageMatrix.total()];
        myImageMatrix.get(0, 0, myImageBuf);

        myVisitedMatrix = new byte[(int) myImageMatrix.total()];

    }
    public void regionGrow()
    {
        calculateInitialRange();
        visitPixel(0, 0);

        ContourProcessor aContourProc = new ContourProcessor(myImageMatrix.size(), myVisitedMatrix);
        aContourProc.adjustContours();
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

        public ContourProcessor(Size anImageSize_in, byte[] aBinaryImage_in)
        {
            myBinImage = aBinaryImage_in;
            myImageSize = anImageSize_in;

        }

        private void selectBigContours(List<MatOfPoint> aContours)
        {
            int index = 0;

            for (int idx = 0; idx < aContours.size(); idx++)
            {
                Mat contour = aContours.get(idx);

                /*MatOfPoint2f contour2f = new MatOfPoint2f( aContours.get(idx).toArray() );
                MatOfPoint2f         approxCurve = new MatOfPoint2f();
                double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

                Rect rect = Imgproc.boundingRect(points);*/

                Rect rect = Imgproc.boundingRect(new MatOfPoint(contour));

                double aRectArea = rect.area();


                if(aRectArea > 50)
                {
                    //aBigContours.add(aContours.get(idx));

                    MatOfPoint aSrcContour = aContours.get(idx);

                    if(index >= 0)
                        myBigContours.add(aSrcContour);

                    if(myBigContours.size() == 30)
                        return;

                    index++;
                }

                //Core.rectangle(contoursFrame, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), (255, 0, 0, 255), 3);
            }

            Log.i("selectBigContours", "Number of big contours :" + myBigContours.size());
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

        private void addNextContour()
        {
            if(myNearestContourIndex < 0)
                return;

            myVisitedArray[myNearestContourIndex] = true;

            MatOfPoint aSingleContour = mySingleContours.get(0);
            int aSingleContourSize = (int)aSingleContour.size().height;


            Mat aNextContour = myBigContours.get(myNearestContourIndex);

            Log.i("addNextContour", "aNextContour size :[" + aNextContour.size().height);
            //Mat aNewContour = Mat.zeros(aNextContour.size(), aNextContour.type());
            Mat aNewContour = new Mat();
            for(int jdx = 0; jdx < aNextContour.size().height; jdx++)
            {
                double[] aPoint = null;
                //aPoint = aNextContour.get(jdx, 0);
                if (myNearestContourDirection == true)
                {
                    aPoint = aNextContour.get(jdx, 0);
                } else
                {
                    aPoint = aNextContour.get((int) aNextContour.size().height - 1 - jdx, 0);
                }


                Log.i("addNextContour", "aPoint :[" + aPoint[0] + ", " + aPoint[1]);
                //aNewContour.put(jdx, 0, aPoint);
                Mat aRows = Mat.zeros(1, 1, aNextContour.type());
                aRows.put(0, 0, aPoint);

                aSingleContour.push_back(aRows);


                /*Mat aRows = Mat.zeros(1, 1, aNextContour.type());
                aRows.put(0, 0, aPoint);

                aSingleContour.push_back(aRows);*/
            }

            Log.i("addNextContour", "Contour added");
            //aSingleContour.push_back(aNewContour);
            //aSingleContour.push_back(aNextContour);
            //mySingleContours.clear();
            //mySingleContours.add(aSingleContour);
            //mySingleContours.set(0, aSingleContour);
            //mySingleContours.set(0, new MatOfPoint(aNextContour));
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

        /*private int getRealNumberOfPoints(Mat aContour_in)
        {
            Mat aVisitedMatrix = Mat.zeros(myImageSize, CvType.CV_8UC4);
            for(int jdx = 1; jdx < aContour_in.size().height - 1; jdx++)
            {
                double[] aPrevPoint = aContour_in.get(jdx - 1, 0);
                double[] aPoint = aContour_in.get(jdx, 0);
                double[] aNextPoint = aContour_in.get(jdx + 1, 0);

                if((int)aPrevPoint[0] == (int)aNextPoint[0] && (int)aPrevPoint[1] == (int)aNextPoint[1])
                {
                    return jdx + 1;
                }
            }
            return (int)aContour_in.size().height;
        }*/
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
                /*for(int jdx = 0; jdx < aRealNumberOfPoints; jdx++)
                {
                    double[] aPoint = aNextContour.get(jdx, 0);

                    Mat aRows = Mat.zeros(1, 1, aNextContour.type());
                    aRows.put(0, 0, aPoint);

                    aSingleContour.push_back(aRows);



                }*/
            }
        }
        private void joinContours()
        {
            //mySingleContours.add(new MatOfPoint(myBigContours.get(0)));
            mySingleContours.add(new MatOfPoint());

            myVisitedArray = new boolean[myBigContours.size()];

            myNearestContourIndex = 0;
            myNearestContourDirection = true;

            while(getNextUnvisited() != -1)
            {
                addNextContour();

                getNextContour();

            }

            Log.i("joinContours", "mySingleContours size :[" + mySingleContours.get(0).size().height);
            for(int jdx = 0; jdx < mySingleContours.get(0).size().height; jdx++)
            {
                double[] aPoint = null;
                aPoint = mySingleContours.get(0).get(jdx, 0);
                Log.i("joinContours", "mySingleContours aPoint :[" + aPoint[0] + ", " + aPoint[1]);
            }
        }
        private void adjustContours()
        {
            Mat aDressOnlyMatGrayScale = Mat.zeros(myImageSize, CvType.CV_8U);
            aDressOnlyMatGrayScale.put(0, 0, myBinImage);

            Imgproc.erode(aDressOnlyMatGrayScale, aDressOnlyMatGrayScale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));
            Imgproc.dilate(aDressOnlyMatGrayScale, aDressOnlyMatGrayScale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

            Mat anEdgeImage = new Mat();
            Imgproc.Canny(aDressOnlyMatGrayScale, anEdgeImage, 0, 255);

            //Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(3,3), new Point(1,1));
            //Imgproc.erode(aDressOnlyMatGrayScale, aDressOnlyMatGrayScale, kernel);

            List<MatOfPoint> aContours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(anEdgeImage, aContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE, new Point(0, 0));

            selectBigContours(aContours);

            removeDuplicatePoints();

            joinContours();

            myContourImage = Mat.zeros(myImageMatrix.size(), CvType.CV_8U);
            Imgproc.drawContours(myContourImage, mySingleContours, -1, new Scalar(255, 255, 255));
            //Imgproc.drawContours(myContourImage, myBigContours, -1, new Scalar(255, 255, 255));


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

    public void visitPixel(int anXIndex_in, int aYIndex_in)
    {
        int anIndex = anXIndex_in * myImageMatrix.cols() + aYIndex_in;
        myVisitedMatrix[anIndex] = (byte)255;


        for(int i = anXIndex_in - 1; i <= anXIndex_in + 1; i++)
        {
            for(int j = aYIndex_in - 1; j <= aYIndex_in + 1; j++)
            {
                if(i >= 0 && j >= 0 && i < myImageMatrix.rows() && j < myImageMatrix.cols())
                {
                    int anCurrentIndex = i * myImageMatrix.cols() + j;
                    if(myVisitedMatrix[anCurrentIndex] == 0 && myImageBuf[anCurrentIndex] <= myMaxVal && myImageBuf[anCurrentIndex] >= myMinVal )
                    {
                        visitPixel(i, j);
                    }
                }
            }
        }
    }

    byte myCurrAvg = -1;
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

        /*byte aCurrAvg = 0;

        for(int i = 0; i < myImageMatrix.rows(); i++)
        {
            byte aVal = myImageBuf[i * myImageMatrix.cols()];
            boolean updateMinMax = true;
            if(i == 0)
            {
                aCurrAvg = aVal;
            }
            else
            {
                if(Math.abs(aVal - aCurrAvg) < 10)
                {
                    aCurrAvg = (byte) (aCurrAvg / 2 + aVal / 2);
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
        }*/

        /*for(int i = 1; i < myImageMatrix.rows() + 1; i++)
        {
            byte aVal = myImageBuf[i * myImageMatrix.cols() - 1];
            myMaxVal = (byte)Math.max(aVal, myMaxVal);
            myMinVal = (byte)Math.min(aVal, myMinVal);
        }*/

        /*for(int j = 0; j < myImageMatrix.cols(); j++)
        {
            byte aVal = myImageBuf[j];
            myMaxVal = (byte)Math.max(aVal, myMaxVal);
            myMinVal = (byte)Math.min(aVal, myMinVal);
        }*/

        /*for(int j = 0; j < myImageMatrix.cols(); j++)
        {
            byte aVal = myImageBuf[myImageMatrix.rows() - 1 - j];
            myMaxVal = (byte)Math.max(aVal, myMaxVal);
            myMinVal = (byte)Math.min(aVal, myMinVal);
        }*/

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
}
