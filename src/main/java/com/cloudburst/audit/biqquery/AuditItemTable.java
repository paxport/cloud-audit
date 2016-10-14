package com.cloudburst.audit.biqquery;

import com.google.api.services.bigquery.model.TableFieldSchema;

import com.cloudburst.audit.Auditor;
import com.cloudburst.audit.model.AuditItem;
import com.cloudburst.bigquery.FieldMode;
import com.cloudburst.bigquery.FieldType;
import com.cloudburst.bigquery.ReflectionBigQueryTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cloudburst.audit.servlet.DefaultAuditFilter.LOGICAL_SESSION_ID;
import static com.cloudburst.audit.servlet.DefaultAuditFilter.REQUEST_ID;
import static com.cloudburst.audit.servlet.DefaultAuditFilter.TRACING_ID;

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

    /**
     * By default add in the 3 tracking headers as columns
     * @return
     */
    @Override
    protected Map<String, TableFieldSchema> customFields() {
        Map<String,TableFieldSchema> fields = new LinkedHashMap<>();
        TableFieldSchema tracking = field("tracking", FieldType.RECORD, FieldMode.NULLABLE);
        tracking.setFields(new ArrayList<>());
        tracking.getFields().add(field(TRACING_ID,FieldType.STRING,FieldMode.NULLABLE));
        tracking.getFields().add(field(LOGICAL_SESSION_ID,FieldType.STRING,FieldMode.NULLABLE));
        tracking.getFields().add(field(REQUEST_ID,FieldType.STRING,FieldMode.NULLABLE));
        fields.put("tracking",tracking);
        return fields;
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
    protected Object valueForProperty(PropertyDescriptor descriptor, Object obj ) {
        if ( obj instanceof AuditItem ) {
            AuditItem item = (AuditItem) obj;

            switch (descriptor.getName()) {
                case "tracking":
                    return trackingMap(item);

                case "timestamp":
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(item.getTimestamp())
                            , ZoneId.systemDefault());
                    return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                default:
                    return super.valueForProperty(descriptor, item);
            }
        }
        else {
            return null;
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
