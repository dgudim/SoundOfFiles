package com.deo.sof;

import java.io.FileOutputStream;

public class BmpWriter {
    
    //--- Private constants
    private final static int BITMAPFILEHEADER_SIZE = 14;
    private final static int BITMAPINFOHEADER_SIZE = 40;
    //--- Private variable declaration
    //--- Bitmap file header
    private final byte[] bfType = {'B', 'M'};
    private int bfSize = 0;
    private final int bfReserved1 = 0;
    private final int bfReserved2 = 0;
    private final int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE;
    //--- Bitmap info header
    private final int biSize = BITMAPINFOHEADER_SIZE;
    private int biWidth = 0;
    private int biHeight = 0;
    private final int biPlanes = 1;
    private final int biBitCount = 24;
    private final int biCompression = 0;
    private int biSizeImage = 0x030000;
    private final int biXPelsPerMeter = 0x0;
    private final int biYPelsPerMeter = 0x0;
    private final int biClrUsed = 0;
    private final int biClrImportant = 0;
    private byte[] rawBytes;
    //--- File section
    private FileOutputStream fo;
    
    //--- Default constructor
    public BmpWriter() {
    }
    
    public void saveBitmap(String parFilename, byte[] rawData, int parWidth, int parHeight) {
        try {
            fo = new FileOutputStream(parFilename);
            save(rawData, parWidth, parHeight);
            fo.close();
        } catch (Exception saveEx) {
            saveEx.printStackTrace();
        }
    }
    
    /*
     *  The saveMethod is the main method of the process. This method
     *  will call the convertImage method to convert the memory image to
     *  a byte array; method writeBitmapFileHeader creates and writes
     *  the bitmap file header; writeBitmapInfoHeader creates the
     *  information header; and writeBitmap writes the image.
     *
     */
    private void save(byte[] rawData, int parWidth, int parHeight) {
        try {
            convertImage(rawData, parWidth, parHeight);
            writeBitmapFileHeader();
            writeBitmapInfoHeader();
            writeBitmap();
        } catch (Exception saveEx) {
            saveEx.printStackTrace();
        }
    }
    
    /*
     * convertImage converts the memory image to the bitmap format (BRG).
     * It also computes some information for the bitmap info header.
     *
     */
    private void convertImage(byte[] rawData, int parWidth, int parHeight) {
        int pad;
        int len = (int) (rawData.length / 3f) * 3;
        byte[] rawDataCut = rawData;
        //if (len >= 0) System.arraycopy(rawData, 0, rawDataCut, 0, len);
        rawBytes = rawDataCut;
        
        pad = (4 - ((parWidth * 3) % 4)) * parHeight;
        biSizeImage = ((parWidth * parHeight) * 3) + pad;
        bfSize = biSizeImage + BITMAPFILEHEADER_SIZE +
                BITMAPINFOHEADER_SIZE;
        biWidth = parWidth;
        biHeight = parHeight;
    }
    
    /*
     * Each scan line must be padded to an even 4-byte boundary.
     */
    private void writeBitmap() {
        int size;
        int j;
        int rowCount;
        int rowIndex;
        int lastRowIndex;
        int pad;
        int padCount;
        size = (biWidth * biHeight) - 1;
        pad = 4 - ((biWidth * 3) % 4);
        if (pad == 4)
            pad = 0;
        rowCount = 1;
        padCount = 0;
        rowIndex = size - biWidth;
        lastRowIndex = rowIndex;
        try {
            for (j = 0; j < size; j++) {
                if (rowCount == biWidth) {
                    padCount += pad;
                    rowCount = 1;
                    rowIndex = lastRowIndex - biWidth;
                    lastRowIndex = rowIndex;
                } else
                    rowCount++;
                rowIndex++;
            }
            bfSize += padCount - pad;
            biSizeImage += padCount - pad;
            fo.write(rawBytes);
        } catch (Exception wb) {
            wb.printStackTrace();
        }
    }
    
    /*
     * writeBitmapFileHeader writes the bitmap file header to the file.
     *
     */
    private void writeBitmapFileHeader() {
        try {
            fo.write(bfType);
            fo.write(intToDWord(bfSize));
            fo.write(intToWord(bfReserved1));
            fo.write(intToWord(bfReserved2));
            fo.write(intToDWord(bfOffBits));
        } catch (Exception wbfh) {
            wbfh.printStackTrace();
        }
    }
    
    /*
     *
     * writeBitmapInfoHeader writes the bitmap information header
     * to the file.
     *
     */
    private void writeBitmapInfoHeader() {
        try {
            fo.write(intToDWord(biSize));
            fo.write(intToDWord(biWidth));
            fo.write(intToDWord(biHeight));
            fo.write(intToWord(biPlanes));
            fo.write(intToWord(biBitCount));
            fo.write(intToDWord(biCompression));
            fo.write(intToDWord(biSizeImage));
            fo.write(intToDWord(biXPelsPerMeter));
            fo.write(intToDWord(biYPelsPerMeter));
            fo.write(intToDWord(biClrUsed));
            fo.write(intToDWord(biClrImportant));
        } catch (Exception wbih) {
            wbih.printStackTrace();
        }
    }
    
    /*
     *
     * intToWord converts an int to a word, where the return
     * value is stored in a 2-byte array.
     *
     */
    private byte[] intToWord(int parValue) {
        byte[] retValue = new byte[2];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x00FF);
        return (retValue);
    }
    
    /*
     *
     * intToDWord converts an int to a double word, where the return
     * value is stored in a 4-byte array.
     *
     */
    private byte[] intToDWord(int parValue) {
        byte[] retValue = new byte[4];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x000000FF);
        retValue[2] = (byte) ((parValue >> 16) & 0x000000FF);
        retValue[3] = (byte) ((parValue >> 24) & 0x000000FF);
        return (retValue);
    }
    
}
