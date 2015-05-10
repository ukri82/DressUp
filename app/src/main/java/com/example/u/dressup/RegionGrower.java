package com.example.u.dressup;

import org.opencv.core.Mat;

/**
 * Created by u on 03.05.2015.
 */
public class RegionGrower
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
