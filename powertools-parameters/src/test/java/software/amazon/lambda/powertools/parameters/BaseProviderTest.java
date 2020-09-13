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
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.cache.NowProvider;
import software.amazon.lambda.powertools.parameters.transform.ObjectToDeserialize;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

public class BaseProviderTest {

    @Spy
    NowProvider nowProvider;

    CacheManager cacheManager;
    TransformationManager transformationManager;
    BasicProvider provider;

    boolean getFromStore = false;

    class BasicProvider extends BaseProvider {

        public BasicProvider(CacheManager cacheManager) {
            super(cacheManager);
        }

        private String value = "valueFromStore";

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        protected String getValue(String key) {
            getFromStore = true;
            return value;
        }
    }

    @BeforeEach
    public void setup() {
        openMocks(this);

        cacheManager = new CacheManager(nowProvider);
        provider = new BasicProvider(cacheManager);
        transformationManager = new TransformationManager();
        provider.setTransformationManager(transformationManager);
    }

    @Test
    public void get_notCached_shouldGetValue() {
        String foo = provider.get("toto");

        assertThat(foo).isEqualTo("valueFromStore");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_cached_shouldGetFromCache() {
        provider.get("foo");
        getFromStore = false;

        String foo = provider.get("foo");
        assertThat(foo).isEqualTo("valueFromStore");
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void get_expired_shouldGetValue() {
        provider.get("bar");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(6, ChronoUnit.SECONDS));

        provider.get("bar");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_customTTL_cached_shouldGetFromCache() {
        provider.withMaxAge(12, ChronoUnit.MINUTES).get("key");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(10, ChronoUnit.MINUTES));

        provider.get("key");
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void get_customTTL_expired_shouldGetValue() {
        provider.withMaxAge(2, ChronoUnit.MINUTES).get("mykey");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(3, ChronoUnit.MINUTES));

        provider.get("mykey");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_customDefaultTTL_cached_shouldGetFromCache() {
        provider.defaultMaxAge(12, ChronoUnit.MINUTES).get("foobar");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(10, ChronoUnit.MINUTES));

        provider.get("foobar");
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void get_customDefaultTTL_expired_shouldGetValue() {
        provider.defaultMaxAge(2, ChronoUnit.MINUTES).get("barbaz");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(3, ChronoUnit.MINUTES));

        provider.get("barbaz");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_customDefaultTTLAndTTL_cached_shouldGetFromCache() {
        provider.defaultMaxAge(12, ChronoUnit.MINUTES)
                .withMaxAge(5, ChronoUnit.SECONDS)
                .get("foobaz");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(4, ChronoUnit.SECONDS));

        provider.get("foobaz");
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void get_customDefaultTTLAndTTL_expired_shouldGetValue() {
        provider.defaultMaxAge(2, ChronoUnit.MINUTES)
                .withMaxAge(5, ChronoUnit.SECONDS)
                .get("bariton");
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(6, ChronoUnit.SECONDS));

        provider.get("bariton");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_basicTransformation_shouldTransformInString() {
        provider.setValue(Base64.getEncoder().encodeToString("bar".getBytes()));

        String value = provider.withTransformation(Transformer.base64).get("base64");

        assertThat(value).isEqualTo("bar");
    }

    @Test
    public void get_complexTransformation_shouldTransformInObject() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        ObjectToDeserialize objectToDeserialize = provider.withTransformation(json).get("foo", ObjectToDeserialize.class);

        assertThat(objectToDeserialize.getFoo()).isEqualTo("Foo");
        assertThat(objectToDeserialize.getBar()).isEqualTo(42);
        assertThat(objectToDeserialize.getBaz()).isEqualTo(123456789);
    }

    @Test
    public void getObject_notCached_shouldGetValue() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        ObjectToDeserialize foo = provider.withTransformation(json).get("foo", ObjectToDeserialize.class);

        assertThat(foo).isNotNull();
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void getObject_cached_shouldGetFromCache() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        ObjectToDeserialize foo = provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(foo).isNotNull();
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void getObject_expired_shouldGetValue() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(6, ChronoUnit.SECONDS));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void getObject_customTTL_cached_shouldGetFromCache() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.withMaxAge(12, ChronoUnit.MINUTES)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(10, ChronoUnit.MINUTES));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void getObject_customTTL_expired_shouldGetValue() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.withMaxAge(2, ChronoUnit.MINUTES)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(3, ChronoUnit.MINUTES));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void getObject_customDefaultTTL_cached_shouldGetFromCache() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.defaultMaxAge(12, ChronoUnit.MINUTES)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(10, ChronoUnit.MINUTES));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void getObject_customDefaultTTL_expired_shouldGetValue() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.defaultMaxAge(2, ChronoUnit.MINUTES)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(3, ChronoUnit.MINUTES));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void getObject_customDefaultTTLAndTTL_cached_shouldGetFromCache() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.defaultMaxAge(12, ChronoUnit.MINUTES)
                .withMaxAge(5, ChronoUnit.SECONDS)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(4, ChronoUnit.SECONDS));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isFalse();
    }

    @Test
    public void getObject_customDefaultTTLAndTTL_expired_shouldGetValue() {
        provider.setValue("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");

        provider.defaultMaxAge(2, ChronoUnit.MINUTES)
                .withMaxAge(5, ChronoUnit.SECONDS)
                .withTransformation(json)
                .get("foo", ObjectToDeserialize.class);
        getFromStore = false;

        when(nowProvider.now()).thenReturn(Instant.now().plus(6, ChronoUnit.SECONDS));

        provider.withTransformation(json).get("foo", ObjectToDeserialize.class);
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void get_noTransformationManager_shouldThrowException() {
        provider.setTransformationManager(null);

        assertThatIllegalStateException().isThrownBy(() -> provider.withTransformation(base64).get("foo"));
    }

    @Test
    public void getObject_noTransformationManager_shouldThrowException() {
        provider.setTransformationManager(null);

        assertThatIllegalStateException().isThrownBy(() -> provider.get("foo", ObjectToDeserialize.class));
    }

    @Test
    public void getTwoParams_shouldResetTTLOptionsInBetween() {
        provider.withMaxAge(50, ChronoUnit.SECONDS).get("foo50");

        provider.get("foo5");

        when(nowProvider.now()).thenReturn(Instant.now().plus(6, ChronoUnit.SECONDS));
        getFromStore = false;

        provider.get("foo5");
        assertThat(getFromStore).isTrue();
    }

    @Test
    public void getTwoParams_shouldResetTransformationOptionsInBetween() {
        provider.setValue(Base64.getEncoder().encodeToString("base64encoded".getBytes()));
        String foob64 = provider.withTransformation(base64).get("foob64");

        provider.setValue("string");
        String foostr = provider.get("foostr");

        assertThat(foob64).isEqualTo("base64encoded");
        assertThat(foostr).isEqualTo("string");
    }
}
