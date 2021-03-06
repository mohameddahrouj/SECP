package com.visucius.secp.Controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.visucius.secp.Controllers.User.UserErrorMessage;
import com.visucius.secp.Controllers.User.UserProfileController;
import com.visucius.secp.DTO.*;
import com.visucius.secp.daos.GroupDAO;
import com.visucius.secp.daos.PermissionDAO;
import com.visucius.secp.daos.RolesDAO;
import com.visucius.secp.daos.UserDAO;
import com.visucius.secp.models.*;
import com.visucius.secp.util.InputValidator;
import com.visucius.secp.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupController {

    private static final Logger LOG = LoggerFactory.getLogger(GroupController.class);

    private GroupDAO groupRepository;
    private UserDAO userRepository;
    private RolesDAO rolesRepository;
    private PermissionDAO permissionsRepository;
    private UserProfileController userProfileController;

    private static final String publicGroupAvatar = "https://user-images.githubusercontent.com/14824913/34922739-e9cbf830-f961-11e7-9a92-a4559596162f.png";
    private static final String privateGroupAvatar = "https://user-images.githubusercontent.com/14824913/34922743-f386cabc-f961-11e7-84af-be3f61f41005.png";


    public GroupController(GroupDAO groupRepository,
                           UserDAO userRepository,
                           RolesDAO rolesRepository,
                           PermissionDAO permissionDAO,
                           UserProfileController userProfileController) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.rolesRepository = rolesRepository;
        this.permissionsRepository = permissionDAO;
        this.userProfileController = userProfileController;
    }


    public Response getGroupsForUser(User user)
    {
        User userWithGroups = userRepository.getUserWithGroups(user.getId()).get();
        Set<GroupDTO> response = userWithGroups.getGroups().stream()
            .map(group -> {
                return getGroupDTOWithMessages(group);
            })
            .collect(Collectors.toSet());

        return Response.status(Response.Status.OK).entity(response).build();
    }

    public Response deleteGroup(int id) {
        Group group = getGroup(id);
        group.setIsActive(false);
        groupRepository.save(group);
        return Response.status(Response.Status.OK).build();
    }

    public Response createPublicGroup(GroupCreateRequest request) {
        Set<Long> permissions = request.permissions.stream().
            map(permission -> permission.getId()).collect(Collectors.toSet());
        Set<Long> roles = request.roles.stream().
            map(role -> role.getId()).collect(Collectors.toSet());
        String error = validateCreateRequest(request.name, permissions, roles);
        if (StringUtils.isNoneEmpty(error)) {
            throw new WebApplicationException(
                error,
                Response.Status.BAD_REQUEST);
        }

        Group group = new Group(request.name);
        return updateOrCreateGroup(group, permissions, roles);
    }

    public Response createPrivateGroup(User creator, UserDTO userDTO) {
        String groupName = getValidGroupName(creator.getUsername(), userDTO.getUsername());

        Group group = new Group(groupName);
        group.setGroupType(GroupType.PRIVATE);
        group.addUser(getUser(creator.getId()));
        group.addUser(getUser(userDTO.getUserID()));
        List<Group> groups = groupRepository.findPrivateGroupForUsers(group.getUsers());

        if(!groups.isEmpty())
        {
            return Response.
                status(Response.Status.OK).
                entity(getGroupResponse(groups.get(0))).build();
        }

        group.setDisplayname(getDisplayNameForGroup(group));
        group.setAvatarurl(getAvatarForGroup(group));
        Group createdGroup = groupRepository.save(group);
        return Response.status(Response.Status.CREATED).entity(getGroupResponse(createdGroup)).build();
    }

    public Response modifyGroup(GroupDTO request) {

        Set<Long> permissions = request.getPermissions().stream().
            map(permission -> permission.getId()).collect(Collectors.toSet());
        Set<Long> roles = request.getRoles().stream().
            map(role -> role.getId()).collect(Collectors.toSet());

        String error = validateRolesAndPermissions(permissions, roles);
        if (StringUtils.isNoneEmpty(error)) {
            throw new WebApplicationException(
                error,
                Response.Status.BAD_REQUEST);
        }

        Group group = getGroup(request.getGroupID());
        if(group.getGroupType() == GroupType.PRIVATE)
        {
            throw new WebApplicationException(
                GroupErrorMessages.MODIFY_PRIVATE_GROUP,
                Response.Status.BAD_REQUEST);
        }

        group.setIsActive(request.isActive());
        return updateOrCreateGroup(group, permissions, roles);
    }

    private Response updateOrCreateGroup(Group group, Set<Long> permissions, Set<Long> roles) {
        group.setPermissions(getPermissions(permissions));
        group.setRoles(getRoles(roles));
        group.setAvatarurl(getAvatarForGroup(group));
        group.setDisplayname(getDisplayNameForGroup(group));

        group.setUsers(this.findUsersWithRolesAndPermissions(group.getRoles(), group.getPermissions()));
        Group createdGroup = groupRepository.save(group);
        return Response.status(Response.Status.CREATED).entity(getGroupResponse(createdGroup)).build();
    }

    private Group getGroup(long groupID) {
        Optional<Group> groupOptional = groupRepository.find(groupID);
        if (!groupOptional.isPresent()) {
            throw new WebApplicationException(
                GroupErrorMessages.GROUP_DOES_NOT_EXIST,
                Response.Status.BAD_REQUEST);
        }

        return groupOptional.get();
    }

    public Response getAllGroups() {
        List<Group> groups = groupRepository.findAllPublicGroups();

        if (Util.isCollectionEmpty(groups)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        Set<GroupDTO> response = groups.stream()
            .map(group -> {
                return getGroupResponse(group);
            })
            .collect(Collectors.toSet());

        return Response.status(Response.Status.OK).entity(response).build();
    }

    public Response getGroupGivenId(String id) {
        if (StringUtils.isEmpty(id) || !StringUtils.isNumeric(id)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        long groupID = Long.parseLong(id);
        Group group = getGroup(groupID);

        return Response.status(Response.Status.OK).entity(getGroupResponse(group)).build();
    }

    private String validateCreateRequest(String name, Set<Long> permissions, Set<Long> roles) {

        if (!isGroupNameValid(name)
            || !InputValidator.isNameValid(name)) {
            return GroupErrorMessages.GROUP_NAME_INVALID;
        }

        return validateRolesAndPermissions(permissions, roles);
    }

    private String validateRolesAndPermissions(Set<Long> permissions, Set<Long> roles) {

        String roleError = validateRoles(roles);
        if (StringUtils.isNoneEmpty(roleError))
            return roleError;

        String permissionError = validatePermissions(permissions);
        if (StringUtils.isNoneEmpty(permissionError))
            return permissionError;

        return StringUtils.EMPTY;
    }

    private String validateRoles(Set<Long> roles) {
        for (long roleID : roles) {
            if (!isRoleIdValid(roleID)) {
                return String.format(GroupErrorMessages.ROLE_ID_INVALID, roleID);
            }
        }

        return StringUtils.EMPTY;
    }

    private String validatePermissions(Set<Long> permissions) {
        for (long permissionID : permissions) {
            if (!isPermissionValid(permissionID)) {
                return String.format(GroupErrorMessages.PERMISSION_ID_INVALID, permissionID);
            }
        }

        return StringUtils.EMPTY;
    }

    private Set<User> findUsersWithRolesAndPermissions(Set<Role> roles, Set<Permission> permissions) {
        Set<User> userWithRole = new HashSet<>();
        Set<User> userWithPermissionLevel = new HashSet<>();

        if(permissions.isEmpty() && roles.isEmpty())
            return new HashSet<>(userRepository.findAll());

        permissions.
            forEach(permission -> userWithPermissionLevel.addAll(permission.getUsers()));
        if (roles.isEmpty())
            return userWithPermissionLevel;
        else {
            roles.forEach(role -> userWithRole.addAll(role.getUsers()));
            return Sets.intersection(userWithPermissionLevel, userWithRole);
        }
    }

    private Set<Role> getRoles(Set<Long> rolesIDs) {
        Set<Role> roles = new HashSet<>();
        rolesIDs.forEach(id -> roles.add(rolesRepository.find(id).get()));
        return roles;
    }

    private Set<Permission> getPermissions(Set<Long> permissionID) {
        Set<Permission> permissions = new HashSet<>();
        permissionID.forEach(id -> permissions.add(permissionsRepository.find(id).get()));
        return permissions;
    }

    private GroupDTO getGroupDTOWithMessages(Group group)
    {
        GroupDTO groupDTO = getGroupResponse(group);
        group.getMessages().stream().forEach(message ->
        {
            groupDTO.addMessage(
                new MessageDTO(message.getId(),
                    group.getId(),
                    message.getUser().getId(),
                    message.getBody(),
                    MessageDTO.MessageType.MESSAGE,
                    message.getTimestamp()));
        });

        if(!Util.isCollectionEmpty(groupDTO.getMessages())) {
            Comparator<MessageDTO> comp = Comparator.comparingLong(MessageDTO::getMessageId);
            MessageDTO lastMessage = groupDTO.getMessages().stream().max(comp).get();
            groupDTO.setLastMessage(lastMessage);
        }

        groupDTO.setDisplayName(getDisplayNameForGroup(group));
        groupDTO.setAvatarUrl(getAvatarForGroup(group));
        return groupDTO;
    }

    private String getAvatarForGroup(Group group) {
        String groupAvatar = group.getAvatarurl();

        if(StringUtils.isEmpty(groupAvatar)) {
            if(group.getGroupType() == null || group.getGroupType().equals(GroupType.PUBLIC)) {
                groupAvatar = publicGroupAvatar;
            } else {
                groupAvatar = privateGroupAvatar;
            }
        }

        return groupAvatar;
    }

    private String getDisplayNameForGroup(Group group) {
        String displayName = group.getDisplayname();

        if(StringUtils.isEmpty(displayName)) {
            displayName =  group.getName();
        }

        return displayName;
    }

    public GroupDTO getGroupResponse(Group group) {
        GroupDTO groupDTO = new GroupDTO(group.getId());
        groupDTO.setName(group.getName());
        groupDTO.setNumOfPermissions(group.getPermissions().size());
        groupDTO.setNumOfRoles(group.getRoles().size());
        groupDTO.setActive(group.isActive());
        groupDTO.setDisplayName(getDisplayNameForGroup(group));
        groupDTO.setAvatarUrl(getAvatarForGroup(group));
        groupDTO.setGroupType(group.getGroupType());

        Set<UserDTO> users = getUsersInGroup(group).stream().filter(user -> user.isActive())
            .map(user -> {
                return userProfileController.getUserProfileResponse(user);
            }).collect(Collectors.toSet());

        Set<RolesOrPermissionDTO> roles = group.getRoles().stream()
            .map(role -> {
                return new RolesOrPermissionDTO(role.getId(), role.getRole(), role.getColor());
            }).collect(Collectors.toSet());

        Set<RolesOrPermissionDTO> permissions = group.getPermissions().stream()
            .map(permission -> {
                return new RolesOrPermissionDTO(permission.getId(), permission.getLevel(), permission.getColor());
            }).collect(Collectors.toSet());

        groupDTO.setUsers(users);
        groupDTO.setRoles(roles);
        groupDTO.setPermissions(permissions);
        groupDTO.setNumOfUsers(users.size());

        return groupDTO;
    }

    private Set<User> getUsersInGroup(Group group) {
        Set<User> users = new HashSet<>();
        Util.addAllIfNotNull(users, group.getUsers());
        return users;
    }

    private User getUser(long userID) {
        Optional<User> userOptional = userRepository.getUserWithDevices(userID);
        if (!userOptional.isPresent()) {
            throw new WebApplicationException(
                UserErrorMessage.USER_ID_INVALID,
                Response.Status.BAD_REQUEST);
        }

        return userOptional.get();
    }

    private String getValidGroupName(String user1, String user2) {
        StringBuilder groupName = new StringBuilder(user1 + "-" + user2);
        int count = 1;

        while (!isGroupNameValid(groupName.toString())) {
            groupName.append(count);
            count++;
        }

        return groupName.toString();
    }

    private boolean isPermissionValid(long id) {
        return permissionsRepository.find(id).isPresent();
    }

    private boolean isRoleIdValid(long id) {
        return rolesRepository.find((int) id).isPresent();
    }

    private boolean isGroupNameValid(String name) {
        return groupRepository.findByName(name) == null;
    }
}
