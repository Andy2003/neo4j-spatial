package org.neo4j.gis.spatial.utilities;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;


public class GeoJsonUtils {

    private GeoJsonUtils() {
    }

    public static Map<String, Object> toGeoJsonStructure(Geometry geometry) {
        return Map.of(
            "type", geometry.getGeometryType(),
            "coordinates", getCoordinates(geometry)
        );
    }

    private static List<?> getCoordinates(Geometry geometry) {
        if (geometry instanceof Point point) {
            return getPoint(point.getCoordinate());
        }
        if (geometry instanceof LineString lineString) {
            return Arrays.stream(lineString.getCoordinates()).map(GeoJsonUtils::getPoint).toList();
        }
        if (geometry instanceof Polygon polygon) {
            return Stream.concat(
                    Stream.of(polygon.getExteriorRing()),
                    IntStream.range(0, polygon.getNumInteriorRing())
                        .mapToObj(polygon::getInteriorRingN)
                )
                .map(GeoJsonUtils::getCoordinates)
                .toList();
        }
        if (geometry instanceof GeometryCollection geometryCollection) {
            return IntStream.range(0, geometryCollection.getNumGeometries())
                .mapToObj(geometryCollection::getGeometryN)
                .map(GeoJsonUtils::getCoordinates)
                .toList();
        }
        throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
    }

    private static List<Object> getPoint(Coordinate coordinate) {
        if (Double.isNaN(coordinate.getZ())) {
            return List.of(coordinate.getX(), coordinate.getY());
        }
        return List.of(
            coordinate.getX(),
            coordinate.getY(),
            coordinate.getZ());
    }
}
