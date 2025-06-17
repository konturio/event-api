package io.kontur.eventapi.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BetaFeaturesServiceTest {

    @Test
    void shouldAllowAccessWhenRolePresent() {
        BetaFeaturesService service = new BetaFeaturesService(new String[]{"cross_provider_merge"});
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", null,
                List.of(new SimpleGrantedAuthority("cross_provider_merge")));
        assertTrue(service.isCrossProviderMergeAllowed(auth));
    }

    @Test
    void shouldDenyAccessWhenRoleAbsent() {
        BetaFeaturesService service = new BetaFeaturesService(new String[]{"cross_provider_merge"});
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", null,
                List.of(new SimpleGrantedAuthority("another_role")));
        assertFalse(service.isCrossProviderMergeAllowed(auth));
    }
}
