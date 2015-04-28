# Developer README

## Developer workflow

### Changes / refactorings involving database modifications

* If an initial changelog does not exist (i.e. a changelog file does not exist in `src/main/resources/liquibase`), generate it using the command:

    ```
    mvn liquibase:generateChangeLog -Ddatabase.driverClassName=oracle.jdbc.OracleDriver -Ddatabase.url=jdbc:oracle:thin:@192.168.60.23:1521:ORA10 -Ddatabase.username=... -Ddatabase.password=...
    ```

    and then remove the `-SNAPSHOT` string from the output file name.

    for complete usage instructions see: <http://www.liquibase.org/documentation/maven/maven_generateChangeLog.html>
 
* Using one of the database refactoring patterns described in <http://www.agiledata.org/essays/databaseRefactoringCatalog.html>, create a new local changeSet containing the database change in `src/main/resources/liquibase/db.changelog-${version}.xml` where `${version}` is the project version without the snapshot suffix.

    e.g. if `avis-webapp` current version is `1.3-SNAPSHOT` then you must put `1.3`

    Note: For instructions on changeSet syntax see <http://www.liquibase.org/documentation/changeset.html>

    WARNING: !!! DO NOT MODIFY ANY CHANGESETS THAT HAVE BEEN ALREADY RELEASED TO DATABASE ENVIRONMENTS OTHER THAN DEVELOPMENT (staging, production, etc)
     
* Using maven, run Liquibase to execute the new changeSet:

    ```
    mvn liquibase:update -Dliquibase.contexts=test -Ddatabase.driverClassName=oracle.jdbc.OracleDriver -Ddatabase.url=jdbc:oracle:thin:@192.168.60.23:1521:ORA10 -Ddatabase.username=DEV_AVIS_... -Ddatabase.password=DEV_AVIS_...
    ```

    Note: You should replace the host, port, user name and password in the command shown above with appropriate values. If you do not know these contact your team leader.

* Perform the corresponding changes in the application code (e.g., Java code)

* Test the new application code together with the database change

    ```
    mvn -Pliquibase -Dliquibase.contexts=test -Dspring.profiles.active="oracle,basic-auth" -Xmx1024m -Xss512k -XX:MaxPermSize=256m -Ddatabase.username=DEV_AVIS_... -Ddatabase.password=DEV_AVIS_... -Ddatabase.url=jdbc:oracle:thin:@192.168.60.23:1521:ORA10 -Ddatabase.driverClassName=oracle.jdbc.OracleDriver
    ```

    Note: You should replace the host, port, user name and password in the command shown above with appropriate values. If you do not know these contact your team leader.
    
* Commit both the changeSet and the application code.

### Liquibase database diff

    In order to perform diff on two databases and generate a changelog which will contain the changes that must be applied, run the following command (Oracle db example):
    
    ```
    mvn liquibase:diff -Ddatabase.driverClassName=oracle.jdbc.OracleDriver -Ddatabase.url=jdbc:oracle:thin:@192.168.60.23:1521:ORA10 
    -Ddatabase.username=... -Ddatabase.password=... -Dliquibase.referenceUrl=jdbc:oracle:thin:@192.168.60.23:1521:ORA10 
    -Dliquibase.referenceDriver=oracle.jdbc.OracleDriver -Dliquibase.referenceUsername=... -Dliquibase.referencePassword=... 
    -Dliquibase.diffChangeLogFile=/path/to/diffChangelog.xml
    ```
    
    Note that database.* properties refer to the base database (the one on which the changes will be applied) and liquibase.reference* properties to the target database.
    In the current setup CI_AVIS schema will be the base database and DEV_AVIS_* will be the target.
    -- WARNING: Always review the generated changelog for consistent changesets (ex. If the changelog contains a changeset which adds an already existing constraint it must be removed)
       and make sure that the changes described in the changelog are the ones we know that we made. 
    -- WARNING: Changesets that contain DROP tables, DROP columns e.t.c should not be applied at once and instead the migration procedure should be followed. (see Database refactoring with transition period paragraph).
       Changes which contain column size modifications (smaller size) should be inside a precondition that checks the max length of data in the column.
    

For more details see Liquibase documentation:
- <http://www.liquibase.org/bestpractices.html>
- <http://www.liquibase.org/documentation/maven/index.html>

### Creating Jasper Reports with Jaspersoft studio

* Use "java" as the report language
* Change the report language to "groovy" temporarily in order to make the preview work. Change it back to "java" when done with preview and you are ready to commit.
* TODO Add: Use templates for new reports and styles for styling report controls
* If you uncheck "Print Repeating Values", set the "Print When"/"Group Changes" appropriately (so that if that group breaks, the value will be printed in all cases)
* Use Open Sans for fonts in the reports. Open Sans fonts are included in the classpath of the application so jasper engine will get them from there. 
* In order to configure iReport to use Open Sans fonts, navigate to Options --> Fonts and browse to the ttf files of the font you have already downloaded. Open the file irfonts.xml and
  edit the elements of the xml file to include only the name the name of the ttf file and nothing more.
   
### Running the web application

Run the web application using `mvn jetty:run` or
deploy to a servlet container at root (/) context providing the following args
-Xmx1024m -Xms1024m  -XX:MaxPermSize=256m
Open the following URL:
<http://localhost:8080/>

### Accessing the  database

When running the webapp, the database console will be available at:
<http://localhost:8080/console/database/> with following parameters

#### H2 Database

* JDBC URL: `jdbc:h2:mem:avis`
* Username: `sa`
* Password:

#### HSQL Database

* JDBC URL: `jdbc:hsqldb:mem:.`
* Username: `sa`
* Password:

## Design Patterns & Best Practices

### Versioning

    Versions are incremented according to [semver](http://semver.org/).

### Analysis & Design

* [Analysis Patterns – Reusable Object Frameworks by Martin Fowler ISBN 0-201-89542-0](http://martinfowler.com/articles.html#id314249)
* [Refactoring](http://www.refactoring.com/)
* [Domain Driven Design](http://en.wikipedia.org/wiki/Domain-driven_design)

### Back-end

#### Database refactoring patterns
 
* <http://www.agiledata.org/essays/databaseRefactoring.html>
* <http://www.agiledata.org/essays/databaseRefactoringCatalog.html>

##### Database refactoring with transition period

Sometimes, when performing a database refactoring, a transition period should be applied in order for third party application to have the necessary time to be re-synched with the database changes.
Following, there is an example on how to handle a table renaming along with a column split into two columns. Liquibase 3.0.7 is used for this purpose.

Supposing we initialy have the table PERSON (ID, NAME, GENDER_CODE, BIRTH_DATE) and we want to rename it to PERSON_NEW and split NAME column to FIRST_NAME and LAST_NAME columns.

The steps we have to make are the following:

* Create a new liquibase changeset which will create the PERSON_NEW table. Mark the creation of the new table as the starting point of transition period after which the old table will be deleted.
* Create a new liquibase changeset which will transfer and transform the data to the new table
* Synchronize the two tables by creating a trigger in a new liquibase changeset
* Create a new changeset containing a precondition which runs only if the required transition time has expired. On successful run of the precondition, drop the trigger and the old table.

Find the code and instructions for the previous example in `./doc/database-refactoring` directory

### Front-end

Line icon set for UI & more // Infinitely scalable
<http://www.behance.net/gallery/Line-icon-set-for-UI-more-Infinitely-scalable/10712283>

#### Angularjs

<http://trochette.github.io/Angular-Design-Patterns-Best-Practices>
<http://ngmodules.org/>
<http://mgcrea.github.io/angular-strap/>

### Tools

#### RESTFul API

<http://www.infoq.com/news/2013/10/raml-rest-api-tools>
<http://www.apihub.com/raml-tools>
<http://raml.org/>

#### Data migration using Excel

Function for creating a UUID in Excel:
`=LOWER(CONCATENATE(DEC2HEX(RANDBETWEEN(0;4294967295);8);DEC2HEX(RANDBETWEEN(0;65535);4);DEC2HEX(RANDBETWEEN(16384;20479);4);DEC2HEX(RANDBETWEEN(32768;49151);4);DEC2HEX(RANDBETWEEN(0;65535);4);DEC2HEX(RANDBETWEEN(0;4294967295);8)))`

#### Drop all database objects of logged in oracle user

``` sql
-- WARNING: The following script will drop all database objects belonging to current user
BEGIN
   FOR cur_rec IN (SELECT object_name, object_type
                     FROM user_objects
                    WHERE object_type IN
                             ('TABLE', 'SEQUENCE')
                  )
   LOOP
      BEGIN
         IF cur_rec.object_type = 'TABLE'
         THEN
            EXECUTE IMMEDIATE    'DROP '
                              || cur_rec.object_type
                              || ' "'
                              || cur_rec.object_name
                              || '" CASCADE CONSTRAINTS';
         ELSE
            EXECUTE IMMEDIATE    'DROP '
                              || cur_rec.object_type
                              || ' "'
                              || cur_rec.object_name
                              || '"';
         END IF;
      EXCEPTION
         WHEN OTHERS
         THEN
            DBMS_OUTPUT.put_line (   'FAILED: DROP '
                                  || cur_rec.object_type
                                  || ' "'
                                  || cur_rec.object_name
                                  || '"'
                                 );
      END;
   END LOOP;
END;
```

#### Function to return a random UUID in Oracle

```
create or replace
function random_uuid return VARCHAR2 is
  v_uuid VARCHAR2(32);
begin
  select regexp_replace(rawtohex(sys_guid()), '([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})', '\1\2\3\4\5') into v_uuid from dual;
  return v_uuid;
end random_uuid;
```
