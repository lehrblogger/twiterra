/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.retrieve;

/**
 * @author Tom Gaskins
 * @version $Id: RetrievalFuture.java 2422 2007-07-25 23:07:49Z tgaskins $
 */
public interface RetrievalFuture extends java.util.concurrent.Future<Retriever>
{
    public Retriever getRetriever();
}
