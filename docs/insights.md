# Insights API Integration

Event API obtains analytics data from Insights API using a GraphQL query. The request body now follows the standard GraphQL structure:

```json
{
  "query": "query ($polygon: String!) { polygonStatistic(polygonStatisticRequest: { polygon: $polygon }) { analytics { ... } } }",
  "variables": {
    "polygon": "<GeoJSON geometry>"
  }
}
```

Requests are sent to `/insights/graphql` endpoint of the Kontur Apps service.

