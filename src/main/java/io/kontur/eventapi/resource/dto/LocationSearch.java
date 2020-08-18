package io.kontur.eventapi.resource.dto;

import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.util.List;

//@ValidLocationFilter
public class LocationSearch {

//    @ValidBbox
    @Parameter(description = "Only hazards that have a geometry that intersects the bounding box are selected. The bounding box is provided as four numbers  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2  The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).  For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).  If a hazard geometry has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries.")
    private List<BigDecimal> bbox;

//    @WktGeometry()
    @Parameter(description = "a WKT geometry showing specifying the search area, e.g. geometry=POINT(82.927 55.028). This parameter is often combined with distance. bbox can not be used with geometry.")
    private String geometry;

    @Parameter(description = "Used together with geometry. Allows to specify search distance from a given point in meters.")
    private BigDecimal distance;

    public List<BigDecimal> getBbox() {
        return bbox;
    }

    public void setBbox(List<BigDecimal> bbox) {
        this.bbox = bbox;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }
}
