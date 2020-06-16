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
package com.qwazr.search.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.search.index.QueryContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.ScoreMode;

import java.io.IOException;
import java.util.Objects;

public class JoinQuery extends AbstractQuery<JoinQuery> {

    @JsonProperty("from_index")
    final public String fromIndex;
    @JsonProperty("from_field")
    final public String fromField;
    final public String toField;
    @JsonProperty("multiple_values_per_document")
    final public Boolean multipleValuesPerDocument;
    @JsonProperty("score_mode")
    final public ScoreMode scoreMode;
    @JsonProperty("from_query")
    final public AbstractQuery<?> fromQuery;

    @JsonCreator
    public JoinQuery(@JsonProperty("from_index") final String fromIndex,
                     @JsonProperty("from_field") final String fromField,
                     @JsonProperty("to_field") final String toField,
                     @JsonProperty("multiple_values_per_document") final Boolean multipleValuesPerDocument,
                     @JsonProperty("score_mode") final ScoreMode scoreMode,
                     @JsonProperty("from_query") final AbstractQuery<?> fromQuery) {
        super(JoinQuery.class);
        this.fromIndex = fromIndex;
        this.fromField = fromField;
        this.toField = toField;
        this.multipleValuesPerDocument = multipleValuesPerDocument;
        this.scoreMode = scoreMode;
        this.fromQuery = fromQuery;
    }

    @Override
    final public Query getQuery(final QueryContext queryContext) throws IOException {
        return queryContext.getIndex(fromIndex).createJoinQuery(this);
    }

    @Override
    protected boolean isEqual(final JoinQuery q) {
        return Objects.equals(fromIndex, q.fromIndex)
            && Objects.equals(fromField, q.fromField)
            && Objects.equals(toField, q.toField)
            && Objects.equals(multipleValuesPerDocument, q.multipleValuesPerDocument)
            && Objects.equals(scoreMode, q.scoreMode)
            && Objects.equals(fromQuery, q.fromQuery);
    }

    @Override
    protected int computeHashCode() {
        return Objects.hash(fromIndex, fromField, toField, multipleValuesPerDocument, scoreMode, fromQuery);
    }
}
