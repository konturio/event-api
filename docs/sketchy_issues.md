# Open Questions and Potential Issues

During review a few areas looked unclear or potentially problematic. They may require further clarification:

## `PdcSqsMessageListener`
The listener contains a TODO comment to "skip products until it is clear how to handle them". It is not documented what kind of SQS messages are ignored and whether products will be supported in the future.

## Caching behaviour
`EventResourceService` enables caching unless the `cacheDisabled` profile is active. There is no documentation describing cache invalidation rules or expected time to live.

## Database functions
Liquibase scripts reference helper functions such as `collectgeometryfromepisodes` and `collectGeomFromGeoJSON`. Their definitions are not present in the repository, making it difficult to understand how geometry is aggregated.

## `feed_event_status` usage
The table `feed_event_status` tracks the latest events per feed, but there are no services referencing it directly in the codebase. It is unclear how the table is maintained.

## External dependencies
Earlier versions required a private Maven repository (`nexus.kontur.io`).
The build now fetches all artifacts from Maven Central and OSGeo, so
no private access is needed.
