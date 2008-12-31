/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * Symbolic constants for the "type" fields found in ImageFileDirectory entries.
 * 
 * @author brownrigg
 * @version $Id$
 */
public interface TiffTypes {
    public static final int BYTE      = 1;
    public static final int ASCII     = 2;
    public static final int SHORT     = 3;
    public static final int LONG      = 4;
    public static final int RATIONAL  = 5;
    public static final int SBYTE     = 6;
    public static final int UNDEFINED = 7;
    public static final int SSHORT    = 8;
    public static final int SLONG     = 9;
    public static final int SRATIONAL = 10;
    public static final int FLOAT     = 11;
    public static final int DOUBLE    = 12;
}
