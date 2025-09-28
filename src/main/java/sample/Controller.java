package sample;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.RiMarkov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
class Controller {

    private static final String COMMENT = "***";
    private static final String START_MARKER = "START";
    private static final String END_MARKER = "END";

    private RiMarkov model;

    @PostConstruct
    void initModel() {
        model = new RiMarkov(5);
        List<String> filenames = List.of("The_Gift_of_the_Magi.txt", "Christmas_Carol.txt");
        long startTime = System.currentTimeMillis();
        try {
            for (String filename : filenames) {
                model.addText(readText(filename));
                readText(filename);
            }
        } catch (IOException | RuntimeException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.info("Time taken to initialize: {}ms", System.currentTimeMillis() - startTime);
    }

    String readText(String inputFilename) throws IOException {
        try (BufferedReader in = createInputStreamReader(inputFilename)) {
            return in.lines()
                    .dropWhile(line -> !(line.startsWith(COMMENT) && line.contains(START_MARKER)))
                    .takeWhile(line -> !(line.startsWith(COMMENT) && line.contains(END_MARKER)))
                    .filter(line -> !line.startsWith(COMMENT))
                    .collect(Collectors.joining(" "));
        }
    }

    BufferedReader createInputStreamReader(String inputFilename) throws IOException {
        return new BufferedReader(new InputStreamReader(
                new ClassPathResource(inputFilename, getClass().getClassLoader()).getInputStream(),
                StandardCharsets.UTF_8));
    }

    @RequestMapping(value = "/")
    ResponseEntity<String> respond() {
        return ResponseEntity.ok(measureResponse());
    }

    String measureResponse() {
        long startTime = System.currentTimeMillis();
        String response = generateResponse();
        log.info("Time taken to respond: {}ms", System.currentTimeMillis() - startTime);
        return response;
    }

    String generateResponse() {
        StringBuilder response = new StringBuilder();
        Arrays.stream(model.generate()).forEach(line -> response.append("<p>").append(line).append("</p>"));
        return response.toString();
    }

}
