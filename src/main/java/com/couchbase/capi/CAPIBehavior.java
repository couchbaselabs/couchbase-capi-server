package com.couchbase.capi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface CAPIBehavior {

    /** Database Operations **/

    boolean databaseExists(String database);

    Map<String, Object> getDatabaseDetails(String database);

    boolean createDatabase(String database);

    boolean deleteDatabase(String database);

    boolean ensureFullCommit(String database);

    Map<String, Object> revsDiff(String database, Map<String, Object> revs);

    List<Object> bulkDocs(String database, List<Map<String, Object>> docs);

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

}
