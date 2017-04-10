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
package com.qwazr.search.analysis;

import com.qwazr.search.field.FieldDefinition;
import com.qwazr.server.ServerException;
import com.qwazr.utils.ClassLoaderUtils;
import com.qwazr.utils.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AnalyzerContext {

	private final static Logger LOGGER = LoggerFactory.getLogger(AnalyzerContext.class);

	public final Map<String, Analyzer> indexAnalyzerMap;
	public final Map<String, Analyzer> queryAnalyzerMap;

	public AnalyzerContext(final ResourceLoader resourceLoader, final Map<String, FieldDefinition> fields,
			final boolean failOnException, final Map<String, ? extends AnalyzerFactory>... analyzerFactoryMaps)
			throws ServerException {

		if (fields == null || fields.size() == 0) {
			this.indexAnalyzerMap = Collections.emptyMap();
			this.queryAnalyzerMap = Collections.emptyMap();
			return;
		}

		this.indexAnalyzerMap = new HashMap<>();
		this.queryAnalyzerMap = new HashMap<>();

		final AnalyzerMapBuilder builder = new AnalyzerMapBuilder(resourceLoader, analyzerFactoryMaps);

		fields.forEach((fieldName, fieldDef) -> {
			try {
				final Analyzer indexAnalyzer =
						StringUtils.isEmpty(fieldDef.analyzer) ? null : builder.findAnalyzer(fieldDef.analyzer);
				if (indexAnalyzer != null)
					indexAnalyzerMap.put(fieldName, indexAnalyzer);

				final Analyzer queryAnalyzer = StringUtils.isEmpty(fieldDef.query_analyzer) ?
						indexAnalyzer :
						builder.findAnalyzer(fieldDef.query_analyzer);
				if (queryAnalyzer != null)
					queryAnalyzerMap.put(fieldName, queryAnalyzer);

			} catch (ReflectiveOperationException | InterruptedException | IOException e) {
				final String msg = "Analyzer class " + fieldDef.analyzer + " not known for the field " + fieldName;
				if (failOnException)
					throw new ServerException(Response.Status.NOT_ACCEPTABLE, msg, e);
				else if (LOGGER.isWarnEnabled())
					LOGGER.warn(msg, e);
			}
		});
	}

	final static String[] analyzerClassPrefixes = { StringUtils.EMPTY, "org.apache.lucene.analysis." };

	final static class AnalyzerMapBuilder {

		private final ResourceLoader resourceLoader;
		private final Map<String, ? extends AnalyzerFactory>[] analyzerFactoryMaps;
		private final Map<String, Analyzer> analyzerSingletonMap;

		AnalyzerMapBuilder(final ResourceLoader resourceLoader,
				final Map<String, ? extends AnalyzerFactory>... analyzerFactoryMaps) {
			this.resourceLoader = resourceLoader;
			this.analyzerFactoryMaps = analyzerFactoryMaps;
			this.analyzerSingletonMap = new HashMap<>();
		}

		Analyzer findAnalyzer(final String analyzerName)
				throws InterruptedException, ReflectiveOperationException, IOException {
			Analyzer analyzer = analyzerSingletonMap.get(analyzerName);
			if (analyzer != null)
				return analyzer;
			analyzer = getFromFactory(analyzerName);
			if (analyzer == null) {
				final Class<Analyzer> analyzerClass = ClassLoaderUtils.findClass(analyzerName, analyzerClassPrefixes);
				analyzer = analyzerClass.newInstance();
			}
			analyzerSingletonMap.put(analyzerName, analyzer);
			return analyzer;
		}

		Analyzer getFromFactory(final String analyzerName) throws IOException, ReflectiveOperationException {
			if (analyzerFactoryMaps == null)
				return null;
			for (final Map<String, ? extends AnalyzerFactory> analyzerFactoryMap : analyzerFactoryMaps) {
				if (analyzerFactoryMap != null) {
					final AnalyzerFactory factory = analyzerFactoryMap.get(analyzerName);
					if (factory != null)
						return factory.createAnalyzer(resourceLoader);
				}
			}
			return null;
		}
	}
}