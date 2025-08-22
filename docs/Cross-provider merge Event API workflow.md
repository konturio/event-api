# Cross-provider merge Event API workflow

What we want:
* Same events from different providers merged into one
* In feeds containing a limited list of providers only merging between those providers should be performed
* Merge operations can be revoked in case of error

What we don't want:

Issues:
* How to track events taken for merge, so that two people don't work on the same events (SOLVED)
* How to re-use merge pairs for different stages (use external disaster ID, if the provider doesn't have an external ID, like firms, use the hash as an external id, save each observation as a separate event and combine it with events from the same provider using merge)

### Changes to the data structure
* Feed Data

|     |     |     |
| --- | --- | --- |
| Column name | Type | Description |
| `merge_done` | boolean | Flag to signal the cross-provider merge ran for this event |
| `revoked` | boolean | Flag to signal that this event is revoked and shouldn't be returned through API |
* Merge Candidate

|     |     |     |
| --- | --- | --- |
| Column name | Type | Description |
| `merge_operation_id` | uuid | The ID of the merge operation between two events that need to be approved |
| `event_id` | text | The external ID of event that is a candidate for merge |
| `primary` | boolean | Shows if the event is primary  |
* Merge Operation

|     |     |     |
| --- | --- | --- |
| Column name | Type | Description |
| `merge_operation_id` | uuid | The ID of the merge operation |
| `event_id1` | text | The external ID of the event that is a first candidate for merge |
| `event_id2` | text | The external ID of the event that is a second candidate for merge |
| `confidence` | double | The confidence level that shows how similar two events are based on rules defined |
| `approved` | boolean | Flag to signal whether or not the merge operation was approved |
| `approved_by` | text | Username of user who approved the merge operation |
| `executed` | boolean | Flag to signal whether the events were already merged in Event API |
| `approved_at` | timestamp with timezone | Time when merge decision was saved to s3 |
| `taken_to_merge_at` | timestamp with timezone | The time when the merge candidates were taken to perform the manual merge. |

### How to merge only specific providers?

Cross-provider merge shouldn't run between all providers (to save time if know that some providers don't have similar events and reduce the number of pairs to approve).

Define a configuration with groups of providers which we want to be able to merge with each other.  The configuration should include Disaster Type and a list of providers we want to merge.

During cross-provider merge, we take only events of a configured type with at least one observation from a configured provider.

### How to avoid double-work

Problem: users get merge candidates on FE to perform merge, we need to avoid the situation when 2 users work on the same set of pairs when there are more pairs in the queue.

When the Event API returns the merge candidates through API, the `taken_to_merge_at` field is updated to the current time. Merge candidates are returned in ASC order by `taken_to_merge_at` (nulls first).

### Cross-provider merge

Cross-provider merge job runs in configurable intervals, goes through events `merge_done = false` , and tries to find candidates to merge with this event. All the candidates that fit the requirements are saved into the Merge Candidate / Decision table with `approved = false` and `confidence` level.

### Administration

DN BE calls Event API to get merge candidates. The user goes through all the pairs and makes a decision on whether events should be merged or not. When a user is done DN BE saves JSON with decisions to S3 in the following format (pairs, where the user is not sure, are not being sent to S3):
* `event_id1` - first event ID
* `event_id2` - second event ID
* `approved` - true/false
* `approved_by` - username

### Import merge decisions into Event API

Event API import job checks S3 in configurable intervals to find new files with merge decisions. 

This job updates `approved` and `approved_by` columns in the Merge Decision table. The `approved_at` is set to the time when the s3 file was updated so that we can determine which files are new later.

### Recalculate events

The recalculation job runs in configurable intervals of time and searches for merge operations that were `approved` but not yet `executed`. 

It updates the table `feed_event_status` to mark event pairs as not `actual`, so they would be taken again for feed composition after that marks merge operation as `executed`.

### Feed Composition

For each event coming to feed composition, we check if the event was merged in the Merge Candidates / Decision table. If not, it's processed as a regular event, if yes, we search for all the events this event was merged with and take all their observations from providers that are listed for the feed. 

![cross-provider merge.drawio (1).png](https://kontur.fibery.io/api/files/841f8e8f-d38f-4fcd-976a-b713b329183e#width=1041&height=1061 "")

### Merge Rules
* Initial merge rules (PDC - EM-DAT):
  * Geometries intersect or the distance is less than 1 km (EM-DAT has large geometry computed using boundary service)
  * Date ranges intersect or the difference is less than 7 days (EM-DAT sometimes has only a year provided)
  * Event type is defined and is the same
  * Confidence level:
    * Geom confidence = 1 - Geom distance (km) / 10 (km) 
    * Date confidence = 1 - Date difference (days) / 7 (days)
    * Confidence = (Geom confidence + Date confidence) / 2
* Initial merge rules (NIFC - InciWeb - CalFire):
  * Names are not blank and one+ word repeats in both names (excluding words with 'fire' in them)
  * Geometries distance less than 1000m
  * Date ranges must intersect
    * InciWeb doesn't post the start date, so the start date is the date of the first update, but CalFire and NIFC post very accurate start dates
    * InciWeb and NIFC have regular updates so ended date is similar, but CalFire sometimes has less updates, so the end date may be earlier
  * Confidence level:
    * If the name has all matching words except words with 'fire' - confidence 100%, don't check other rules
    * If there is no pair with a matching name:
      * Geom confidence = 1 - Geom distance (m) / 1000 (m)
      * Date confidence = 1 - Dates overlap (sec) / length of smaller event (sec)
      * Confidence = (Geom confidence + Date confidence) / 2

### Priorities for fields:
* (NIFC - InciWeb - CalFire)
  * `name` - NIFC > InciWeb > CalFire
  * `properName` - NIFC > CalFire > InciWeb
  * `description` - InciWeb > NIFC > CalFire
  * `type` -  NIFC > CalFire > InciWeb
  * `active` - CalFire > NIFC > InciWeb
  * `severity` - NIFC > CalFire > InciWeb
  * `startedAt` - Do we need to define priority here?
  * `endedAt` - ??
  * `sourceUpdatedAt` - ??
  * `URLs` - ??
  * `location` - CalFire > NIFC > InciWeb
  * `geometries` - NIFC.perimeter > NIFC.location > InciWeb > CalFire

Ideas:
* Generate priorities using a script, interactive through UI (config in DB)
* Use external IDs for cross-provider merge to be able to reuse the merge decision across stages
* Duplicate decisions on events - store the latest, make a flow to resolve conflicts

