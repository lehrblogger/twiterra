/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.zip.*;

/**
 * @author Tom Gaskins
 * @version $Id: URLRetriever.java 4598 2008-03-04 21:38:21Z tgaskins $
 */
public abstract class URLRetriever extends WWObjectImpl implements Retriever
{
    private volatile String state = RETRIEVER_STATE_NOT_STARTED;
    private volatile int contentLength = 0;
    private AtomicInteger contentLengthRead = new AtomicInteger(0);
    private volatile String contentType;
    private volatile ByteBuffer byteBuffer;
    private volatile URLConnection connection;
    private final URL url;
    private final RetrievalPostProcessor postProcessor;
    private int connectTimeout = Configuration.getIntegerValue(AVKey.URL_CONNECT_TIMEOUT, 8000);
    private int readTimeout = Configuration.getIntegerValue(AVKey.URL_READ_TIMEOUT, 5000);
    private int staleRequestLimit = -1;
    private long submitTime;
    private long beginTime;
    private long endTime;

    /**
     * @param url           the URL of the resource to retrieve.
     * @param postProcessor the retrieval post-processor to invoke when the resource is retrieved.
     * @throws IllegalArgumentException if <code>url</code> or <code>postProcessor</code> is null.
     */
    public URLRetriever(URL url, RetrievalPostProcessor postProcessor)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (postProcessor == null)
        {
            String message = Logging.getMessage("nullValue.PostProcessorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.url = url;
        this.postProcessor = postProcessor;
    }

    public final URL getUrl()
    {
        return url;
    }

    public final int getContentLength()
    {
        return this.contentLength;
    }

    protected void setContentLengthRead(int length)
    {
        this.contentLengthRead.set(length);
    }

    public final int getContentLengthRead()
    {
        return this.contentLengthRead.get();
    }

    public final String getContentType()
    {
        return this.contentType;
    }

    public final ByteBuffer getBuffer()
    {
        return this.byteBuffer;
    }

    public final String getName()
    {
        return this.url.toString();
    }

    public final String getState()
    {
        return this.state;
    }

    protected final URLConnection getConnection()
    {
        return this.connection;
    }

    public final RetrievalPostProcessor getPostProcessor()
    {
        return postProcessor;
    }

    public final int getConnectTimeout()
    {
        return connectTimeout;
    }

    public int getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    public int getStaleRequestLimit()
    {
        return staleRequestLimit;
    }

    public void setStaleRequestLimit(int staleRequestLimit)
    {
        this.staleRequestLimit = staleRequestLimit;
    }

    public final void setConnectTimeout(int connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public long getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(long submitTime)
    {
        this.submitTime = submitTime;
    }

    public long getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(long beginTime)
    {
        this.beginTime = beginTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public final Retriever call() throws Exception
    {
        if (this.interrupted())
            return this;

        try
        {
            this.setState(RETRIEVER_STATE_STARTED);

            if (!this.interrupted())
            {
                this.setState(RETRIEVER_STATE_CONNECTING);
                this.connection = this.openConnection();
            }

            if (!this.interrupted())
            {
                this.setState(RETRIEVER_STATE_READING);
                this.byteBuffer = this.read();
            }

            if (!this.interrupted())
                this.setState(RETRIEVER_STATE_SUCCESSFUL);

            WorldWind.getNetworkStatus().logAvailableHost(this.url);
        }
        catch (UnknownHostException e)
        {
            this.setState(RETRIEVER_STATE_ERROR);
            WorldWind.getNetworkStatus().logUnavailableHost(this.url);
            throw e;
        }
        catch (SocketException e)
        {
            this.setState(RETRIEVER_STATE_ERROR);
            WorldWind.getNetworkStatus().logUnavailableHost(this.url);
            throw e;
        }
        catch (Exception e)
        {
            this.setState(RETRIEVER_STATE_ERROR);
            if (!(e instanceof java.net.SocketTimeoutException))
            {
                Logging.logger().log(Level.SEVERE,
                    Logging.getMessage("URLRetriever.ErrorAttemptingToRetrieve", this.url.toString()), e);
            }
            throw e;
        }
        finally
        {
            this.end();
        }

        return this;
    }

    private void setState(String state)
    {
        String oldState = this.state;
        this.state = state;
        this.firePropertyChange(AVKey.RETRIEVER_STATE, oldState, this.state);
    }

    private boolean interrupted()
    {
        if (Thread.currentThread().isInterrupted())
        {
            this.setState(RETRIEVER_STATE_INTERRUPTED);
            String message = Logging.getMessage("URLRetriever.RetrievalInterruptedFor", this.url.toString());
            Logging.logger().fine(message);
            return true;
        }
        return false;
    }

    private URLConnection openConnection() throws IOException
    {
        try
        {
            Proxy proxy = WWIO.configureProxy();
            if (proxy != null)
                this.connection = this.url.openConnection(proxy);
            else
                this.connection = this.url.openConnection();
        }
        catch (java.io.IOException e)
        {
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("URLRetriever.ErrorOpeningConnection", this.url.toString()), e);
            throw e;
        }

        if (this.connection == null) // java.net.URL docs imply that this won't happen. We check anyway.
        {
            String message = Logging.getMessage("URLRetriever.NullReturnedFromOpenConnection", this.url);
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.connection.setConnectTimeout(this.connectTimeout);
        this.connection.setReadTimeout(this.readTimeout);

        return connection;
    }

    private void end() throws Exception
    {
        try
        {
            if (this.postProcessor != null)
            {
                this.byteBuffer = this.postProcessor.run(this);
            }
        }
        catch (Exception e)
        {
            this.setState(RETRIEVER_STATE_ERROR);
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("URLRetriever.ErrorPostProcessing", this.url.toString()), e);
            throw e;
        }
    }

    private ByteBuffer read() throws Exception
    {
        try
        {
            ByteBuffer buffer = this.doRead(this.connection);
            if (buffer == null)
                this.contentLength = 0;
            return buffer;
        }
        catch (Exception e)
        {
            if (!(e instanceof java.net.SocketTimeoutException || e instanceof UnknownHostException
                || e instanceof SocketException))
            {
                Logging.logger().log(Level.SEVERE,
                    Logging.getMessage("URLRetriever.ErrorReadingFromConnection", this.url.toString()), e);
            }
            throw e;
        }
    }

    /**
     * @param connection the connection to read from.
     * @return a buffer containing the content read from the connection
     * @throws Exception                if <code>connection</code> is null or an exception occurs during reading.
     * @throws IllegalArgumentException if <code>connection</code> is null
     */
    protected ByteBuffer doRead(URLConnection connection) throws Exception
    {
        if (connection == null)
        {
            String msg = Logging.getMessage("nullValue.ConnectionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.contentLength = this.connection.getContentLength();

        ByteBuffer buffer;
        InputStream inputStream = null;
        try
        {
            inputStream = this.connection.getInputStream();
            if (inputStream == null)
            {
                Logging.logger().log(Level.SEVERE, "URLRetriever.InputStreamFromConnectionNull", connection.getURL());
                return null;
            }

            // TODO: Make decompression of zip file configurable
            // TODO: Make this more generally configurable based on content type
            // todo: make a zip reader that handles streams of unknown length
            // TODO: add a gzip reader
            // TODO: this code is checking content type for compression when it should be checking content encoding,
            // but the WW servers are sending application/zip as the content type, and null for the content encoding.
            this.contentType = connection.getContentType();
            if (this.contentType != null && this.contentType.equalsIgnoreCase("application/zip"))
                buffer =
                    this.readZipStream(inputStream, connection.getURL()); // assume single file in zip and decompress it
            else
                buffer = this.readNonSpecificStream(inputStream, connection);

            this.contentType = connection.getContentType();
        }
        finally
        {
            if (inputStream != null)
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                    Logging.logger().log(Level.SEVERE, "URLRetriever.ExceptionClosingInputStreamToConnection",
                        connection.getURL());
                }
        }

        return buffer;
    }

    private ByteBuffer readNonSpecificStream(InputStream inputStream, URLConnection connection) throws IOException
    {
        if (inputStream == null)
        {
            String message = Logging.getMessage("URLRetriever.InputStreamNullFor", connection.getURL());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.contentLength < 1)
        {
            return readNonSpecificStreamUnknownLength(inputStream);
        }

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocate(this.contentLength);

        int numBytesRead = 0;
        while (!this.interrupted() && numBytesRead >= 0 && numBytesRead < buffer.limit())
        {
            int count = channel.read(buffer);
            if (count > 0)
                this.contentLengthRead.getAndAdd(numBytesRead += count);
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    private ByteBuffer readNonSpecificStreamUnknownLength(InputStream inputStream) throws IOException
    {
        final int pageSize = (int) Math.ceil(Math.pow(2, 15));

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);

        int count = 0;
        int numBytesRead = 0;
        while (!this.interrupted() && count >= 0)
        {
            count = channel.read(buffer);
            if (count > 0)
                this.contentLengthRead.getAndAdd(numBytesRead += count);

            if (count > 0 && !buffer.hasRemaining())
            {
                ByteBuffer biggerBuffer = ByteBuffer.allocate(buffer.limit() + pageSize);
                biggerBuffer.put((ByteBuffer) buffer.rewind());
                buffer = biggerBuffer;
            }
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    /**
     * @param inputStream a stream to the zip connection.
     * @param url         the URL of the zip resource.
     * @return a buffer containing the content read from the zip stream.
     * @throws java.io.IOException if the stream does not refer to a zip resource or an exception occurs during reading.
     * @throws IllegalArgumentException if <code>inputStream</code> is null
     */
    private ByteBuffer readZipStream(InputStream inputStream, URL url) throws IOException
    {
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry ze = zis.getNextEntry();
        if (ze == null)
        {
            Logging.logger().severe(Logging.getMessage("URLRetriever.NoZipEntryFor") + url);
            return null;
        }

        ByteBuffer buffer = null;
        if (ze.getSize() > 0)
        {
            buffer = ByteBuffer.allocate((int) ze.getSize());

            byte[] inputBuffer = new byte[8192];
            while (buffer.hasRemaining())
            {
                int count = zis.read(inputBuffer);
                if (count > 0)
                {
                    buffer.put(inputBuffer, 0, count);
                    this.contentLengthRead.getAndAdd(buffer.position() + 1);
                }
            }
        }
        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final URLRetriever that = (URLRetriever) o;

        // Retrievers are considered identical if they are for the same URL. This convention is used by the
        // retrieval service to filter out duplicate retreival requests.
        return !(url != null ? !url.toString().contentEquals(that.url.toString()) : that.url != null);
    }

    @Override
    public int hashCode()
    {
        int result;
        result = (url != null ? url.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return this.getName() != null ? this.getName() : super.toString();
    }
}
