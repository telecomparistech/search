/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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
package com.qwazr.search.index;

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.TimeTracker;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.qwazr.search.field.FieldDefinition.SCORE_FIELD;

class ResultDocumentsBuilder {

    final Map<String, Object> collectors;
    final LinkedHashMap<String, Map<String, Number>> facets;
    final String queryDebug;
    final TimeTracker.Status timeTrackerStatus;
    final long totalHits;

    ResultDocumentsBuilder(final QueryDefinition queryDefinition,
                           final TopDocs topDocs,
                           final IndexSearcher indexSearcher,
                           final Query luceneQuery,
                           final Highlighters highlighters,
                           final Map<String, Object> externalCollectorsResults,
                           final TimeTracker timeTracker,
                           final FacetsBuilder facetsBuilder,
                           final long totalHits,
                           @NotNull final ResultDocumentsInterface resultDocuments) throws IOException {

        this.collectors = externalCollectorsResults;

        if (topDocs != null && topDocs.scoreDocs != null) {

            int pos = 0;
            for (final ScoreDoc scoreDoc : topDocs.scoreDocs) {
                if (scoreDoc instanceof FieldDoc && queryDefinition.getSorts().containsKey(SCORE_FIELD)) {
                    scoreDoc.score = extractScoreValue((FieldDoc) scoreDoc, queryDefinition.getSorts().keySet());
                }
                resultDocuments.doc(indexSearcher, pos++, scoreDoc);
            }

            if (timeTracker != null)
                timeTracker.next("documents");

            if (highlighters != null && topDocs.scoreDocs.length > 0) {
                final LinkedHashMap<String, String[]> snippetsMap = highlighters.highlights(luceneQuery, topDocs);
                snippetsMap.forEach((name, snippetsByDoc) -> {
                    int pos2 = 0;
                    for (String snippet : snippetsByDoc)
                        resultDocuments.highlight(pos2++, name, snippet);
                });
                if (timeTracker != null)
                    timeTracker.next("highlighting");
            }
        }

        this.totalHits = totalHits;

        this.facets = facetsBuilder == null ? null : facetsBuilder.results;
        this.queryDebug = Boolean.TRUE.equals(queryDefinition.getQueryDebug()) && luceneQuery != null ?
            luceneQuery.toString(StringUtils.EMPTY) :
            null;

        this.timeTrackerStatus = timeTracker == null ? null : timeTracker.getStatus();
    }

    private float extractScoreValue(final FieldDoc scoreDoc, final Set<String> sortFields) {
        final int scoreFieldIndex = (int) sortFields.stream()
            .takeWhile(field -> !SCORE_FIELD.equals(field)).count();
        return (float) scoreDoc.fields[scoreFieldIndex];
    }

}
