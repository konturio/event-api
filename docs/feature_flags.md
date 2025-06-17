# Feature Flags

This project uses simple feature flags to enable experimental functionality for a limited set of users.

## `cross_provider_merge`

Enables cross-provider merge mode in DN. Access is granted only to users that have one of the roles listed in `features.cross_provider_merge.roles` in `application.yml` (by default `cross_provider_merge`).

The feature is considered **beta** and should only be enabled for testers from the relevant Keycloak role group.
