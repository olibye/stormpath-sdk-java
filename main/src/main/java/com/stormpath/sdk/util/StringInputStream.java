/*
 * Copyright 2012 Stormpath, Inc.
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
 */
package com.stormpath.sdk.util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * @since 0.1
 */
public class StringInputStream extends ByteArrayInputStream {

    private final String string;

    public StringInputStream(String s) {
        super(getBytes(s));
        this.string = s;
    }

    private static byte[] getBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is unavailable on the current JVM.", e);
        }
    }

    @Override
    public String toString() {
        return this.string;
    }
}
