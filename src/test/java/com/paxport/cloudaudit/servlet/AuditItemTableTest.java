package com.paxport.cloudaudit.servlet;

import com.paxport.cloudaudit.biqquery.AuditItemTable;
import com.paxport.cloudaudit.biqquery.AuditTableIdentifier;
import com.paxport.cloudaudit.model.AuditItem;
import com.paxport.bigquery.BigQueryFactory;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by ajchesney on 03/08/2016.
 */
//@Ignore
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
