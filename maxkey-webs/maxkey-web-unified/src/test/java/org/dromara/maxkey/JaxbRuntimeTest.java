package org.dromara.maxkey;

import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JaxbRuntimeTest {

    @Test
    void createsJaxbContextWithRuntimeImplementation() {
        assertDoesNotThrow(() -> JAXBContext.newInstance(JaxbRuntimeTest.class));
    }
}
