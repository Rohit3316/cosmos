package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.hawkbit.repository.RoleManagement;
import org.eclipse.hawkbit.repository.exception.EntityCannotNullException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.hawkbit.repository.model.dto.RoleDTO;
import org.eclipse.hawkbit.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RoleManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRoleManagement implements RoleManagement {


    private final RoleRepository roleRepository;

    @Autowired
    public JpaRoleManagement(final RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> getRoles() {
        return Collections.unmodifiableList(roleRepository.findAll());
    }

    @Override
    public Role getRole(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }

    @Override
    @Modifying
    @Transactional
    public List<Role> create(List<RoleDTO> roles) {
        if (roles != null && !roles.isEmpty()) {
            List<JpaRole> createdRoles = roleRepository.saveAll(MapperUtil.convertToList(roles, JpaRole.class));
            return Collections.unmodifiableList(createdRoles);
        } else {
            throw new EntityCannotNullException("Null or empty list of roles");
        }
    }

    @Override
    @Modifying
    @Transactional
    public void delete(Collection<Long> ids) {
        roleRepository.deleteAllById(ids);
    }
}
