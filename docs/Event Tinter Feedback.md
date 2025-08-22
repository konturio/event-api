# Event Tinter Feedback


The first version of Event Merger is an application [Event Tinter](https://event-tinter.surge.sh/ "https://event-tinter.surge.sh/"). 
* The complex and not obvious flow of collecting, downloading, and uploading data. Will be solved by automatically collecting events from the event-api.
* Not all information about the events is displayed
* Not all fields of the event are editable
* Async scroll in panels with fields of events, not easy to find similarity
* Small dots and lines on the map - hard to see
* The non-interactive timeline doesn't have a connection with the map
* Not optimized for a notebook screen
* "Not sure" — is it a needed button for the new approach, maybe it's enough to have Match|Not Match (smth as "Skip" may be appropriate)
* Data sources names not displayed
* Not obvious navigation through pairs, one event can have several pairs from different sources
* How to decide which field has more correct data
* When the duration is short (1 day) and events are instant, happened at different times, then it's hard to say looking at the timeline if they are a match
* Events have a limited number of fields that should have close enough values to be matched: type, start date, severity, and location. Is this set of fields enough for basic comparison?
* would be great to see groups of events at once. There are a lot of cases when more than two events need to be merged, so it would great to see them somehow grouped.
* not easy to rich raw data (i checked EM-DAT coordinates and EM-DAT magnitude) but maybe it is the sign that we need to keep such data in the feed
* Good: different colors for providers (when I see 2 events on the map at the same time, I need to understand where each of them is).
* Good - automatic navigation between events (go to the next pair at once after the decision is made)
* The map doesn't zoom in to see both events. Often need to zoom out to see both events
