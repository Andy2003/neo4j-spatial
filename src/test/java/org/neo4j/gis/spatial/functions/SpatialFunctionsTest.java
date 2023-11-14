package org.neo4j.gis.spatial.functions;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.neo4j.exceptions.KernelException;
import org.neo4j.gis.spatial.AbstractApiTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SpatialFunctionsTest extends AbstractApiTest {

    @Override
    protected void registerApiProceduresAndFunctions() throws KernelException {
        registerProceduresAndFunctions(SpatialFunctions.class);
    }

    @Test
    public void wktToGeoJson() {
        String wkt = "MULTIPOLYGON(((15.3 60.2, 15.3 60.4, 15.7 60.4, 15.7 60.2, 15.3 60.2)))";
        Object json = executeObject("return spatial.convert.wktToGeoJson($wkt) as json", Map.of("wkt", wkt), "json");
        assertThat(json, equalTo(Map.of(
            "type", "MultiPolygon",
            "coordinates", List.of( // MultiPolygon
                List.of( // Polygon
                    List.of( // LineString
                        List.of(15.3, 60.2),
                        List.of(15.3, 60.4),
                        List.of(15.7, 60.4),
                        List.of(15.7, 60.2),
                        List.of(15.3, 60.2)
                    )
                )
            )
        )));
    }
}
