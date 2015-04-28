package gr.com.ist.commun.persistence.jpa;

import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * @see {@link RevisionEntityInformation}.
 */
public class AppRevisionEntityInformation implements RevisionEntityInformation {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.support.RevisionEntityInformation#getRevisionNumberType()
	 */
	public Class<?> getRevisionNumberType() {
		return Long.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.support.RevisionEntityInformation#isDefaultRevisionEntity()
	 */
	public boolean isDefaultRevisionEntity() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.support.RevisionEntityInformation#getRevisionEntityClass()
	 */
	public Class<?> getRevisionEntityClass() {
		return AppRevisionEntity.class;
	}
}
