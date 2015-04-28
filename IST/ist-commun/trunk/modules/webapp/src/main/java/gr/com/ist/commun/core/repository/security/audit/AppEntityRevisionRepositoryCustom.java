package gr.com.ist.commun.core.repository.security.audit;

import gr.com.ist.commun.core.domain.security.EntityBase;
import gr.com.ist.commun.core.domain.security.User;
import gr.com.ist.commun.core.domain.security.audit.entity.AppEntityRevision;
import gr.com.ist.commun.core.domain.security.audit.entity.EntityChange;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppEntityRevisionRepositoryCustom {

    public static class AppRevisionCriteria {

        private User who;

        private Date dateFrom;

        private Date dateTo;
        
        private String entityName;

        public User getWho() {
            return who;
        }

        public void setWho(User who) {
            this.who = who;
        }

        public Date getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Date dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Date getDateTo() {
            return dateTo;
        }

        public void setDateTo(Date dateTo) {
            this.dateTo = dateTo;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    Page<AppEntityRevision> complexSearch(AppRevisionCriteria criteria, Pageable pageable);

    boolean isRevisionAccesible(AppEntityRevision appEntityRevision);

    List<EntityBase> findEntitiesChangedAtRevision(long revisionNumber);

    List<EntityChange> getEntityChangesForRevision(long revisionId, Pageable pageable, List<EntityBase> changedEntities);
}
