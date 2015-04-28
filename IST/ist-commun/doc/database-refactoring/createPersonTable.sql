CREATE TABLE PERSON
     (
        ID             NUMBER          NOT NULL , 
        NAME       VARCHAR2(20)        NULL , 
        GENDER_CODE     CHAR(1)             NULL , 
        BIRTH_DATE      DATE                NULL ,  
        CONSTRAINT PK_PERSON PRIMARY KEY (ID)
     );
     
insert into PERSON values (1,'hello world', 'f',sysdate);
insert into PERSON values (2,'bye world', 'f',sysdate);
insert into PERSON values (3,'george migkos', 'm',sysdate);

commit;