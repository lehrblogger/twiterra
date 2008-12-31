/* Copyright (C) 2001, 2008 United States Government as represented by
   the Administrator of the National Aeronautics and Space Administration.
   All Rights Reserved.
 */

package gov.nasa.worldwind.formats.tiff;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author brownrigg
 * @version $Id$
 */
public class GeotiffImageReader extends ImageReader {
    
    public GeotiffImageReader(ImageReaderSpi provider) {
        super(provider);
    }
    
    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        // TODO:  This should allow for multiple images that may be present. For now, we'll ignore all but first.
        return 1;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        if (imageIndex < 0 || imageIndex >= getNumImages(true)) 
            throw new IllegalArgumentException(this.getClass().getName() + ".getWidth(): illegal imageIndex: " + imageIndex);
        TiffIFDEntry widthEntry = getByTag(ifds.get(imageIndex), TiffTags.IMAGE_WIDTH);
        return (int)widthEntry.asLong();
    }
    
    @Override
    public int getHeight(int imageIndex) throws IOException {
        if (imageIndex < 0 || imageIndex >= getNumImages(true)) 
            throw new IllegalArgumentException(this.getClass().getName() + ".getHeight(): illegal imageIndex: " + imageIndex);        
        TiffIFDEntry heightEntry = getByTag(ifds.get(imageIndex), TiffTags.IMAGE_LENGTH);
        return (int)heightEntry.asLong();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        // TODO: For this first implementation, we are completly ignoring the ImageReadParam given to us.
        //       Our target functionality is not the entire ImageIO, but only that needed to support the static
        //       read method ImageIO.read("myImage.tif").
        
        // TODO: more generally, the following test should reflect that more than one image is possible in a Tiff.
        if (imageIndex != 0)
            throw new IllegalArgumentException(this.getClass().getName() + ".read(): illegal imageIndex: " + imageIndex);
        
        readIFDs();

        // Extract the various IFD tags we need to read this image...
        TiffIFDEntry widthEntry = null;
        TiffIFDEntry lengthEntry = null;
        TiffIFDEntry bitsPerSampleEntry = null;
        TiffIFDEntry samplesPerPixelEntry = null;
        TiffIFDEntry photoInterpEntry = null;
        TiffIFDEntry stripOffsetsEntry = null;
        TiffIFDEntry stripCountsEntry = null;
        TiffIFDEntry rowsPerStripEntry = null;     
        TiffIFDEntry planarConfigEntry = null;
       
        TiffIFDEntry[] ifd = ifds.get(imageIndex);
        for (int i=0; i<ifd.length; i++) {
            TiffIFDEntry entry = ifd[i];
            switch (entry.tag) {
                case TiffTags.IMAGE_WIDTH:
                    widthEntry = entry;  break;
                case TiffTags.IMAGE_LENGTH:
                    lengthEntry = entry;  break;
                case TiffTags.BITS_PER_SAMPLE:
                    bitsPerSampleEntry = entry;  break;
                case TiffTags.SAMPLES_PER_PIXEL:
                    samplesPerPixelEntry = entry;  break;
                case TiffTags.PHOTO_INTERPRETATION:
                    photoInterpEntry = entry;  break;
                case TiffTags.STRIP_OFFSETS:
                    stripOffsetsEntry = entry;  break;
                case TiffTags.STRIP_BYTE_COUNTS:
                    stripCountsEntry = entry;  break;
                case TiffTags.ROWS_PER_STRIP:
                    rowsPerStripEntry = entry;  break;
                case TiffTags.PLANAR_CONFIGURATION:
                    planarConfigEntry = entry;  break;
            }
        }
        
        if (widthEntry == null || lengthEntry == null || samplesPerPixelEntry == null || photoInterpEntry == null ||
                stripOffsetsEntry == null || stripCountsEntry == null || rowsPerStripEntry == null || planarConfigEntry == null)
            // note that bitsPerSample is an optional entry, so not checked above;  its default is "1".
            throw new IIOException(this.getClass().getName() + ".read(): unable to decipher image organization");
        
        int width = (int) widthEntry.asLong();
        int height = (int) lengthEntry.asLong();
        int samplesPerPixel = (int) samplesPerPixelEntry.asLong();
        long photoInterp = photoInterpEntry.asLong();
        long rowsPerStrip = rowsPerStripEntry.asLong();
        long planarConfig = planarConfigEntry.asLong();
        int[] bitsPerSample = getBitsPerSample(bitsPerSampleEntry);
        long[] stripOffsets = getStripsArray(stripOffsetsEntry);
        long[] stripCounts = getStripsArray(stripCountsEntry);
        
         
        // TODO: deal with samples-sizes other than byte (?)
        // make sure a DataBufferByte is going to do the trick
        for (int i = 0; i < bitsPerSample.length; i++) {
            if (bitsPerSample[i] != 8)
                throw new IIOException(this.getClass().getName() + ".read(): only expecting 8 bits/sample; found " +
                        bitsPerSample[i]);        
        }
        
        ColorSpace colorSpace = (samplesPerPixel > 1) ? 
            ColorSpace.getInstance(ColorSpace.CS_sRGB) :
            ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int transparency = Transparency.OPAQUE;
        boolean hasAlpha = false;
        if (samplesPerPixel == 4) {
            transparency = Transparency.TRANSLUCENT;
            hasAlpha = true;
        }
        
        int[] bankOffsets = new int[samplesPerPixel];
        for (int i=0; i<samplesPerPixel; i++) bankOffsets[i] = i;
        int[] offsets = new int[(planarConfig == TiffConstants.PLANARCONFIG_CHUNKY) ? 1 : samplesPerPixel];
        for (int i=0; i<offsets.length; i++) offsets[i] = 0;
        
        ComponentColorModel colorModel = new ComponentColorModel(colorSpace, bitsPerSample, hasAlpha, false,
                transparency, DataBuffer.TYPE_BYTE);
        
        ComponentSampleModel sampleModel;
        if (samplesPerPixel == 1) 
            sampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, bankOffsets);
        else
            sampleModel = (planarConfig == TiffConstants.PLANARCONFIG_CHUNKY) ?
                new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, samplesPerPixel, 
                    width*samplesPerPixel, bankOffsets) :
                new BandedSampleModel(DataBuffer.TYPE_BYTE, width, height, width, bankOffsets, offsets);
        
        byte[][] imageData;
        if (planarConfig == TiffConstants.PLANARCONFIG_CHUNKY)
            imageData = readPixelInterleaved(width, height, samplesPerPixel, stripOffsets, stripCounts);
        else
            imageData = readPlanar(width, height, samplesPerPixel, stripOffsets, stripCounts, rowsPerStrip);
        

        DataBufferByte dataBuff = new DataBufferByte(imageData, width*height, offsets);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuff, new Point(0,0));
        return new BufferedImage(colorModel, raster, false, null);
    }

    /*
     * Coordinates reading all the ImageFileDirectories in a Tiff file (there's typically only one).
     * 
     */
    private void readIFDs() throws IOException {
        if (this.theStream != null) 
            return;

        if (super.input == null || !(super.input instanceof ImageInputStream)) {
            throw new IIOException(this.getClass().getName() + ": null/invalid ImageInputStream");
        }
        this.theStream = (ImageInputStream) super.input;

        // determine byte ordering...
        byte[] ifh = new byte[2];  // Tiff image-file header
        try {
            theStream.readFully(ifh);
            if (ifh[0] == 0x4D && ifh[1] == 0x4D) {
                theStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            } else if (ifh[0] == 0x49 && ifh[1] == 0x49) {
                theStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            } else {
                throw new IOException();
            }
        } catch (IOException ex) {
            throw new IIOException(this.getClass().getName() + ": error reading signature");
        }
        
        // skip the magic number and get offset to first (and likely only) ImageFileDirectory...
        theStream.readFully(ifh);
        long ifdOffset = theStream.readUnsignedInt();
        readIFD(ifdOffset); 
    }
    
    /*
     * Reads an ImageFileDirectory and places it in our list.  Calls itself recursively if additional
     * IFDs are indicated.
     *
     */
    private void readIFD(long offset) throws IIOException {
        try {
            theStream.seek(offset);
            int numEntries = theStream.readUnsignedShort();
            TiffIFDEntry[] ifd = new TiffIFDEntry[numEntries];
            byte[] valoffset = new byte[4];
            for (int i=0; i<numEntries; i++) {
                int tag = theStream.readUnsignedShort();
                int type = theStream.readUnsignedShort();
                long count = theStream.readUnsignedInt();
                theStream.readFully(valoffset);
                ifd[i] = new TiffIFDEntry(tag, type, count, valoffset);
            }
            
            ifds.add(ifd);
            
            /****** TODO: UNCOMMENT;  IN GENERAL, THERE CAN BE MORE THAN ONE IFD IN A TIFF FILE
            long nextIFDOffset = theStream.readUnsignedInt();
            if (nextIFDOffset > 0)
                readIFD(nextIFDOffset);
             */
            
        } catch (Exception ex) {
            throw new IIOException("Error reading Tiff IFD: " + ex.getMessage());
        }
    }
    
    /*
     * Reads image data organized as a singular image plane (and pixel interleaved, in the case of color images).
     * 
     */
    private byte[][] readPixelInterleaved(int width, int height, int samplesPerPixel, 
           long[] stripOffsets, long[] stripCounts) throws IOException 
    {
        byte[][] data = new byte[1][width * height * samplesPerPixel];
        int offset = 0;
        for (int i = 0; i < stripOffsets.length; i++) {
            this.theStream.seek(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((offset + len) >= data[0].length)
                len = data[0].length - offset;
            this.theStream.readFully(data[0], offset, len);
            offset += stripCounts[i];
        }
        return data;
    }

    /*
     * Reads image data organized as separate image planes.
     * 
     */
    private byte[][] readPlanar(int width, int height, int samplesPerPixel,
            long[] stripOffsets, long[] stripCounts, long rowsPerStrip) throws IOException 
    {
        byte[][] data = new byte[samplesPerPixel][width * height];
        int band = 0;
        int offset = 0;
        int numRows = 0;
        for (int i = 0; i < stripOffsets.length; i++) {
            this.theStream.seek(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((offset+len) >= data[band].length) len = data[band].length - offset;
            this.theStream. readFully(data[band], offset, len);
            offset += stripCounts[i];
            numRows += rowsPerStrip;
            if (numRows >= height) {
                ++band;
                numRows = 0;
                offset = 0;
            }
        }
        
        return data;
    }

    /*
     * Returns the (first!) IFD-Entry with the given tag, or null if not found.
     * 
     */
    private TiffIFDEntry getByTag(TiffIFDEntry[] ifd, int tag) {
        for (int i = 0; i < ifd.length; i++) {
            if (ifd[i].tag == tag) {
                return ifd[i];
            }
        }
        return null;
    }
    
    /*
     * Utility method intended to read the array of StripOffsets or StripByteCounts.
     */
    private long[] getStripsArray(TiffIFDEntry stripsEntry) throws IOException {
        long[] offsets = new long[(int)stripsEntry.count];
        long fileOffset = stripsEntry.asLong();
        this.theStream.seek(fileOffset);
        if (stripsEntry.type == TiffTypes.SHORT)
            for (int i=0; i<stripsEntry.count; i++) 
                offsets[i] = this.theStream.readUnsignedShort();
        else
            for (int i=0; i<stripsEntry.count; i++)
                offsets[i] = this.theStream.readUnsignedInt();
        return offsets;
    }

    /*
     * Utility to extract bitsPerSample info (if present). This is a bit tricky, because if the samples/pixel == 1,
     * the bitsPerSample will fit in the offset/value field of the ImageFileDirectory element. In contrast, when 
     * samples/pixel == 3, the 3 shorts that make up bitsPerSample don't fit in the offset/value field, so we have
     * to go track them down elsewhere in the file.  Finally, as bitsPerSample is optional for bilevel images,
     * we'll return something sane  if this tag is absent.
     */
    private int[] getBitsPerSample(TiffIFDEntry entry) throws IOException {
        if (entry == null) {
            return new int[]{1};
        }  // the default according to the Tiff6.0 spec.

        if (entry.count == 1) {
            return new int[]{(int)entry.asLong()};
        }
        
        long[] tmp = getStripsArray(entry);
        int[] bits = new int[tmp.length];
        for (int i=0; i<tmp.length; i++) {
            bits[i] = (int)tmp[i];
        }
        
        return bits;
    }

    private ImageInputStream theStream = null;
    private ArrayList<TiffIFDEntry[]> ifds = new ArrayList<TiffIFDEntry[]>(1);
}
