/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.rtf;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * RTF parser
 */
public class RTFParser implements Parser {

    private static final Set<MediaType> SUPPORTED_TYPES =
        Collections.singleton(MediaType.application("rtf"));

    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        try {
            DefaultStyledDocument sd =
                new DefaultStyledDocument(new NoReclaimStyleContext());
            new RTFEditorKit().read(stream, sd, 0);

            XHTMLContentHandler xhtml =
                new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();
            xhtml.element("p", sd.getText(0, sd.getLength()));
            xhtml.endDocument();
        } catch (BadLocationException e) {
            throw new TikaException("Error parsing an RTF document", e);
        } catch (InternalError e) {
            throw new TikaException(
                    "Internal error parsing an RTF document, see TIKA-282", e);
        } catch (NoClassDefFoundError e) {
            if (Boolean.getBoolean("java.awt.headless")) {
                throw new TikaException(
                        "Unexpected RTF parsing error in headless mode", e);
            } else {
                throw new TikaException(
                        "GUI environment expected by the javax.swing.text.rtf"
                        + " package is not available, see JCR-386;"
                        + " try running with -Djava.awt.headless=true", e);
            }
        }
    }

    /**
     * @deprecated This method will be removed in Apache Tika 1.0.
     */
    public void parse(
            InputStream stream, ContentHandler handler, Metadata metadata)
            throws IOException, SAXException, TikaException {
        parse(stream, handler, metadata, new ParseContext());
    }

    /**
     * A workaround to
     * <a href="https://issues.apache.org/jira/browse/TIKA-282">TIKA-282</a>:
     * RTF parser expects a GUI environment. This class simply disables the
     * troublesome SwingUtilities.isEventDispatchThread() call that's made in
     * the {@link StyleContext#reclaim(AttributeSet)} method.
     */
    private static class NoReclaimStyleContext extends StyleContext {

        /** Ignored. */
        public void reclaim(AttributeSet a) {
        }

    }
}
