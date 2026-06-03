package cn.zerobot.api.permission;

/**
 * 权限服务。
 * <p>
 * 插件应通过这个接口判断用户是否拥有某个权限节点，而不是自己硬编码 QQ 号。
 */
public interface PermissionService {
    /**
     * 判断主体是否拥有指定权限节点。
     * <p>
     * 权限节点建议使用小写命名空间格式，例如 {@code zerobot.command.reload}。
     */
    boolean hasPermission(PermissionSubject subject, String permission);

    /**
     * 判断主体是否拥有指定权限节点，并指定默认结果。
     * <p>
     * 适合“命令有权限节点，但默认所有人可用”的场景。权限服务没有明确授予时返回 {@code defaultAllowed}。
     */
    default boolean hasPermission(PermissionSubject subject, String permission, boolean defaultAllowed) {
        return hasPermission(subject, permission) || defaultAllowed;
    }
}
