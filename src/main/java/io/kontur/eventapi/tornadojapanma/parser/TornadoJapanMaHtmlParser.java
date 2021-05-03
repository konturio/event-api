package io.kontur.eventapi.tornadojapanma.parser;

import io.kontur.eventapi.tornadojapanma.dto.JefScale;
import io.kontur.eventapi.tornadojapanma.dto.MainDamageSituation;
import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TornadoJapanMaHtmlParser extends HtmlParser {

    @Override
    public String parseUpdatedAt() throws IOException {
        return parseUpdatedAt(1);
    }

    @Override
    public Set<ParsedCase> parseCases() {
        Set<ParsedCase> parsedCases = new HashSet<>();
        try {
            getDocument(BASE_URL + "new/list_new.html").select(".data2_s tr")
                    .forEach(row -> parsedCase(row).ifPresent(parsedCases::add));
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return parsedCases;
    }

    private Optional<ParsedCase> parsedCase(Element row) {
        List<String> values = row.select("td").stream().map(Element::text).collect(Collectors.toList());
        if (values.size() == 13) {
            JefScale jefScale = new JefScale(values.get(3), values.get(4));
            MainDamageSituation damage = new MainDamageSituation(values.get(7), values.get(8), values.get(9), values.get(10));
            ParsedCase parsedCase = ParsedCase.builder()
                    .type(values.get(0))
                    .occurrenceDateTime(values.get(1))
                    .occurrencePlace(values.get(2))
                    .jefScale(jefScale)
                    .damageWidth(values.get(5))
                    .damageLength(values.get(6))
                    .mainDamageSituation(damage)
                    .viewingArea(values.get(11))
                    .remarks(values.get(12))
                    .build();
            return Optional.of(parsedCase);
        }
        return Optional.empty();
    }
}
