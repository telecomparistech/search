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

import com.qwazr.search.field.CopyToFieldType;
import com.qwazr.search.field.CustomFieldDefinition;
import com.qwazr.search.field.FieldDefinition;
import com.qwazr.search.field.FieldTypeInterface;
import com.qwazr.search.field.SmartDynamicTypes;
import com.qwazr.utils.WildcardMatcher;
import java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.facet.FacetsConfig;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FieldMap {

    private final SmartDynamicTypes smartDynamicTypes;
    private final String primaryKey;
    private final Map<String, FieldDefinition> fieldDefinitionMap;
    private final Map<String, FieldTypeInterface> nameDefMap;
    private final Collection<Pair<WildcardMatcher, FieldTypeInterface>> wildcardMap;
    private final FacetsConfig facetsConfig;
    public final String sortedSetFacetField;
    public final String recordField;

    public FieldMap(final String primaryKey,
                    final Map<String, FieldDefinition> fieldDefinitions,
                    final String sortedSetFacetField,
                    final String recordField) {

        this.primaryKey = primaryKey == null || primaryKey.isBlank() ? FieldDefinition.ID_FIELD : primaryKey;

        this.smartDynamicTypes = new SmartDynamicTypes(SmartDynamicTypes.primary(primaryKey));

        this.sortedSetFacetField =
            sortedSetFacetField == null ? FieldDefinition.DEFAULT_SORTEDSET_FACET_FIELD : sortedSetFacetField;

        this.recordField = recordField;

        nameDefMap = new HashMap<>();
        wildcardMap = new ArrayList<>();

        this.fieldDefinitionMap = Collections.unmodifiableMap(fieldDefinitions);

        this.fieldDefinitionMap.forEach((name, definition) -> {
            final FieldTypeInterface fieldType;
            if (name.indexOf('*') != -1 || name.indexOf('?') != -1) {
                final WildcardMatcher wildcardMatcher = new WildcardMatcher(name);
                fieldType = definition.newFieldType(name, wildcardMatcher, primaryKey);
                wildcardMap.add(Pair.of(wildcardMatcher, fieldType));
            } else {
                fieldType = definition.newFieldType(name, null, primaryKey);
            }
            nameDefMap.put(name, fieldType);
        });

        // Handle copy-to
        final HashMap<String, FieldTypeInterface> newFields = new HashMap<>();
        nameDefMap.forEach((name, fieldType) -> {
            final FieldDefinition definition = fieldType.getDefinition();
            if (definition.copyFrom == null)
                return;
            for (String copyFrom : definition.copyFrom) {
                final FieldTypeInterface fieldDest;
                if (nameDefMap.containsKey(copyFrom))
                    fieldDest = nameDefMap.get(copyFrom);
                else
                    fieldDest = newFields.computeIfAbsent(copyFrom, n -> new CopyToFieldType());
                fieldDest.copyTo(name, fieldType);
            }
        });
        nameDefMap.putAll(newFields);

        facetsConfig = new FacetsConfig();
    }

    public final boolean isEmpty() {
        return fieldDefinitionMap.isEmpty();
    }

    public final void forEach(final BiConsumer<String, FieldTypeInterface> consumer) {
        nameDefMap.forEach(consumer);
    }

    private FieldTypeInterface findFieldType(final String genericFieldName,
                                             final String concreteFieldName) {

        if (genericFieldName == null && concreteFieldName == null)
            throw new IllegalArgumentException("The field name is missing");
        // Annotated can find wildcarded fields directly using genericFieldName
        if (genericFieldName != null) {
            final FieldTypeInterface fieldType = nameDefMap.get(genericFieldName);
            if (fieldType != null)
                return fieldType;
        }
        if (concreteFieldName != null) {
            final FieldTypeInterface fieldType = nameDefMap.get(concreteFieldName);
            if (fieldType != null)
                return fieldType;
        }

        // Check record and primary key fields
        if (recordField != null && recordField.equals(concreteFieldName))
            return smartDynamicTypes.getDoNothingType();
        if (primaryKey != null && primaryKey.equals(concreteFieldName))
            return smartDynamicTypes.getPrimaryKeyType();

        //Second chance, using the wildcard collection
        final String searchField = concreteFieldName != null ? concreteFieldName : genericFieldName;
        for (Pair<WildcardMatcher, FieldTypeInterface> entry : wildcardMap)
            if (entry.getLeft().match(searchField))
                return entry.getRight();

        return null;
    }

    @NotNull
    final public FieldTypeInterface getFieldType(final String genericFieldName,
                                                 final String concreteFieldName,
                                                 final Object contentValue) {

        final FieldTypeInterface fieldType = findFieldType(genericFieldName, concreteFieldName);
        if (fieldType != null)
            return fieldType;

        // Guess field type from value
        final FieldTypeInterface smartFieldType = smartDynamicTypes.getTypeFromValue(primaryKey, concreteFieldName, contentValue);
        if (smartFieldType != null)
            return smartFieldType;

        throw new IllegalArgumentException(
            "The field has not been found: " + (genericFieldName == null ? concreteFieldName : genericFieldName));
    }

    @NotNull
    final public FieldTypeInterface getFieldType(final String genericFieldName,
                                                 final String concreteFieldName) {
        final FieldTypeInterface fieldType = findFieldType(genericFieldName, concreteFieldName);
        if (fieldType != null)
            return fieldType;
        throw new IllegalArgumentException(
            "The field has not been found: " + (genericFieldName == null ? concreteFieldName : genericFieldName));
    }

    final Map<String, FieldDefinition> getFieldDefinitionMap() {
        return fieldDefinitionMap;
    }

    final String getPrimaryKey() {
        return primaryKey;
    }

    private void checkFacetConfig(final String genericFieldName, final String concreteFieldName) {
        if (facetsConfig.getDimConfigs().containsKey(concreteFieldName))
            return;
        final FieldTypeInterface fieldType = findFieldType(genericFieldName, concreteFieldName);
        if (fieldType == null)
            return;
        fieldType.applyFacetsConfig(concreteFieldName, this, facetsConfig);
        final FieldDefinition definition = fieldType.getDefinition();
        if (definition == null)
            return;
        if (definition instanceof CustomFieldDefinition) {
            final CustomFieldDefinition customDef = (CustomFieldDefinition) definition;
            final FieldDefinition.Template template =
                customDef.template == null ? FieldDefinition.Template.NONE : customDef.template;
            switch (template) {
                case SortedSetDocValuesFacetField:
                    facetsConfig.setIndexFieldName(concreteFieldName, sortedSetFacetField);
                    break;
                case FacetField:
                    facetsConfig.setIndexFieldName(concreteFieldName, FieldDefinition.TAXONOMY_FACET_FIELD);
                    break;
                case IntAssociatedField:
                    facetsConfig.setIndexFieldName(concreteFieldName, FieldDefinition.TAXONOMY_INT_ASSOC_FACET_FIELD);
                    break;
                case FloatAssociatedField:
                    facetsConfig.setIndexFieldName(concreteFieldName, FieldDefinition.TAXONOMY_FLOAT_ASSOC_FACET_FIELD);
                    break;
                default:
                    break;
            }
        }
    }

    static public String[] resolveFieldNames(final String[] fields, final Function<String, String> resolver) {
        if (fields == null)
            return null;
        final String[] resolvedFields = new String[fields.length];
        int i = 0;
        for (String field : fields)
            resolvedFields[i++] = resolver.apply(field);
        return resolvedFields;
    }

    /**
     * @param fields         the key is the concrete field name, the value is the generic field name
     * @param resolvedFields the given map will be filled with the resolved fields
     * @param resolver       the resolving function applied to each field
     * @param <T>            the expected type of field
     * @return the map provided with the parameter resolvedFields
     */
    static public <T> Map<String, T> resolveFieldNames(final Map<String, T> fields,
                                                       final Map<String, T> resolvedFields,
                                                       final Function<String, String> resolver) {
        fields.forEach((f, t) -> resolvedFields.put(resolver.apply(f), t));
        return resolvedFields;
    }

    /**
     * @param fieldNames The key is the concreteFieldName, the value is the GenericFieldName
     * @return a
     */
    final public FacetsConfig getFacetsConfig(final Map<String, String> fieldNames) {
        fieldNames.forEach(
            (concreteFieldName, genericFieldName) -> checkFacetConfig(genericFieldName, concreteFieldName));
        return facetsConfig;
    }

    final public FacetsConfig getFacetsConfig(final String genericFieldName, final String concreteFieldName) {
        checkFacetConfig(genericFieldName, concreteFieldName);
        return facetsConfig;
    }

    final String getSortedSetFacetField() {
        return sortedSetFacetField;
    }

    final Set<String> getStaticFieldSet() {
        return nameDefMap.keySet();
    }

}
