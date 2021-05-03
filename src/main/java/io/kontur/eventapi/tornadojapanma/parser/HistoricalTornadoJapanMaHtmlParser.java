package io.kontur.eventapi.tornadojapanma.parser;

import io.kontur.eventapi.tornadojapanma.dto.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HistoricalTornadoJapanMaHtmlParser extends HtmlParser {

    private final List<String> pages = List.of("1961.html", "1971.html", "1981.html", "1991.html", "2001.html", "2011.html");

    @Override
    public String parseUpdatedAt() throws IOException {
        return parseUpdatedAt(2);
    }

    @Override
    public Set<ParsedCase> parseCases() {
        Set<ParsedCase> parsedCases = new HashSet<>();
        for (String page : pages) {
            try {
                getDocument(BASE_URL + "list/" + page).select(".data2_s tr")
                        .forEach(row -> parseHistoricalCase(row).ifPresent(parsedCases::add));
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }
        return parsedCases;
    }

    private Optional<ParsedCase> parseHistoricalCase(Element row) {
        Elements cells = row.select("td");
        if (cells.size() != 12) return Optional.empty();
        List<String> values = cells.stream().map(Element::text).collect(Collectors.toList());
        String detailsLink = cells.get(3).select("a").attr("href");
        try {
            Details details = detailsLink.isBlank() ? null : parseDetailsPage(BASE_URL + detailsLink.substring(3));
            MainDamageSituation damage = new MainDamageSituation(values.get(7), values.get(8), values.get(9), values.get(10));
            ParsedCase parsedCase = ParsedCase.builder()
                    .type(values.get(0))
                    .occurrenceDateTime(values.get(1))
                    .occurrencePlace(values.get(2))
                    .details(details)
                    .fScale(values.get(4))
                    .damageWidth(values.get(5))
                    .damageLength(values.get(6))
                    .mainDamageSituation(damage)
                    .viewingArea(values.get(11))
                    .build();
            return Optional.of(parsedCase);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return Optional.empty();
    }

    private Details parseDetailsPage(String url) throws Exception {
        List<String> values = getDocument(url).select(".data2_s tr td")
                .stream().map(Element::text).collect(Collectors.toList());
        Place occurrencePlace = new Place(values.get(2), values.get(3), values.get(4), values.get(5), values.get(6));
        Place disappearancePlace = new Place(values.get(8), values.get(9), values.get(10), values.get(11), values.get(12));
        InjuredPerson injuredPerson = new InjuredPerson(values.get(27), values.get(28), values.get(29));
        Damage dwellingDamage = new Damage(values.get(30), values.get(31), values.get(32), values.get(33));
        Damage nonResidentialDamage = new Damage(values.get(34), values.get(35), values.get(36), values.get(37));
        DamageSituation damage = new DamageSituation(values.get(26), injuredPerson, dwellingDamage, nonResidentialDamage, values.get(38));
        return Details.builder()
                .type(values.get(0))
                .occurrenceDateTime(values.get(1))
                .occurrencePlace(occurrencePlace)
                .disappearanceDateTime(values.get(7))
                .disappearancePlace(disappearancePlace)
                .fScale(values.get(13))
                .damageWidth(values.get(14))
                .damagedLength(values.get(15))
                .movementDirection(List.of(values.get(16), values.get(17)))
                .movementSpeed(values.get(18))
                .duration(values.get(19))
                .rotationDirection(values.get(20))
                .originDifferentiation(values.get(21))
                .viewingArea(List.of(values.get(22), values.get(23), values.get(24)))
                .positionFromTheOverallScaleDisturbance(values.get(25))
                .damageSituation(damage)
                .features(values.get(39))
                .build();
    }
}
