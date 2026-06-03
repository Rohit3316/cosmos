package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.UserElement;
import org.eclipse.hawkbit.repository.model.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;


/**
 * {@link User} repository.
 *
 * Please use JPA for CRUD operation and custom query for other kind of op's
 */
@Transactional(readOnly = true)
public interface UserElementRepository extends CrudRepository<UserElement, Long>, JpaSpecificationExecutor<UserElement> {

}

