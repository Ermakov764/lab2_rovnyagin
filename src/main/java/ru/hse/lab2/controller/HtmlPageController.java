package ru.hse.lab2.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.FilmDto;
import ru.hse.lab2.api.dto.TicketDto;
import ru.hse.lab2.api.dto.ViewerDto;
import ru.hse.lab2.debug.DebugProbe;
import ru.hse.lab2.api.request.FilmRequest;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.api.request.ViewerRequest;
import ru.hse.lab2.exception.DomainException;
import ru.hse.lab2.service.FilmService;
import ru.hse.lab2.service.TicketService;
import ru.hse.lab2.service.ViewerService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
public class HtmlPageController {

    private final FilmService filmService;
    private final ViewerService viewerService;
    private final TicketService ticketService;

    public HtmlPageController(FilmService filmService, ViewerService viewerService, TicketService ticketService) {
        this.filmService = filmService;
        this.viewerService = viewerService;
        this.ticketService = ticketService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String homePage() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <title>Cinema Lab2</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; }
                    a { margin-right: 1rem; }
                  </style>
                </head>
                <body>
                  <h1>Cinema Lab2</h1>
                  <p>Web layer is available. Choose a page:</p>
                  <nav>
                    <a href="/films/page">Films</a>
                    <a href="/viewers/page">Viewers</a>
                    <a href="/tickets/page">Tickets</a>
                  </nav>
                </body>
                </html>
                """;
    }

    @GetMapping(value = "/films/page", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String filmsPage() {
        List<FilmDto> films = filmService.getAll();
        StringBuilder rows = new StringBuilder();
        for (FilmDto film : films) {
            rows.append("<tr>")
                    .append("<td>").append(film.getId()).append("</td>")
                    .append("<td>").append(escapeHtml(film.getTitle())).append("</td>")
                    .append("<td>").append(escapeHtml(film.getGenre())).append("</td>")
                    .append("<td>").append(film.getDurationMinutes()).append("</td>")
                    .append("</tr>");
        }
        return pageShell("Films", """
                <h1>Films</h1>
                <p>REST endpoint: <code>/api/films</code></p>
                <p><a href="/films/page/create">Create film</a></p>
                <table border="1" cellpadding="6" cellspacing="0">
                  <thead>
                    <tr><th>Id</th><th>Title</th><th>Genre</th><th>Duration (min)</th></tr>
                  </thead>
                  <tbody>
                """ + rows + """
                  </tbody>
                </table>
                """);
    }

    @GetMapping(value = "/films/page/create", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String createFilmPage(@RequestParam(required = false) String error) {
        return pageShell("Create film", """
                <h1>Create film</h1>
                """ + errorBlock(error) + """
                <form method="post" action="/films/page/create">
                  <label>Title: <input name="title" required /></label><br/><br/>
                  <label>Genre: <input name="genre" /></label><br/><br/>
                  <label>Duration (minutes): <input name="durationMinutes" type="number" min="1" /></label><br/><br/>
                  <button type="submit">Create</button>
                </form>
                """);
    }

    @PostMapping("/films/page/create")
    public ResponseEntity<Void> createFilm(
            @RequestParam String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String durationMinutes
    ) {
        try {
            Integer parsedDuration = (durationMinutes == null || durationMinutes.isBlank())
                    ? null
                    : Integer.valueOf(durationMinutes);
            filmService.create(new FilmRequest(title, genre, parsedDuration));
            return redirect("/films/page");
        } catch (DomainException | IllegalArgumentException ex) {
            return redirectWithError("/films/page/create", ex.getMessage());
        }
    }

    @GetMapping(value = "/viewers/page", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String viewersPage() {
        List<ViewerDto> viewers = viewerService.getAll();
        StringBuilder rows = new StringBuilder();
        for (ViewerDto viewer : viewers) {
            rows.append("<tr>")
                    .append("<td>").append(viewer.getId()).append("</td>")
                    .append("<td>").append(escapeHtml(viewer.getName())).append("</td>")
                    .append("<td>").append(escapeHtml(viewer.getEmail())).append("</td>")
                    .append("</tr>");
        }
        return pageShell("Viewers", """
                <h1>Viewers</h1>
                <p>REST endpoint: <code>/api/viewers</code></p>
                <p><a href="/viewers/page/create">Create viewer</a></p>
                <table border="1" cellpadding="6" cellspacing="0">
                  <thead>
                    <tr><th>Id</th><th>Name</th><th>Email</th></tr>
                  </thead>
                  <tbody>
                """ + rows + """
                  </tbody>
                </table>
                """);
    }

    @GetMapping(value = "/viewers/page/create", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String createViewerPage(@RequestParam(required = false) String error) {
        return pageShell("Create viewer", """
                <h1>Create viewer</h1>
                """ + errorBlock(error) + """
                <form method="post" action="/viewers/page/create">
                  <label>Name: <input name="name" required /></label><br/><br/>
                  <label>Email: <input name="email" type="email" required /></label><br/><br/>
                  <button type="submit">Create</button>
                </form>
                """);
    }

    @PostMapping("/viewers/page/create")
    public ResponseEntity<Void> createViewer(
            @RequestParam String name,
            @RequestParam String email
    ) {
        try {
            viewerService.create(new ViewerRequest(name, email));
            return redirect("/viewers/page");
        } catch (DomainException | IllegalArgumentException ex) {
            return redirectWithError("/viewers/page/create", ex.getMessage());
        }
    }

    @GetMapping(value = "/tickets/page", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String ticketsPage() {
        List<TicketDto> tickets = ticketService.getAll();
        StringBuilder rows = new StringBuilder();
        for (TicketDto ticket : tickets) {
            rows.append("<tr>")
                    .append("<td>").append(ticket.getId()).append("</td>")
                    .append("<td>").append(ticket.getViewerId()).append("</td>")
                    .append("<td>").append(ticket.getFilmId()).append("</td>")
                    .append("<td>").append(ticket.getSessionDate()).append("</td>")
                    .append("<td>").append(ticket.getSessionTime()).append("</td>")
                    .append("<td>").append(escapeHtml(ticket.getSeatNumber())).append("</td>")
                    .append("<td>").append(ticket.getPrice()).append("</td>")
                    .append("</tr>");
        }
        return pageShell("Tickets", """
                <h1>Tickets</h1>
                <p>REST endpoint: <code>/api/tickets</code></p>
                <p>Analytics endpoint: <code>/api/tickets/analytics/max-viewers?filmId=&lt;id&gt;</code></p>
                <p><a href="/tickets/page/create">Create ticket</a></p>
                <table border="1" cellpadding="6" cellspacing="0">
                  <thead>
                    <tr>
                      <th>Id</th><th>Viewer Id</th><th>Film Id</th><th>Date</th><th>Time</th><th>Seat</th><th>Price</th>
                    </tr>
                  </thead>
                  <tbody>
                """ + rows + """
                  </tbody>
                </table>
                """);
    }

    @GetMapping(value = "/tickets/page/create", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String createTicketPage(@RequestParam(required = false) String error) {
        return pageShell("Create ticket", """
                <h1>Create ticket</h1>
                """ + errorBlock(error) + """
                <form method="post" action="/tickets/page/create">
                  <label>Viewer id: <input name="viewerId" type="number" min="1" required /></label><br/><br/>
                  <label>Film id: <input name="filmId" type="number" min="1" required /></label><br/><br/>
                  <label>Date: <input name="sessionDate" type="date" required /></label><br/><br/>
                  <label>Time: <input name="sessionTime" type="time" required /></label><br/><br/>
                  <label>Seat: <input name="seatNumber" required /></label><br/><br/>
                  <label>Price: <input name="price" type="number" step="0.01" min="0" /></label><br/><br/>
                  <button type="submit">Create</button>
                </form>
                """);
    }

    @PostMapping("/tickets/page/create")
    public ResponseEntity<Void> createTicket(
            @RequestParam String viewerId,
            @RequestParam String filmId,
            @RequestParam String sessionDate,
            @RequestParam String sessionTime,
            @RequestParam String seatNumber,
            @RequestParam(required = false) String price
    ) {
        try {
            Long parsedViewerId = Long.valueOf(viewerId);
            Long parsedFilmId = Long.valueOf(filmId);
            LocalDate parsedSessionDate = LocalDate.parse(sessionDate);
            LocalTime parsedSessionTime = LocalTime.parse(sessionTime);
            Double parsedPrice = (price == null || price.isBlank()) ? null : Double.valueOf(price);
            TicketRequest request = new TicketRequest(
                    parsedViewerId,
                    parsedFilmId,
                    parsedSessionDate,
                    parsedSessionTime,
                    seatNumber,
                    parsedPrice
            );
            ticketService.create(request);
            return redirect("/tickets/page");
        } catch (DomainException | IllegalArgumentException | DateTimeException ex) {
            LinkedHashMap<String, Object> data = new LinkedHashMap<>();
            data.put("viewerIdRaw", viewerId);
            data.put("filmIdRaw", filmId);
            data.put("sessionDateRaw", sessionDate);
            data.put("sessionTimeRaw", sessionTime);
            data.put("errorType", ex.getClass().getSimpleName());
            // #region agent log
            DebugProbe.log(
                    "run-1",
                    "H5",
                    "HtmlPageController#createTicket",
                    "HTML ticket create rejected",
                    data
            );
            // #endregion
            return redirectWithError("/tickets/page/create", ex.getMessage());
        }
    }

    private String pageShell(String title, String body) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <title>""" + title + """
                </title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; }
                    nav a { margin-right: 1rem; }
                    table { margin-top: 1rem; border-collapse: collapse; }
                  </style>
                </head>
                <body>
                  <nav>
                    <a href="/">Home</a>
                    <a href="/films/page">Films</a>
                    <a href="/viewers/page">Viewers</a>
                    <a href="/tickets/page">Tickets</a>
                  </nav>
                """ + body + """
                </body>
                </html>
                """;
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(302).location(URI.create(location)).build();
    }

    private ResponseEntity<Void> redirectWithError(String basePath, String message) {
        String safeMessage = message == null ? "Request failed" : message;
        String encodedMessage = URLEncoder.encode(safeMessage, StandardCharsets.UTF_8);
        return redirect(basePath + "?error=" + encodedMessage);
    }

    private String errorBlock(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return "<p style=\"color:#b00020;\">Error: " + escapeHtml(message) + "</p>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
