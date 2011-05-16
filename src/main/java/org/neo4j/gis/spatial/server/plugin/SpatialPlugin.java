/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.server.plugin;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.gis.spatial.DynamicLayer;
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.query.SearchWithin;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

@Description("a set of extensions that perform operations using the neo4j-spatial component")
public class SpatialPlugin extends ServerPlugin {

	@PluginTarget(GraphDatabaseService.class)
	@Description("add a new layer specialized at storing simple point location data")
	public Iterable<Node> addSimplePointLayer(
			@Source GraphDatabaseService db,
			@Description("The layer to find or create.") @Parameter(name = "layer") String layer,
			@Description("The node property that contains the latitude. Default is 'lat'") @Parameter(name = "lat", optional = true) String lat,
			@Description("The node property that contains the longitude. Default is 'lon'") @Parameter(name = "lon", optional = true) String lon) {
		System.out.println("Creating new layer '" + layer + "' unless it already exists");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);
		return toArray(spatialService.getOrCreatePointLayer(layer, lon, lat).getLayerNode());
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("add a new layer specialized at storing generic geometry data in WKB")
	public Iterable<Node> addEditableLayer(@Source GraphDatabaseService db,
			@Description("The layer to find or create.") @Parameter(name = "layer") String layer) {
		System.out.println("Creating new layer '" + layer + "' unless it already exists");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);
		return toArray(spatialService.getOrCreateEditableLayer(layer).getLayerNode());
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("add a new dynamic layer exposing a filtered view of an existing layer")
	public Iterable<Node> addCQLDynamicLayer(
			@Source GraphDatabaseService db,
			@Description("The master layer to find") @Parameter(name = "master_layer") String master_layer,
			@Description("The name for the new dynamic layer") @Parameter(name = "name") String name,
			@Description("The type of geometry to use for streaming data from the new view") @Parameter(name = "geometry", optional = true) String geometry,
			@Description("The CQL query to use for defining this dynamic layer") @Parameter(name = "layer") String query) {
		System.out.println("Creating new dynamic layer '" + name + "' from existing layer '" + master_layer + "'");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);
		DynamicLayer dynamicLayer = spatialService.asDynamicLayer(spatialService.getLayer(master_layer));
		int gtype = SpatialDatabaseService.convertGeometryNameToType(geometry);
		return toArray(dynamicLayer.addLayerConfig(name, gtype, query).getLayerNode());
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("find an existing layer")
	public Iterable<Node> getLayer(@Source GraphDatabaseService db,
			@Description("The layer to find.") @Parameter(name = "layer") String layer) {
		System.out.println("Finding layer '" + layer + "'");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);
		return toArray(spatialService.getLayer(layer).getLayerNode());
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("add a geometry node to a layer, as long as the node contains the geometry information appropriate to this layer.")
	public Iterable<Node> addNodeToLayer(@Source GraphDatabaseService db,
			@Description("The node representing a geometry to add to the layer") @Parameter(name = "node") Node node,
			@Description("The layer to add the node to.") @Parameter(name = "layer") String layer) {
		System.out.println("Finding layer '" + layer + "'");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);

		EditableLayer spatialLayer = (EditableLayer) spatialService.getLayer(layer);
		Transaction tx = db.beginTx();
		try {
		    spatialLayer.add(node);
		    tx.success();
		} catch (Exception e) {
		    tx.failure();
		} finally {
		    tx.finish();
		}
		return toArray(node);
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("add a geometry specified in WKT format to a layer, encoding in the specified layers encoding schemea.")
	public Iterable<Node> addGeometryWKTToLayer(@Source GraphDatabaseService db,
			@Description("The geometry in WKT to add to the layer") @Parameter(name = "node") String geometryWKT,
			@Description("The layer to add the node to.") @Parameter(name = "layer") String layer) {
		System.out.println("Adding geometry to layer '" + layer + "': " + geometryWKT);
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);

		EditableLayer spatialLayer = (EditableLayer) spatialService.getLayer(layer);
		try {
			WKTReader reader = new WKTReader(spatialLayer.getGeometryFactory());
			Geometry geometry = reader.read(geometryWKT);
			SpatialDatabaseRecord record = spatialLayer.add(geometry);
			return toArray(record.getGeomNode());
		} catch (ParseException e) {
			System.err.println("Invalid Geometry: " + e.getLocalizedMessage());
		}
		return null;
	}

	@PluginTarget(GraphDatabaseService.class)
	@Description("search a layer for geometries in a bonding box. To achieve more complex CQL searches, pre-define the dynamic layer with addCQLDynamicLayer.")
	public Iterable<Node> findGeometriesInLayer(
			@Source GraphDatabaseService db,
			@Description("The minimum x value of the bounding box") @Parameter(name = "minx") double minx,
			@Description("The maximum x value of the bounding box") @Parameter(name = "maxx") double maxx,
			@Description("The minimum y value of the bounding box") @Parameter(name = "miny") double miny,
			@Description("The maximum y value of the bounding box") @Parameter(name = "maxy") double maxy,
			@Description("The layer to search. Can be a dynamic layer with pre-defined CQL filter.") @Parameter(name = "layer") String layerName) {
//		System.out.println("Finding layer '" + layerName + "'");
		SpatialDatabaseService spatialService = new SpatialDatabaseService(db);

		Layer layer = spatialService.getDynamicLayer(layerName);
		if(layer == null ) {
		    layer = spatialService.getLayer(layerName);
		}
		SearchWithin withinQuery = new SearchWithin(layer.getGeometryFactory().toGeometry(new Envelope(minx, maxx, miny, maxy)));
		layer.getIndex().executeSearch(withinQuery);
		List<SpatialDatabaseRecord> results = withinQuery.getResults();
		return toIterable(results);
	}

	private Iterable<Node> toArray(Node node) {
		ArrayList<Node> result = new ArrayList<Node>();
		if (result != null)
			result.add(node);
		return result;
	}

	private Iterable<Node> toIterable(List<SpatialDatabaseRecord> records) {
		ArrayList<Node> result = new ArrayList<Node>();
		for (SpatialDatabaseRecord record : records) {
			result.add(record.getGeomNode());
		}
		return result;
	}
}
