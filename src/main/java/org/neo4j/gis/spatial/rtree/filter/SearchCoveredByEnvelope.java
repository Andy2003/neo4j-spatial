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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.rtree.filter;

import org.neo4j.gis.spatial.rtree.Envelope;
import org.neo4j.gis.spatial.rtree.EnvelopeDecoder;
import org.neo4j.graphdb.Node;

/**
 * Find Envelopes covered by the given Envelope
 */
public class SearchCoveredByEnvelope extends AbstractSearchEnvelopeIntersection {

	public SearchCoveredByEnvelope(EnvelopeDecoder decoder, Envelope referenceEnvelope) {
		super(decoder, referenceEnvelope);
	}

	@Override
	protected boolean onEnvelopeIntersection(Node geomNode, Envelope geomEnvelope) {
		// check if every point of this Envelope is a point of the Reference Envelope
	    return referenceEnvelope.contains(geomEnvelope);
	}

}