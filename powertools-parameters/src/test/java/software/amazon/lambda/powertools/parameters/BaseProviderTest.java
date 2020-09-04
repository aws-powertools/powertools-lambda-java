/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package software.amazon.lambda.powertools.parameters;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.ObjectToDeserialize;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

public class BaseProviderTest {
    @Spy
    SSMProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void get_notCached_shouldGetValue() {
        doReturn("bar").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));
        String foo = provider.get("foo");

        assertEquals("bar", foo);
        verify(provider, times(1)).getValue("foo");
    }

    @Test
    public void get_cached_shouldGetFromCache() {
        doReturn("bar").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.get("foo");
        assertTrue(provider.hasNotExpired("foo"));

        String foo = provider.get("foo");
        assertEquals("bar", foo);
        verify(provider, times(1)).getValue("foo");
    }

    @Test
    public void get_changedMaxAge_shouldRestoreMaxAgeForNextGet() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");
        doReturn("titi").when(provider).getValue("toto");

        provider.withMaxAge(2).get("foo");
        provider.get("toto"); // default max age = 5 sec

        Thread.sleep(4000);
        assertTrue(provider.hasNotExpired("toto"));
        assertFalse(provider.hasNotExpired("foo"));
    }

    @Test
    public void get_changeDefaultMaxAge_shouldRestoreMaxAgeForNextGet() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");
        doReturn("titi").when(provider).getValue("toto");

        provider.defaultMaxAge(4).get("foo");
        provider.get("toto"); // default max age = 4 sec

        Thread.sleep(4040);
        assertFalse(provider.hasNotExpired("toto"));
        assertFalse(provider.hasNotExpired("foo"));
    }

    @Test
    public void get_changeDefaultMaxAgeAndCustomMaxAge_shouldUseCustomMaxAgeForNextGet() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");
        doReturn("titi").when(provider).getValue("toto");

        provider.defaultMaxAge(4).get("foo");
        provider.withMaxAge(2).get("toto");

        Thread.sleep(2020);
        assertFalse(provider.hasNotExpired("toto"));
        assertTrue(provider.hasNotExpired("foo"));
    }

    @Test
    public void get_TTLExpired_shouldGetValue() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.get("foo");

        Thread.sleep(BaseProvider.DEFAULT_MAX_AGE_SECS * 1010);

        assertFalse(provider.hasNotExpired("foo"));
        String foo = provider.get("foo");

        assertEquals("bar", foo);
        verify(provider, times(2)).getValue("foo");
    }

    @Test
    public void get_customTTLExpired_shouldGetValue() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.withMaxAge(2).get("foo");

        Thread.sleep( 2020);

        assertFalse(provider.hasNotExpired("foo"));

        String foo = provider.get("foo");
        assertEquals("bar", foo);
        verify(provider, times(2)).getValue("foo");
    }

    @Test
    public void get_customTTLCached_shouldGetFromCache() throws InterruptedException {
        doReturn("bar").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.withMaxAge(2).get("foo");

        Thread.sleep( 1400);

        assertTrue(provider.hasNotExpired("foo"));
        String foo = provider.get("foo");

        assertEquals("bar", foo);
        verify(provider, times(1)).getValue("foo");
    }

    @Test
    public void get_basicTransformation_shouldTransformInString() {
        doReturn(Base64.getEncoder().encodeToString("bar".getBytes())).when(provider).getValue("foo");

        String foo = provider.withTransformation(base64).get("foo");

        assertEquals("bar", foo);
    }

    @Test
    public void get_basicTransformationWithWrongTransformer_shouldThrowException() {
        doReturn(Base64.getEncoder().encodeToString("bar".getBytes())).when(provider).getValue("foo");

        assertThrows(IllegalArgumentException.class, () -> provider.withTransformation(json).get("foo"));
    }

    @Test
    public void get_complexTransformation_shouldTransformInObject() {
        doReturn("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}").when(provider).getValue("foo");

        ObjectToDeserialize objectToDeserialize = provider.withTransformation(json).get("foo", ObjectToDeserialize.class);

        assertEquals("Foo", objectToDeserialize.getFoo());
        assertEquals(42, objectToDeserialize.getBar());
        assertEquals(123456789, objectToDeserialize.getBaz());
    }

    @Test
    public void get_complexTransformationWithNoTransformer_shouldThrowException() {
        doReturn("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}").when(provider).getValue("foo");

        assertThrows(IllegalArgumentException.class, () -> provider.get("foo", ObjectToDeserialize.class));
    }

    @Test
    public void get_complexTransformationWithWrongTransformer_shouldThrowException() {
        doReturn("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}").when(provider).getValue("foo");

        assertThrows(TransformationException.class, () -> provider.withTransformation(base64).get("foo", ObjectToDeserialize.class));
    }

    @Test
    public void get_basicTransformationCached_shouldTransformInStringAndGetFromCache() {
        doReturn(Base64.getEncoder().encodeToString("bar".getBytes())).when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.withTransformation(base64).get("foo");
        assertTrue(provider.hasNotExpired("foo"));

        String foo = provider.withTransformation(base64).get("foo");
        assertEquals("bar", foo);
        verify(provider, times(1)).getValue("foo");
    }

    @Test
    public void get_complexTransformationCached_shouldTransformInObjectAndGetFromCache() {
        doReturn("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}").when(provider).getValue("foo");

        assertFalse(provider.hasNotExpired("foo"));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertTrue(provider.hasNotExpired("foo"));

        ObjectToDeserialize objectToDeserialize = provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertEquals("Foo", objectToDeserialize.getFoo());
        assertEquals(42, objectToDeserialize.getBar());
        assertEquals(123456789, objectToDeserialize.getBaz());

        verify(provider, times(1)).getValue("foo");
    }
}
