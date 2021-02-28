/*
 * Copyright (c) 2010-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Spatial.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class LayerSignatureTest extends Neo4jTestCase implements Constants {
    private SpatialDatabaseService spatialService;

    @Before
    public void setup() throws Exception {
        super.setUp();
        spatialService = new SpatialDatabaseService(graphDb());
    }

    @Test
    public void testSimplePointLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=SimplePointEncoder(x='lng', y='lat', bbox='bbox'))",
                tx -> spatialService.createSimplePointLayer(tx, "test", "lng", "lat"));
    }

    @Test
    public void testNativePointLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=NativePointEncoder(geometry='position', bbox='mbr', crs=4326))",
                tx -> spatialService.createNativePointLayer(tx, "test", "position", "mbr"));
    }

    @Test
    public void testDefaultSimplePointLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=SimplePointEncoder(x='longitude', y='latitude', bbox='bbox'))",
                tx -> spatialService.createSimplePointLayer(tx, "test"));
    }

    @Test
    public void testSimpleWKBLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=WKBGeometryEncoder(geom='geometry', bbox='bbox'))",
                tx -> spatialService.createWKBLayer(tx, "test"));
    }

    @Test
    public void testWKBLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=WKBGeometryEncoder(geom='wkb', bbox='bbox'))",
                tx -> spatialService.getOrCreateEditableLayer(tx, "test", "wkb", "wkb"));
    }

    @Test
    public void testWKTLayer() {
        testLayerSignature("EditableLayer(name='test', encoder=WKTGeometryEncoder(geom='wkt', bbox='bbox'))",
                tx -> spatialService.getOrCreateEditableLayer(tx, "test", "wkt", "wkt"));
    }

    private Layer testLayerSignature(String signature, Function<Transaction, Layer> layerMaker) {
        Layer layer;
        try (Transaction tx = graphDb().beginTx()) {
            layer = layerMaker.apply(tx);
            tx.commit();
        }
        assertEquals(signature, layer.getSignature());
        return layer;
    }

    private void inTx(Consumer<Transaction> txFunction) {
        try (Transaction tx = graphDb().beginTx()) {
            txFunction.accept(tx);
            tx.commit();
        }
    }

    @Test
    public void testDynamicLayer() {
        Layer layer = testLayerSignature("EditableLayer(name='test', encoder=WKTGeometryEncoder(geom='wkt', bbox='bbox'))",
                tx -> spatialService.getOrCreateEditableLayer(tx, "test", "wkt", "wkt"));
        inTx(tx -> {
            DynamicLayer dynamic = spatialService.asDynamicLayer(tx, layer);
            assertEquals("EditableLayer(name='test', encoder=WKTGeometryEncoder(geom='wkt', bbox='bbox'))", dynamic.getSignature());
            DynamicLayerConfig points = dynamic.addCQLDynamicLayerOnAttribute(tx, "is_a", "point", GTYPE_POINT);
            assertEquals("DynamicLayer(name='CQL:is_a-point', config={layer='CQL:is_a-point', query=\"geometryType(the_geom) = 'Point' AND is_a = 'point'\"})", points.getSignature());
        });
    }

}
