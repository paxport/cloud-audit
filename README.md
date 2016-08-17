Cloud Audit
===================

Infrastructure for logging request/response pairs and other debugging/auditing info.

First off logging into Google BigQuery.

Uses Spring.

## Features

### AuditItem model

Provides Immutables based model for AuditItems of type LOG, EXCEPTION, REQUEST & RESPONSE

### Audit To BigQuery

AuditItemTable is a Big Query table definition which should be extended to add your required tracking attributes.

You should subclass with something like:

    @Component
    public class MyAuditor extends AuditItemTable {
    
        @Autowired
        public MyAuditor(AuditTableIdentifier identifier) {
            super(identifier);
        }
    
        @Override
        protected Map<String, TableFieldSchema> customFields() {
            Map<String,TableFieldSchema> fields = new LinkedHashMap<>();
            TableFieldSchema tracking = field("tracking", FieldType.RECORD, FieldMode.NULLABLE);
            tracking.setFields(new ArrayList<>());
            tracking.getFields().add(field("internal_tracing_id",FieldType.STRING,FieldMode.NULLABLE));
            tracking.getFields().add(field("logical_session_id",FieldType.STRING,FieldMode.NULLABLE));
            fields.put("tracking",tracking);    
            return fields;
        }
    }

### DefaultAuditFilter

When configured to run as a servlet filter this will map request headers into a TrackingMap for use within AuditItem.
It will audit the incoming request and outgoing response to the configured auditor.

You will need to subclass DefaultAuditFilter like:

    @Component
    public class MyAuditFilter extends DefaultAuditFilter {
    
        @Autowired
        public MyAuditFilter(BackgroundAuditor<AuditItem> auditor) {
            super(auditor);
        }
    }

And then configure with something like:

    @Configuration
    @ComponentScan(basePackages = {"com.cloudburst"})
    public class MyAuditFilterConfiguration {
    
        @Bean
        @Autowired
        public FilterRegistrationBean auditFilterRegistration(MyAuditFilter filter) {
            FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
            filterRegistrationBean.setFilter(filter);
            filterRegistrationBean.setOrder(1); // ordering in the filter chain
            return filterRegistrationBean;
        }
    
    }

### Logback Appender

Add this to your logback.xml to log WARN and ERROR messages to audit DB:

    <appender name="AUDIT" class="com.cloudburst.audit.logback.AuditAppender"/>
    
If your TrackingMap contains something like audit-level=DEBUG then you will get DEBUG and INFO as well. 
This means that you can potentially add request headers in order to elevate the audit level for that request.

### JAX-WS Handler

Add new AuditSOAPHandler() to your SOAP handler chain in order to audit the SOAP requests and responses.

### Profiling

Add an aspect to you spring config like below to add timing info to the audit trace

    @Aspect
    @Configuration
    @Profile("profiling") // turn on and off with spring profiles
    @NoProfiling
    public class Profiler extends MethodTimingProfiler {
    
        @Pointcut("within(com.mystuff..*)")
        protected void myPackage() {}
    
        @Pointcut("myPackage() && profilingIsOkay()")
        @Override
        protected void myPointcut() {}
    }


### Basic Web Gui

If you subclass AbstractAuditItemController with something like:

    @RestController
    @RequestMapping("/v1/audit/items")
    public class AuditController extends AbstractAuditItemController {
    
        @Override
        protected Set<String> queryableTrackingColumns() {
            Set<String> result = new HashSet<>();
            result.add("internal_tracing_id");
            result.add("logical_session_id");
            return result;
        }
    }
    
Then you will get a basic web gui so you can retrieve a trace by visiting:

__/v1/audit/items/overview.html?internal_tracing_id=foo__

## JCenter Dependency

Add JCenter to your repositories if not already:

    <repositories>
        <repository>
            <id>jcenter-snapshots</id>
            <name>jcenter</name>
            <url>https://jcenter.bintray.com/</url>
        </repository>
    </repositories>
    
Add cloud audit dependency:

    <dependency>
        <groupId>com.cloudburst</groupId>
        <artifactId>cloud-audit</artifactId>
        <version>1.0.3</version>
    </dependency>


## To Release new version to Bintray

    mvn clean release:prepare -Darguments="-Dmaven.javadoc.skip=true"
    mvn release:perform -Darguments="-Dmaven.javadoc.skip=true"


