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
package com.qwazr.search.field;

import com.qwazr.search.field.Converters.ValueConverter;
import com.qwazr.search.index.BytesRefUtils;
import com.qwazr.search.index.FieldConsumer;
import com.qwazr.server.ServerException;
import com.qwazr.utils.WildcardMatcher;
import jdk.nashorn.api.scripting.JSObject;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class FieldTypeAbstract implements FieldTypeInterface {

	final private WildcardMatcher wildcardMatcher;
	final protected FieldDefinition definition;
	final protected BytesRefUtils.Converter bytesRefConverter;
	final private Map<FieldTypeInterface, String> copyToFields;

	protected FieldTypeAbstract(final WildcardMatcher wildcardMatcher, final FieldDefinition definition,
			final BytesRefUtils.Converter bytesRefConverter) {
		this.wildcardMatcher = wildcardMatcher;
		this.definition = definition;
		this.bytesRefConverter = bytesRefConverter;
		this.copyToFields = new LinkedHashMap<>();
	}

	@Override
	final public void copyTo(final String fieldName, final FieldTypeInterface fieldType) {
		copyToFields.put(fieldType, fieldName);
	}

	@Override
	final public FieldDefinition getDefinition() {
		return definition;
	}

	protected void fillArray(final String fieldName, final int[] values, final Float boost,
			final FieldConsumer consumer) {
		for (int value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillArray(final String fieldName, final long[] values, final Float boost,
			final FieldConsumer consumer) {
		for (long value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillArray(final String fieldName, final double[] values, final Float boost,
			final FieldConsumer consumer) {
		for (double value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillArray(final String fieldName, final float[] values, final Float boost,
			final FieldConsumer consumer) {
		for (float value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillArray(final String fieldName, final Object[] values, final Float boost,
			final FieldConsumer consumer) {
		for (Object value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillArray(final String fieldName, final String[] values, final Float boost,
			final FieldConsumer consumer) {
		for (String value : values)
			fill(fieldName, value, boost, consumer);
	}

	protected void fillCollection(final String fieldName, final Collection<Object> values, final Float boost,
			final FieldConsumer consumer) {
		values.forEach(value -> {
			if (value != null)
				fill(fieldName, value, boost, consumer);
		});
	}

	protected void fillMap(final String fieldName, final Map<Object, Object> values, final Float boost,
			final FieldConsumer consumer) {
		throw new ServerException(Response.Status.NOT_ACCEPTABLE,
				() -> "Map is not asupported type for the field: " + fieldName);
	}

	protected void fillJSObject(final String fieldName, final JSObject values, final Float boost,
			final FieldConsumer consumer) {
		fillCollection(fieldName, values.values(), boost, consumer);
	}

	protected void fillWildcardMatcher(final String wildcardName, final Object value, final Float boost,
			final FieldConsumer fieldConsumer) {
		if (value instanceof Map) {
			((Map<String, Object>) value).forEach((fieldName, valueObject) -> {
				if (!wildcardMatcher.match(fieldName))
					throw new ServerException(Response.Status.NOT_ACCEPTABLE,
							() -> "The field name does not match the field pattern: " + wildcardName);
				fill(fieldName, valueObject, boost, fieldConsumer);
			});
		} else
			fill(wildcardName, value, boost, fieldConsumer);
	}

	protected void fill(final String fieldName, final Object value, final Float boost,
			final FieldConsumer fieldConsumer) {
		if (value == null)
			return;
		if (value instanceof String[])
			fillArray(fieldName, (String[]) value, boost, fieldConsumer);
		else if (value instanceof int[])
			fillArray(fieldName, (int[]) value, boost, fieldConsumer);
		else if (value instanceof long[])
			fillArray(fieldName, (long[]) value, boost, fieldConsumer);
		else if (value instanceof double[])
			fillArray(fieldName, (double[]) value, boost, fieldConsumer);
		else if (value instanceof float[])
			fillArray(fieldName, (float[]) value, boost, fieldConsumer);
		else if (value instanceof Object[])
			fillArray(fieldName, (Object[]) value, boost, fieldConsumer);
		else if (value instanceof Collection)
			fillCollection(fieldName, (Collection) value, boost, fieldConsumer);
		else if (value instanceof JSObject)
			fillJSObject(fieldName, (JSObject) value, boost, fieldConsumer);
		else if (value instanceof Map)
			fillMap(fieldName, (Map) value, boost, fieldConsumer);
		else
			fillValue(fieldName, value, boost, fieldConsumer);
	}

	protected void fillValue(final String fieldName, final Object value, final Float boost,
			final FieldConsumer fieldConsumer) {
		throw new ServerException(Response.Status.NOT_ACCEPTABLE,
				() -> "Not supported type for the field: " + fieldName + ": " + value.getClass());
	}

	@Override
	final public void dispatch(final String fieldName, final Object value, final Float boost,
			final FieldConsumer fieldConsumer) {
		if (value == null)
			return;
		if (wildcardMatcher != null)
			fillWildcardMatcher(fieldName, value, boost, fieldConsumer);
		else {
			fill(fieldName, value, boost, fieldConsumer);
			if (!copyToFields.isEmpty())
				copyToFields.forEach(
						(fieldType, copyFieldName) -> fieldType.dispatch(copyFieldName, value, null, fieldConsumer));
		}
	}

	@Override
	public ValueConverter getConverter(final String fieldName, final IndexReader reader) throws IOException {
		return ValueConverter.newConverter(fieldName, definition, reader);
	}

	@Override
	final public Object toTerm(final BytesRef bytesRef) {
		return bytesRef == null ? null : bytesRefConverter == null ? null : bytesRefConverter.to(bytesRef);
	}

}
