package io.kontur.eventapi.emdat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmDatFileStatisticResponse {

    private Data data;

    public EmDatPublicFile getEmDatPublicFile() {
        return data.getEmdatPublic();
    }

    public void setData(Data data) {
        this.data = data;
    }

    private static class Data {

        @JsonProperty("emdat_public")
        private EmDatPublicFile emDatPublic;

        public EmDatPublicFile getEmdatPublic() {
            return emDatPublic;
        }

        public void setEmdatPublic(EmDatPublicFile emDatPublic) {
            this.emDatPublic = emDatPublic;
        }
    }
}
