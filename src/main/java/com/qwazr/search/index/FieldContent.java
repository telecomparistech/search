/**
 * Copyright 2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class FieldContent {

	final public String[] terms;
	final public Integer[] increments;
	final public Integer[] offsets_start;
	final public Integer[] offsets_end;

	FieldContent(String[] terms, Integer[] increments, Integer[] offsets_start,
			Integer[] offsets_end) {
		this.terms = terms;
		this.increments = increments;
		this.offsets_start = offsets_start;
		this.offsets_end = offsets_end;
	}

	public FieldContent() {
		this(null, null, null, null);
	}

}
