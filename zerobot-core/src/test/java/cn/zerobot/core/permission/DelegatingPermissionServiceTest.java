package cn.zerobot.core.permission;

import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DelegatingPermissionServiceTest {
    private static final PermissionSubject SUBJECT = new PermissionSubject("10001", "20001", "group");

    @Test
    void registeredServiceTakesOverUntilClosed() throws Exception {
        DelegatingPermissionService service = new DelegatingPermissionService((subject, permission) -> false);

        EventSubscription subscription = service.register((subject, permission) -> true);

        assertThat(service.hasPermission(SUBJECT, "example.use")).isTrue();

        subscription.close();

        assertThat(service.hasPermission(SUBJECT, "example.use")).isFalse();
    }

    @Test
    void staleRegistrationDoesNotReplaceNewerServiceOnClose() throws Exception {
        DelegatingPermissionService service = new DelegatingPermissionService((subject, permission) -> false);

        EventSubscription first = service.register((subject, permission) -> true);
        PermissionService newer = (subject, permission) -> "newer.use".equals(permission);
        service.register(newer);

        first.close();

        assertThat(service.hasPermission(SUBJECT, "newer.use")).isTrue();
        assertThat(service.active()).isSameAs(newer);
    }
}
