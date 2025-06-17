package io.kontur.eventapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BetaFeaturesService {

    private final List<String> crossProviderMergeRoles;

    public BetaFeaturesService(@Value("${features.cross_provider_merge.roles:}") String[] roles) {
        this.crossProviderMergeRoles = roles != null ? Arrays.asList(roles) : Collections.emptyList();
    }

    public boolean isCrossProviderMergeAllowed(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(crossProviderMergeRoles::contains);
    }
}
