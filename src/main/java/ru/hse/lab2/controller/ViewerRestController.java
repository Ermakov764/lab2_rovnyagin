package ru.hse.lab2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.ViewerDto;
import ru.hse.lab2.api.request.ViewerRequest;
import ru.hse.lab2.service.ViewerService;

import java.util.List;

@RestController
@RequestMapping("/api/viewers")
public class ViewerRestController {

    private final ViewerService viewerService;

    public ViewerRestController(ViewerService viewerService) {
        this.viewerService = viewerService;
    }

    @GetMapping
    public List<ViewerDto> getAll() {
        return viewerService.getAll();
    }

    @GetMapping("/{id}")
    public ViewerDto getById(@PathVariable Long id) {
        return viewerService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ViewerDto create(@RequestBody ViewerRequest request) {
        return viewerService.create(request);
    }

    @PutMapping("/{id}")
    public ViewerDto update(@PathVariable Long id, @RequestBody ViewerRequest request) {
        return viewerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        viewerService.delete(id);
    }
}
