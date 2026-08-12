package org.dromara.maxkey.authz.saml20;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensaml.DefaultBootstrap;
import org.owasp.esapi.ESAPI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SamlRuntimeDependenciesTest {

    @BeforeAll
    static void bootstrapsOpenSamlWithEsapi() {
        assertDoesNotThrow(DefaultBootstrap::bootstrap);
    }

    @Test
    void providesEncoderUsedByOpenSamlBindings() {
        assertEquals("&lt;MaxKey&gt;", ESAPI.encoder().encodeForHTML("<MaxKey>"));
    }

    @Test
    void doesNotPullUnusedLegacyEsapiDependenciesIntoAuthenticationRuntime() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.log4j.Logger"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.owasp.validator.html.AntiSamy"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.commons.fileupload.FileItem"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.commons.configuration.Configuration"));
    }
}
