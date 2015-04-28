package gr.com.ist.commun.core.domain.security.audit.entity;

import de.danielbechler.diff.node.*;
import de.danielbechler.diff.visitor.Visit;
import de.danielbechler.util.Strings;

import java.util.*;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmbedabblePropertyChangeAuditVisitor implements Node.Visitor {
    
    private static final Logger LOG = LoggerFactory.getLogger(EmbedabblePropertyChangeAuditVisitor.class);
    
    private final String fieldName;
    
    private final Object working;
    
    private final Object base;
    
    private final List<PropertyChange> changes = new ArrayList<PropertyChange>();

    public List<PropertyChange> getChanges() {
        return changes;
    }

    public EmbedabblePropertyChangeAuditVisitor(final String fieldName, final Object working, final Object base) {
        this.base = base;
        this.working = working;
        this.fieldName = fieldName;
    }

    public void accept(final Node node, final Visit visit) {
        Log.info("im in");
        if (filter(node)) {
            addPropertyChange(this.fieldName + "." + node.getPropertyPath().toString().substring(1), node.getState(), node.canonicalGet(base), node.canonicalGet(working));
        }
    }

    protected boolean filter(final Node node) {
        return (node.isRootNode() && !node.hasChanges())
                || (node.hasChanges() && node.getChildren().isEmpty());
    }

    private void addPropertyChange(String propertyPath, final Node.State state, final Object base, final Object modified) {
        if (state == Node.State.CHANGED) {
            changes.add(new PropertyChange(propertyPath, Strings.toSingleLineString(modified),Strings.toSingleLineString(base)));
        }
        else if (state == Node.State.ADDED) {
            changes.add(new PropertyChange(propertyPath, modified, null));
        }
        else if (state == Node.State.REMOVED) {
            changes.add(new PropertyChange(propertyPath, null , base));
        }
    }
}