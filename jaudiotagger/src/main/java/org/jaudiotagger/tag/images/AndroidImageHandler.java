package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import org.jaudiotagger.tag.id3.valuepair.ImageFormats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 Image Handling to to use when running on Android
 */
public class AndroidImageHandler implements ImageHandler
{
    private static AndroidImageHandler instance;

    public static AndroidImageHandler getInstanceOf()
    {
        if(instance==null)
        {
            instance = new AndroidImageHandler();
        }
        return instance;
    }

    private AndroidImageHandler()
    {

    }

    /**
     * Resize the image until the total size require to store the image is less than maxsize
     * @param artwork
     * @param maxSize
     * @throws IOException
     */
    public void reduceQuality(Artwork artwork, int maxSize) throws IOException
    {
        while(artwork.getBinaryData().length > maxSize)
        {
            Bitmap srcImage = (Bitmap) artwork.getImage();
            int w = srcImage.getWidth();
            int newSize = w /2;
            makeSmaller(artwork,newSize);
        }
    }
     /**
     * Resize image using Bitmap
     * @param artwork
     * @param size
     * @throws java.io.IOException
     */
    public void makeSmaller(Artwork artwork,int size) throws IOException
    {
        Bitmap srcImage = (Bitmap)artwork.getImage();

        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        // Determine the scaling required to get desired result.
        float scaleW = (float) size / (float) w;
        float scaleH = (float) size / (float) h;

        // scale Bitmap
        Matrix matrix = new Matrix();
        matrix.setScale(scaleW, scaleH);
        Bitmap bi = Bitmap.createBitmap(srcImage, 0, 0, srcImage.getWidth(), srcImage.getHeight(), matrix, true);


        if(artwork.getMimeType()!=null && isMimeTypeWritable(artwork.getMimeType()))
        {
            artwork.setBinaryData(writeImage(bi, artwork.getMimeType()));
        }
        else
        {
            artwork.setBinaryData(writeImageAsPng(bi));
        }
    }

    public boolean isMimeTypeWritable(String mimeType)
    {
        switch (mimeType) {
            case ImageFormats.MIME_TYPE_JPG:
            case ImageFormats.MIME_TYPE_JPEG:
                return true;
        }
        return false;
    }
    /**
     *  Write buffered image as required format
     *
     * @param bi
     * @param mimeType
     * @return
     * @throws IOException
     */
    public byte[] writeImage(Object bi,String mimeType) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((Bitmap)bi).compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }

    /**
     *
     * @param bi
     * @return
     * @throws IOException
     */
    public byte[] writeImageAsPng(Object bi) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((Bitmap)bi).compress(Bitmap.CompressFormat.PNG, 80, baos);
        return baos.toByteArray();
    }

    @Override
    public Object getImage(byte[] bytes) {
        return BitmapFactory.decodeStream(new ByteArrayInputStream(bytes));
    }

    /**
     * Show read formats
     *
     * On Windows supports png/jpeg/bmp/gif
     */
    public void showReadFormats()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Show write formats
     *
     * On Windows supports png/jpeg/bmp
     */
    public void showWriteFormats()
    {
        throw new UnsupportedOperationException();
    }
}
