package cn.zerobot.api.event;

/**
 * 群文件上传监听器。
 */
@FunctionalInterface
public interface GroupFileUploadListener {
    /**
     * 收到群文件上传通知时触发。
     */
    void onGroupFileUpload(GroupFileUploadEvent event) throws Exception;
}
