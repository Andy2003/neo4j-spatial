package org.neo4j.gis.spatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.neo4j.gis.spatial.utilities.GeoJsonUtils;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class SpatialFunctions {

    @UserFunction("spatial.convert.wktToGeoJson")
    @Description("Converts a WKT to GeoJson structure")
    public Object wktToGeoJson(@Name("wkt") String wkt) throws ParseException {
        if (wkt == null) {
            return null;
        }
        WKTReader wktReader = new WKTReader();
        Geometry geometry = wktReader.read(wkt);
        return GeoJsonUtils.toGeoJsonStructure(geometry);
    }
}
