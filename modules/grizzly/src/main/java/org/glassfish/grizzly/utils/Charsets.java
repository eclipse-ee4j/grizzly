/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.utils;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.ThreadCache.CachedTypeIndex;

/**
 * Charset utility class.
 *
 * @author Alexey Stashok
 */
public final class Charsets {
    static {
        if (Boolean.getBoolean(Charsets.class.getName() + ".preloadAllCharsets")) {
            preloadAllCharsets();
        }
    }
    
    /**
     * The default character encoding of this Java virtual machine.
     */
    public static final String DEFAULT_CHARACTER_ENCODING =
            Charset.defaultCharset().name();

    private static final ConcurrentMap<String, Charset> charsetAliasMap =
            new ConcurrentHashMap<>(8);

    public static final Charset ASCII_CHARSET = lookupCharset("ASCII");
    public static final Charset UTF8_CHARSET = lookupCharset("UTF-8");
    
    /**
     * Returns the default charset of this Java virtual machine.
     * @see Charset#defaultCharset()
     */
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    public static final int CODECS_CACHE_SIZE = 4;
    private static final CharsetCodecResolver DECODER_RESOLVER =
            new DecoderResolver();
    private static final CharsetCodecResolver ENCODER_RESOLVER =
            new EncoderResolver();
    
    private static final CachedTypeIndex<CodecsCache> CODECS_CACHE =
            ThreadCache.obtainIndex(CodecsCache.class, 1);
    
    private static volatile boolean areCharsetsPreloaded;
    
    /**
     * Lookup a {@link Charset} by name.
     * Fixes Charset concurrency issue (http://paul.vox.com/library/post/the-mysteries-of-java-character-set-performance.html)
     *
     * @param charsetName
     * @return {@link Charset}
     */
    public static Charset lookupCharset(final String charsetName) {
        final String charsetLowerCase = charsetName.toLowerCase(Locale.US);
        
        Charset charset = charsetAliasMap.get(charsetLowerCase);
        if (charset == null) {
            if (areCharsetsPreloaded) {
                // if all charsets are preloaded - throw Exception right away
                throw new UnsupportedCharsetException(charsetName);
            }
            
            final Charset newCharset = Charset.forName(charsetLowerCase);
            final Charset prevCharset =
                    charsetAliasMap.putIfAbsent(charsetLowerCase, newCharset);
            
            charset = prevCharset == null ? newCharset : prevCharset;
        }

        return charset;
    }
    
    /**
     * Preloads all {@link Charset}s available to the JMV, which makes charset
     * searching faster (at the cost of memory).  The speed gain is most noticable
     * in the case of non-existing charsets as it allows us to avoid an expensive
     * call to {@link Charset#forName(java.lang.String)}.
     */
    public static void preloadAllCharsets() {
        synchronized (charsetAliasMap) {
            final Map<String, Charset> charsetsMap = Charset.availableCharsets();
            for (Charset charset : charsetsMap.values()) {
                charsetAliasMap.put(
                        charset.name().toLowerCase(Locale.US), charset);
                for (String alias : charset.aliases()) {
                    charsetAliasMap.put(alias.toLowerCase(Locale.US), charset);
                }
            }
            
            areCharsetsPreloaded = true;
        }
    }
    
    /**
     * Remove all preloaded charsets.
     */
    public static void drainAllCharsets() {
        synchronized (charsetAliasMap) {
            areCharsetsPreloaded = false;
            charsetAliasMap.clear();
        }
    }    
    
    /**
     * Return the {@link Charset}'s {@link CharsetDecoder}.
     * The <tt>Charsets</tt> class maintains the {@link CharsetDecoder} thread-local
     * cache.  Be aware - this shouldn't be used by multiple threads.
     * 
     * @param charset {@link Charset}.
     * @return the {@link Charset}'s {@link CharsetDecoder}.
     */
    public static CharsetDecoder getCharsetDecoder(final Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("Charset can not be null");
        }
        
        final CharsetDecoder decoder = obtainCodecsCache().getDecoder(charset);
        decoder.reset();
        
        return decoder;
    }
    
    /**
     * Return the {@link Charset}'s {@link CharsetEncoder}.
     * The <tt>Charsets</tt> class maintains the {@link CharsetEncoder} thread-local
     * cache.  Be aware - this shouldn't be used by multiple threads.
     * 
     * @param charset {@link Charset}.
     * @return the {@link Charset}'s {@link CharsetEncoder}.
     */
    public static CharsetEncoder getCharsetEncoder(final Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("Charset can not be null");
        }
        
        final CharsetEncoder encoder = obtainCodecsCache().getEncoder(charset);
        encoder.reset();
        
        return encoder;
    }

    private static CodecsCache obtainCodecsCache() {
        CodecsCache cache = ThreadCache.getFromCache(CODECS_CACHE);
        if (cache == null) {
            cache = new CodecsCache();
            ThreadCache.putToCache(CODECS_CACHE, cache);
        }
        
        return cache;
    }
    
    private static final class CodecsCache {
        private final Object[] decoders =
                new Object[CODECS_CACHE_SIZE];
        private final Object[] encoders =
                new Object[CODECS_CACHE_SIZE];
        
        public CharsetDecoder getDecoder(final Charset charset) {
            return (CharsetDecoder) obtainElementByCharset(
                    decoders, charset, DECODER_RESOLVER);
        }
        
        public CharsetEncoder getEncoder(final Charset charset) {
            return (CharsetEncoder) obtainElementByCharset(
                    encoders, charset, ENCODER_RESOLVER);
        }
        
        private static Object obtainElementByCharset(final Object[] array,
                final Charset charset, final CharsetCodecResolver resolver) {
            
            int i = 0;
            for (; i < array.length; i++) {
                final Object currentElement = array[i];
                
                if (currentElement == null) {
                    i++; // to make 
                    break;
                }
                
                if (charset.equals(resolver.charset(currentElement))) {
                    makeFirst(array, i, currentElement);
                    return currentElement;
                }
            }

            final Object newElement = resolver.newElement(charset);
            makeFirst(array, i - 1, newElement);
            return newElement;            
        }
        
        private static void makeFirst(final Object[] array, final int offs,
                final Object element) {
            System.arraycopy(array, 0, array, 1, offs - 1 + 1);
            
            array[0] = element;
        }       
    }
    
    private interface CharsetCodecResolver {
        Charset charset(Object element);
        Object newElement(Charset charset);
    }

    private final static class DecoderResolver implements CharsetCodecResolver {
        @Override
        public Charset charset(final Object element) {
            return ((CharsetDecoder) element).charset();
        }

        @Override
        public Object newElement(final Charset charset) {
            return charset.newDecoder();
        }

    }

    private final static class EncoderResolver implements CharsetCodecResolver {

        @Override
        public Charset charset(final Object element) {
            return ((CharsetEncoder) element).charset();
        }

        @Override
        public Object newElement(final Charset charset) {
            return charset.newEncoder();
        }
    }
    
}
