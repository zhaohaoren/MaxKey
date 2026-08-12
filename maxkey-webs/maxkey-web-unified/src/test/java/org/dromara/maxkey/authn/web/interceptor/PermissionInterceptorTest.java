package org.dromara.maxkey.authn.web.interceptor;

import org.dromara.maxkey.authn.SignPrincipal;
import org.dromara.maxkey.authn.jwt.AuthTokenService;
import org.dromara.maxkey.authn.session.SessionManager;
import org.dromara.maxkey.configuration.ApplicationConfig;
import org.dromara.maxkey.web.WebConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PermissionInterceptorTest {

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void managementInterceptorRejectsAuthenticatedNonAdministrator() throws Exception {
        MockHttpServletRequest request = authenticatedRequest(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        bindRequestContext(request, response);

        boolean allowed = managementInterceptor().preHandle(request, response, new Object());

        assertFalse(allowed);
        assertTrue(response.getForwardedUrl().endsWith("/auth/entrypoint"));
    }

    @Test
    void managementInterceptorAllowsAuthenticatedAdministrator() throws Exception {
        MockHttpServletRequest request = authenticatedRequest(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        bindRequestContext(request, response);

        boolean allowed = managementInterceptor().preHandle(request, response, new Object());

        assertTrue(allowed);
        assertNull(response.getForwardedUrl());
    }

    private PermissionInterceptor managementInterceptor() {
        return new PermissionInterceptor(
                mock(ApplicationConfig.class),
                mock(SessionManager.class),
                mock(AuthTokenService.class),
                true);
    }

    private MockHttpServletRequest authenticatedRequest(boolean administrator) {
        SignPrincipal principal = new SignPrincipal();
        principal.setRoleAdministrators(administrator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/apps/fetch");
        request.getSession().setAttribute(
                WebConstants.AUTHENTICATION,
                new UsernamePasswordAuthenticationToken(principal, null));
        return request;
    }

    private void bindRequestContext(
            MockHttpServletRequest request,
            MockHttpServletResponse response) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }
}
