/*
         * $Id: StaticNamespaceContext.java,v 1.1 2004/07/08 14:29:42 cniles Exp $
         */
        package javanet.staxutils;

        import javax.xml.namespace.NamespaceContext;

        /**
         * Marker interface used to denote {@link NamespaceContext} implementations whose
         * state is not transient or dependent on external objects/events and will remain
         * stable unless explicitly updated.
         *
         * @author Christian Niles
         * @version $Revision: 1.1 $
         */
        public interface StaticNamespaceContext extends NamespaceContext {

        }
