/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
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

package software.amazon.lambda.powertools.parameters.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

import java.util.Base64;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

class TransformationManagerTest {

    TransformationManager manager;

    Class<BasicTransformer> basicTransformer = BasicTransformer.class;

    @BeforeEach
    void setup() {
        manager = new TransformationManager();
    }

    @Test
    void setTransformer_shouldTransform() {
        manager.setTransformer(json);

        assertThat(manager.shouldTransform()).isTrue();
    }

    @Test
    void notSetTransformer_shouldNotTransform() {
        assertThat(manager.shouldTransform()).isFalse();
    }

    @Test
    void performBasicTransformation_noTransformer_shouldThrowException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performBasicTransformation("value"));
    }

    @Test
    void performBasicTransformation_notBasicTransformer_shouldThrowException() {
        manager.setTransformer(json);

        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performBasicTransformation("value"));
    }

    @Test
    void performBasicTransformation_abstractTransformer_throwsTransformationException() {
        manager.setTransformer(basicTransformer);

        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() -> manager.performBasicTransformation("value"));
    }

    @Test
    void performBasicTransformation_shouldPerformTransformation() {
        manager.setTransformer(base64);

        String expectedValue = "bar";
        String value = manager.performBasicTransformation(Base64.getEncoder().encodeToString(expectedValue.getBytes()));

        assertThat(value).isEqualTo(expectedValue);
    }

    @Test
    void performComplexTransformation_noTransformer_shouldThrowException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performComplexTransformation("value", ObjectToDeserialize.class));
    }

    @Test
    void performComplexTransformation_shouldPerformTransformation() {
        manager.setTransformer(json);

        ObjectToDeserialize object = manager.performComplexTransformation(
                "{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}",
                ObjectToDeserialize.class);

        assertThat(object).isNotNull();
    }

    @Test
    void performComplexTransformation_throwsTransformationException() {
        manager.setTransformer(basicTransformer);

        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() -> manager.performComplexTransformation("value", ObjectToDeserialize.class));
    }

    @Test
    void unsetTransformer_shouldCleanUpThreadLocal() {
        // GIVEN
        manager.setTransformer(json);
        assertThat(manager.shouldTransform()).isTrue();

        // WHEN
        manager.unsetTransformer();

        // THEN
        assertThat(manager.shouldTransform()).isFalse();
    }

    @Test
    void setTransformer_concurrentCalls_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        boolean[] success = new boolean[2];
        CountDownLatch latch = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                manager.setTransformer(json);
                // Thread 1 expects json transformer
                String result = manager.performComplexTransformation(
                        "{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}",
                        ObjectToDeserialize.class).getFoo();
                success[0] = "Foo".equals(result);
            } catch (Exception e) {
                e.printStackTrace();
                success[0] = false;
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                manager.setTransformer(base64);
                // Thread 2 expects base64 transformer
                String result = manager.performBasicTransformation(
                        Base64.getEncoder().encodeToString("bar".getBytes()));
                success[1] = "bar".equals(result);
            } catch (Exception e) {
                e.printStackTrace();
                success[1] = false;
            }
        });

        // WHEN - Start both threads concurrently
        thread1.start();
        thread2.start();

        // THEN - Both threads should complete without errors
        thread1.join();
        thread2.join();

        assertThat(success[0]).as("Thread 1 with JSON transformer should succeed").isTrue();
        assertThat(success[1]).as("Thread 2 with Base64 transformer should succeed").isTrue();
    }

    @Test
    void unsetTransformer_concurrentCalls_shouldNotAffectOtherThreads() throws InterruptedException {
        // GIVEN
        boolean[] success = new boolean[2];
        CountDownLatch latch = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                manager.setTransformer(json);
                // Thread 1 should still have json transformer even if thread 2 unsets
                assertThat(manager.shouldTransform()).isTrue();
                success[0] = true;
            } catch (Exception e) {
                e.printStackTrace();
                success[0] = false;
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                manager.setTransformer(base64);
                manager.unsetTransformer();
                // Thread 2 should have no transformer after unset
                assertThat(manager.shouldTransform()).isFalse();
                success[1] = true;
            } catch (Exception e) {
                e.printStackTrace();
                success[1] = false;
            }
        });

        // WHEN
        thread1.start();
        thread2.start();

        // THEN
        thread1.join();
        thread2.join();

        assertThat(success[0]).as("Thread 1 should still have transformer").isTrue();
        assertThat(success[1]).as("Thread 2 should have unset transformer").isTrue();
    }
}
