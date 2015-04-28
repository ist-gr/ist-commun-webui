package gr.com.ist.commun.persistence.jpa;

import org.joda.time.DateTime;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.util.Assert;

public class AppRevisionMetadata implements RevisionMetadata<Long> {

	private final AppRevisionEntity entity;

	/**
	 * Creates a new {@link AppRevisionMetadata}.
	 * 
	 * @param entity must not be {@literal null}.
	 */
	public AppRevisionMetadata(AppRevisionEntity entity) {

		Assert.notNull(entity);
		this.entity = entity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionNumber()
	 */
	public Long getRevisionNumber() {
		return entity.getId();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	public DateTime getRevisionDate() {
		return new DateTime(entity.getTimestamp());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getDelegate()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDelegate() {
		return (T) entity;
	}
}
