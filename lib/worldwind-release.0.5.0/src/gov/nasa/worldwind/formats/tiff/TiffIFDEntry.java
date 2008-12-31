/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * A bag for holding individual entries from a Tiff ImageFileDirectory.
 * 
 * @author brownrigg
 * @version $Id$
 */
public class TiffIFDEntry {
    
    public TiffIFDEntry(int tag, int type, long count, byte[] values) throws IllegalArgumentException {
        this.tag = tag;
        this.type = type;
        this.count = count;
        if (values == null || values.length != 4) 
            throw new IllegalArgumentException("bogus value/offset bytes passed to " + this.getClass().getName());
        
        this.values = new byte[4];
        this.values[0] = values[0];  // unroll the loop
        this.values[1] = values[1];
        this.values[2] = values[2];
        this.values[3] = values[3];
    }
    
    public long asLong() throws IllegalStateException {
        if (this.type != TiffTypes.SHORT && this.type != TiffTypes.LONG)
            throw new IllegalStateException("Attempt to access Tiff IFD-entry as int: tag/type=" + 
                    Long.toHexString(tag) + "/" + type);
        return ((0x000000ff & this.values[3]) << 24) + 
                ((0x000000ff & this.values[2]) << 16) + 
                ((0x000000ff & this.values[1]) << 8) + 
                (0x000000ff & this.values[0]);
    }
    
    int tag;
    int type;
    long count;
    byte[] values = new byte[4];
}
