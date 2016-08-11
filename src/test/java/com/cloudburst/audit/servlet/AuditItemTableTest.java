package com.cloudburst.audit.servlet;

import com.google.api.services.bigquery.model.TableDataInsertAllResponse;

import com.cloudburst.audit.biqquery.AuditItemTable;
import com.cloudburst.audit.biqquery.AuditTableIdentifier;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.BigQueryFactory;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by ajchesney on 03/08/2016.
 */
@Ignore
public class AuditItemTableTest {

    private static BigQueryFactory factory = new BigQueryFactory();
    private static AuditTableIdentifier identifier = new AuditTableIdentifier("paxportcloud","audit","items");
    private static AuditItemTable exampleTable = new AuditItemTable(identifier);

    @BeforeClass
    public static void setup() {
        exampleTable.setBigquery(factory.getBigquery());
        //exampleTable.ensureExists();
    }


    @Test
    public void testStreaming () throws IOException {

        AuditItem item = AuditItem.request("URL","module","label","headers","body", "contentType");

        TableDataInsertAllResponse res = exampleTable.insertItem(item);



    }


}
