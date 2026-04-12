package ru.hse.lab2.api.mapper;

import ru.hse.lab2.api.dto.ViewerDto;
import ru.hse.lab2.api.request.ViewerRequest;
import ru.hse.lab2.entity.Viewer;

public final class ViewerMapper {

    private ViewerMapper() {
    }

    public static ViewerDto toDto(Viewer viewer) {
        return new ViewerDto(
                viewer.getId(),
                viewer.getName(),
                viewer.getEmail()
        );
    }

    public static Viewer toEntity(ViewerRequest request) {
        return new Viewer(
                request.getName(),
                request.getEmail()
        );
    }

    public static void updateEntity(Viewer target, ViewerRequest request) {
        target.setName(request.getName());
        target.setEmail(request.getEmail());
    }
}
