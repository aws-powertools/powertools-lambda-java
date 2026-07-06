package software.amazon.lambda.powertools.common.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClassPreLoaderTest {

    // Making this volatile so the Thread Context doesn't need any special handling
    static volatile boolean dummyClassLoaded = false;
    static volatile boolean leadingSpaceDummyClassLoaded = false;

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

    /**
     * Dummy class whose name is referenced with a leading space in
     * <i>powertools-common/src/test/resources/classesloaded.txt</i>.
     * Some modules ship a classesloaded.txt with a leading space on every line, so the loader must
     * strip leading whitespace before calling Class.forName.
     */
    static class LeadingSpaceDummyClass {
        static {
            leadingSpaceDummyClassLoaded = true;
        }
    }

    @Test
    void preloadClasses_shouldIgnoreInvalidClassesAndLoadValidClasses() {

        dummyClassLoaded = false;
        leadingSpaceDummyClassLoaded = false;
        // A class only runs its static initializer the first time it is loaded, so this test calls
        // preloadClasses once and asserts on all classes listed in classesloaded.txt together.
        // powertools-common/src/test/resources/classesloaded.txt has a class that does not exist.
        // Verify that the missing class did not throw any exception
        assertDoesNotThrow(ClassPreLoader::preloadClasses);

        // When the classloaded.txt is a mixed bag of valid and invalid classes, valid classes must load
        assertTrue(dummyClassLoaded, "DummyClass should be loaded");
        // classesloaded.txt references LeadingSpaceDummyClass with a leading space. The loader must
        // strip it before Class.forName, otherwise the class is never loaded.
        assertTrue(leadingSpaceDummyClassLoaded,
                "LeadingSpaceDummyClass should be loaded despite the leading space");
    }
}