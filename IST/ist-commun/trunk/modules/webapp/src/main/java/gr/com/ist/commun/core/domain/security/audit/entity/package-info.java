//This resolves envers,querydsl issue described in https://github.com/mysema/querydsl/issues/239
@QueryEntities({AppRevisionEntity.class})
package gr.com.ist.commun.core.domain.security.audit.entity;

import gr.com.ist.commun.persistence.jpa.AppRevisionEntity;

import com.mysema.query.annotations.QueryEntities;

