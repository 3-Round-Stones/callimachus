package javanet.staxutils;

/*
 * $Id: XMLEventPipe.java,v 1.2 2004/07/05 23:46:51 cniles Exp $
 * 
 * Copyright (c) 2004, Christian Niles, Unit12
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *		*   Redistributions of source code must retain the above copyright
 *          notice, this list of conditions and the following disclaimer.
 * 
 *	    *	Redistributions in binary form must reproduce the above copyright
 *          notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution.
 * 
 *      *   Neither the name of Christian Niles, Unit12, nor the names of its
 *          contributors may be used to endorse or promote products derived from
 *          this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javanet.staxutils.BaseXMLEventReader;
import javanet.staxutils.BaseXMLEventWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Provides the ability to pipe the {@link XMLEvent}s written to one
 * {@link XMLEventWriter} to be read from a {@link XMLEventReader}. The
 * implementation is based on a bounded-buffer with a specifiable maximum
 * capacity. When that capacity is reached, the write end of the pipe will
 * block until events are read from the read end. Similarly, when an attempt is
 * made to read from an empty queue, the operation will block until more events
 * are written to the buffer. The write end of the pipe will repair namespaces
 * and buffer attribute/namespace events as defined in the specification of
 * the {@link XMLEventWriter} interface.
 * <br /><br />
 * Both the read and write ends of this pipe are fully synchronized to allow
 * multiple threads to read or write events to the pipe. However, care must be
 * taken that the order of events is consistent, and that the stream is properly
 * closed when writing is complete. <b>If the write end is never closed the read
 * end may block indefinitely, waiting for further events.</b> To help prevent
 * this, the write end will automatically close when an END_DOCUMENT event is
 * written.
 * <br /><br />
 * To properly obey the expected behaviour of {@link XMLEventReader} and
 * {@link javax.xml.stream.XMLStreamWriter}, methods such as 
 * {@link XMLEventReader#peek()} and {@link XMLEventReader#hasNext()} may block. 
 * This is necessary to prevent {@link XMLEventReader#hasNext()} from returning
 * <code>true</code> just before the write end is closed, or <code>false</code> 
 * just before additional events are added. If the read end is closed before the
 * writer, then the write end will silently discard all elements written to it 
 * until it is closed.
 * 
 * @author Christian Niles
 * @version $Revision: 1.2 $
 */
public class XMLEventPipe {

    /** 
     * Default maximum number of events that may be stored by this pipe until
     * the write end blocks.
     */
    public static final int QUEUE_CAPACITY = 16;

    /** List of events ready to be read. */
    private List eventQueue = new LinkedList();

    /** The maximum capacity of the queue, after which the pipe should block. */
    private int capacity = QUEUE_CAPACITY;

    /** Whether the read end has been closed. */
    private boolean readEndClosed;

    /** Whether the write end has been closed. */
    private boolean writeEndClosed;

    /** 
     * The read end of the pipe. This will be <code>null</code> until
     * {@link #getReadEnd()} is called for the first time.
     */
    private PipedXMLEventReader readEnd = new PipedXMLEventReader(this );

    /** 
     * The write end of the pipe. This will be <code>null</code> until
     * {@link #getWriteEnd()} is called for the first time.
     */
    private PipedXMLEventWriter writeEnd = new PipedXMLEventWriter(this );

    /**
     * Constructs a new XMLEventPipe with the default capacity.
     */
    public XMLEventPipe() {
    }

    /**
     * Constructs a new XMLEventPipe with the specified capacity.
     * 
     * @param capacity The number of events to buffer until the pipe will block.
     * 		A number less than or equal to 0 means the pipe will buffer an
     * 		unbounded number of events.
     */
    public XMLEventPipe(int capacity) {

        this .capacity = capacity;

    }

    /**
     * Returns the read end of the pipe, from which events written to the write
     * end of the pipe will be available.
     * 
     * @return The read end of the pipe.
     */
    public synchronized XMLEventReader getReadEnd() {

        if (readEnd == null) {

            readEnd = new PipedXMLEventReader(this );

        }

        return readEnd;

    }

    /**
     * Returns the write end of the pipe, whose events will be available from
     * the read end of this pipe.
     * 
     * @return The write end of the pipe.
     */
    public synchronized XMLEventWriter getWriteEnd() {

        if (writeEnd == null) {

            writeEnd = new PipedXMLEventWriter(this );

        }

        return writeEnd;

    }

    /**
     * {@link XMLEventWriter} implementation used to provide the write end of
     * the pipe.
     * 
     * @author christian
     * @version $Revision: 1.2 $
     */
    private static final class PipedXMLEventWriter extends
            BaseXMLEventWriter {

        /** The pipe we're connected to. */
        private XMLEventPipe pipe;

        public PipedXMLEventWriter(XMLEventPipe pipe) {

            this .pipe = pipe;

        }

        public synchronized void close() throws XMLStreamException {

            super .close();

            synchronized (pipe) {

                if (pipe.readEndClosed) {

                    pipe.eventQueue.clear();

                }

                pipe.writeEndClosed = true;
                pipe.notifyAll();

            }

        }

        protected void sendEvent(XMLEvent event)
                throws XMLStreamException {

            synchronized (pipe) {

                if (pipe.readEndClosed) {

                    // if read end is closed, throw away event
                    return;

                }

                if (pipe.capacity > 0) {

                    while (pipe.eventQueue.size() >= pipe.capacity) {

                        try {

                            pipe.wait();

                        } catch (InterruptedException e) {

                            e.printStackTrace();

                        }

                    }

                }

                pipe.eventQueue.add(event);
                if (pipe.eventQueue.size() == 1) {

                    pipe.notifyAll();

                }

                if (event.isEndDocument()) {

                    close();

                }

            }

        }

    }

    /**
     * {@link XMLEventReader} implementation used to provide the read end of
     * the pipe.
     * 
     * @author christian
     * @version $Revision: 1.2 $
     */
    private static final class PipedXMLEventReader extends
            BaseXMLEventReader {

        /** THe pipe this stream is connected to. */
        private XMLEventPipe pipe;

        public PipedXMLEventReader(XMLEventPipe pipe) {

            this .pipe = pipe;

        }

        public synchronized XMLEvent nextEvent()
                throws XMLStreamException {

            if (closed) {

                throw new XMLStreamException("Stream has been closed");

            }

            synchronized (pipe) {

                while (pipe.eventQueue.size() == 0) {

                    if (pipe.writeEndClosed) {

                        throw new NoSuchElementException(
                                "Stream has completed");

                    }

                    try {

                        pipe.wait();

                    } catch (InterruptedException e) {

                        e.printStackTrace();

                    }

                }

                boolean notify = pipe.capacity > 0
                        && pipe.eventQueue.size() >= pipe.capacity;

                // remove next event from the queue
                XMLEvent nextEvent = (XMLEvent) pipe.eventQueue
                        .remove(0);
                if (notify) {

                    pipe.notifyAll();

                }
                return nextEvent;

            }

        }

        public synchronized boolean hasNext() {

            if (closed) {

                return false;

            }

            synchronized (pipe) {

                while (pipe.eventQueue.size() == 0) {

                    if (pipe.writeEndClosed) {

                        break;

                    }

                    try {

                        pipe.wait();

                    } catch (InterruptedException e) {
                    }

                }

                return pipe.eventQueue.size() > 0;

            }

        }

        public synchronized XMLEvent peek() throws XMLStreamException {

            if (closed) {

                return null;

            }

            synchronized (pipe) {

                // wait until the queue has more events
                while (pipe.eventQueue.size() == 0) {

                    if (pipe.writeEndClosed) {

                        return null;

                    }

                    try {

                        pipe.wait();

                    } catch (InterruptedException e) {
                    }

                }

                return (XMLEvent) pipe.eventQueue.get(0);

            }

        }

        public synchronized void close() throws XMLStreamException {

            if (closed) {

                return;

            }

            synchronized (pipe) {

                pipe.readEndClosed = true;
                pipe.notifyAll();

            }

            super .close();

        }

        public void finalize() {

            if (!closed) {

                synchronized (pipe) {

                    pipe.readEndClosed = true;
                    pipe.notifyAll();

                }

            }

        }

    }

}

