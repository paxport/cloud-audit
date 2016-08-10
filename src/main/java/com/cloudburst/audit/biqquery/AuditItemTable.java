package com.cloudburst.audit.biqquery;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.ReflectionBigQueryTable;
import com.cloudburst.bigquery.TableIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AuditItemTable extends ReflectionBigQueryTable<AuditItem> implements Auditor<AuditItem> {

    private final static Logger logger = LoggerFactory.getLogger(AuditItemTable.class);

    public AuditItemTable(TableIdentifier identifier) {
        super(AuditItem.class, identifier);
    }

    @Override
    public void audit(AuditItem item) {
        try {
            super.insertItem(item);
        } catch (IOException e) {
            logger.error("Audit failed for: " + item.getModule(), e);
        }
    }
}
