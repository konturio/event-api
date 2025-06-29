# Open Questions and Potential Issues

During review a few areas looked unclear or potentially problematic. They may require further clarification:

## `PdcSqsMessageListener`
The listener previously skipped `PING` and `PRODUCT` messages silently. Debug
logs have been added to make it obvious when such messages are ignored.
Handling of `PRODUCT` messages is still not implemented and requires further
clarification.

## Service Level Agreement
There is currently **no SLA** defined for Event API. Availability and response
times are provided on a best effort basis.

## Caching behaviour
`EventResourceService` enables caching unless the `cacheDisabled` profile is active. There is no documentation describing cache invalidation rules or expected time to live.

## Database functions
Liquibase scripts reference helper functions such as `collectgeometryfromepisodes` and `collectGeomFromGeoJSON`. Their definitions are not present in the repository, making it difficult to understand how geometry is aggregated.

## `feed_event_status` usage
The table `feed_event_status` tracks the latest events per feed, but there are no services referencing it directly in the codebase. It is unclear how the table is maintained.

## External dependencies
The project depends on a private Maven repository (`nexus.kontur.io`). Without access to it the build and tests cannot be executed.
