package com.cloudburst.audit.servlet;

import com.google.api.services.bigquery.model.TableDataInsertAllResponse;

import com.cloudburst.audit.biqquery.AuditItemTable;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.BigQueryFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Created by ajchesney on 03/08/2016.
 */
public class AuditItemTableTest {

    private static BigQueryFactory factory = new BigQueryFactory();
    private static AuditItemTable exampleTable = new AuditItemTable("paxportcloud","audit","items");

    @BeforeClass
    public static void setup() {
        exampleTable.setBigquery(factory.getBigquery());
        //exampleTable.ensureExists();
    }


    @Test
    public void testStreaming () throws IOException {

        AuditItem item = AuditItem.requestResponse("DEBUG","module","message","req","res", 1234l);

        TableDataInsertAllResponse res = exampleTable.insertItem(item);



    }


}
