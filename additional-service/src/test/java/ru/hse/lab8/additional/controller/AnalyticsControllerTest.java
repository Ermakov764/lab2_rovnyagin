package ru.hse.lab8.additional.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.lab.shared.analytics.TopFilmsReportLimits;
import ru.hse.lab8.additional.service.AnalyticsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController controller;

    @Test
    void nullLimit_usesSharedDefault() {
        when(analyticsService.maxViewersSummaries(eq(TopFilmsReportLimits.DEFAULT))).thenReturn(List.of());

        controller.maxViewersSummary(null);

        verify(analyticsService).maxViewersSummaries(TopFilmsReportLimits.DEFAULT);
    }
}
