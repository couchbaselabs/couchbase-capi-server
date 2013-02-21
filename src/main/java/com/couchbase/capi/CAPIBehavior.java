/**
 * Copyright (c) 2012 Couchbase, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.couchbase.capi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.UnavailableException;

public interface CAPIBehavior {

    /** Database Operations **/

    boolean databaseExists(String database);

    Map<String, Object> getDatabaseDetails(String database);

    boolean createDatabase(String database);

    boolean deleteDatabase(String database);

    boolean ensureFullCommit(String database);

    Map<String, Object> revsDiff(String database, Map<String, Object> revs) throws UnavailableException;

    List<Object> bulkDocs(String database, List<Map<String, Object>> docs) throws UnavailableException;

    /** Document Operations **/

    Map<String, Object> getDocument(String database, String docId);

    Map<String, Object> getLocalDocument(String database, String docId);

    String storeDocument(String database, String docId, Map<String, Object> document);

    String storeLocalDocument(String database, String docId, Map<String, Object> document);

    /** Attachment Operations **/

    InputStream getAttachment(String database, String docId, String attachmentName);

    String storeAttachment(String database, String docId, String attachmentName, String contentType, InputStream input);

    InputStream getLocalAttachment(String databsae, String docId, String attachmentName);

    String storeLocalAttachment(String database, String docId, String attachmentName, String contentType, InputStream input);

     Map<String, Object> getStats();

}
