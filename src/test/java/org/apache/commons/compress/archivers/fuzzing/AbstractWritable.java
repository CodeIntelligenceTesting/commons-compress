/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.fuzzing;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public abstract class AbstractWritable {

    public abstract int getRecordSize();

    public abstract void writeTo(ByteBuffer buffer);

    protected void writeOctalString(ByteBuffer buffer, long value, int length) {
        final byte[] bytes = Long.toOctalString(value).getBytes(US_ASCII);
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    "Value " + value + " is too large to fit in " + length + " octal digits");
        }
        buffer.put(bytes);
        pad(buffer, bytes.length, length, (byte) ' ');
    }

    protected void writeBytes(ByteBuffer buffer, byte[] bytes, int length) {
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    "Byte array with \"" + bytes.length + "\" do not fit in " + length + " remaining bytes");
        }
        buffer.put(bytes);
        pad(buffer, bytes.length, length, (byte) 0);
    }

    protected void writeString(ByteBuffer buffer, String value, Charset charset, int length) {
        final byte[] bytes = value.getBytes(charset);
        if (bytes.length > length) {
            throw new IllegalArgumentException(
                    "String \"" + value + "\" is too long to fit in " + length + " bytes");
        }
        writeBytes(buffer, bytes, length);
    }

    protected void pad(ByteBuffer buffer, int written, int length, byte padByte) {
        while (written % length != 0) {
            buffer.put(padByte);
            written++;
        }
    }
}
