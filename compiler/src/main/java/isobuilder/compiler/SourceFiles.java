/*
 * Copyright (C) 2014 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;

/**
 * Utilities for generating files.
 *
 * @author Gregory Kick
 * @since 2.0
 */
class SourceFiles {

  private static final Joiner CLASS_FILE_NAME_JOINER = Joiner.on('_');

  static String classFileName(ClassName className) {
    return CLASS_FILE_NAME_JOINER.join(className.simpleNames());
  }

  private SourceFiles() {
  }
}
