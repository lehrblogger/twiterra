package gov.nasa.worldwind.formats.nitfs;

import gov.nasa.worldwind.formats.rpf.RPFFrameFileComponents;
import gov.nasa.worldwind.util.StringUtil;
/*
Copyright (C) 2001, 2007 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

/**
 * @author Lado Garakanidze
 * @version $Id: UserDefinedImageSubheader Mar 31, 2007 9:42:33 PM
 */
public class UserDefinedImageSubheader
{
    public short getOverflow()
    {
        return this.overflow;
    }

    public String getTag()
    {
        return this.tag;
    }

    public int getDataLength()
    {
        return this.dataLength;
    }

    public RPFFrameFileComponents getRPFFrameFileComponents()
    {
        return this.rpfFrameFileComponents;
    }

    private RPFFrameFileComponents rpfFrameFileComponents = null;

    private short    overflow;
    private String   tag;
    private int      dataLength;

    public UserDefinedImageSubheader(java.nio.ByteBuffer buffer) throws NITFSRuntimeException {
        this.overflow = NITFSUtil.getShortNumeric(buffer, 3);
        this.tag = NITFSUtil.getString(buffer, 6);
        this.dataLength = NITFSUtil.getShortNumeric(buffer, 5);

        if(0 < this.dataLength)
        {
            if(StringUtil.Equals(tag, RPFFrameFileComponents.DATA_TAG))
                this.rpfFrameFileComponents = new RPFFrameFileComponents(buffer);
            else
                throw new NITFSRuntimeException("NITFSReader.UnknownOrUnsupportedUserDefinedImageSubheader");
        }
    }
}
