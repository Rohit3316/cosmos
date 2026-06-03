package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/**
 * {@link User} repository.
 *
 */
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<JpaUser, Long>, JpaSpecificationExecutor<JpaUser> {

    /**
     * Retrieves an Action with all lazy attributes.
     *
     * @param id
     *            the ID of the action
     * @return the found {@link User}
     */
    @EntityGraph(value = "User.all", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> getUserById(Long id);

    /**
     * Find a user by given username
     * @param username string
     * @return User
     */
    Optional<JpaUser> findByUsername(String username);

    /**
     *            to use
     * @param id
     *            to set
     * @param modifiedAt
     *            current time
     * @param firstname
     *            to update
     * @param lastname
     *                to update
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaUser p SET p.lastModifiedAt = :lastModifiedAt, p.firstname = :firstname, p.lastname = :lastname WHERE p.id = :id")
    void updateUser(@Param("id") Long id, @Param("lastModifiedAt") Long modifiedAt, @Param("firstname") String firstname, @Param("lastname") String lastname);
}
