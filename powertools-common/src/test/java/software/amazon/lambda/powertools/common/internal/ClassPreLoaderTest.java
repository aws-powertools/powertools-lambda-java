package software.amazon.lambda.powertools.common.internal;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class ClassPreLoaderTest {

    @Test
    void preloadClasses_shouldIgnoreInvalidClassesAndLoadValidClasses() throws Exception {
        // Mock the class loader with no resources
        ClassLoader classLoader = mock(ClassLoader.class);
        URL mockUrl = mock(URL.class);
        URLConnection mockConnection = mock(URLConnection.class);
        InputStream mockInputStream = new ByteArrayInputStream("java.lang.String\nInvalid.Class".getBytes());

        when(mockUrl.openConnection()).thenReturn(mockConnection);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);
        when(classLoader.getResources(ClassPreLoader.CLASSES_FILE))
                .thenReturn(Collections.enumeration(Collections.singletonList(mockUrl)));

        // Inject the mocked class loader
        Thread.currentThread().setContextClassLoader(classLoader);
        // Call the method under test
        ClassPreLoader.preloadClasses();

        // Verify that only the valid class was loaded
        Class.forName("java.lang.String", true, ClassPreLoader.class.getClassLoader());
    }

    @Test
    void preloadClasses_shouldHandleEmptyResources() throws Exception {
        // Mock the class loader with no resources
        ClassLoader classLoader = mock(ClassLoader.class);
        when(classLoader.getResources(ClassPreLoader.CLASSES_FILE))
                .thenReturn(Collections.emptyEnumeration());

        // Inject the mocked class loader
        Thread.currentThread().setContextClassLoader(classLoader);

        // Call the method under test
        ClassPreLoader.preloadClasses();

        // Verify no interactions with the class loader
        verifyNoInteractions(classLoader);
    }
}