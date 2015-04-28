package gr.com.ist.commun.core.domain;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;

/**
 * Implements a generic enum user type identified by a single identifier / column.
 * <p><ul>
 *    <li>The identifier representing an enum value is retrieved by the identifierMethod.
 *        The name of the identifier method can be specified by the 
 *        'identifierMethod' property and its default is 'getId'.</li>
 *    <li>The identifier type is automatically determined by 
 *        the type of the identifierMethod.</li>
 *    <li>The valueOfMethod is the name of the static factory method returning
 *        an enumeration object represented by a given indentifier. The valueOfMethod's
 *        name can be specified by setting the 'valueOfMethod' property. The
 *        default valueOfMethod's name is 'fromId'.</li>
 * </p> 
 * **/


@SuppressWarnings("rawtypes")
public class GenericEnumUserType implements DynamicParameterizedType, EnhancedUserType {

    private Class<? extends Enum> enumClass;
    public static final String ENUM = "enumClass";
    public static final String TYPE = "type";
    private Class<?> identifierType;
    private Method identifierMethod;
    private Method valueOfMethod;
    private static final String defaultIdentifierMethodName = "getId";
    private static final String defaultValueOfMethodName = "fromId";
    private AbstractSingleColumnStandardBasicType type;
    private int[] sqlTypes;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void setParameterValues(Properties parameters) {
        ParameterType reader = (ParameterType) parameters.get(PARAMETER_TYPE);
        if (reader != null) {
            enumClass = reader.getReturnedClass().asSubclass(Enum.class);
        } else {
            String enumClassName = (String) parameters.get(ENUM);
            try {
                enumClass = ReflectHelper.classForName(enumClassName, this.getClass()).asSubclass(Enum.class);
            } catch (ClassNotFoundException exception) {
                throw new HibernateException("Enum class not found", exception);
            }
        }

        String identifierMethodName = parameters.getProperty("identifierMethod", defaultIdentifierMethodName);
        try {
            identifierMethod = enumClass.getMethod(identifierMethodName, new Class[0]);
            identifierType = identifierMethod.getReturnType();
        } catch (Exception exception) {
            throw new HibernateException("Failed to obtain identifier method", exception);
        }

        TypeResolver tr = new TypeResolver();
        type = (AbstractSingleColumnStandardBasicType) tr.basic(identifierType.getName());
        if (type == null) {
            throw new HibernateException("Unsupported identifier type " + identifierType.getName());
        }
        sqlTypes = new int[] { type.sqlType() };

        String valueOfMethodName = parameters.getProperty("valueOfMethod", defaultValueOfMethodName);

        try {
            valueOfMethod = enumClass.getMethod(valueOfMethodName, new Class[] { identifierType });
        } catch (Exception exception) {
            throw new HibernateException("Failed to obtain valueOf method", exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        Object identifier = type.get(rs, names[0], session);
        try {
            return valueOfMethod.invoke(enumClass, new Object[] { identifier });
        } catch (Exception exception) {
            throw new HibernateException("Exception while invoking valueOfMethod of enumeration class: ", exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        try {
            Object identifier = value != null ? identifierMethod.invoke(value, new Object[0]) : null;
            st.setObject(index, identifier);
        } catch (Exception exception) {
            throw new HibernateException("Exception while invoking identifierMethod of enumeration class: ", exception);

        }
    }

    @Override
    public String objectToSQLString(Object value) {
        try {
            Object identifier = value != null ? identifierMethod.invoke(value, new Object[0]) : null;
            return identifier == null ? null : "'"+identifier+"'";
        } catch (Exception exception) {
            throw new HibernateException("Exception while invoking identifierMethod of enumeration class: ", exception);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public int[] sqlTypes() {
        return sqlTypes;
    }

    /** {@inheritDoc} */
    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    /** {@inheritDoc} */
    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    /** {@inheritDoc} */
    public boolean isMutable() {
        return false;
    }

    /** {@inheritDoc} */
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    /** {@inheritDoc} */
    @Override
    public Class returnedClass() {
        return enumClass;
    }

    @Override
    @Deprecated
    public String toXMLString(Object value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Object fromXMLString(String xmlValue) {
        // TODO Auto-generated method stub
        return null;
    }
}