# Handling the 180th meridian

Incoming geometries may cross the antimeridian. To keep coordinates consistent the API applies the following steps:

1. **Shift longitudes** using `ST_ShiftLongitude` so that all points are within `[-180, 180]` range.
2. **Split** geometries that span more than 180 degrees into separate parts along the antimeridian. This is done by the `split_antimeridian` database function.

Both `collectGeomFromGeoJSON` and `collectGeometryFromEpisodes` use this helper, so stored geometries never wrap around the 180° meridian.
