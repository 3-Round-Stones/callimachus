/*
 * $Id: BaseXMLEventReader.java,v 1.1 2004/07/05 23:13:31 cniles Exp $
 * 
 * Copyright (c) 2004, Christian Niles, unit12.net
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
package javanet.staxutils;

import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

/**
 * Abstract base class for {@link XMLEventReader} implementations.
 *
 * @author Christian Niles
 * @version $Revision: 1.1 $
 */
public abstract class BaseXMLEventReader implements XMLEventReader {

    /** Whether we've been closed or not. */
    protected boolean closed;

    public synchronized String getElementText() throws XMLStreamException {

        if (closed) {

            throw new XMLStreamException("Stream has been closed");

        }

        // TODO At the moment, this simply coalesces all Characters events up to a
        // terminal EndElement event.
        StringBuffer buffer = new StringBuffer();
        while (true) {

            XMLEvent event = nextEvent();
            if (event.isCharacters()) {

                // don't return ignorable whitespace
                if (event.getEventType() != XMLEvent.SPACE) {

                    buffer.append(event.asCharacters().getData());

                }

            } else if (event.isEndElement()) {

                break;

            } else {

                throw new XMLStreamException(
                        "Non-text event encountered in getElementText(): "
                                + event);

            }

        }

        return buffer.toString();

    }

    public XMLEvent nextTag() throws XMLStreamException {

        if (closed) {

            throw new XMLStreamException("Stream has been closed");

        }

        XMLEvent event;
        do {

            if (hasNext()) {

                event = nextEvent();
                if (event.isStartElement() || event.isEndElement()) {

                    return event;

                } else if (event.isCharacters()) {

                    if (!event.asCharacters().isWhiteSpace()) {

                        throw new XMLStreamException(
                                "Non-ignorable space encountered");

                    }

                } else if (!(event instanceof Comment)) {

                    throw new XMLStreamException(
                            "Non-ignorable event encountered: " + event);

                }

            } else {

                throw new XMLStreamException("Ran out of events in nextTag()");

            }

        } while (!event.isStartElement() && !event.isEndElement());

        return event;

    }

    public Object getProperty(String name) throws IllegalArgumentException {

        throw new IllegalArgumentException("Property not supported: " + name);

    }

    public synchronized void close() throws XMLStreamException {

        if (!closed) {

            closed = true;

        }

    }

    public Object next() {

        try {

            return nextEvent();

        } catch (XMLStreamException e) {

            NoSuchElementException ex = new NoSuchElementException(
                    "Error getting next event");
            ex.initCause(e);
            throw ex;

        }

    }

    public void remove() {

        throw new UnsupportedOperationException();

    }

}