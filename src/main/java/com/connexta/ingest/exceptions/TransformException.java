/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.ingest.exceptions;

public class TransformException extends IngestException {

  public TransformException(Throwable cause) {
    super("Transform service exception", cause);
  }
}
