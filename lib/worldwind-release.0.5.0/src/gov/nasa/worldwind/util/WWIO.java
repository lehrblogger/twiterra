/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

/**
 * @author Tom Gaskins
 * @version $Id: WWIO.java 5246 2008-05-01 01:41:02Z patrickmurris $
 */
package gov.nasa.worldwind.util;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class WWIO
{
    public static final String ILLEGAL_FILE_PATH_PART_CHARACTERS = "[" + "?/\\\\=+<>:;\\,\"\\|^\\[\\]" + "]";

    public static String formPath(String... pathParts)
    {
        StringBuilder sb = new StringBuilder();

        for (String pathPart : pathParts)
        {
            if (pathPart == null)
                continue;

            if (sb.length() > 0)
                sb.append(File.separator);
            sb.append(pathPart.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_"));
        }

        return sb.toString();
    }

    public static String stripIllegalFileNameCharacters(String s)
    {
        return s.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_");
    }

    public static boolean saveBuffer(ByteBuffer buffer, File file) throws IOException
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileOutputStream fos = null;
        FileChannel channel = null;
        FileLock lock;
        int numBytesWritten = 0;
        try
        {
            fos = new FileOutputStream(file);
            channel = fos.getChannel();
            lock = channel.tryLock();
            if (lock == null)
            {
                // The file is being written to, or some other process is keeping it to itself.
                // This is an okay condition, but worth noting.
                Logging.logger().log(Level.FINER, "WWIO.UnableToAcquireLockFor", file.getPath());
                return false;
            }

            for (buffer.rewind(); buffer.hasRemaining();)
            {
                numBytesWritten += channel.write(buffer);
            }

            channel.force(true);
            fos.flush();
            return true;
        }
        catch (IOException e)
        {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("WWIO.ErrorSavingBufferTo", file.getPath()), e);

            if (numBytesWritten > 0) // don't leave behind incomplete files
                file.delete();

            throw e;
        }
        finally
        {
            try
            {
                if (channel != null)
                    channel.close(); // also releases the lock
                else if (fos != null)
                    fos.close();
            }
            catch (java.io.IOException e)
            {
                Logging.logger().severe(Logging.getMessage("WWIO.ErrorTryingToClose", file.getPath()));
            }
        }
    }

    public static MappedByteBuffer mapFile(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream is = new FileInputStream(file);
        try
        {
            return is.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        }
        finally
        {
            is.close();
        }
    }

    public static ByteBuffer readURLContentToBuffer(URL url) throws IOException
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = url.openStream();
        return readStreamToBuffer(is);
    }

    public static ByteBuffer readFileToBuffer(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream is = new FileInputStream(file);
        try
        {
            FileChannel fc = is.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) fc.size());
            for (int count = 0; count >= 0 && buffer.hasRemaining();)
            {
                count = fc.read(buffer);
            }
            buffer.flip();
            return buffer;
        }
        finally
        {
            is.close();
        }
    }

    public static ByteBuffer readZipEntryToBuffer(File zipFile, String entryName) throws IOException
    {
        if (zipFile == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = null;
        ZipEntry ze;
        try
        {
            ZipFile zf = new ZipFile(zipFile);
            if (zf.size() < 1)
            {
                String message = Logging.getMessage("WWIO.ZipFileIsEmpty", zipFile.getPath());
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }

            if (entryName != null)
            {   // Read the specified entry
                ze = zf.getEntry(entryName);
                if (ze == null)
                {
                    String message = Logging.getMessage("WWIO.ZipFileEntryNIF", entryName, zipFile.getPath());
                    Logging.logger().severe(message);
                    throw new IOException(message);
                }
            }
            else
            {   // Read the first entry
                ze = zf.entries().nextElement(); // get the first entry
            }

            is = zf.getInputStream(ze);
            ByteBuffer buffer = null;
            if (ze.getSize() > 0)
            {
                buffer = transferStreamToByteBuffer(is, (int) ze.getSize());
                buffer.rewind();
            }
            return buffer;
        }
        finally
        {
            if (is != null)
                is.close();
        }
    }

    private static ByteBuffer transferStreamToByteBuffer(InputStream stream, int numBytes) throws IOException
    {
        if (stream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (numBytes < 1)
        {
            Logging.logger().severe("WWIO.NumberBytesTransferLessThanOne");
            throw new IllegalArgumentException(Logging.getMessage("WWIO.NumberBytesTransferLessThanOne"));
        }

        int bytesRead = 0;
        int count = 0;
        byte[] bytes = new byte[numBytes];
        while (count >= 0 && (numBytes - bytesRead) > 0)
        {
            count = stream.read(bytes, bytesRead, numBytes - bytesRead);
            if (count > 0)
            {
                bytesRead += count;
            }
        }
        ByteBuffer buffer = BufferUtil.newByteBuffer(bytes.length); // to get a jogl-compatible buffer
        return buffer.put(bytes);
    }

    public static ByteBuffer readStreamToBuffer(InputStream inputStream) throws IOException
    {
        final int PAGE_SIZE = 8192;

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(PAGE_SIZE);

        int count = 0;
        while (count >= 0)
        {
            count = channel.read(buffer);
            if (count > 0 && !buffer.hasRemaining())
            {
                ByteBuffer biggerBuffer = ByteBuffer.allocate(buffer.limit() + PAGE_SIZE);
                biggerBuffer.put((ByteBuffer) buffer.rewind());
                buffer = biggerBuffer;
            }
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    public static String replaceSuffix(String in, String newSuffix)
    {
        if (in == null)
        {
            String message = Logging.getMessage("nullValue.InputFileNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return in.substring(0, in.lastIndexOf(".")) + (newSuffix != null ? newSuffix : "");
    }

    public static File saveBufferToTempFile(ByteBuffer buffer, String suffix) throws IOException
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File outputFile = java.io.File.createTempFile("WorldWind", suffix != null ? suffix : "");
        outputFile.deleteOnExit();
        buffer.rewind();
        WWIO.saveBuffer(buffer, outputFile);

        return outputFile;
    }

    public static boolean isFileOutOfDate(URL url, long expiryTime)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            // Determine whether the file can be treated like a File, e.g., a jar entry.
            URI uri = url.toURI();
            if (uri.isOpaque())
                return false; // TODO: Determine how to check the date of non-Files

            File file = new File(uri);

            return file.exists() && file.lastModified() < expiryTime;
        }
        catch (URISyntaxException e)
        {
            Logging.logger().log(Level.SEVERE, "WWIO.ExceptionValidatingFileExpiration", url);
            return false;
        }
    }

    public static Proxy configureProxy()
    {
        String proxyHost = Configuration.getStringValue(AVKey.URL_PROXY_HOST);
        if (proxyHost == null)
            return null;

        Proxy proxy = null;

        try
        {
            int proxyPort = Configuration.getIntegerValue(AVKey.URL_PROXY_PORT);
            String proxyType = Configuration.getStringValue(AVKey.URL_PROXY_TYPE);

            SocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            if (proxyType.equals("Proxy.Type.Http"))
                proxy = new Proxy(Proxy.Type.HTTP, addr);
            else if (proxyType.equals("Proxy.Type.SOCKS"))
                proxy = new Proxy(Proxy.Type.SOCKS, addr);
        }
        catch (Exception e)
        {
            Logging.logger().log(Level.WARNING,
                Logging.getMessage("URLRetriever.ErrorConfiguringProxy", proxyHost), e);
        }

        return proxy;
    }

    public static String makeSuffixForMimeType(String mimeType)
    {
        if (mimeType == null)
        {
            String message = Logging.getMessage("nullValue.ImageFomat");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!mimeType.contains("/") || mimeType.endsWith("/"))
        {
            String message = Logging.getMessage("generic.InvalidImageFormat");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        String suffix = mimeType.substring(mimeType.lastIndexOf("/") + 1);

        return "." + suffix.replaceFirst("jpeg", "jpg"); // if jpeg, replace with conventional suffix
    }

    public static Object getFileOrResourceAsStream(String path, Class c)
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        File file = new File(path);
        if (file.exists())
        {
            try
            {
                return new FileInputStream(file);
            }
            catch (Exception e)
            {
                return e;
            }
        }

        if (c == null)
            c = WWIO.class.getClass();
        
        try
        {
            return c.getResourceAsStream("/" + path);
        }
        catch (Exception e)
        {
            return e;
        }
    }
}
