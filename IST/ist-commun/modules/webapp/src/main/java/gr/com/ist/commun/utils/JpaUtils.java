package gr.com.ist.commun.utils;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.support.PersistenceProvider;
import org.springframework.util.Assert;

import com.mysema.query.jpa.EclipseLinkTemplates;
import com.mysema.query.jpa.HQLTemplates;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLTemplates;
import com.mysema.query.jpa.OpenJPATemplates;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Operation;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.OrderSpecifier.NullHandling;
import com.mysema.query.types.Path;
import com.mysema.query.types.PathImpl;

public class JpaUtils {

    public static JPQLTemplates jpqlTemplates(EntityManager em) {
        switch (PersistenceProvider.fromEntityManager(em)) {
        case ECLIPSELINK:
            return EclipseLinkTemplates.DEFAULT;
        case HIBERNATE:
            return HQLTemplates.DEFAULT;
        case OPEN_JPA:
            return OpenJPATemplates.DEFAULT;
        case GENERIC_JPA:
        default:
            return null;
        }
    }

    public static JPAQuery createQuery(EntityManager em) {
        return new JPAQuery(em, jpqlTemplates(em));
    }

    public static JPAUpdateClause createUpdateClause(EntityManager em, EntityPath<?> entity) {
	    return new JPAUpdateClause(em, entity, jpqlTemplates(em));
	}

    /**
     * Given a @Path it returns the full property path
     */
    public static <T> String propName(Path<T> namePath) {
        return namePath.toString().replaceFirst("^[^\\.]*\\.", "");
    }

    /**
     * Applies paging and sorting on JPQL query. Sorting supports aliases on result set fields. 
     * To support nesting of order properties, underscores in aliases correspond to dots in 
     * order properties since dots are not valid characters in database field aliases.
     * Example: alias aircraft_type corresponds to aircraft.type order property
     * 
     */
    public static void applyPagingAndSorting(final Pageable pageable, final JPQLQuery query, final Expression<?>[] resultSetFields) {
        class Impl {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            private OrderSpecifier<?> toOrderSpecifier(Order order, JPQLQuery query) {
                Expression<?> sortPropertyExpression = buildOrderPropertyPathFrom(order);
                if (sortPropertyExpression == null) {
                    return null;
                }
                return new OrderSpecifier(order.isAscending() ? com.mysema.query.types.Order.ASC
                        : com.mysema.query.types.Order.DESC, sortPropertyExpression,
                        toQueryDslNullHandling(order.getNullHandling()));
            }
            
            private NullHandling toQueryDslNullHandling(org.springframework.data.domain.Sort.NullHandling nullHandling) {

                Assert.notNull(nullHandling, "NullHandling must not be null!");

                switch (nullHandling) {

                    case NULLS_FIRST:
                        return NullHandling.NullsFirst;

                    case NULLS_LAST:
                        return NullHandling.NullsLast;

                    case NATIVE:
                    default:
                        return NullHandling.Default;
                }
            }        
            
            private Expression<?> buildOrderPropertyPathFrom(Order order) {

                Assert.notNull(order, "Order must not be null!");

                for (Expression<?> fieldExpression : resultSetFields) {
                    if (order.getProperty().equals(fieldExpression.toString())) {
                        return fieldExpression;
                    }
                    String alias = getAlias(fieldExpression);
                    if (alias != null && order.getProperty().equals(alias.replace('_', '.'))) {
                        return new PathImpl<>(fieldExpression.getType(), alias);
                    }
                }

                return null;
//                throw new IllegalArgumentException("Invalid sort specifier: "+order.getProperty()+". Allowed values: "+allowedPropertyNames(resultSetFields));
            }

/*
            private List<String> allowedPropertyNames(Expression<?>[] fields) {
                List<String> propertyNames = new ArrayList<>();
                for (Expression<?> fieldExpression : fields) {
                    String alias = getAlias(fieldExpression);
                    if (alias != null) {
                        propertyNames.add(alias);
                    } else {
                        propertyNames.add(fieldExpression.toString());
                    }
                }
                return propertyNames;
            }
*/
            private String getAlias(Expression<?> fieldExpression) {
                if (!(fieldExpression instanceof Operation<?>)) {
                    return null;
                }
                Operation<?> operation = (Operation<?>) fieldExpression;
                if (!"ALIAS".equals(operation.getOperator().getId())) {
                    return null;
                }
                return operation.getArg(1).toString();
            }

        }
        if (pageable == null) {
            return;
        }
        if (pageable.getOffset() > 0) {
            query.offset(pageable.getOffset());
        }
        if (pageable.getPageSize() > 0) {
            query.limit(pageable.getPageSize());
        }
        Impl impl = new Impl();
        Sort sort = pageable.getSort();
        if (sort == null) {
            return;
        }
        for (Order order : sort) {
            OrderSpecifier<?> orderSpecifier = impl.toOrderSpecifier(order, query);
            if (orderSpecifier != null) {
                query.orderBy(orderSpecifier);
            }
        }
    }
    
}
