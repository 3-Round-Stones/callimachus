/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.fluid.consumers.helpers;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * CharArrayReader is used as a buffered character input stream on a character
 * array.
 */
public class CharSequenceReader extends Reader {
    /**
     * Buffer for characters
     */
    protected CharBuffer buf;

    /**
     * Construct a CharSequenceReader on the CharSequence <code>buffer</code>. The
     * size of the reader is set to the <code>length()</code> of the buffer
     * and the Object to synchronize access through is set to
     * <code>buffer</code>.
     * 
     * @param buf
     *            the CharSequence to filter reads on.
     */
    public CharSequenceReader(CharSequence buf) {
        super(buf);
        this.buf = CharBuffer.wrap(buf);
    }

    /**
     * This method closes this CharArrayReader. Once it is closed, you can no
     * longer read from it. Only the first invocation of this method has any
     * effect.
     */
    @Override
    public void close() {
        synchronized (lock) {
            if (isOpen()) {
                buf = null;
            }
        }
    }

    /**
     * Answer a boolean indicating whether or not this CharArrayReader is open.
     * 
     * @return <code>true</code> if the reader is open, <code>false</code>
     *         otherwise.
     */
    private boolean isOpen() {
        return buf != null;
    }

    /**
     * Answer a boolean indicating whether or not this CharArrayReader is
     * closed.
     * 
     * @return <code>true</code> if the reader is closed, <code>false</code>
     *         otherwise.
     */
    private boolean isClosed() {
        return buf == null;
    }

    /**
     * Set a Mark position in this Reader. The parameter <code>readLimit</code>
     * is ignored for CharArrayReaders. Sending reset() will reposition the
     * reader back to the marked position provided the mark has not been
     * invalidated.
     * 
     * @param readLimit
     *            ignored for CharArrayReaders.
     * 
     * @throws IOException
     *             If an error occurs attempting to mark this CharArrayReader.
     */
    @Override
    public void mark(int readLimit) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            buf.mark();
        }
    }

    /**
     * Answers a boolean indicating whether or not this CharArrayReader supports
     * mark() and reset(). This method always returns true.
     * 
     * @return indicates whether or not mark() and reset() are supported.
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Reads a single character from this CharArrayReader and returns the result
     * as an int. The 2 higher-order bytes are set to 0. If the end of reader
     * was encountered then return -1.
     * 
     * @return int the character read or -1 if end of reader.
     * 
     * @throws IOException
     *             If the CharArrayReader is already closed.
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get();
        }
    }

    /**
     * Reads at most <code>count</code> characters from this CharArrayReader
     * and stores them at <code>offset</code> in the character array
     * <code>buf</code>. Returns the number of characters actually read or -1
     * if the end of reader was encountered.
     * 
     * 
     * @param buffer
     *            character array to store the read characters
     * @param offset
     *            offset in buf to store the read characters
     * @param len
     *            maximum number of characters to read
     * @return number of characters read or -1 if end of reader.
     * 
     * @throws IOException
     *             If the CharArrayReader is closed.
     */
    @Override
    public int read(char buffer[], int offset, int len) throws IOException {
        // avoid int overflow
        if (offset < 0 || offset > buffer.length || len < 0
                || len > buffer.length - offset) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            if (buf.hasRemaining()) {
                int bytesRead = len > buf.remaining() ? buf.remaining() : len;
                buf.get(buffer, offset, bytesRead);
                return bytesRead;
            }
            return -1;
        }
    }

    /**
     * Answers a <code>boolean</code> indicating whether or not this
     * CharArrayReader is ready to be read without blocking. If the result is
     * <code>true</code>, the next <code>read()</code> will not block. If
     * the result is <code>false</code> this Reader may or may not block when
     * <code>read()</code> is sent. The implementation in CharArrayReader
     * always returns <code>true</code> even when it has been closed.
     * 
     * @return <code>true</code> if the receiver will not block when
     *         <code>read()</code> is called, <code>false</code> if unknown
     *         or blocking will occur.
     * 
     * @throws IOException
     *             If the CharArrayReader is closed.
     */
    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            return buf.hasRemaining();
        }
    }

    /**
     * Reset this CharArrayReader's position to the last <code>mark()</code>
     * location. Invocations of <code>read()/skip()</code> will occur from
     * this new location. If this Reader was not marked, the CharArrayReader is
     * reset to the beginning of the String.
     * 
     * @throws IOException
     *             If this CharArrayReader has already been closed.
     */
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            buf.reset();
        }
    }

    /**
     * Skips <code>count</code> number of characters in this CharArrayReader.
     * Subsequent <code>read()</code>'s will not return these characters
     * unless <code>reset()</code> is used.
     * 
     * @param n
     *            The number of characters to skip.
     * @return long The number of characters actually skipped.
     * 
     * @throws IOException
     *             If this CharArrayReader has already been closed.
     */
    @Override
    public long skip(long n) throws IOException {
        synchronized (lock) {
            if (isClosed()) {
                throw new IOException("Reader already closed");
            }
            if (n <= 0) {
                return 0;
            }
            long skipped = 0;
            if (n < buf.limit() - buf.position()) {
            	buf.position(buf.position() + (int) n);
                skipped = n;
            } else {
                skipped = buf.limit() - buf.position();
                buf.position(buf.limit());
            }
            return skipped;
        }
    }
}
