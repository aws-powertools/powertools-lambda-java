package software.amazon.lambda.powertools.common.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClassPreLoaderTest {

    // Making this volatile so the Thread Context doesn't need any special handling
    static volatile boolean dummyClassLoaded = false;

    /**
     * Dummy class to be loaded by ClassPreLoader in test.
     * <b>The class name is referenced in <i>powertools-common/src/test/resources/classesloaded.txt</i></b>
     * This class is used to verify that the ClassPreLoader can load valid classes.
     * The static block sets a flag to indicate that the class has been loaded.
     */
    static class DummyClass {
        static {
            dummyClassLoaded = true;
        }
    }
    @Test
    void preloadClasses_shouldIgnoreInvalidClassesAndLoadValidClasses() {

        dummyClassLoaded = false;
        // powertools-common/src/test/resources/classesloaded.txt has a class that does not exist
        // Verify that the missing class did not throw any exception
        assertDoesNotThrow(ClassPreLoader::preloadClasses);

        // When the classloaded.txt is a mixed bag of valid and invalid classes, Valid class must load
        assertTrue(dummyClassLoaded, "DummyClass should be loaded");
    }
}