/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
 
package org.callimachusproject.io;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Stack;

/**
 * A character-stream reader that allows characters, strings and even the
 * contents of streams and readers to be pushed back into the stream. This class
 * resembles java.io.PushbackReader but its pushback buffer has 'infinite' room
 * for more characters.
 */
public class PushbackReader extends Reader {

    /**
     * A stack of Readers which should be read top-down. Data that is pushed
     * back appears as a reader on the top of the stack.
     */
    private Stack<Reader> readerStack = new Stack<Reader>();

    /**
     * Creates a new PushbackReader.
     */
    public PushbackReader() {
        super();
    }

    /**
     * Creates a new PushbackReader.
     *
     * @param r
     *        The first reader to be pushed back
     */
    public PushbackReader(Reader r) {
        this();
        pushback(r);
    }

    /**
     * Push the contents of a stream back into the stream.
     *
     * @param r
     *        The reader to push back
     */
    public void pushback(Reader r) {
        readerStack.push(r);
    }

    /**
     * Push a string back into the stream.
     *
     * @param s
     *        The String to push back
     */
    public void pushback(String s) {
        pushback(new StringReader(s));
    }

    /**
     * Push a character array back into the stream.
     *
     * @param chars
     *        The characters to push back
     */
    public void pushback(char[] chars) {
        pushback(new CharArrayReader(chars));
    }

    /**
     * Push a part of a character array back into the stream.
     *
     * @param chars
     *        The character array
     * @param off
     *        Offset of first character to push back
     * @param len
     *        Number of characters to push back
     */
    public void pushback(char[] chars, int off, int len) {
        pushback(new CharArrayReader(chars, off, len));
    }

    @Override
    public int read() throws IOException {
        int c = -1;

        while (c == -1 && !readerStack.empty()) {
            Reader r = readerStack.peek();
            c = r.read();

            if (c == -1) {
                readerStack.pop();
            }
        }

        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int readSoFar = 0;

        while (readSoFar < len && !readerStack.empty()) {
            Reader r = readerStack.peek();
            int nofChars = r.read(cbuf, off + readSoFar, len - readSoFar);

            if (nofChars == -1) {
                readerStack.pop();
            }
            else {
                readSoFar += nofChars;
            }
        }

        if (readSoFar == 0 && readerStack.empty()) {
            return -1;
        }

        return readSoFar;
    }

    @Override
    public void close()
        throws IOException {
        while (!readerStack.empty()) {
            Reader r = readerStack.pop();
            r.close();
        }
    }
}