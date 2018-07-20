package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;

import org.jaudiotagger.tag.id3.valuepair.ImageFormats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 Image Handling used when running on standard JVM
 */
public class StandardImageHandler implements ImageHandler
{
    private static StandardImageHandler instance;

    public static StandardImageHandler getInstanceOf()
    {
        if(instance==null)
        {
            instance = new StandardImageHandler();
        }
        return instance;
    }

    private StandardImageHandler()
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
//            Image srcImage = (Image)artwork.getImage();
//            int w = srcImage.getWidth(null);
            Bitmap srcImage = (Bitmap) artwork.getImage();
            int w = srcImage.getWidth();
            int newSize = w /2;
            makeSmaller(artwork,newSize);
        }
    }
     /**
     * Resize image using Java 2D
      * @param artwork
      * @param size
      * @throws java.io.IOException
      */
    public void makeSmaller(Artwork artwork,int size) throws IOException
    {
//        Image srcImage = (Image)artwork.getImage();
//
//        int w = srcImage.getWidth(null);
//        int h = srcImage.getHeight(null);
        Bitmap srcImage = (Bitmap)artwork.getImage();

        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        // Determine the scaling required to get desired result.
        float scaleW = (float) size / (float) w;
        float scaleH = (float) size / (float) h;

        //Create an image buffer in which to paint on, create as an opaque Rgb type image, it doesnt matter what type
        //the original image is we want to convert to the best type for displaying on screen regardless
//        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
//
//        // Set the scale.
//        AffineTransform tx = new AffineTransform();
//        tx.scale(scaleW, scaleH);
//
//        // Paint image.
//        Graphics2D g2d = bi.createGraphics();
//        g2d.drawImage(srcImage, tx, null);
//        g2d.dispose();
        Matrix matrix = new Matrix();
        matrix.setScale(scaleW, scaleH);
        Bitmap bi = Bitmap.createBitmap(srcImage, 0, 0, srcImage.getWidth(), srcImage.getHeight(), matrix, true);


        if(artwork.getMimeType()!=null && isMimeTypeWritable(artwork.getMimeType()))
        {
//            artwork.setBinaryData(writeImage(bi,artwork.getMimeType()));
            artwork.setBinaryData(writeImage(bi, artwork.getMimeType()));
        }
        else
        {
//            artwork.setBinaryData(writeImageAsPng(bi));
            artwork.setBinaryData(writeImageAsPng(srcImage));
        }
    }

    public boolean isMimeTypeWritable(String mimeType)
    {
//        Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
//        return writers.hasNext();
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
//        Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
//        if(writers.hasNext())
//        {
//            ImageWriter writer = writers.next();
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            writer.setOutput(ImageIO.createImageOutputStream(baos));
//            writer.write(bi);
//            return baos.toByteArray();
//        }
//        throw new IOException("Cannot write to this mimetype");
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
        //ImageIO.write(bi, ImageFormats.MIME_TYPE_PNG,baos);
        ((Bitmap)bi).compress(Bitmap.CompressFormat.PNG, 80, baos);
        return baos.toByteArray();
    }

    @Override
    public Object getImage(byte[] bytes) {
//        return ImageIO.read(new ByteArrayInputStream(getRawImageData()));
        return BitmapFactory.decodeStream(new ByteArrayInputStream(bytes));
    }

    /**
     * Show read formats
     *
     * On Windows supports png/jpeg/bmp/gif
     */
    public void showReadFormats()
    {
//         String[] formats = ImageIO.getReaderMIMETypes();
//        for(String f:formats)
//        {
//            System.out.println("r"+f);
//        }
    }

    /**
     * Show write formats
     *
     * On Windows supports png/jpeg/bmp
     */
    public void showWriteFormats()
    {
//         String[] formats = ImageIO.getWriterMIMETypes();
//        for(String f:formats)
//        {
//            System.out.println(f);
//        }
    }
}
