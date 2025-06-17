# EM-Dat Type Classification

The EM-Dat provider supplies hazard records using its own set of disaster types.
To keep the internal event type list small, these values are mapped to the
`EventType` enumeration. The mapping is stored in
`src/main/resources/emdat-classification.json` and is loaded on start up by
`EmDatTypeClassifier`.

When normalizing EM-Dat data the classifier checks **Disaster Subsubtype**, then
**Subtype** and finally **Type**. The first known entry determines the
`EventType` value for the observation. Unknown values fall back to
`EventType.OTHER`.
