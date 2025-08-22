# Event Administration UI

Currently, Event API processes events in a fully automated manner. But sometimes we need a manual control for them. We need a UI and business logic for event management from the Kontur side. 

It should be used for internal needs *and may be used by our customers (ex. PDC). They can add events to their feed and manage them through our panel. Or they have their own installation of Event API and DN and they can use this UI (How to organize access?)*

## Main goals
* Merge events 
* Update events, including fixing the result of the hotspot type classification
* Create events
* Event Viewer
* Delete events

## Use-cases
* Fix misclassified thermal spots 
* As an Admin I want to manage the pairs of events automatically merged by Event API depending on the confidence level of a merge operation in order to prevent the merging of different events. 
* As an Admin I want to create events so that the Event Feed contains an important event not reported by any of the external providers we use
* As an Admin I want to revoke the events so duplicate or wrong events won't be shown to users
* As an Event API user I want to retrieve only important events during some time period
* [[Product management/Use case: Kontur PMM wants to find a particular event in the event feed to make sure he can start promoting and share a link to that event#^c1dfa7d0-f067-11eb-8dae-6f35e26f3e74/18cc6810-b590-11ec-8700-21f69371d97d]] 

## Features

The design of the application should be easy and involve a minimal number of steps to perform a certain action.
* The possibility to use all Admin functions is accessible only by registered users with the necessary permission.

### Event Viewer
* The default list of events includes the latest updated events from the selected feed 
* Filter and sort events, not real-time, need a button to submit a request ([Swagger](https://apps.kontur.io/events/swagger-ui/index.html?configUrl=/events/v3/api-docs/swagger-config#/Events/searchEvents "https://apps.kontur.io/events/swagger-ui/index.html?configUrl=/events/v3/api-docs/swagger-config#/Events/searchEvents")):
  * Filter by: feed, type, severity, update date, start/end dates, `bbox` (draw `bbox` on map), episode filter type
  * Sort events ASC/DESC by update date
* If the user request includes more events than the limit, load new chunks separately (gradually), don't request all chunks at once ("load more" button, paging, or load new data when the user reaches the end of a list) - max 1000 events per request
* Show all available information about the disaster and its geometry (generic, all new fields are shown automatically). Currently, the event contains the following fields: 
  * `eventId`, `version`, `name`, `properName`, `description`, `startedAt`, `endedAt`, `updatedAt`, `eventDetails`, `urls`, `location`
  * `observations`, `episodes`, `geometries` fields will be used to display raw data, episodes, and geometry (they should not appear among other informative fields).
* Iterate (navigate) through episodes of events, and see the information about the selected episode and its geometry. Currently, the episode contains the following fields: 
  * `name`, `properName`, `description`, `type`, `active`, `severity`, `startedAt`, `endedAt`, `updatedAt`, `sourceUpdatedAt`, `episodeDetails`, `urls`, `location`
  * `observations`, `geometries` fields will be used to display row data and geometry (they should not appear among other informative fields).
* View in a new tab raw data of event/episode in JSON format (new request needed)
* **Future scope (improvements):**
  * *(Optional):* Auto-zoom flag which allows to automatically zoom to each selected event or manually zoom if the user wants just to see the information about selected events (useful when a user goes through events in the event panel very quickly)
  * Style geometry according to its type (with different colors). Display geometry properties. Overlapped geometries need to be reachable (clickable) (to see properties, maybe if geometries completely overlap we could display a list of features and click on geometry from the list)
  * Add more filters: search keywords in the name, description, location; select a custom polygon on the map to filter event geometry (like `bbox`)
  * Sort by severity, start date

### Event Creator
* Fill in all the fields from UI or load a GeoJson file in a specific format (validation of properties)
* Create geometry by drawing it on the map, using the one from the file (if the user uploaded one), or using the boundaries selector. 
* Make severity, event type, and geometry type selectable
* The following rules are applied for geometry types
  * `centerPoint`, `startPoint`, `globalPoint` - only for Points
  * `exposure`, `alertArea`, `globalArea` - area > 0 (Polygons, MultiPolygons) 
  * `globalArea` - by default, if the geometry is created using Boundaries Selector
  * `globalPoint` - if the point is not related to event exposure, but to a place (center of a town, village)
  * `position` - for points (???)
  * `track` - for lines

### Event Editor
* Edit values of event/episode fields on UI: `name`, `properName`, `description`, `startedAt`, `endedAt`, `updatedAt`, `eventDetails`, `urls`, `location`, `type` (for episode only), `active` (for episode only), `severity` (for episode only)
* Update more than one field at a time
* **Future scope (improvements):**
  * Edit geometry by dragging points on the map or uploading a new one.

### Event Merger
* Select groups of similar events from Event API
* Show the groups of events on UI
* Merge/Not Merge decision making
* Merge events in Event API
* [[Tasks/document: Event Tinter Feedback#^b2d59af0-3b70-11e9-be77-04d77e8d50cb/e25a5290-e638-11ec-835e-eb0311e631c6]] 

### Possible Future scope
* Revert changes
* View geometry properties
* *Optional:* Event versions
* Complex filters: by keywords in name, description
