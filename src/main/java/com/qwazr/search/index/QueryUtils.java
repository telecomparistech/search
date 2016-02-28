/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.search.index;

import com.qwazr.search.analysis.AnalyzerContext;
import com.qwazr.search.field.SortUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.TimeTracker;
import com.qwazr.utils.server.ServerException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

class QueryUtils {

	final static String getFinalQueryString(QueryDefinition queryDef) {
		// Deal wih query string
		final String qs;
		// Check if we have to escape some characters
		if (queryDef.escape_query != null && queryDef.escape_query) {
			if (queryDef.escaped_chars != null && queryDef.escaped_chars.length > 0)
				qs = StringUtils.escape_chars(queryDef.query_string, queryDef.escaped_chars);
			else
				qs = QueryParser.escape(queryDef.query_string);
		} else
			qs = queryDef.query_string;
		return qs;
	}

	final static Query getLuceneQuery(QueryContext queryContext)
			throws QueryNodeException, ParseException, IOException, ReflectiveOperationException {

		Query query = queryContext.queryDefinition.query == null ?
				new MatchAllDocsQuery() :
				queryContext.queryDefinition.query.getBoostedQuery(queryContext);

		return query;
	}

	final static ResultDefinition search(final QueryContext queryContext)
			throws ServerException, IOException, QueryNodeException, InterruptedException, ParseException,
			ReflectiveOperationException {

		final QueryDefinition queryDef = queryContext.queryDefinition;

		Query query = getLuceneQuery(queryContext);

		final TimeTracker timeTracker = new TimeTracker();

		final AnalyzerContext analyzerContext = queryContext.analyzer.getContext();
		final Sort sort =
				queryDef.sorts == null ? null : SortUtils.buildSort(analyzerContext.fieldTypes, queryDef.sorts);

		final int numHits = queryDef.getEnd();
		final boolean bNeedScore = sort != null ? sort.needsScores() : true;

		final QueryCollectors queryCollectors = new QueryCollectors(bNeedScore, sort, numHits, queryDef.facets,
				queryDef.functions, analyzerContext.fieldTypes);

		queryContext.indexSearcher.search(query, queryCollectors.finalCollector);
		final TopDocs topDocs = queryCollectors.getTopDocs();
		final Integer totalHits = queryCollectors.getTotalHits();

		timeTracker.next("search_query");

		final FacetsBuilder facetsBuilder = queryCollectors.facetsCollector == null ?
				null :
				new FacetsBuilder(queryContext, queryDef.facets, query, queryCollectors.facetsCollector, timeTracker);

		Map<String, String[]> postingsHighlightersMap = null;
		if (queryDef.postings_highlighter != null && topDocs != null) {
			postingsHighlightersMap = new LinkedHashMap<>();
			for (Map.Entry<String, Integer> entry : queryDef.postings_highlighter.entrySet()) {
				String field = entry.getKey();
				PostingsHighlighter highlighter = new PostingsHighlighter(entry.getValue());
				String highlights[] = highlighter.highlight(field, query, queryContext.indexSearcher, topDocs);
				if (highlights != null) {
					postingsHighlightersMap.put(field, highlights);
				}
			}
			timeTracker.next("postings_highlighters");
		}

		return new ResultDefinition(analyzerContext.fieldTypes, timeTracker, queryContext.indexSearcher, totalHits,
				topDocs, queryDef, facetsBuilder, postingsHighlightersMap, queryCollectors.functionsCollectors, query);
	}

}
