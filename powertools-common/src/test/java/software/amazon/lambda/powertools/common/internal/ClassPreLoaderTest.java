package software.amazon.lambda.powertools.common.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClassPreLoaderTest {

    @Test
    void preloadClasses_shouldIgnoreInvalidClassesAndLoadValidClasses() {
        // Verify that the missing class did not throw any exception
        assertDoesNotThrow(ClassPreLoader::preloadClasses);
    }
}