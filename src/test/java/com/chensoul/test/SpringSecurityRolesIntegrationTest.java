/*
 * Copyright © 2023-2024 chensoul.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chensoul.test;

import com.chensoul.config.TestDbConfig;
import com.chensoul.persistence.dao.PrivilegeRepository;
import com.chensoul.persistence.dao.RoleRepository;
import com.chensoul.persistence.dao.UserRepository;
import com.chensoul.persistence.model.Privilege;
import com.chensoul.persistence.model.Role;
import com.chensoul.persistence.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestDbConfig.class)
@Transactional
public class SpringSecurityRolesIntegrationTest {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PrivilegeRepository privilegeRepository;

    private final PasswordEncoder passwordEncoder;

    private User user;
    private Role role;
    private Privilege privilege;

    // tests

    @Test
    public void testDeleteUser() {
        role = new Role("TEST_ROLE");
        roleRepository.save(role);

        user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword(passwordEncoder.encode("123"));
        user.setEmail("john@doe.com");
        user.setRoles(Arrays.asList(role));
        user.setEnabled(true);
        userRepository.save(user);

        assertNotNull(userRepository.findByEmail(user.getEmail()));
        assertNotNull(roleRepository.findByName(role.getName()));
        user.setRoles(null);
        userRepository.delete(user);

        assertNull(userRepository.findByEmail(user.getEmail()));
        assertNotNull(roleRepository.findByName(role.getName()));
    }

    @Test
    public void testDeleteRole() {
        privilege = new Privilege("TEST_PRIVILEGE");
        privilegeRepository.save(privilege);

        role = new Role("TEST_ROLE");
        role.setPrivileges(Arrays.asList(privilege));
        roleRepository.save(role);

        user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword(passwordEncoder.encode("123"));
        user.setEmail("john@doe.com");
        user.setRoles(Arrays.asList(role));
        user.setEnabled(true);
        userRepository.save(user);

        assertNotNull(privilegeRepository.findByName(privilege.getName()));
        assertNotNull(userRepository.findByEmail(user.getEmail()));
        assertNotNull(roleRepository.findByName(role.getName()));

        user.setRoles(new ArrayList<>());
        role.setPrivileges(new ArrayList<>());
        roleRepository.delete(role);

        assertNull(roleRepository.findByName(role.getName()));
        assertNotNull(privilegeRepository.findByName(privilege.getName()));
        assertNotNull(userRepository.findByEmail(user.getEmail()));
    }

    @Test
    public void testDeletePrivilege() {
        privilege = new Privilege("TEST_PRIVILEGE");
        privilegeRepository.save(privilege);

        role = new Role("TEST_ROLE");
        role.setPrivileges(Arrays.asList(privilege));
        roleRepository.save(role);

        assertNotNull(roleRepository.findByName(role.getName()));
        assertNotNull(privilegeRepository.findByName(privilege.getName()));

        role.setPrivileges(new ArrayList<>());
        privilegeRepository.delete(privilege);

        assertNull(privilegeRepository.findByName(privilege.getName()));
        assertNotNull(roleRepository.findByName(role.getName()));
    }
}
