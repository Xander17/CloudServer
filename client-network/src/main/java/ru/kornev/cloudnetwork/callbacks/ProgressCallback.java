package ru.kornev.cloudnetwork.callbacks;

import ru.kornev.cloudcommon.services.transfer.Progress;

public interface ProgressCallback {
    void callback(Progress progress);
}
