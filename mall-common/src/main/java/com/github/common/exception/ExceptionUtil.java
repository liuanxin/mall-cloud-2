package com.github.common.exception;

public final class ExceptionUtil {

    public static boolean hasProjectException(Throwable e) {
        return e instanceof BadRequestException
                || e instanceof ForbiddenException
                || e instanceof NotFoundException
                || e instanceof NotLoginException
                || e instanceof ParamException
                || e instanceof ServiceException;
    }

    private static Throwable getInnerException(int depth, Throwable e) {
        if (e.getCause() == null || depth > 3 || hasProjectException(e)) {
            return e;
        }
        return getInnerException(depth + 1, e.getCause());
    }

    public static Throwable boxInnerException(Throwable e) {
        return getInnerException(1, e.getCause());
    }
}
