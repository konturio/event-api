# RFC2 - Timestamps

Requirements to keep in mind:
* Here time is System Time - it never goes back and can be used as pagination.
* Here time is Valid Time - if we learned yesterday about an event 500 years ago, it should be in the range request of 500 years ago, not in request for yesterday.
* We have two timelines: Valid time progression is encoded in Episodes, System time progression is encoded as Event Snapshot Versions.
* Kontur wants external entities to be able to use Event API installations with their data flows.
* Event API can be chained into other Event API.

Thus:
* All timestamp fields in the system need to be marked as Valid or System time.
  * observation has:
    * started_at - valid time
    * ended_at - valid time
    * loaded_at - system time
    * source_updated_at - system time
  * version has:
    * started_at - valid time
    * ended_at - valid time
    * updated_at == loaded_at - system time
  * episode has:
    * startedAt - valid time
    * endedAt - valid time
    * updatedAt==loaded_at - system time
    * sourceUpdatedAt - system time
    * episode.geometries.features.properties updated_at == source_updated_at - system time
* Each episode needs to have a valid time.
  * started_at and ended_at should be present for all the episodes.
  * intervals of episodes of \[started_at, ended_at) should not overlap.
  * started_at and ended_at may be in the future as well as in the past.
* Each event needs to have a system time.
  * It does not need to be an interval so can be just one timestamp.
  * It must never go back so that it can be used for pagination.
