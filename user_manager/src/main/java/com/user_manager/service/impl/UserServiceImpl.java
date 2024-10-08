package com.user_manager.service.impl;

import com.user_manager.dto.UserCreationRequest;
import com.user_manager.dto.UserInfoDto;
import com.user_manager.enums.NotificationTopic;
import com.user_manager.exception.NotFoundException;
import com.user_manager.kafka.producer.NotificationProducer;
import com.user_manager.model.Department;
import com.user_manager.enums.Role;
import com.user_manager.model.User;
import com.user_manager.repository.UserRepository;
import com.user_manager.service.DepartmentService;
import com.user_manager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.user_manager.utility.TimeFormatter.formatter;
import static com.user_manager.utility.TimeFormatter.now;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final NotificationProducer notificationProducer;
    private final UserNotificationDeletionService userNotificationDeletionService;


    @Override
    public User createUser(UserCreationRequest request) throws NotFoundException {
        Department department = departmentService.getDepartmentById(request.getDepartmentId());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .contacts(request.getContacts())
                .position(request.getPosition())
                .role(request.getRole())
                .department(department)
                .creationDate(now.format(formatter))
                .modificationDate(now.format(formatter))
                .isActive(true)
                .build();

        userRepository.save(user);

        //notification about new user
        Long id = user.getId();
        notificationProducer.sendNotification(id, NotificationTopic.USER_CREATED);

        return user;
    }

    @Override
    public User editUserInfo(Long id, UserInfoDto request) throws NotFoundException {
        User user = getUserById(id);
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        Map<String, String> contacts = user.getContacts();

        if (request.getFirstName() != null) {
            firstName = request.getFirstName();
        }
        if (request.getLastName() != null) {
            lastName = request.getLastName();
        }
        if (request.getContacts() != null) {
            contacts = request.getContacts();
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setContacts(contacts);
        user.setModificationDate(now.format(formatter));
        userRepository.save(user);

        //notification about user info update
        notificationProducer.sendNotification(id, NotificationTopic.USER_INFO_EDITED);

        return user;
    }

    @Override
    public String updateUserPosition(Long id, String position) throws NotFoundException {
        User user = getUserById(id);
        user.setPosition(position);
        userRepository.save(user);

        //notification about user position update
        notificationProducer.sendNotification(id, NotificationTopic.USER_POSITION_UPDATE);

        return "User position is updated";
    }

    @Override
    public String updateUserRole(Long id, Role role) throws NotFoundException {
        User user = getUserById(id);
        user.setRole(role);
        userRepository.save(user);

        //notification about user role update
        notificationProducer.sendNotification(id, NotificationTopic.USER_ROLE_UPDATE);

        return "User role is updated";
    }

    @Override
    public String updateUserDepartment(Long id, Long departmentId) throws NotFoundException {
        User user = getUserById(id);
        Department department = departmentService.getDepartmentById(departmentId);
        user.setDepartment(department);
        userRepository.save(user);

        //notification about user department update
        notificationProducer.sendNotification(id, NotificationTopic.USER_DEPARTMENT_UPDATE);
        notificationProducer.sendNotification(id, NotificationTopic.USER_ADDED_TO_DEPARTMENT);

        return "User department is updated";
    }

    @Override
    public String activateUser(Long id) throws NotFoundException {
        User user = getUserById(id);
        user.setIsActive(true);
        user.setModificationDate(now.format(formatter));
        userRepository.save(user);

        //notification about user activation
        notificationProducer.sendNotification(id, NotificationTopic.USER_ACTIVATED);

        return "User is activated";
    }

    @Override
    public String deactivateUser(Long id) throws NotFoundException {
        User user = getUserById(id);
        user.setIsActive(false);
        user.setModificationDate(now.format(formatter));
        userRepository.save(user);

        //notification about user deactivation
        notificationProducer.sendNotification(id, NotificationTopic.USER_DEACTIVATED);

        return "User is deactivated";
    }

    @Transactional
    @Override
    public String delete(Long id) throws NotFoundException {
        User user = getUserById(id);
        //delete notification
        userNotificationDeletionService.deleteUserNotifications(user);
        userRepository.delete(user);

        //notification about user deletion
        notificationProducer.sendNotification(id, NotificationTopic.USER_DELETED);
        return "User is deleted";
    }

    @Override
    public User getInfo(Long id) throws NotFoundException {
        return getUserById(id);
    }

    @Override
    public List<User> getAllUserOfDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId);
    }


    @Override
    public Boolean existsById(Long id) {
        return userRepository.existsById(id);
    }


    @Override
    public User getUserById(Long id) throws NotFoundException {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User with " + id + " does not exist"));

    }

    @Override
    public List<Long> getAllUserIds() {
        return userRepository.findAllUserIds();
    }

    @Override
    public List<Long> findAllUserIdsOfDepartment(Department department) {
        return userRepository.findAllUserIdsOfDepartment(department);
    }
}
