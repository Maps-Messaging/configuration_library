/*
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.persistance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PersistentObjectTest extends PersistentObject {

  @Test
  void testIntRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int value = 0x12345678;

    writeInt(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    int result = readInt(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testLongRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    long value = 0x0123456789ABCDEFL;

    writeLong(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    long result = readLong(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testIntBoundaryValues() throws IOException {
    int[] values = {
        0,
        -1,
        Integer.MAX_VALUE,
        Integer.MIN_VALUE
    };

    for (int value : values) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      writeInt(byteArrayOutputStream, value);
      ByteArrayInputStream byteArrayInputStream =
          new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
      int result = readInt(byteArrayInputStream);
      assertEquals(value, result);
    }
  }

  @Test
  void testLongBoundaryValues() throws IOException {
    long[] values = {
        0L,
        -1L,
        Long.MAX_VALUE,
        Long.MIN_VALUE
    };

    for (long value : values) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      writeLong(byteArrayOutputStream, value);
      ByteArrayInputStream byteArrayInputStream =
          new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
      long result = readLong(byteArrayInputStream);
      assertEquals(value, result);
    }
  }

  @Test
  void testStringRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    String value = "PersistentObject-String";

    writeString(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    String result = readString(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testEmptyStringRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    String value = "";

    writeString(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    String result = readString(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testNullStringEncoding() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    writeString(byteArrayOutputStream, null);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    // With current implementation: len < 0 -> ""
    String result = readString(byteArrayInputStream);
    assertEquals("", result);
  }

  @Test
  void testUtf8StringRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    String value = "é漢字🚀 MQTT";

    writeString(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    String result = readString(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testLongStringRoundTrip() throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < 10_000; i++) {
      stringBuilder.append("x");
    }
    String value = stringBuilder.toString();

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    writeString(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    String result = readString(byteArrayInputStream);
    assertEquals(value, result);
  }

  @Test
  void testByteArrayRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] value = new byte[]{0x01, 0x23, (byte) 0xFF, 0x00, 0x55};

    writeByteArray(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    byte[] result = readByteArray(byteArrayInputStream);
    assertArrayEquals(value, result);
  }

  @Test
  void testEmptyByteArrayRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] value = new byte[0];

    writeByteArray(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    byte[] result = readByteArray(byteArrayInputStream);
    assertArrayEquals(value, result);
  }

  @Test
  void testByteArrayNullRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    writeByteArray(byteArrayOutputStream, null);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    byte[] result = readByteArray(byteArrayInputStream);
    assertNull(result);
  }

  @Test
  void testLargeByteArrayRoundTrip() throws IOException {
    byte[] value = new byte[8192];
    Random random = new Random(12345L);
    random.nextBytes(value);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    writeByteArray(byteArrayOutputStream, value);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    byte[] result = readByteArray(byteArrayInputStream);
    assertArrayEquals(value, result);
  }

  @Test
  void testMixedTypesRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    int intValue = 0x7F00FF00;
    long longValue = 0x0102030405060708L;
    String stringValue = "MixedTypes";
    byte[] byteArrayValue = new byte[]{10, 20, 30, 40};

    writeInt(byteArrayOutputStream, intValue);
    writeLong(byteArrayOutputStream, longValue);
    writeString(byteArrayOutputStream, stringValue);
    writeByteArray(byteArrayOutputStream, byteArrayValue);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    int readIntValue = readInt(byteArrayInputStream);
    long readLongValue = readLong(byteArrayInputStream);
    String readStringValue = readString(byteArrayInputStream);
    byte[] readByteArrayValue = readByteArray(byteArrayInputStream);

    assertEquals(intValue, readIntValue);
    assertEquals(longValue, readLongValue);
    assertEquals(stringValue, readStringValue);
    assertArrayEquals(byteArrayValue, readByteArrayValue);
  }

  @Test
  void testMixedTypesWithUtf8StringRoundTrip() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    int intValue = -123456789;
    long longValue = 0x0F0E0D0C0B0A0908L;
    String stringValue = "UTF8: é漢字🚀";
    byte[] byteArrayValue = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};

    writeInt(byteArrayOutputStream, intValue);
    writeLong(byteArrayOutputStream, longValue);
    writeString(byteArrayOutputStream, stringValue);
    writeByteArray(byteArrayOutputStream, byteArrayValue);

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    int readIntValue = readInt(byteArrayInputStream);
    long readLongValue = readLong(byteArrayInputStream);
    String readStringValue = readString(byteArrayInputStream);
    byte[] readByteArrayValue = readByteArray(byteArrayInputStream);

    assertEquals(intValue, readIntValue);
    assertEquals(longValue, readLongValue);
    assertEquals(stringValue, readStringValue);
    assertArrayEquals(byteArrayValue, readByteArrayValue);
  }

  @Test
  void testReadFullBufferWithPartialReads() throws IOException {
    byte[] expected = new byte[1024];
    Random random = new Random(6789L);
    random.nextBytes(expected);

    ByteArrayInputStream baseInputStream = new ByteArrayInputStream(expected);
    InputStream chunkedInputStream = new ChunkedInputStream(baseInputStream, 7);

    byte[] actual = readFullBuffer(chunkedInputStream, expected.length);
    assertArrayEquals(expected, actual);
  }

  private static class ChunkedInputStream extends InputStream {

    private final ByteArrayInputStream delegate;
    private final int maxChunkSize;

    private ChunkedInputStream(ByteArrayInputStream delegate, int maxChunkSize) {
      this.delegate = delegate;
      this.maxChunkSize = maxChunkSize;
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
      if (buffer == null) {
        return 0;
      }
      return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int chunkSize = Math.min(length, maxChunkSize);
      return delegate.read(buffer, offset, chunkSize);
    }
  }
}
