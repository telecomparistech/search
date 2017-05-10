/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.search.index.QueryContext;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.Query;

import java.io.IOException;

public class LatLonPointBBoxQuery extends AbstractGeoBoxQuery {

	@JsonCreator
	public LatLonPointBBoxQuery(@JsonProperty("field") final String field,
			@JsonProperty("min_latitude") final double minLatitude,
			@JsonProperty("max_latitude") final double maxLatitude,
			@JsonProperty("min_longitude") final double minLongitude,
			@JsonProperty("max_longitude") final double maxLongitude) {
		super(field, minLatitude, maxLatitude, minLongitude, maxLongitude);
	}

	@Override
	final public Query getQuery(final QueryContext queryContext) throws IOException {
		return LatLonPoint.newBoxQuery(field, min_latitude, max_latitude, min_longitude, max_longitude);
	}
}
