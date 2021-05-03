package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {
    private String latitude;
    private String longitude;
    private String prefectures;
    private String municipalities;
    private String address;
}
