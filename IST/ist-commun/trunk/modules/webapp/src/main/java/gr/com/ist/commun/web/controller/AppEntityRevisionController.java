package gr.com.ist.commun.web.controller;

import gr.com.ist.commun.core.domain.security.EntityBase;
import gr.com.ist.commun.core.domain.security.annotations.HasAnyRole;
import gr.com.ist.commun.core.domain.security.annotations.SecuredWith;
import gr.com.ist.commun.core.domain.security.annotations.UrlAuthority;
import gr.com.ist.commun.core.domain.security.audit.entity.AppEntityRevision;
import gr.com.ist.commun.core.domain.security.audit.entity.EntityChange;
import gr.com.ist.commun.core.domain.security.audit.entity.RevisionChange;
import gr.com.ist.commun.core.repository.security.audit.AppEntityRevisionRepository;
import gr.com.ist.commun.core.repository.security.audit.AppEntityRevisionRepositoryCustom.AppRevisionCriteria;
import gr.com.ist.commun.utils.SecurityUtils.SecurityMethods;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppEntityRevisionController {
    private static Logger LOG = LoggerFactory.getLogger(AppEntityRevisionController.class);

    @Autowired
    AppEntityRevisionRepository revisionRepository;
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }
    
    @SecuredWith(
            hasAnyRole = @HasAnyRole(
                    urlAuthority = {
                            @UrlAuthority(role = "entityRevisions", method = SecurityMethods.VIEWER)}
                    ))
    @ResponseBody
    @RequestMapping(value = "/entityRevisions/query/{id}", method = RequestMethod.GET, produces = {
            "application/json" })
    public ResponseEntity<?> getSingleEntityRevision(@PathVariable long id, Pageable pageable) {
        LOG.trace("pageable:{}", pageable);
        AppEntityRevision appEntityRevision = revisionRepository.findOne(id);
        if (appEntityRevision == null) {
            return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        } 
        if (!revisionRepository.isRevisionAccesible(appEntityRevision)) {
            return new ResponseEntity<Object>(HttpStatus.FORBIDDEN);
        }
        RevisionChange revisionChange = populateRevisionChange(appEntityRevision, pageable);
        Page<EntityChange> myPage = new PageImpl<EntityChange>(revisionChange.getEntityChanges(), pageable, revisionChange.getTotalEntityChanges());
        return new ResponseEntity<>(myPage, HttpStatus.OK);
    }


    /**
     * Returns list of revisions for all entities
     * @param pageable
     * @param TimePeriod
     * @param User
     * @param RevisionType (insert/update/delete)
     * @param entity
     * @param property
     * @param propertyValue
     * @return
     */
    @SecuredWith(
            hasAnyRole = @HasAnyRole(
                    urlAuthority = {
                            @UrlAuthority(role = "entityRevisions", method = SecurityMethods.VIEWER)}
                    ))
    @RequestMapping(value = "/entityRevisions/query", produces = { MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Page<RevisionChange>> findRevisions(Pageable pageable, @RequestParam(value = "q", required = false) String term, AppRevisionCriteria appRevisionCriteria) {
        if (pageable.getSort() == null) {
            Sort sort = new Sort(new Order(Direction.DESC, "timestamp"));
            pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }
        if (appRevisionCriteria.getDateTo() != null) {
            // Set the time part equal to the max milliseconds in that date 
            appRevisionCriteria.setDateTo(new Date(appRevisionCriteria.getDateTo().getTime() + (24 /*hours*/ * 3600000 /*millis per hour*/ - 1)));
        }
        Page<AppEntityRevision> page = (term == null) ? revisionRepository.complexSearch(appRevisionCriteria, pageable) : revisionRepository.searchTerm(term, pageable);
        List<AppEntityRevision> revisions = page.getContent();
        ListIterator<AppEntityRevision> revisionsIter = revisions.listIterator(revisions.size());
        List<RevisionChange> revisionChanges = new ArrayList<RevisionChange>();
        while (revisionsIter.hasPrevious()) {
            AppEntityRevision lastRevision = revisionsIter.previous();
            LOG.trace("Populating changeset for revision:{}", lastRevision.getId());
            RevisionChange revisionChange = populateRevisionChange(lastRevision, null);
            revisionChanges.add(revisionChange);
        }
        Collections.reverse(revisionChanges);
        Page<RevisionChange> myPage = new PageImpl<RevisionChange>(revisionChanges, pageable, page.getTotalElements());
        return new ResponseEntity<>(myPage, HttpStatus.OK);
    }
    
    private RevisionChange populateRevisionChange(AppEntityRevision appEntityRevision, Pageable pageable) {
        RevisionChange revisionChange = new RevisionChange();
        revisionChange.setWho(appEntityRevision.getUser());
        revisionChange.setWhen(appEntityRevision.getRevisionDate());
        revisionChange.setId(appEntityRevision.getId());
        revisionChange.setModifiedEntityNames(appEntityRevision.getModifiedEntityNames());
        if (pageable != null) {
            List<EntityBase> changedEntities = revisionRepository.findEntitiesChangedAtRevision(appEntityRevision.getId());
            List<EntityChange> entityChanges = new ArrayList<EntityChange>();
            revisionChange.setTotalEntityChanges(changedEntities.size());
            entityChanges.addAll(revisionRepository.getEntityChangesForRevision(appEntityRevision.getId(), pageable, changedEntities));
            revisionChange.setEntityChanges(entityChanges);        
        }
        return revisionChange;
    }

}
