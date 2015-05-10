package com.example.u.dressup;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by u on 03.05.2015.
 */
public class RegionGrowerQBased
{
    private Mat myImageMatrix;
    private byte[] myImageBuf;
    private byte[] myFilledMatrix;

    private byte[] myVisitedMatrix;

    private Queue<Point> myPoints2BVisited = new LinkedList<>();


    private byte myMinVal = (byte)255;
    private byte myMaxVal = 0;

    private boolean my8ConnectionNeeded = true;

    public RegionGrowerQBased(Mat anImageBuffer_in, byte aMinVal_in, byte aMaxVal_in, boolean a8connNeeded_in)
    {
        myImageMatrix = anImageBuffer_in;
        myImageBuf = new byte[(int) myImageMatrix.total()];
        myImageMatrix.get(0, 0, myImageBuf);

        myFilledMatrix = new byte[(int) myImageMatrix.total()];
        myVisitedMatrix = new byte[(int) myImageMatrix.total()];

        myMinVal = aMinVal_in;
        myMaxVal = aMaxVal_in;

        my8ConnectionNeeded = a8connNeeded_in;
    }

    public void grow(int x, int y)
    {
        myPoints2BVisited.add(new Point(x,y));
        while(!myPoints2BVisited.isEmpty())
        {
            Point p = myPoints2BVisited.remove();
            visitPixel((int)p.x, (int)p.y);
        }
    }

    public byte[] getVisitedMatrix()
    {
        return myFilledMatrix;

    }

    private void visitPixel(int anXIndex_in, int aYIndex_in)
    {
        int anIndex = anXIndex_in * myImageMatrix.cols() + aYIndex_in;
        myFilledMatrix[anIndex] = (byte)255;

        for (int i = anXIndex_in - 1; i <= anXIndex_in + 1; i++)
        {
            for (int j = aYIndex_in - 1; j <= aYIndex_in + 1; j++)
            {

                if (i >= 0 && j >= 0 && i < myImageMatrix.rows() && j < myImageMatrix.cols())
                {
                    int anCurrentIndex = i * myImageMatrix.cols() + j;
                    if (myVisitedMatrix[anCurrentIndex] == 1)
                        continue;

                    myVisitedMatrix[anCurrentIndex] = (byte)1;
                    if(i == anXIndex_in && j == aYIndex_in)
                        continue;

                    if(!my8ConnectionNeeded)
                    {
                        double aDist = Math.pow(i - anXIndex_in, 2) + Math.pow(j - aYIndex_in, 2);
                        if(aDist > 1)
                            continue;
                    }

                    if (myImageBuf[anCurrentIndex] <= myMaxVal && myImageBuf[anCurrentIndex] >= myMinVal)
                    {
                        myPoints2BVisited.add(new Point(i,j));
                    }
                }
            }
        }


    }
}
