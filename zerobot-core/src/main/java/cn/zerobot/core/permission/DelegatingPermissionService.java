package cn.zerobot.core.permission;

import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DelegatingPermissionService implements PermissionService {
    private final PermissionService fallback;
    private final List<Registration> registrations = new ArrayList<>();
    private volatile PermissionService active;

    public DelegatingPermissionService(PermissionService fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.active = fallback;
    }

    public EventSubscription register(PermissionService permissionService) {
        Objects.requireNonNull(permissionService, "permissionService");
        Registration registration = new Registration(permissionService);
        synchronized (this) {
            registrations.add(registration);
            active = permissionService;
        }
        return () -> {
            synchronized (this) {
                if (registrations.remove(registration)) {
                    active = registrations.isEmpty()
                            ? fallback
                            : registrations.get(registrations.size() - 1).permissionService();
                }
            }
        };
    }

    public PermissionService active() {
        return active;
    }

    public PermissionService fallback() {
        return fallback;
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String permission) {
        return active.hasPermission(subject, permission);
    }

    @Override
    public boolean hasPermission(PermissionSubject subject, String permission, boolean defaultAllowed) {
        return active.hasPermission(subject, permission, defaultAllowed);
    }

    private record Registration(PermissionService permissionService) {
    }
}
