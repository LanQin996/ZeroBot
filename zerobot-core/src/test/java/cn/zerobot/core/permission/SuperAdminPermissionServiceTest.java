package cn.zerobot.core.permission;

import cn.zerobot.api.permission.PermissionSubject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SuperAdminPermissionServiceTest {
    @Test
    void superAdminHasEveryPermission() {
        SuperAdminPermissionService service = new SuperAdminPermissionService(List.of("10001"));

        assertThat(service.hasPermission(new PermissionSubject("10001", null, "private"), "anything.here")).isTrue();
        assertThat(service.hasPermission(new PermissionSubject("10002", null, "private"), "anything.here")).isFalse();
    }

    @Test
    void defaultAllowedGrantsPermissionToRegularUsers() {
        SuperAdminPermissionService service = new SuperAdminPermissionService(List.of("10001"));

        assertThat(service.hasPermission(new PermissionSubject("10002", null, "private"), "public.command", true)).isTrue();
        assertThat(service.hasPermission(new PermissionSubject("10002", null, "private"), "private.command", false)).isFalse();
    }
}
