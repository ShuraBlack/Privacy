package de.shurablack.http;

import de.shurablack.util.BlockedRequestLogger;

import java.util.List;

public class RequestBlocker {

    private final BlockedPath[] blockedBlockedPaths;

    public RequestBlocker(List<BlockedPath> blockedBlockedPaths) {
        this.blockedBlockedPaths = blockedBlockedPaths.toArray(new BlockedPath[0]);
    }

    public boolean blockedPath(Request request) {
        if (contains(request.getMethod(), request.getPath())) {
            RequestHandler.BLACKLIST.put(
                    request.getOrigin().getRemoteAddress().getAddress().toString(),
                    System.currentTimeMillis()
            );
            BlockedRequestLogger.log(request);
            request.getOrigin().close();
            return true;
        }
        return false;
    }

    private boolean contains(Method method, String path) {
        for (BlockedPath blockedPath : blockedBlockedPaths) {
            if (blockedPath.getMethod() == method &&
                    blockedPath.getPath().equals(path)) {
                return blockedPath.getComparison().apply(blockedPath.getPath(), path);
            }
        }
        return false;
    }
}
