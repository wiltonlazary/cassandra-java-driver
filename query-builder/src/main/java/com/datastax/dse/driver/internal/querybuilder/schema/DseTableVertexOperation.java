/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.dse.driver.internal.querybuilder.schema;

import com.datastax.oss.driver.api.core.CqlIdentifier;

public class DseTableVertexOperation {

  private final DseTableGraphOperationType type;
  private final CqlIdentifier label;

  public DseTableVertexOperation(DseTableGraphOperationType type, CqlIdentifier label) {
    this.type = type;
    this.label = label;
  }

  public DseTableGraphOperationType getType() {
    return type;
  }

  public CqlIdentifier getLabel() {
    return label;
  }

  public void append(StringBuilder builder) {
    builder.append("VERTEX LABEL");
    if (label != null) {
      builder.append(' ').append(label.asCql(true));
    }
  }
}
