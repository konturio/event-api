package io.kontur.eventapi.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestPageMetadata implements Serializable {

	@Serial
	private static final long serialVersionUID = 7302082487456118724L;

	private OffsetDateTime nextAfterValue;

	@Serial
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		nextAfterValue = (OffsetDateTime) in.readObject();
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(nextAfterValue);
	}
}
