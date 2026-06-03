package cn.zerobot.api;

public record ActionResponse<T>(
        String status,
        int retcode,
        T data,
        String message,
        String wording,
        String echo
) {
    public boolean ok() {
        return "ok".equalsIgnoreCase(status) && retcode == 0;
    }
}
