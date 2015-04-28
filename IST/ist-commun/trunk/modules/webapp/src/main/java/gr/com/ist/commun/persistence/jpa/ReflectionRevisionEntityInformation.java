/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.com.ist.commun.persistence.jpa;

import org.hibernate.envers.RevisionNumber;
import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RevisionEntityInformation} that uses reflection to inspect a property annotated with {@link RevisionNumber} to
 * find out about the revision number type.
 * 
 * @author Oliver Gierke
 */
public class ReflectionRevisionEntityInformation implements RevisionEntityInformation {

	private final Class<?> revisionEntityClass;
	private final Class<?> revisionNumberType;

	/**
	 * Creates a new {@link ReflectionRevisionEntityInformation} inspecting the given revision entity class.
	 * 
	 * @param revisionEntityClass must not be {@literal null}.
	 */
	public ReflectionRevisionEntityInformation(Class<?> revisionEntityClass) {

		Assert.notNull(revisionEntityClass);

		AnnotationDetectionFieldCallback fieldCallback = new AnnotationDetectionFieldCallback(RevisionNumber.class);
		ReflectionUtils.doWithFields(revisionEntityClass, fieldCallback);

		this.revisionNumberType = fieldCallback.getType();
		this.revisionEntityClass = revisionEntityClass;

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.support.RevisionEntityInformation#getRevisionNumberType()
	 */
	public Class<?> getRevisionNumberType() {
		return revisionNumberType;
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
		return revisionEntityClass;
	}
}
