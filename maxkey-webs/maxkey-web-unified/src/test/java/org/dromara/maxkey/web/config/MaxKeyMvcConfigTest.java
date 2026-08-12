package org.dromara.maxkey.web.config;

import org.dromara.maxkey.authn.provider.AbstractAuthenticationProvider;
import org.dromara.maxkey.authn.jwt.AuthTokenService;
import org.dromara.maxkey.authn.session.SessionManager;
import org.dromara.maxkey.authn.support.kerberos.KerberosService;
import org.dromara.maxkey.authn.web.interceptor.PermissionInterceptor;
import org.dromara.maxkey.configuration.ApplicationConfig;
import org.dromara.maxkey.web.auth.interceptor.HistorySingleSignOnInterceptor;
import org.dromara.maxkey.web.auth.interceptor.SingleSignOnInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MaxKeyMvcConfigTest {

    @Test
    void separatesUserAndAdminPermissionInterceptors() {
        MaxKeyMvcConfig config = new MaxKeyMvcConfig();
        config.applicationConfig = mock(ApplicationConfig.class);
        config.authenticationProvider = mock(AbstractAuthenticationProvider.class);
        config.kerberosService = mock(KerberosService.class);
        config.permissionInterceptor = mock(PermissionInterceptor.class);
        config.sessionManager = mock(SessionManager.class);
        config.authTokenService = mock(AuthTokenService.class);
        config.singleSignOnInterceptor = mock(SingleSignOnInterceptor.class);
        config.historySingleSignOnInterceptor = mock(HistorySingleSignOnInterceptor.class);

        TestInterceptorRegistry registry = new TestInterceptorRegistry();
        config.addInterceptors(registry);

        List<MappedInterceptor> permissionMappings = registry.interceptors().stream()
                .filter(MappedInterceptor.class::isInstance)
                .map(MappedInterceptor.class::cast)
                .filter(mapped -> mapped.getInterceptor() instanceof PermissionInterceptor)
                .toList();

        assertEquals(2, permissionMappings.size());
        assertEquals(2, permissionMappings.stream().map(MappedInterceptor::getInterceptor).distinct().count());

        MappedInterceptor userMapping = permissionMappings.stream()
                .filter(mapped -> mapped.getInterceptor() == config.permissionInterceptor)
                .findFirst()
                .orElseThrow();
        MappedInterceptor managementMapping = permissionMappings.stream()
                .filter(mapped -> mapped.getInterceptor() != config.permissionInterceptor)
                .findFirst()
                .orElseThrow();

        assertSame(config.permissionInterceptor, userMapping.getInterceptor());
        assertNotSame(config.permissionInterceptor, managementMapping.getInterceptor());

        assertTrue(matches(userMapping, "/appList"));
        assertFalse(matches(userMapping, "/admin/apps/fetch"));
        assertTrue(matches(managementMapping, "/admin/apps/fetch"));
        assertFalse(matches(managementMapping, "/appList"));
        assertFalse(matches(managementMapping, "/admin/login/get"));
    }

    private boolean matches(MappedInterceptor interceptor, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        ServletRequestPathUtils.parseAndCache(request);
        return interceptor.matches(request);
    }

    private static class TestInterceptorRegistry extends InterceptorRegistry {
        List<Object> interceptors() {
            return getInterceptors();
        }
    }
}
