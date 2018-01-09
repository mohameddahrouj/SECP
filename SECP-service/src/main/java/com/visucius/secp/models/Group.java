package com.visucius.secp.models;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "Groups")
@NamedQueries(
    {
        @NamedQuery(
            name = "com.visucius.secp.models.Group.findByName",
            query = "from Group g where g.name = :name"
        ),
        @NamedQuery(
            name = "com.visucius.secp.models.Group.findGroupsWithRole",
            query = "select g from Group g join g.roles r where r.id = :roleID"
        ),
        @NamedQuery(
            name = "com.visucius.secp.models.Group.findGroupsWithPermissionLevel",
            query = "select g from Group g join g.permissions p where p.id = :permissionID"
        ),
        @NamedQuery(
            name = "com.visucius.secp.models.Group.findGroupsForUser",
            query = "select g from Group g join g.permissions p join g.roles r where p.id = :permissionID and r.id in (:roleIDS)"
        )
    }
)
public class Group {

    @Id
    @GeneratedValue
    @Column(name = "id", unique = true, nullable = false)
    private long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @ManyToMany()
    @JoinTable(name = "group_roles",
        joinColumns = { @JoinColumn(name = "group_id") },
        inverseJoinColumns = { @JoinColumn(name = "role_id") })
    private Set<Role> roles = new HashSet<>();

    @ManyToMany()
    @JoinTable(name = "group_permissions",
        joinColumns = { @JoinColumn(name = "group_id") },
        inverseJoinColumns = { @JoinColumn(name = "permission_id") })
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany()
    @JoinTable(name = "group_user",
        joinColumns = { @JoinColumn(name = "group_id") },
        inverseJoinColumns = { @JoinColumn(name = "user_id") })
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private Set<Message> messages = new HashSet<>();

    public Group()
    {

    }

    public Group(String name) {
        this.name = name;
    }

    public long getId(){return id;}

    public void setId(long id){this.id = id;}

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Set<User> getUsers() {
        return this.users;
    }

    public void setUsers(Set<User> users){this.users = users;}

    public Set<Role> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<Role> roles){this.roles = roles;}

    public void setPermissions(Set<Permission> permissions){this.permissions = permissions;}

    public Set<Message> getMessages() {return this.messages;}

    public Set<Permission> getPermissions() {
        return this.permissions;
    }

    public void setMessages(Set<Message> messages){this.messages = messages;}

    public void addPermissions(Collection<Permission> permissions)
    {
        this.permissions.addAll(permissions);
    }

    public void addRoles(Collection<Role> roles)
    {
        this.roles.addAll(roles);
    }

    public void removeRoles(Collection<Role> roles)
    {
        this.roles.removeAll(roles);
    }

    public void removePermissions(Collection<Permission> permissions)
    {
        this.permissions.removeAll(permissions);
    }

    public void addUser(User user) {
        users.add(user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        Group group = (Group) o;
        return id == group.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id,this.name);
    }
}
