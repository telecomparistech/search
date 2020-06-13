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
package com.qwazr.search.field;

import com.qwazr.search.field.converters.MultiReader;
import com.qwazr.search.field.converters.ValueConverter;
import com.qwazr.search.index.DocumentBuilder;
import com.qwazr.search.index.FieldMap;
import com.qwazr.search.index.QueryDefinition;
import com.qwazr.utils.WildcardMatcher;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

public interface FieldTypeInterface {

    void dispatch(final String fieldName, final Object value, final DocumentBuilder luceneDocumentBuilder);

    SortField getSortField(final String fieldName, final QueryDefinition.SortEnum sortEnum);

    ValueConverter<?> getConverter(final String fieldName, final MultiReader reader);

    Object toTerm(final BytesRef bytesRef);

    String resolveFieldName(@Nullable final String fieldName, @Nullable final FieldType fieldType, @Nullable final ValueType valueType);

    Term newPrimaryTerm(final String fieldName, final Object value);

    Term newIndexTerm(final String fieldName, final Object value);

    ValueType getValueType();

    FieldType findFirstOf(final FieldType... expectedTypes);

    FieldDefinition getDefinition();

    void copyTo(final String fieldName, final FieldTypeInterface fieldType);

    void applyFacetsConfig(final String fieldName, final FieldMap fieldMap, final FacetsConfig facetsConfig);

    @FunctionalInterface
    interface Supplier<T extends FieldDefinition> {
        FieldTypeInterface newFieldType(final String genericFieldName,
                                        final WildcardMatcher wildcardMatcher,
                                        final T definition);
    }

    @FunctionalInterface
    interface FacetSupplier {
        void setConfig(final String fieldName,
                       final FieldMap fieldMap,
                       final FacetsConfig facetsConfig);
    }

    @FunctionalInterface
    interface FieldSupplier {
        void addFields(final String fieldName,
                       final Object value,
                       final DocumentBuilder documentBuilder);
    }

    @FunctionalInterface
    interface SortFieldSupplier {
        SortField newSortField(final String fieldname,
                               final QueryDefinition.SortEnum sortEnum);
    }

    @FunctionalInterface
    interface TermSupplier {
        Term newTerm(final String fieldname,
                     final Object value);
    }

    @FunctionalInterface
    interface FieldNameResolver {
        String resolve(final String fieldName, final FieldType fieldType, final ValueType valueType);
    }

    enum FieldType {

        storedField('r'),
        stringField('s'),
        facetField('f'),
        docValues('d'),
        textField('t'),
        pointField('p');

        final char prefix;

        FieldType(char prefix) {
            this.prefix = prefix;
        }
    }

    enum ValueType {

        textType('t'),
        longType('l'),
        integerType('i'),
        doubleType('d'),
        floatType('f');

        final char prefix;

        ValueType(char prefix) {
            this.prefix = prefix;
        }
    }

}
