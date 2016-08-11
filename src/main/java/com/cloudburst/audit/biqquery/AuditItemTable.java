package com.cloudburst.audit.biqquery;

import com.google.api.services.bigquery.model.TableFieldSchema;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.FieldMode;
import com.cloudburst.bigquery.FieldType;
import com.cloudburst.bigquery.ReflectionBigQueryTable;
import com.cloudburst.bigquery.TableIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AuditItemTable extends ReflectionBigQueryTable<AuditItem> implements Auditor<AuditItem> {

    private final static Logger logger = LoggerFactory.getLogger(AuditItemTable.class);

    public AuditItemTable(AuditTableIdentifier identifier) {
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

    @Override
    protected TableFieldSchema fieldFromPropertyDescriptor(PropertyDescriptor prop) {
        switch ( prop.getName() ) {
            case "timestamp" :
                return field("timestamp", FieldType.TIMESTAMP, FieldMode.REQUIRED);

            default :
                return super.fieldFromPropertyDescriptor(prop);
        }
    }

    @Override
    protected Object valueForProperty(PropertyDescriptor descriptor, AuditItem item) {
        switch ( descriptor.getName() ) {
            case "tracking" :
                return trackingMap ( item );

            case "timestamp" :
                ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.getTimestamp())
                        , ZoneId.systemDefault());
                return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            default :
                return super.valueForProperty(descriptor, item);
        }
    }

    /**
     * Opportunity to convert incoming headers to tracking record
     * @param item
     * @return
     */
    protected Map<String,String> trackingMap(AuditItem item) {
        return item.getTracking();
    }

}
