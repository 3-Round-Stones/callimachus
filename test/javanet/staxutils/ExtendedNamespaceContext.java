/*
         * $Id: ExtendedNamespaceContext.java,v 1.1 2004/07/05 23:10:46 cniles Exp $
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

        import java.util.Iterator;

        import javax.xml.namespace.NamespaceContext;

        /**
         * Extended namespace context interface that allows the context tree to be navigated
         * and to list all known prefixes.
         * 
         * @author Christian Niles
         * @version $Revision: 1.1 $
         */
        public interface ExtendedNamespaceContext extends NamespaceContext {

            /**
             * Returns a reference to the parent of this context.
             * 
             * @return The parent context, or <code>null</code> if this is a root
             * 		context.
             */
            public NamespaceContext getParent();

            /**
             * Determines if the specified prefix is declared within this context,
             * irrespective of any ancestor contexts.
             * 
             * @param prefix The prefix to check.
             * @return <code>true</code> if the prefix is declared in this context,
             * 		<code>false</code> otherwise.
             */
            public boolean isPrefixDeclared(String prefix);

            /**
             * Returns an {@link Iterator} of all namespace prefixes in scope within this
             * context, including those inherited from ancestor contexts.
             * 
             * @return An {@link Iterator} of prefix {@link String}s.
             */
            public Iterator getPrefixes();

            /**
             * Returns an {@link Iterator} of all namespace prefixes declared within
             * this context, irrespective of any ancestor contexts.
             * 
             * @return An {@link Iterator} of prefix {@link String}s.
             */
            public Iterator getDeclaredPrefixes();

        }