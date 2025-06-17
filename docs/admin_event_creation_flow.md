# Admin UI Event Creation Flow

This document outlines a high level design of how events can be created from the Administration UI.

## Overview
Administrators can manually add events when automated imports do not provide enough control. The UI exposes a form that collects basic metadata, geometry and optional attachments. When the form is submitted the data is sent to the Event API which stores the event in the database and triggers any downstream processing.

## Steps
1. **Authentication** – an administrator signs in using existing credentials.
2. **Open Create Event page** – the page is available from the navigation menu.
3. **Fill in event details** – feed name, event type, severity and description are mandatory. Start and end dates are optional.
4. **Provide geometry** – the UI allows drawing a polygon or uploading GeoJSON. Geometry is validated in the browser.
5. **Add attachments** – documents or images can be uploaded and will be stored in the static data bucket.
6. **Review and submit** – after validation the UI sends a `POST` request to `/v1/admin/events` with a JSON payload describing the event.
7. **Confirmation** – on success the user is redirected to the newly created event page.

## Notes
* The Event API should check that the user possesses the `create:event` permission.
* Validation errors returned by the API are displayed inline next to the corresponding fields.
* Submitted events become immediately available for search through regular API endpoints.
