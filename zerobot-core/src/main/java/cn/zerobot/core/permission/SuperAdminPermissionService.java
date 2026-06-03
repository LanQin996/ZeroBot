package cn.zerobot.core.permission;

import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SuperAdminPermissionService implements PermissionService {
    private final Set<String> superAdmins;

    public SuperAdminPermissionService(Collection<String> superAdmins) {
        this.superAdmins = superAdmins.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String permission) {
        return subject != null && subject.userId() != null && superAdmins.contains(subject.userId());
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String permission, boolean defaultAllowed) {
        return hasPermission(subject, permission) || defaultAllowed;
    }
}
