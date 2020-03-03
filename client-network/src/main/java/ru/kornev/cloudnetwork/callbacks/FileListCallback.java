package ru.kornev.cloudnetwork.callbacks;

import ru.kornev.cloudcommon.resources.FileRepresentation;

import java.util.List;

public interface FileListCallback {
    void callback(List<FileRepresentation> list);
}
