# Event Combination

This process groups normalized observations into events. A scheduled job selects
observations that are not yet linked to an event and tries to find a matching
event for each of them. If none exists, a new event ID is generated.

1. **Select observations** – rows from `normalized_observations` where
   `recombined` is `false` are fetched for the configured providers.
2. **Find or create event** – an `EventCombinator` searches for an event
   corresponding to the observation. The default implementation looks up an
   event by `external_event_id`.
3. **Append observation** – the observation is inserted into `kontur_events`
   with `(event_id, provider, observation_id)` and marked as recombined so it is
   not processed again.
4. **Feed update** – other jobs later build feed data from the accumulated
   observations.

Provider specific combinators may override this logic. For example FIRMS
wildfire observations are clustered by geometry before new events are created.

## USGS earthquakes

Earthquake observations from USGS do not contain separate episodes. Each update
represents the same event with more details, so the default combination job just
groups observations by `external_event_id`. When several updates for the same
earthquake are present, the observation with the most recent ShakeMap geometry
provides the geometry for the resulting event.

During normalization the `place` property is split by the last comma and the
trailing part is stored as the observation `region`. A human‑readable episode
description is composed using the event start time, magnitude and depth from the
source data, for example: `On 7/5/2025 7:42:20 PM, an earthquake occurred 55 km
SSW of Lithakiá, Greece. The earthquake had Magnitude 4.7M, Depth:10km.` The
observation `name` becomes `M <magnitude> - <place>` and `proper_name` remains
`null`.
