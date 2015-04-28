In order to run this example perform the following steps:

1) Run createPersonTable.sql in a development Oracle database which has no DATABASECHANGELOG and DATABASECHANGELOGLOCK tables.

2) Execute sample.db.refactoring.changelog.xml via Liquibase maven profile (see README.md for instructions)

3) Verify that the values of PERSON have been copied to PERSON_NEW and the NAME column values have been split to FIRST_NAME, LAST_NAME in the new table.
   Also check that when inserting/updating/deleting a row in PERSON table the PERSON_NEW is updated.

4) Find the row of DATABASECHANGELOG table corresponding in changeset with id 'deprecate-person'. Change the value of DATEEXECUTED column 
   to be < 2 months ago (The precondition defines that the old table will be dropped after two months of the creation of the new table have passed).

5) Re-execute the changelog and see that both the old table PERSON and the trigger PERSON_TRIGGER have been dropped.
